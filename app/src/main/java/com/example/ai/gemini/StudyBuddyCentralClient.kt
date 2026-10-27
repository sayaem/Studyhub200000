package com.example.ai.gemini

import com.example.buddy.BuddyAction
import com.example.buddy.BuddyAiResult
import com.example.buddy.BuddyGenerationRequest
import com.example.buddy.StudyBuddyAiProvider
import com.example.buddy.StudyBuddyContextEngine
import com.example.buddy.StudyHubVerifiedFacts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Result chunk for streaming responses.
 */
sealed interface GeminiStreamChunk {
    data class TextDelta(val delta: String, val accumulatedText: String) : GeminiStreamChunk
    data class Complete(val fullText: String, val usage: GeminiUsageMetadata? = null) : GeminiStreamChunk
    data class Error(val error: ClassifiedGeminiError, val partialText: String) : GeminiStreamChunk
}

/**
 * Centralized, production-grade Gemini API client for StudyHub.
 * Implements intelligent multi-key rotation, failover, task routing, model fallback,
 * bounded retries with jitter, request deduplication, and streaming.
 */
class StudyBuddyCentralClient(
    val config: GeminiConfig = GeminiConfig.load(),
    val keyManager: GeminiKeyManager = GeminiKeyManager(config),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.requestTimeoutMs, TimeUnit.MILLISECONDS)
        .build()
) : StudyBuddyAiProvider {

    private val deduplicator = GeminiRequestDeduplicator<BuddyAiResult>()

    /**
     * Primary entry point for generating conversational or artifact responses.
     */
    override suspend fun generateReply(request: BuddyGenerationRequest): BuddyAiResult = withContext(Dispatchers.IO) {
        val prompt = request.userPrompt.trim()
        val idempotencyKey = "${request.conversationId}_${prompt.hashCode()}"

        deduplicator.executeOrJoin(idempotencyKey) {
            val deferred = CompletableDeferred<BuddyAiResult>()
            val result = executeWithFailover(request)
            deferred.complete(result)
            deferred
        }
    }

    /**
     * Executes the API call with multi-key rotation, model fallback, and bounded retries.
     */
    private suspend fun executeWithFailover(request: BuddyGenerationRequest): BuddyAiResult {
        val requestId = UUID.randomUUID().toString().take(8)
        val startTime = System.currentTimeMillis()

        // 1. Check if any keys are configured
        if (keyManager.totalKeyCount == 0) {
            return BuddyAiResult.Error(
                errorMessage = "Gemini API key is missing or not configured. Please add your GEMINI_API_KEY in the Secrets panel in AI Studio.",
                canRetry = false
            )
        }

        // 2. Check if shared project quota cooldown is active
        if (keyManager.isSharedQuotaExhausted()) {
            val waitSecs = ((keyManager.getEarliestRetryWaitMs() ?: 60_000L) / 1000L).coerceAtLeast(1)
            return BuddyAiResult.Error(
                errorMessage = "AI service is temporarily busy (shared project quota limit reached). Please retry in $waitSecs seconds.",
                isQuotaExhausted = true,
                canRetry = true
            )
        }

        // 3. Determine task category and model plan
        val modelPlan = GeminiModelRouter.route(request.userPrompt, config.maxOutputTokens)
        val candidateModels = if (config.enableModelFallback) {
            (listOf(modelPlan.primaryModel) + modelPlan.fallbackModels).distinct()
        } else {
            listOf(modelPlan.primaryModel)
        }

        val attemptedKeys = mutableSetOf<String>()
        var retryCount = 0
        var currentModelIndex = 0
        var lastClassifiedError: ClassifiedGeminiError? = null

        val systemInstruction = StudyBuddyContextEngine.buildSystemInstruction(request.contextEnvelope)
        val payloadJson = buildRequestPayload(request, systemInstruction, modelPlan.temperature, modelPlan.maxOutputTokens)

        while (retryCount <= config.maxRetries && currentModelIndex < candidateModels.size) {
            val activeModel = candidateModels[currentModelIndex]

            // Acquire key
            val managedKey = keyManager.acquireKey(attemptedKeys)
            if (managedKey == null) {
                val allQuarantined = keyManager.areAllKeysQuarantined()
                val lastError = lastClassifiedError

                if (allQuarantined && lastError != null && (lastError.category == GeminiErrorCategory.AUTH_INVALID ||
                            lastError.category == GeminiErrorCategory.PERMISSION_OR_BILLING ||
                            lastError.category == GeminiErrorCategory.BAD_REQUEST)) {
                    return BuddyAiResult.Error(
                        errorMessage = lastError.userFacingMessage,
                        canRetry = lastError.canRetry
                    )
                }

                val waitMs = keyManager.getEarliestRetryWaitMs() ?: 60_000L
                val waitSecs = (waitMs / 1000L).coerceAtLeast(1)

                val errorMessage = if (allQuarantined) {
                    "All configured Gemini API keys failed authentication. Please check your API keys in the Secrets panel."
                } else {
                    "All configured AI keys are temporarily busy. Please wait $waitSecs seconds before retrying."
                }

                GeminiObservability.recordMetrics(
                    GeminiRequestMetrics(
                        requestId = requestId,
                        taskCategory = modelPlan.taskCategory,
                        model = activeModel,
                        keyOpaqueId = "NoneAvailable",
                        latencyMs = System.currentTimeMillis() - startTime,
                        success = false,
                        errorCategory = if (allQuarantined) GeminiErrorCategory.AUTH_INVALID else GeminiErrorCategory.ALL_KEYS_EXHAUSTED,
                        retryCount = retryCount
                    )
                )
                return BuddyAiResult.Error(
                    errorMessage = errorMessage,
                    isQuotaExhausted = !allQuarantined,
                    canRetry = !allQuarantined
                )
            }

            val sanitizedRawKey = GeminiConfig.sanitizeKey(managedKey.key)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent?key=$sanitizedRawKey"
            val requestBody = payloadJson.toRequestBody("application/json; charset=utf-8".toMediaType())
            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val callStartTime = System.currentTimeMillis()
            var httpStatusCode: Int? = null
            var responseBody: String? = null
            var retryAfterHeader: String? = null
            var caughtException: Throwable? = null

            try {
                httpClient.newCall(httpRequest).execute().use { response ->
                    httpStatusCode = response.code
                    responseBody = response.body?.string()
                    retryAfterHeader = response.header("Retry-After")
                }
            } catch (e: Throwable) {
                caughtException = e
            }

            val callLatency = System.currentTimeMillis() - callStartTime

            if (httpStatusCode == 200 && !responseBody.isNullOrBlank()) {
                // Success!
                keyManager.recordSuccess(managedKey)

                val parsed = parseSuccessResponse(responseBody!!)
                if (parsed != null) {
                    val suggestedActions = StudyBuddyContextEngine.suggestActions(
                        intent = request.contextEnvelope.queryIntent,
                        facts = request.contextEnvelope.verifiedFacts
                    )

                    GeminiObservability.recordMetrics(
                        GeminiRequestMetrics(
                            requestId = requestId,
                            taskCategory = modelPlan.taskCategory,
                            model = activeModel,
                            keyOpaqueId = managedKey.opaqueId,
                            latencyMs = System.currentTimeMillis() - startTime,
                            success = true,
                            retryCount = retryCount,
                            usage = parsed.second
                        )
                    )

                    return BuddyAiResult.Success(
                        responseText = parsed.first,
                        suggestedActions = suggestedActions
                    )
                } else {
                    // Safety block or malformed response
                    val safetyError = parseSafetyRefusal(responseBody!!)
                    if (safetyError != null) {
                        return BuddyAiResult.Error(
                            errorMessage = safetyError,
                            canRetry = false
                        )
                    }
                }
            }

            // Failure handling
            val classified = GeminiErrorClassifier.classify(
                httpStatusCode = httpStatusCode,
                responseBody = responseBody,
                exception = caughtException,
                retryAfterHeader = retryAfterHeader
            )
            lastClassifiedError = classified

            keyManager.recordFailure(
                key = managedKey,
                errorCategory = classified.category,
                retryAfterMs = classified.retryAfterMs
            )

            // Decision on rotation, fallback, or retry
            when (classified.category) {
                GeminiErrorCategory.AUTH_INVALID -> {
                    // Key is invalid -> quarantine and try next key without incrementing retry count
                    attemptedKeys.add(managedKey.key)
                    GeminiObservability.recordKeyFailover(managedKey.opaqueId, "NextKey", classified.category)
                    continue
                }

                GeminiErrorCategory.RATE_LIMIT -> {
                    // Key rate limited -> if more keys exist, rotate key
                    attemptedKeys.add(managedKey.key)
                    if (config.enableKeyRotation && attemptedKeys.size < keyManager.totalKeyCount) {
                        GeminiObservability.recordKeyFailover(managedKey.opaqueId, "NextKey", classified.category)
                        continue
                    } else {
                        // All keys rate limited
                        break
                    }
                }

                GeminiErrorCategory.SHARED_QUOTA_EXHAUSTED -> {
                    // Shared account quota exhausted -> do not cycle other keys
                    GeminiObservability.recordQuotaCooldown(managedKey.opaqueId, classified.retryAfterMs ?: config.keyCooldownMs, true)
                    break
                }

                GeminiErrorCategory.MODEL_NOT_FOUND -> {
                    // Model deprecated or unavailable -> switch to fallback model
                    if (currentModelIndex < candidateModels.size - 1) {
                        val nextModel = candidateModels[currentModelIndex + 1]
                        GeminiObservability.recordModelFallback(activeModel, nextModel, "HTTP 404")
                        currentModelIndex++
                        attemptedKeys.clear()
                        continue
                    } else {
                        break
                    }
                }

                GeminiErrorCategory.BAD_REQUEST,
                GeminiErrorCategory.SAFETY_REFUSAL -> {
                    // Non-retryable user request or safety refusal
                    break
                }

                GeminiErrorCategory.SERVER_TRANSIENT,
                GeminiErrorCategory.REQUEST_TIMEOUT,
                GeminiErrorCategory.NETWORK_INTERRUPT -> {
                    // Transient failure: apply jittered backoff and retry
                    retryCount++
                    if (retryCount <= config.maxRetries) {
                        val backoff = (500L * (1 shl retryCount)) + Random.nextLong(100L, 500L)
                        delay(backoff)
                        // If multiple retries failed on this key, rotate key as well
                        if (retryCount >= 2 && config.enableKeyRotation) {
                            attemptedKeys.add(managedKey.key)
                        }
                    }
                }

                else -> {
                    retryCount++
                }
            }
        }

        val allQuarantined = keyManager.areAllKeysQuarantined()
        val finalError = if (allQuarantined) {
            lastClassifiedError ?: ClassifiedGeminiError(
                category = GeminiErrorCategory.AUTH_INVALID,
                httpStatusCode = 401,
                internalMessage = "All keys quarantined",
                userFacingMessage = "The AI service authentication failed. Please verify your Gemini API key in the Secrets panel.",
                canRetry = false
            )
        } else if (lastClassifiedError?.category == GeminiErrorCategory.AUTH_INVALID) {
            ClassifiedGeminiError(
                category = GeminiErrorCategory.SERVER_TRANSIENT,
                httpStatusCode = null,
                internalMessage = "Transient key cooldown",
                userFacingMessage = "AI service is temporarily busy. Please retry.",
                canRetry = true
            )
        } else {
            lastClassifiedError ?: ClassifiedGeminiError(
                category = GeminiErrorCategory.SERVER_TRANSIENT,
                httpStatusCode = null,
                internalMessage = "Exhausted retries",
                userFacingMessage = "AI service is temporarily busy. Please retry.",
                canRetry = true
            )
        }

        GeminiObservability.recordMetrics(
            GeminiRequestMetrics(
                requestId = requestId,
                taskCategory = modelPlan.taskCategory,
                model = candidateModels.getOrElse(currentModelIndex) { modelPlan.primaryModel },
                keyOpaqueId = "FailoverExhausted",
                latencyMs = System.currentTimeMillis() - startTime,
                success = false,
                errorCategory = finalError.category,
                retryCount = retryCount
            )
        )

        return BuddyAiResult.Error(
            errorMessage = finalError.userFacingMessage,
            isQuotaExhausted = (finalError.category == GeminiErrorCategory.RATE_LIMIT || finalError.category == GeminiErrorCategory.SHARED_QUOTA_EXHAUSTED),
            isNetworkError = (finalError.category == GeminiErrorCategory.NETWORK_INTERRUPT),
            canRetry = finalError.canRetry
        )
    }

    /**
     * Real-time streaming API emitting text deltas and final response chunks.
     * Guaranteed never to blindly restart across keys once tokens have reached the caller.
     */
    fun streamReply(request: BuddyGenerationRequest): Flow<GeminiStreamChunk> = flow {
        if (keyManager.totalKeyCount == 0) {
            emit(
                GeminiStreamChunk.Error(
                    error = ClassifiedGeminiError(
                        category = GeminiErrorCategory.AUTH_INVALID,
                        httpStatusCode = null,
                        internalMessage = "No keys configured",
                        userFacingMessage = "Gemini API key is missing. Please configure GEMINI_API_KEY.",
                        canRetry = false
                    ),
                    partialText = ""
                )
            )
            return@flow
        }

        val modelPlan = GeminiModelRouter.route(request.userPrompt, config.maxOutputTokens)
        val managedKey = keyManager.acquireKey()
        if (managedKey == null) {
            emit(
                GeminiStreamChunk.Error(
                    error = ClassifiedGeminiError(
                        category = GeminiErrorCategory.ALL_KEYS_EXHAUSTED,
                        httpStatusCode = 429,
                        internalMessage = "All keys busy",
                        userFacingMessage = "AI service is temporarily busy. Please wait a moment.",
                        canRetry = true
                    ),
                    partialText = ""
                )
            )
            return@flow
        }

        val systemInstruction = StudyBuddyContextEngine.buildSystemInstruction(request.contextEnvelope)
        val payload = buildRequestPayload(request, systemInstruction, modelPlan.temperature, modelPlan.maxOutputTokens)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${modelPlan.primaryModel}:streamGenerateContent?alt=sse&key=${managedKey.key}"

        val httpRequest = Request.Builder()
            .url(url)
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val accumulated = StringBuilder()
        var hasEmittedTokens = false

        try {
            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val classified = GeminiErrorClassifier.classify(response.code, response.body?.string(), null)
                    emit(GeminiStreamChunk.Error(classified, accumulated.toString()))
                    return@flow
                }

                val reader = BufferedReader(InputStreamReader(response.body?.byteStream() ?: return@flow))
                var line: String? = reader.readLine()

                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val dataJson = line.removePrefix("data: ").trim()
                        if (dataJson.isNotBlank() && dataJson != "[DONE]") {
                            val chunkText = extractTextFromSseChunk(dataJson)
                            if (chunkText.isNotEmpty()) {
                                accumulated.append(chunkText)
                                hasEmittedTokens = true
                                emit(GeminiStreamChunk.TextDelta(chunkText, accumulated.toString()))
                            }
                        }
                    }
                    line = reader.readLine()
                }

                keyManager.recordSuccess(managedKey)
                emit(GeminiStreamChunk.Complete(accumulated.toString(), null))
            }
        } catch (e: Throwable) {
            val classified = GeminiErrorClassifier.classify(null, null, e)
            keyManager.recordFailure(managedKey, classified.category)
            emit(GeminiStreamChunk.Error(classified, accumulated.toString()))
        }
    }

    private fun extractTextFromSseChunk(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val candidates = json.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val first = candidates.getJSONObject(0)
            val parts = first.optJSONObject("content")?.optJSONArray("parts") ?: return ""
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val t = parts.getJSONObject(i).optString("text")
                if (t.isNotEmpty()) sb.append(t)
            }
            sb.toString()
        } catch (_: Throwable) {
            ""
        }
    }

    internal fun buildRequestPayload(
        request: BuddyGenerationRequest,
        systemInstruction: String,
        temperature: Double,
        maxOutputTokens: Int
    ): String {
        val root = JSONObject()

        // System Instruction
        root.put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemInstruction) })
            })
        })

        // Conversation History (Bounded Context Budget: Keep last 6 relevant turns)
        val contentsArray = JSONArray()
        val recentTurns = request.recentTurns.takeLast(6)
        for (turn in recentTurns) {
            val role = if (turn.first.equals("USER", ignoreCase = true)) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", turn.second) })
                })
            })
        }

        // Current Prompt with optional attached files (images, PDFs, text documents)
        val userParts = JSONArray().apply {
            put(JSONObject().apply { put("text", request.userPrompt) })
        }
        for (file in request.attachedFiles) {
            if (!file.base64Content.isNullOrBlank()) {
                userParts.put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", file.mimeType)
                        put("data", file.base64Content)
                    })
                })
            } else if (!file.textContent.isNullOrBlank()) {
                val docText = "[Attached Document: ${file.fileName}]\n${file.textContent.take(12000)}"
                userParts.put(JSONObject().apply { put("text", docText) })
            }
        }
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", userParts)
        })

        root.put("contents", contentsArray)

        // Generation Config
        root.put("generationConfig", JSONObject().apply {
            put("temperature", temperature)
            put("topP", 0.95)
            put("topK", 40)
            put("maxOutputTokens", maxOutputTokens)
        })

        return root.toString()
    }

    private fun parseSuccessResponse(responseBody: String): Pair<String, GeminiUsageMetadata?>? {
        return try {
            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val first = candidates.getJSONObject(0)
            val parts = first.optJSONObject("content")?.optJSONArray("parts") ?: return null

            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val text = parts.getJSONObject(i).optString("text")
                if (text.isNotBlank()) {
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(text)
                }
            }

            val usageObj = json.optJSONObject("usageMetadata")
            val usage = usageObj?.let {
                GeminiUsageMetadata(
                    promptTokens = it.optInt("promptTokenCount", 0),
                    candidatesTokens = it.optInt("candidatesTokenCount", 0),
                    totalTokens = it.optInt("totalTokenCount", 0)
                )
            }

            val fullText = sb.toString().trim()
            if (fullText.isNotBlank()) Pair(fullText, usage) else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseSafetyRefusal(responseBody: String): String? {
        return try {
            val json = JSONObject(responseBody)
            val promptFeedback = json.optJSONObject("promptFeedback")
            val blockReason = promptFeedback?.optString("blockReason")
            if (!blockReason.isNullOrBlank()) {
                "Response was restricted due to safety guidelines ($blockReason). Please rephrase your query."
            } else null
        } catch (_: Throwable) {
            null
        }
    }
}
