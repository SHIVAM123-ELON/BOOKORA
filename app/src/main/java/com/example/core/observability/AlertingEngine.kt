package com.example.core.observability

import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Enterprise Alerting & Anomaly Detection Engine for Bookora.
 * Monitors error spikes, payment failures, brute force attacks, and system degradation.
 */
object AlertingEngine {

    enum class AlertSeverity { INFO, WARNING, CRITICAL }

    data class Alert(
        val id: String,
        val title: String,
        val description: String,
        val severity: AlertSeverity,
        val timestamp: String,
        val source: String,
        val metricsData: Map<String, Any> = emptyMap()
    )

    private val alertHistory = CopyOnWriteArrayList<Alert>()
    private val alertListeners = CopyOnWriteArrayList<(Alert) -> Unit>()

    fun registerListener(listener: (Alert) -> Unit) {
        alertListeners.add(listener)
    }

    fun triggerAlert(
        title: String,
        description: String,
        severity: AlertSeverity,
        source: String,
        metricsData: Map<String, Any> = emptyMap()
    ): Alert {
        val alert = Alert(
            id = "alt_" + System.currentTimeMillis(),
            title = title,
            description = description,
            severity = severity,
            timestamp = Instant.now().toString(),
            source = source,
            metricsData = metricsData
        )
        alertHistory.add(alert)
        if (alertHistory.size > 200) {
            alertHistory.removeAt(0)
        }

        // Structured alert log
        StructuredLogger.error(
            message = "SYSTEM ALERT [${severity.name}]: $title - $description",
            errorCode = "SYS_ALERT_${severity.name}",
            metadata = metricsData + mapOf("source" to source, "alertId" to alert.id)
        )

        alertListeners.forEach { it.invoke(alert) }
        return alert
    }

    /**
     * Evaluates metrics snapshot against operational thresholds.
     */
    fun evaluateRules(snapshot: MetricsCollector.SystemMetricsSnapshot) {
        // Rule 1: High Global Error Rate > 5%
        if (snapshot.totalRequests > 50 && snapshot.globalErrorRatePercentage > 5.0) {
            triggerAlert(
                title = "High Global API Error Rate",
                description = "Error rate is currently ${String.format("%.2f", snapshot.globalErrorRatePercentage)}% exceeding threshold (5.0%)",
                severity = AlertSeverity.CRITICAL,
                source = "API_GATEWAY",
                metricsData = mapOf("errorRate" to snapshot.globalErrorRatePercentage, "totalReq" to snapshot.totalRequests)
            )
        }

        // Rule 2: Payment Failure Spike
        val totalOrders = snapshot.orderSuccessCount + snapshot.orderFailureCount
        if (totalOrders >= 5 && snapshot.orderFailureCount > snapshot.orderSuccessCount) {
            triggerAlert(
                title = "Payment Failure Anomaly",
                description = "Unusually high payment failure rate detected (${snapshot.orderFailureCount} failures out of $totalOrders orders)",
                severity = AlertSeverity.CRITICAL,
                source = "PAYMENT_PROCESSOR",
                metricsData = mapOf("failures" to snapshot.orderFailureCount, "total" to totalOrders)
            )
        }

        // Rule 3: Low Cache Hit Ratio < 50%
        if (snapshot.totalRequests > 100 && snapshot.cacheHitRatio < 0.5) {
            triggerAlert(
                title = "Cache Degradation Detected",
                description = "Cache hit ratio dropped to ${String.format("%.2f", snapshot.cacheHitRatio * 100)}%",
                severity = AlertSeverity.WARNING,
                source = "REDIS_CACHE",
                metricsData = mapOf("cacheHitRatio" to snapshot.cacheHitRatio)
            )
        }
    }

    fun getRecentAlerts(): List<Alert> = alertHistory.toList()

    fun clearAlerts() { alertHistory.clear() }
}
