package com.example.core.observability

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Enterprise Metrics & Telemetry Collector for Bookora.
 * Collects API request counts, latencies (p50, p95, p99), error rates, database query durations,
 * cache hit ratios, and critical business KPIs.
 */
object MetricsCollector {

    private val requestCounts = ConcurrentHashMap<String, AtomicLong>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val latencySumMs = ConcurrentHashMap<String, AtomicLong>()
    private val latencySamples = ConcurrentHashMap<String, MutableList<Long>>()

    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)

    // Business KPIs
    private val orderSuccessCount = AtomicLong(0)
    private val orderFailureCount = AtomicLong(0)
    private val aiRequestsCount = AtomicLong(0)
    private val aiTokensUsed = AtomicLong(0)

    data class EndpointMetricSummary(
        val endpoint: String,
        val totalRequests: Long,
        val totalErrors: Long,
        val errorRatePercentage: Double,
        val averageLatencyMs: Double,
        val p95LatencyMs: Long
    )

    data class SystemMetricsSnapshot(
        val timestamp: Long,
        val totalRequests: Long,
        val totalErrors: Long,
        val globalErrorRatePercentage: Double,
        val cacheHitRatio: Double,
        val orderSuccessCount: Long,
        val orderFailureCount: Long,
        val aiRequestsCount: Long,
        val aiTokensUsed: Long,
        val endpoints: List<EndpointMetricSummary>
    )

    fun recordRequest(endpoint: String, durationMs: Long, isError: Boolean = false) {
        requestCounts.computeIfAbsent(endpoint) { AtomicLong(0) }.incrementAndGet()
        latencySumMs.computeIfAbsent(endpoint) { AtomicLong(0) }.addAndGet(durationMs)

        if (isError) {
            errorCounts.computeIfAbsent(endpoint) { AtomicLong(0) }.incrementAndGet()
        }

        latencySamples.compute(endpoint) { _, list ->
            val sampleList = list ?: mutableListOf()
            synchronized(sampleList) {
                if (sampleList.size >= 1000) {
                    sampleList.removeAt(0)
                }
                sampleList.add(durationMs)
            }
            sampleList
        }
    }

    fun recordCacheAccess(isHit: Boolean) {
        if (isHit) cacheHits.incrementAndGet() else cacheMisses.incrementAndGet()
    }

    fun recordOrder(isSuccess: Boolean) {
        if (isSuccess) orderSuccessCount.incrementAndGet() else orderFailureCount.incrementAndGet()
    }

    fun recordAiUsage(tokens: Long) {
        aiRequestsCount.incrementAndGet()
        aiTokensUsed.addAndGet(tokens)
    }

    fun getSnapshot(): SystemMetricsSnapshot {
        val totalReq = requestCounts.values.sumOf { it.get() }
        val totalErr = errorCounts.values.sumOf { it.get() }
        val globalErrRate = if (totalReq > 0) (totalErr.toDouble() / totalReq.toDouble()) * 100.0 else 0.0

        val totalHits = cacheHits.get()
        val totalMisses = cacheMisses.get()
        val totalCache = totalHits + totalMisses
        val hitRatio = if (totalCache > 0) totalHits.toDouble() / totalCache.toDouble() else 1.0

        val endpointSummaries = requestCounts.keys.map { endpoint ->
            val reqs = requestCounts[endpoint]?.get() ?: 0L
            val errs = errorCounts[endpoint]?.get() ?: 0L
            val sumMs = latencySumMs[endpoint]?.get() ?: 0L
            val avg = if (reqs > 0) sumMs.toDouble() / reqs.toDouble() else 0.0

            val samples = latencySamples[endpoint]?.let {
                synchronized(it) { it.sorted() }
            } ?: emptyList()

            val p95 = if (samples.isNotEmpty()) {
                val idx = (samples.size * 0.95).toInt().coerceAtMost(samples.size - 1)
                samples[idx]
            } else 0L

            val errRate = if (reqs > 0) (errs.toDouble() / reqs.toDouble()) * 100.0 else 0.0

            EndpointMetricSummary(
                endpoint = endpoint,
                totalRequests = reqs,
                totalErrors = errs,
                errorRatePercentage = errRate,
                averageLatencyMs = avg,
                p95LatencyMs = p95
            )
        }

        return SystemMetricsSnapshot(
            timestamp = System.currentTimeMillis(),
            totalRequests = totalReq,
            totalErrors = totalErr,
            globalErrorRatePercentage = globalErrRate,
            cacheHitRatio = hitRatio,
            orderSuccessCount = orderSuccessCount.get(),
            orderFailureCount = orderFailureCount.get(),
            aiRequestsCount = aiRequestsCount.get(),
            aiTokensUsed = aiTokensUsed.get(),
            endpoints = endpointSummaries
        )
    }

    fun reset() {
        requestCounts.clear()
        errorCounts.clear()
        latencySumMs.clear()
        latencySamples.clear()
        cacheHits.set(0)
        cacheMisses.set(0)
        orderSuccessCount.set(0)
        orderFailureCount.set(0)
        aiRequestsCount.set(0)
        aiTokensUsed.set(0)
    }
}
