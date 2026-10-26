package com.example.ai.gemini

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Key representation with operational state.
 * Raw API key is never exposed via toString() or logging.
 */
data class ManagedKey(
    val key: String,
    val index: Int,
    val opaqueId: String = "Key #${index + 1}"
) {
    var isQuarantined: Boolean = false
        internal set
    var cooldownUntilMs: Long = 0L
        internal set
    var consecutiveFailures: Int = 0
        internal set
    var lastUsedMs: Long = 0L
        internal set
    var lastFailureCategory: GeminiErrorCategory? = null
        internal set

    fun isAvailable(currentTimeMs: Long): Boolean {
        if (isQuarantined) return false
        return currentTimeMs >= cooldownUntilMs
    }

    override fun toString(): String = opaqueId
}

/**
 * Thread-safe multi-key rotation and failover manager.
 * Implements least-recently-used / round-robin selection with health and cooldown awareness.
 */
class GeminiKeyManager(
    private val config: GeminiConfig
) {
    private val mutex = Mutex()
    private val managedKeys: List<ManagedKey> = config.keys.mapIndexed { index, rawKey ->
        ManagedKey(key = rawKey, index = index)
    }

    // Shared project quota tracking
    private var sharedProjectQuotaUntilMs: Long = 0L
    private var lastRateLimitTimes = mutableListOf<Long>()

    val totalKeyCount: Int
        get() = managedKeys.size

    /**
     * Acquires the best eligible key.
     * Prioritizes healthy keys that are not quarantined and not in cooldown.
     * Among eligible keys, picks the least recently used key.
     */
    suspend fun acquireKey(excludedKeys: Set<String> = emptySet()): ManagedKey? = mutex.withLock {
        val now = System.currentTimeMillis()

        // If shared project quota is active, no key can succeed right now
        if (now < sharedProjectQuotaUntilMs) {
            return null
        }

        val eligible = managedKeys.filter { candidate ->
            !excludedKeys.contains(candidate.key) && candidate.isAvailable(now)
        }

        if (eligible.isEmpty()) {
            return null
        }

        // Select the least recently used key to distribute load
        val selected = eligible.minByOrNull { it.lastUsedMs } ?: eligible.first()
        selected.lastUsedMs = now
        return selected
    }

    /**
     * Records a successful request for a key.
     */
    suspend fun recordSuccess(key: ManagedKey) = mutex.withLock {
        key.consecutiveFailures = 0
        key.lastFailureCategory = null
    }

    /**
     * Records a failure for a key and applies appropriate cooldown or quarantine.
     */
    suspend fun recordFailure(
        key: ManagedKey,
        errorCategory: GeminiErrorCategory,
        retryAfterMs: Long? = null
    ) = mutex.withLock {
        val now = System.currentTimeMillis()
        key.consecutiveFailures++
        key.lastFailureCategory = errorCategory

        when (errorCategory) {
            GeminiErrorCategory.AUTH_INVALID -> {
                // Permanent key failure (e.g. 401 Unauthorized or bad API key)
                key.isQuarantined = true
            }

            GeminiErrorCategory.RATE_LIMIT -> {
                // Apply cooldown to this key
                val cooldown = retryAfterMs ?: config.keyCooldownMs
                key.cooldownUntilMs = now + cooldown

                // Track rate limit frequency across keys to detect shared project quota
                lastRateLimitTimes.add(now)
                // Keep only events from the last 30 seconds
                lastRateLimitTimes.removeAll { now - it > 30_000L }

                // If 2 or more different keys hit rate limits within 30s, treat as shared project quota
                if (lastRateLimitTimes.size >= 2 && managedKeys.size > 1) {
                    sharedProjectQuotaUntilMs = now + (retryAfterMs ?: config.keyCooldownMs)
                }
            }

            GeminiErrorCategory.SHARED_QUOTA_EXHAUSTED -> {
                // The error explicitly indicated project-level quota exhaustion
                val cooldown = retryAfterMs ?: config.keyCooldownMs
                sharedProjectQuotaUntilMs = now + cooldown
                key.cooldownUntilMs = now + cooldown
            }

            GeminiErrorCategory.SERVER_TRANSIENT,
            GeminiErrorCategory.REQUEST_TIMEOUT,
            GeminiErrorCategory.NETWORK_INTERRUPT -> {
                // Short exponential backoff per key: 2s, 4s, 8s
                val backoff = (2000L * (1 shl (key.consecutiveFailures.coerceAtMost(3) - 1))).coerceAtMost(10_000L)
                key.cooldownUntilMs = now + backoff
            }

            else -> {
                // Other non-retryable errors
            }
        }
    }

    /**
     * Returns true if a shared project-level quota exhaustion is currently active.
     */
    suspend fun isSharedQuotaExhausted(): Boolean = mutex.withLock {
        System.currentTimeMillis() < sharedProjectQuotaUntilMs
    }

    /**
     * Returns time in milliseconds until the earliest key cooldown expires, or null if all are quarantined.
     */
    suspend fun getEarliestRetryWaitMs(): Long? = mutex.withLock {
        val now = System.currentTimeMillis()
        if (now < sharedProjectQuotaUntilMs) {
            return sharedProjectQuotaUntilMs - now
        }
        val nonQuarantined = managedKeys.filter { !it.isQuarantined }
        if (nonQuarantined.isEmpty()) return null
        val minCooldown = nonQuarantined.minOfOrNull { it.cooldownUntilMs } ?: return null
        return (minCooldown - now).coerceAtLeast(0L)
    }

    /**
     * Resets quarantine and cooldown states (for tests or manual refresh).
     */
    suspend fun resetAll() = mutex.withLock {
        sharedProjectQuotaUntilMs = 0L
        lastRateLimitTimes.clear()
        for (k in managedKeys) {
            k.isQuarantined = false
            k.cooldownUntilMs = 0L
            k.consecutiveFailures = 0
            k.lastFailureCategory = null
        }
    }
}
