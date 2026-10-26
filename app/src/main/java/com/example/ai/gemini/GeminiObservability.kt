package com.example.ai.gemini

import android.util.Log

data class GeminiUsageMetadata(
    val promptTokens: Int = 0,
    val candidatesTokens: Int = 0,
    val totalTokens: Int = 0
)

data class GeminiRequestMetrics(
    val requestId: String,
    val taskCategory: GeminiTaskCategory,
    val model: String,
    val keyOpaqueId: String,
    val latencyMs: Long,
    val success: Boolean,
    val errorCategory: GeminiErrorCategory? = null,
    val retryCount: Int = 0,
    val usage: GeminiUsageMetadata? = null
)

object GeminiObservability {
    private const val TAG = "StudyHubAiEngine"

    fun recordMetrics(metrics: GeminiRequestMetrics) {
        val status = if (metrics.success) "SUCCESS" else "FAILURE(${metrics.errorCategory})"
        val tokenInfo = metrics.usage?.let { "tokens=[in:${it.promptTokens}, out:${it.candidatesTokens}, tot:${it.totalTokens}]" } ?: "tokens=N/A"
        Log.i(
            TAG,
            "requestId=${metrics.requestId} task=${metrics.taskCategory} model=${metrics.model} key=${metrics.keyOpaqueId} " +
                    "latency=${metrics.latencyMs}ms status=$status retries=${metrics.retryCount} $tokenInfo"
        )
    }

    fun recordKeyFailover(fromKey: String, toKey: String, reason: GeminiErrorCategory) {
        Log.w(TAG, "KEY_FAILOVER: from=$fromKey to=$toKey reason=$reason")
    }

    fun recordModelFallback(fromModel: String, toModel: String, reason: String) {
        Log.w(TAG, "MODEL_FALLBACK: from=$fromModel to=$toModel reason=$reason")
    }

    fun recordQuotaCooldown(keyOpaqueId: String, cooldownMs: Long, isProjectWide: Boolean) {
        Log.w(TAG, "QUOTA_COOLDOWN: key=$keyOpaqueId cooldownMs=$cooldownMs projectWide=$isProjectWide")
    }
}
