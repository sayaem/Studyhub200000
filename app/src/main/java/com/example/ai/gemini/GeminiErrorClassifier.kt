package com.example.ai.gemini

import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class GeminiErrorCategory {
    BAD_REQUEST,              // HTTP 400: Invalid payload/parameters. Do not retry/rotate.
    AUTH_INVALID,             // HTTP 401: Invalid API key. Quarantine key, rotate.
    PERMISSION_OR_BILLING,    // HTTP 403: Policy, billing, or permission issue.
    MODEL_NOT_FOUND,          // HTTP 404: Endpoint or model unavailable. Trigger model fallback.
    REQUEST_TIMEOUT,          // HTTP 408 or client timeout: Transient, bounded retry.
    RATE_LIMIT,               // HTTP 429: Key-level rate limit. Cooldown + failover.
    SHARED_QUOTA_EXHAUSTED,   // HTTP 429 with project/account-wide quota exhaustion.
    SERVER_TRANSIENT,         // HTTP 500, 502, 503, 504: Bounded retry with exponential backoff.
    NETWORK_INTERRUPT,        // UnknownHostException, Connection reset. Bounded retry.
    SAFETY_REFUSAL,           // Content blocked by safety guidelines. Never rotate.
    MALFORMED_OUTPUT,         // Empty or invalid model response.
    ALL_KEYS_EXHAUSTED        // All available keys in cooldown or quarantined.
}

data class ClassifiedGeminiError(
    val category: GeminiErrorCategory,
    val httpStatusCode: Int?,
    val internalMessage: String,
    val userFacingMessage: String,
    val retryAfterMs: Long? = null,
    val isKeySpecific: Boolean = false,
    val isModelSpecific: Boolean = false,
    val canRetry: Boolean = false
)

object GeminiErrorClassifier {

