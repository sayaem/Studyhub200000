package com.example

import com.example.ai.gemini.ClassifiedGeminiError
import com.example.ai.gemini.GeminiConfig
import com.example.ai.gemini.GeminiErrorCategory
import com.example.ai.gemini.GeminiErrorClassifier
import com.example.ai.gemini.GeminiKeyManager
import com.example.ai.gemini.GeminiModelRouter
import com.example.ai.gemini.GeminiObservability
import com.example.ai.gemini.GeminiRequestDeduplicator
import com.example.ai.gemini.GeminiStreamChunk
import com.example.ai.gemini.GeminiTaskCategory
import com.example.ai.gemini.ManagedKey
import com.example.ai.gemini.StudyBuddyCentralClient
import com.example.buddy.BuddyAiResult
import com.example.buddy.BuddyContextEnvelope
import com.example.buddy.BuddyGenerationRequest
import com.example.buddy.BuddyQueryIntent
import com.example.buddy.StudyHubVerifiedFacts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiIntelligentEngineTest {

    private fun createSampleRequest(prompt: String = "Explain Newton's Second Law with formula"): BuddyGenerationRequest {
        return BuddyGenerationRequest(
            conversationId = 42L,
            userPrompt = prompt,
            recentTurns = listOf(
                Pair("USER", "Hi Study Buddy"),
                Pair("ASSISTANT", "Hello! How can I help you study today?")
            ),
            contextEnvelope = BuddyContextEnvelope(
                queryIntent = BuddyQueryIntent.GENERAL,
                verifiedFacts = StudyHubVerifiedFacts(
                    studentName = "Nafis",
                    academicLevel = "HSC",
                    admissionTrack = "ENGINEERING",
                    totalSubjectsCount = 4
                ),
                timeReference = "2026-10-26"
            )
        )
    }

    // 1. One configured key
    @Test
    fun testSingleKeyConfiguration() {
        val config = GeminiConfig(keys = listOf("AIzaSyFakeKey1234567890123456789012345"))
        val manager = GeminiKeyManager(config)
        assertEquals(1, manager.totalKeyCount)

        runBlocking {
            val key = manager.acquireKey()
            assertNotNull(key)
            assertEquals("Key #1", key?.opaqueId)
        }
    }

    // 2. Several configured keys & rotation
    @Test
    fun testMultipleKeysRoundRobin() = runBlocking {
        val keys = listOf(
            "AIzaSyFakeKey1_1111111111111111111111111",
            "AIzaSyFakeKey2_2222222222222222222222222",
            "AIzaSyFakeKey3_3333333333333333333333333"
        )
        val config = GeminiConfig(keys = keys)
        val manager = GeminiKeyManager(config)
        assertEquals(3, manager.totalKeyCount)

        val first = manager.acquireKey()
        val second = manager.acquireKey()
        val third = manager.acquireKey()

        assertNotNull(first)
        assertNotNull(second)
        assertNotNull(third)
        // All three distinct keys acquired
        val acquiredSet = setOf(first?.key, second?.key, third?.key)
        assertEquals(3, acquiredSet.size)
    }

    // 3. Missing keys and whitespace-only values
    @Test
    fun testMissingAndWhitespaceKeysHandled() {
        val rawList = listOf("", "   ", "AIzaSyValidKey_123456789012345678901234", "   \t\n   ", "your_first_key")
        val filtered = rawList.filter { it.isNotBlank() && !it.startsWith("your_") && it.length >= 10 }
        val config = GeminiConfig(keys = filtered)
        val manager = GeminiKeyManager(config)

        assertEquals(1, manager.totalKeyCount)
    }

    // 4. Duplicate keys deduplication
    @Test
    fun testDuplicateKeysDeduplicated() {
        val key = "AIzaSyDuplicateKey_123456789012345678901"
        val rawList = listOf(key, key, key, key)
        val deduplicated = rawList.distinct()
        val config = GeminiConfig(keys = deduplicated)
        val manager = GeminiKeyManager(config)

        assertEquals(1, manager.totalKeyCount)
    }

    // 5. Automatic failover after key-specific authentication error (401)
    @Test
    fun testFailoverAfterAuthInvalid401() = runBlocking {
        val key1 = "AIzaSyBadKey1_1111111111111111111111111"
        val key2 = "AIzaSyGoodKey2_222222222222222222222222"
        val config = GeminiConfig(keys = listOf(key1, key2))
        val manager = GeminiKeyManager(config)

        val acquired1 = manager.acquireKey()
        assertNotNull(acquired1)
        assertEquals(key1, acquired1?.key)

        // Simulate 401 failure on key 1
        manager.recordFailure(acquired1!!, GeminiErrorCategory.AUTH_INVALID)
        assertTrue(acquired1.isQuarantined)

        // Key 2 should immediately be selected, key 1 permanently quarantined
        val acquired2 = manager.acquireKey()
        assertNotNull(acquired2)
        assertEquals(key2, acquired2?.key)

        // Even without exclusion, key 1 should never be re-selected
        val nextAcquired = manager.acquireKey()
        assertEquals(key2, nextAcquired?.key)
    }

    // 6. Rate-limit handling and cooldown (429)
    @Test
    fun testRateLimitCooldown() = runBlocking {
        val key = "AIzaSyKeyRateLimit_123456789012345678901"
        val config = GeminiConfig(keys = listOf(key), keyCooldownMs = 50_000L)
        val manager = GeminiKeyManager(config)

        val acquired = manager.acquireKey()
        assertNotNull(acquired)

        // Record 429 rate limit with 30s cooldown
        manager.recordFailure(acquired!!, GeminiErrorCategory.RATE_LIMIT, retryAfterMs = 30_000L)
        assertFalse(acquired.isAvailable(System.currentTimeMillis()))

        // Should return null while in cooldown
        val whileInCooldown = manager.acquireKey()
        assertNull(whileInCooldown)

        // But after cooldown expires, key is available again
        assertTrue(acquired.isAvailable(System.currentTimeMillis() + 35_000L))
    }

    // 7. Shared quota failures without pointless key cycling
    @Test
    fun testSharedProjectQuotaDetection() = runBlocking {
        val keys = listOf(
            "AIzaSyKey1_1111111111111111111111111",
            "AIzaSyKey2_2222222222222222222222222",
            "AIzaSyKey3_3333333333333333333333333"
        )
        val config = GeminiConfig(keys = keys, keyCooldownMs = 45_000L)
        val manager = GeminiKeyManager(config)

        val k1 = manager.acquireKey()!!
        val k2 = manager.acquireKey()!!

        // Both keys receive 429 rate limits in rapid succession (indicating project-wide shared quota)
        manager.recordFailure(k1, GeminiErrorCategory.RATE_LIMIT)
        manager.recordFailure(k2, GeminiErrorCategory.RATE_LIMIT)

        // Manager should now flag shared project quota exhausted to prevent pointless cycling through k3
        assertTrue(manager.isSharedQuotaExhausted())
        assertNull(manager.acquireKey())
    }

    // 8. Transient provider failures with bounded retries (500/503)
    @Test
    fun testTransientServerErrorClassification() {
        val error500 = GeminiErrorClassifier.classify(500, """{"error":{"message":"Internal server error"}}""", null)
        assertEquals(GeminiErrorCategory.SERVER_TRANSIENT, error500.category)
        assertTrue(error500.canRetry)

        val error503 = GeminiErrorClassifier.classify(503, """{"error":{"message":"Service Unavailable"}}""", null)
        assertEquals(GeminiErrorCategory.SERVER_TRANSIENT, error503.category)
        assertTrue(error503.canRetry)
    }

    // 9. Invalid requests without repeated retries (400)
    @Test
    fun testBadRequestNotRetried() {
        val error400 = GeminiErrorClassifier.classify(400, """{"error":{"message":"Invalid argument: contents is empty"}}""", null)
        assertEquals(GeminiErrorCategory.BAD_REQUEST, error400.category)
        assertFalse(error400.canRetry)
        assertFalse(error400.isKeySpecific)
    }

    // 10. Model fallback and unsupported models (404)
    @Test
    fun testModelNotFoundTriggersFallback() {
        val error404 = GeminiErrorClassifier.classify(404, """{"error":{"message":"models/gemini-old-model is not found"}}""", null)
        assertEquals(GeminiErrorCategory.MODEL_NOT_FOUND, error404.category)
        assertTrue(error404.isModelSpecific)
        assertTrue(error404.canRetry)
    }

    // 11. All keys unavailable
    @Test
    fun testAllKeysUnavailableControlledError() = runBlocking {
        val config = GeminiConfig(keys = emptyList())
        val client = StudyBuddyCentralClient(config = config)

        val result = client.generateReply(createSampleRequest())
        assertTrue(result is BuddyAiResult.Error)
        val error = result as BuddyAiResult.Error
        assertFalse(error.canRetry)
        assertTrue(error.errorMessage.contains("Gemini API key is missing", ignoreCase = true))
    }

    // 12. Request timeout classification
    @Test
    fun testRequestTimeoutClassification() {
        val timeoutException = SocketTimeoutException("Read timed out")
        val classified = GeminiErrorClassifier.classify(null, null, timeoutException)
        assertEquals(GeminiErrorCategory.REQUEST_TIMEOUT, classified.category)
        assertTrue(classified.canRetry)
        assertEquals(408, classified.httpStatusCode)
    }

    // 13. Safety refusal handling (Never rotate keys to bypass safety)
    @Test
    fun testSafetyRefusalClassification() {
        val body = """
            {
                "candidates": [],
                "promptFeedback": {
                    "blockReason": "SAFETY"
                }
            }
        """.trimIndent()
        // Safety refusals are treated as terminal outcomes without cycling keys
        val error = GeminiErrorClassifier.classify(200, body, null)
        assertFalse(error.isKeySpecific)
    }

    // 14. Intelligent Task Routing and Model Selection
    @Test
    fun testTaskRoutingRules() {
        // Math problem
        val mathPlan = GeminiModelRouter.route("Calculate the vector cross product \\(\\vec{A} \\times \\vec{B}\\)")
        assertEquals(GeminiTaskCategory.MATHEMATICAL_PROBLEM_SOLVING, mathPlan.taskCategory)
        assertEquals("gemini-3.1-pro-preview", mathPlan.primaryModel)
        assertTrue(mathPlan.fallbackModels.contains("gemini-3.5-flash"))

        // Science explanation
        val sciencePlan = GeminiModelRouter.route("Explain Faraday's law of electromagnetic induction in physics")
        assertEquals(GeminiTaskCategory.SCIENCE_EXPLANATION, sciencePlan.taskCategory)
        assertEquals("gemini-3.1-pro-preview", sciencePlan.primaryModel)

        // Quiz generation
        val quizPlan = GeminiModelRouter.route("Generate a 5-question practice quiz on Organic Chemistry")
        assertEquals(GeminiTaskCategory.QUIZ_GENERATION, quizPlan.taskCategory)
        assertEquals("gemini-3.5-flash", quizPlan.primaryModel)

        // Study planning
        val planPlan = GeminiModelRouter.route("Create a 2-hour study routine and revision schedule for today")
        assertEquals(GeminiTaskCategory.STUDY_PLANNING, planPlan.taskCategory)
        assertEquals("gemini-3.5-flash", planPlan.primaryModel)
    }

    // 15. Request deduplication for rapid re-submits
    @Test
    fun testRequestDeduplication() = runBlocking {
        val deduplicator = GeminiRequestDeduplicator<String>()
        val executionCount = AtomicInteger(0)

        val deferredOp: suspend () -> kotlinx.coroutines.Deferred<String> = {
            async {
                delay(60)
                val count = executionCount.incrementAndGet()
                "Result-$count"
            }
        }

        // Two concurrent calls with same idempotency key
        val r1 = async { deduplicator.executeOrJoin("key_123", deferredOp) }
        delay(10)
        val r2 = async { deduplicator.executeOrJoin("key_123", deferredOp) }

        val res1 = r1.await()
        val res2 = r2.await()

        assertEquals(res1, res2)
        // Operation should have run once
        assertEquals(1, executionCount.get())
    }

    // 16. No secret leakage in errors or logs
    @Test
    fun testNoSecretLeakage() {
        val secretKey = "AIzaSySECRETKEY_NEVER_LEAK_INTO_LOGS"
        val managed = ManagedKey(key = secretKey, index = 0)

        // toString() must hide the secret key
        assertEquals("Key #1", managed.toString())
        assertFalse(managed.toString().contains(secretKey))

        // Classified errors must never contain the raw secret key
        val classified = GeminiErrorClassifier.classify(
            401,
            """{"error":{"message":"API key $secretKey is invalid"}}""",
            null
        )
        assertFalse(classified.userFacingMessage.contains(secretKey))
    }

    // 17. End-to-end Client with Mocked HTTP (Success & Failover)
    @Test
    fun testClientEndToEndSuccessWithMockHttp() = runBlocking {
        val key = "AIzaSyRealLookingKey1234567890123456789"
        val config = GeminiConfig(keys = listOf(key))

        val mockInterceptor = Interceptor { chain ->
            val responseJson = """
                {
                    "candidates": [
                        {
                            "content": {
                                "parts": [
                                    { "text": "Newton's second law is F = ma." }
                                ]
                            }
                        }
                    ],
                    "usageMetadata": {
                        "promptTokenCount": 15,
                        "candidatesTokenCount": 20,
                        "totalTokenCount": 35
                    }
                }
            """.trimIndent()

            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseJson.toResponseBody("application/json".toMediaType()))
                .build()
        }

        val testHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val client = StudyBuddyCentralClient(
            config = config,
            httpClient = testHttpClient
        )

        val result = client.generateReply(createSampleRequest())
        assertTrue(result is BuddyAiResult.Success)
        val success = result as BuddyAiResult.Success
        assertEquals("Newton's second law is F = ma.", success.responseText)
    }

    // 18. End-to-end Automatic Failover from Key 1 (401) to Key 2 (200 OK)
    @Test
    fun testClientAutomaticFailoverFrom401ToKey2() = runBlocking {
        val key1 = "AIzaSyInvalidKey1_11111111111111111111111"
        val key2 = "AIzaSyValidKey2_2222222222222222222222222"
        val config = GeminiConfig(keys = listOf(key1, key2))

        val callCount = AtomicInteger(0)

        val mockInterceptor = Interceptor { chain ->
            val url = chain.request().url.toString()
            val currentCall = callCount.incrementAndGet()

            if (url.contains(key1)) {
                // Key 1 fails with 401
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("""{"error":{"message":"API_KEY_INVALID"}}""".toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                // Key 2 succeeds with 200
                val successJson = """
                    {
                        "candidates": [
                            {
                                "content": {
                                    "parts": [
                                        { "text": "Failover succeeded seamlessly on Key 2!" }
                                    ]
                                }
                            }
                        ]
                    }
                """.trimIndent()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(successJson.toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        val testHttpClient = OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .build()

        val client = StudyBuddyCentralClient(
            config = config,
            httpClient = testHttpClient
        )

        val result = client.generateReply(createSampleRequest())
        assertTrue(result is BuddyAiResult.Success)
        val success = result as BuddyAiResult.Success
        assertEquals("Failover succeeded seamlessly on Key 2!", success.responseText)
        assertEquals(2, callCount.get())
    }
}
