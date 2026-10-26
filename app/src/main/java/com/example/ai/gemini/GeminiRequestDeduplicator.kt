package com.example.ai.gemini

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Deduplicates concurrent identical requests (e.g. from rapid double-clicks)
 * to avoid duplicate token consumption and provider quota waste.
 */
class GeminiRequestDeduplicator<T> {
    private val mutex = Mutex()
    private val inFlightRequests = mutableMapOf<String, Deferred<T>>()

    /**
     * Executes or joins an in-flight operation with the given idempotency key.
     */
    suspend fun executeOrJoin(
        key: String,
        operation: suspend () -> Deferred<T>
    ): T {
        val existing = mutex.withLock { inFlightRequests[key] }
        if (existing != null) {
            return existing.await()
        }

        val deferred = mutex.withLock {
            val doubleCheck = inFlightRequests[key]
            if (doubleCheck != null) {
                return@withLock doubleCheck
            }
            val newDeferred = operation()
            inFlightRequests[key] = newDeferred
            newDeferred
        }

        return try {
            deferred.await()
        } finally {
            mutex.withLock {
                inFlightRequests.remove(key)
            }
        }
    }
}