    /**
     * Defensively classifies HTTP responses and exceptions into structured categories.
     * Sanitizes internal details so no raw keys or server URLs are leaked to users.
     */
    fun classify(
        httpStatusCode: Int?,
        responseBody: String?,
        exception: Throwable?,
        retryAfterHeader: String? = null
    ): ClassifiedGeminiError {
        val retryAfterMs = parseRetryAfter(retryAfterHeader)

        if (exception != null) {
            return when (exception) {
                is SocketTimeoutException -> ClassifiedGeminiError(
                    category = GeminiErrorCategory.REQUEST_TIMEOUT,
                    httpStatusCode = 408,
                    internalMessage = "Socket timeout: ${exception.message}",
                    userFacingMessage = "Request timed out while waiting for Study Buddy. Check your connection and retry.",
                    canRetry = true
                )
                is UnknownHostException -> ClassifiedGeminiError(
                    category = GeminiErrorCategory.NETWORK_INTERRUPT,
                    httpStatusCode = null,
                    internalMessage = "Host resolution failed: ${exception.message}",
                    userFacingMessage = "Unable to connect to Gemini servers. Please check your internet connection.",
                    canRetry = true
                )
                is IOException -> ClassifiedGeminiError(
                    category = GeminiErrorCategory.NETWORK_INTERRUPT,
                    httpStatusCode = null,
                    internalMessage = "Network I/O failure: ${exception.message}",
                    userFacingMessage = "A temporary network issue occurred. Please retry.",
                    canRetry = true
                )
                else -> ClassifiedGeminiError(
                    category = GeminiErrorCategory.SERVER_TRANSIENT,
                    httpStatusCode = null,
                    internalMessage = "Unexpected exception: ${exception.javaClass.simpleName}: ${exception.message}",
                    userFacingMessage = "A temporary service issue occurred. Please try again.",
                    canRetry = true
                )
            }
        }

        val jsonError = try {
            if (!responseBody.isNullOrBlank()) {
                JSONObject(responseBody).optJSONObject("error")
            } else null
        } catch (_: Throwable) {
            null
        }

        val parsedMessage = jsonError?.optString("message") ?: ""
        val parsedStatus = jsonError?.optString("status") ?: ""

        return when (httpStatusCode) {
            400 -> ClassifiedGeminiError(
                category = GeminiErrorCategory.BAD_REQUEST,
                httpStatusCode = 400,
                internalMessage = "Bad Request (400): $parsedMessage",
                userFacingMessage = "Your request is too large or formatted incorrectly. Please shorten or simplify your input.",
                canRetry = false
            )
            401 -> ClassifiedGeminiError(
                category = GeminiErrorCategory.AUTH_INVALID,
                httpStatusCode = 401,
                internalMessage = "Unauthorized (401): $parsedMessage",
                userFacingMessage = "The AI service authentication failed. Please verify your Gemini API key in the Secrets panel.",
                isKeySpecific = true,
                canRetry = false
            )
            403 -> {
                val isLeakedKey = parsedMessage.contains("API_KEY_INVALID", ignoreCase = true) ||
                        parsedMessage.contains("API key not valid", ignoreCase = true)
                val isBilling = parsedMessage.contains("BILLING_DISABLED", ignoreCase = true) ||
                        parsedMessage.contains("billing", ignoreCase = true)

                ClassifiedGeminiError(
                    category = if (isLeakedKey) GeminiErrorCategory.AUTH_INVALID else GeminiErrorCategory.PERMISSION_OR_BILLING,
                    httpStatusCode = 403,
                    internalMessage = "Forbidden (403): $parsedMessage",
                    userFacingMessage = if (isBilling) {
                        "AI service billing or project configuration requires review."
                    } else if (isLeakedKey) {
                        "API key is invalid or lacks necessary permissions."
                    } else {
                        "Permission denied by AI provider. Please check project configuration."
                    },
                    isKeySpecific = isLeakedKey,
                    canRetry = false
                )
            }
            404 -> ClassifiedGeminiError(
                category = GeminiErrorCategory.MODEL_NOT_FOUND,
                httpStatusCode = 404,
                internalMessage = "Model or endpoint not found (404): $parsedMessage",
                userFacingMessage = "The requested AI model is unavailable. Switching to a compatible fallback model...",
                isModelSpecific = true,
                canRetry = true
            )
            408 -> ClassifiedGeminiError(
                category = GeminiErrorCategory.REQUEST_TIMEOUT,
                httpStatusCode = 408,
                internalMessage = "Request timeout (408): $parsedMessage",
                userFacingMessage = "The AI request took too long to complete. Please retry.",
                canRetry = true
            )
            429 -> {
                val isProjectQuota = parsedMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) &&
                        (parsedMessage.contains("quota", ignoreCase = true) || parsedMessage.contains("Project", ignoreCase = true))
                ClassifiedGeminiError(
                    category = if (isProjectQuota) GeminiErrorCategory.SHARED_QUOTA_EXHAUSTED else GeminiErrorCategory.RATE_LIMIT,
                    httpStatusCode = 429,
                    internalMessage = "Rate limit/Quota (429): status=$parsedStatus, msg=$parsedMessage",
                    userFacingMessage = "AI service is temporarily busy (rate limit reached). Please wait a moment before trying again.",
                    retryAfterMs = retryAfterMs ?: 60_000L,
                    isKeySpecific = !isProjectQuota,
                    canRetry = true
                )
            }
            500, 502, 503, 504 -> ClassifiedGeminiError(
                category = GeminiErrorCategory.SERVER_TRANSIENT,
                httpStatusCode = httpStatusCode,
                internalMessage = "Server error ($httpStatusCode): $parsedMessage",
                userFacingMessage = "AI service is temporarily busy or undergoing maintenance. Retrying momentarily...",
                retryAfterMs = retryAfterMs,
                canRetry = true
            )
            else -> ClassifiedGeminiError(
                category = GeminiErrorCategory.SERVER_TRANSIENT,
                httpStatusCode = httpStatusCode,
                internalMessage = "HTTP $httpStatusCode: $parsedMessage",
                userFacingMessage = "Request failed. Please retry.",
                canRetry = true
            )
        }
    }

    private fun parseRetryAfter(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        return try {
            val seconds = header.trim().toLongOrNull()
            if (seconds != null) {
                seconds * 1000L
            } else null
        } catch (_: Throwable) {
            null
        }
    }
}
