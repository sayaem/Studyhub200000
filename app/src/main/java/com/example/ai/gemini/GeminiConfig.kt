package com.example.ai.gemini

import com.example.BuildConfig
import java.lang.reflect.Field

/**
 * Centralized, immutable configuration for Gemini API requests.
 * Loads multi-key environment variables (GEMINI_API_KEY_1..N, GEMINI_API_KEY),
 * validates values, deduplicates keys, and provides safe defaults.
 */
data class GeminiConfig(
    val keys: List<String>,
    val maxRetries: Int = 2,
    val keyCooldownMs: Long = 60_000L,
    val requestTimeoutMs: Long = 60_000L,
    val maxOutputTokens: Int = 4096,
    val enableModelFallback: Boolean = true,
    val enableKeyRotation: Boolean = true,
    val defaultModel: String = "gemini-3.5-flash"
) {
    companion object {
        private const val PLACEHOLDER_PREFIX = "your_"

        /**
         * Loads configuration from BuildConfig and System.getenv().
         * Automatically discovers all GEMINI_API_KEY_N and GEMINI_API_KEY fields.
         */
        fun load(): GeminiConfig {
            val discoveredKeys = mutableListOf<String>()

            // 1. Discover from BuildConfig fields via reflection
            try {
                val fields: Array<Field> = BuildConfig::class.java.declaredFields
                val keyFields = fields.filter {
                    it.name.startsWith("GEMINI_API_KEY")
                }.sortedBy { it.name }

                for (field in keyFields) {
                    try {
                        field.isAccessible = true
                        val value = field.get(null)?.toString()?.trim() ?: ""
                        if (isValidKey(value)) {
                            discoveredKeys.add(value)
                        }
                    } catch (_: Throwable) {
                    }
                }
            } catch (_: Throwable) {
            }

            // 2. Discover from System.getenv()
            try {
                val env = System.getenv()
                val envKeys = env.keys.filter { it.startsWith("GEMINI_API_KEY") }.sorted()
                for (k in envKeys) {
                    val value = env[k]?.trim() ?: ""
                    if (isValidKey(value)) {
                        discoveredKeys.add(value)
                    }
                }
            } catch (_: Throwable) {
            }

            // Deduplicate keys while preserving order
            val uniqueKeys = discoveredKeys.distinct()

            // Read behavior settings from BuildConfig or environment with safe fallbacks
            val maxRetries = readIntConfig("GEMINI_MAX_RETRIES", 2)
            val cooldownMs = readLongConfig("GEMINI_KEY_COOLDOWN_MS", 60_000L)
            val timeoutMs = readLongConfig("GEMINI_REQUEST_TIMEOUT_MS", 60_000L)
            val maxTokens = readIntConfig("GEMINI_MAX_OUTPUT_TOKENS", 4096)
            val enableFallback = readBooleanConfig("GEMINI_ENABLE_MODEL_FALLBACK", true)
            val enableRotation = readBooleanConfig("GEMINI_ENABLE_KEY_ROTATION", true)

            return GeminiConfig(
                keys = uniqueKeys,
                maxRetries = maxRetries.coerceIn(0, 5),
                keyCooldownMs = cooldownMs.coerceIn(5_000L, 600_000L),
                requestTimeoutMs = timeoutMs.coerceIn(10_000L, 180_000L),
                maxOutputTokens = maxTokens.coerceIn(512, 8192),
                enableModelFallback = enableFallback,
                enableKeyRotation = enableRotation
            )
        }

        private fun isValidKey(value: String): Boolean {
            if (value.isBlank()) return false
            if (value.startsWith(PLACEHOLDER_PREFIX, ignoreCase = true)) return false
            if (value.equals("your_gemini_api_key_here", ignoreCase = true)) return false
            if (value.equals("your_first_key", ignoreCase = true)) return false
            // Real Gemini keys are typically 39 chars starting with AIza
            return value.length >= 10
        }

        private fun readIntConfig(name: String, default: Int): Int {
            return try {
                val field = BuildConfig::class.java.getDeclaredField(name)
                val str = field.get(null)?.toString()
                str?.toIntOrNull() ?: System.getenv(name)?.toIntOrNull() ?: default
            } catch (_: Throwable) {
                System.getenv(name)?.toIntOrNull() ?: default
            }
        }

        private fun readLongConfig(name: String, default: Long): Long {
            return try {
                val field = BuildConfig::class.java.getDeclaredField(name)
                val str = field.get(null)?.toString()
                str?.toLongOrNull() ?: System.getenv(name)?.toLongOrNull() ?: default
            } catch (_: Throwable) {
                System.getenv(name)?.toLongOrNull() ?: default
            }
        }

        private fun readBooleanConfig(name: String, default: Boolean): Boolean {
            return try {
                val field = BuildConfig::class.java.getDeclaredField(name)
                val str = field.get(null)?.toString()
                str?.toBooleanStrictOrNull() ?: System.getenv(name)?.toBooleanStrictOrNull() ?: default
            } catch (_: Throwable) {
                System.getenv(name)?.toBooleanStrictOrNull() ?: default
            }
        }
    }
}
