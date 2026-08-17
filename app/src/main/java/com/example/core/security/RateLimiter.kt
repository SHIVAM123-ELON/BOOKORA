package com.example.core.security

import java.util.concurrent.ConcurrentHashMap

/**
 * Enterprise Configurable Token-Bucket Rate Limiter for Bookora.
 * Provides custom limits per endpoint type:
 * - Authentication: 5 req/min
 * - Search: 60 req/min
 * - Book Catalog: 120 req/min
 * - Reviews: 10 req/min
 * - AI/Gemini endpoints: 15 req/min (Strict Protection)
 * - Payments/Checkout: 10 req/min (Strict Fraud Protection)
 * - Admin Operations: 30 req/min
 * - File Downloads: 20 req/min
 */
enum class RateLimitTier(
    val maxTokens: Int,
    val refillIntervalMs: Long
) {
    AUTH(maxTokens = 5, refillIntervalMs = 60_000L),
    SEARCH(maxTokens = 60, refillIntervalMs = 60_000L),
    CATALOG(maxTokens = 120, refillIntervalMs = 60_000L),
    REVIEWS(maxTokens = 10, refillIntervalMs = 60_000L),
    AI_INFERENCE(maxTokens = 15, refillIntervalMs = 60_000L),
    PAYMENTS(maxTokens = 10, refillIntervalMs = 60_000L),
    ADMIN(maxTokens = 30, refillIntervalMs = 60_000L),
    FILE_DOWNLOAD(maxTokens = 20, refillIntervalMs = 60_000L),
    WEBHOOKS(maxTokens = 100, refillIntervalMs = 60_000L)
}

object RateLimiter {

    data class ClientBucket(
        var availableTokens: Double,
        var lastRefillTimestamp: Long
    )

    data class RateLimitDecision(
        val isAllowed: Boolean,
        val remainingTokens: Int,
        val retryAfterSeconds: Long = 0L
    )

    private val buckets = ConcurrentHashMap<String, ClientBucket>()

    /**
     * Checks rate limit and consumes 1 token if allowed.
     */
    fun tryAcquire(clientKey: String, tier: RateLimitTier, tokensToConsume: Double = 1.0): RateLimitDecision {
        val compositeKey = "${tier.name}:$clientKey"
        val now = System.currentTimeMillis()

        val bucket = buckets.compute(compositeKey) { _, existing ->
            if (existing == null) {
                ClientBucket(
                    availableTokens = tier.maxTokens.toDouble(),
                    lastRefillTimestamp = now
                )
            } else {
                val elapsed = (now - existing.lastRefillTimestamp).coerceAtLeast(0L)
                val refillRatePerMs = tier.maxTokens.toDouble() / tier.refillIntervalMs.toDouble()
                val refilled = existing.availableTokens + (elapsed * refillRatePerMs)
                existing.availableTokens = refilled.coerceAtMost(tier.maxTokens.toDouble())
                existing.lastRefillTimestamp = now
                existing
            }
        }!!

        synchronized(bucket) {
            if (bucket.availableTokens >= tokensToConsume) {
                bucket.availableTokens -= tokensToConsume
                return RateLimitDecision(
                    isAllowed = true,
                    remainingTokens = bucket.availableTokens.toInt(),
                    retryAfterSeconds = 0L
                )
            } else {
                val deficit = tokensToConsume - bucket.availableTokens
                val refillRatePerMs = tier.maxTokens.toDouble() / tier.refillIntervalMs.toDouble()
                val waitTimeMs = if (refillRatePerMs > 0) (deficit / refillRatePerMs).toLong() else 60000L
                val retryAfterSec = (waitTimeMs / 1000L).coerceAtLeast(1L)

                return RateLimitDecision(
                    isAllowed = false,
                    remainingTokens = 0,
                    retryAfterSeconds = retryAfterSec
                )
            }
        }
    }

    fun resetLimits(clientKey: String? = null) {
        if (clientKey == null) {
            buckets.clear()
        } else {
            buckets.keys.filter { it.endsWith(":$clientKey") }.forEach { buckets.remove(it) }
        }
    }
}
