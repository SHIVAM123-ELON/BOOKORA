package com.example.core.observability

import java.util.concurrent.ConcurrentHashMap

enum class ServiceHealthState {
    OPERATIONAL,
    DEGRADED_PERFORMANCE,
    PARTIAL_OUTAGE,
    MAJOR_OUTAGE,
    MAINTENANCE
}

data class ServiceStatus(
    val name: String,
    val state: ServiceHealthState,
    val uptimePercent30Days: Double = 99.98,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class PublicStatusOverview(
    val overallState: ServiceHealthState,
    val message: String,
    val services: List<ServiceStatus>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Public Status Page & Incident Architecture for Bookora.
 * Real-time aggregated health states for core platform services.
 */
object StatusPageService {

    private val serviceMap = ConcurrentHashMap<String, ServiceStatus>()

    init {
        serviceMap["API & Storefront"] = ServiceStatus("API & Storefront", ServiceHealthState.OPERATIONAL)
        serviceMap["Payments & Checkout"] = ServiceStatus("Payments & Checkout", ServiceHealthState.OPERATIONAL)
        serviceMap["Digital Reader & DRM"] = ServiceStatus("Digital Reader & DRM", ServiceHealthState.OPERATIONAL)
        serviceMap["Gemini AI Assistant"] = ServiceStatus("Gemini AI Assistant", ServiceHealthState.OPERATIONAL)
        serviceMap["Catalog & Semantic Search"] = ServiceStatus("Catalog & Semantic Search", ServiceHealthState.OPERATIONAL)
        serviceMap["Push & Email Notifications"] = ServiceStatus("Push & Email Notifications", ServiceHealthState.OPERATIONAL)
    }

    fun updateServiceStatus(serviceName: String, state: ServiceHealthState) {
        serviceMap[serviceName]?.let { current ->
            serviceMap[serviceName] = current.copy(state = state, lastUpdated = System.currentTimeMillis())
        }
    }

    fun getStatusOverview(): PublicStatusOverview {
        val list = serviceMap.values.toList()
        val hasMajor = list.any { it.state == ServiceHealthState.MAJOR_OUTAGE }
        val hasPartial = list.any { it.state == ServiceHealthState.PARTIAL_OUTAGE }
        val hasDegraded = list.any { it.state == ServiceHealthState.DEGRADED_PERFORMANCE }

        val overall = when {
            hasMajor -> ServiceHealthState.MAJOR_OUTAGE
            hasPartial -> ServiceHealthState.PARTIAL_OUTAGE
            hasDegraded -> ServiceHealthState.DEGRADED_PERFORMANCE
            else -> ServiceHealthState.OPERATIONAL
        }

        val message = when (overall) {
            ServiceHealthState.OPERATIONAL -> "All Bookora Systems Operational"
            ServiceHealthState.DEGRADED_PERFORMANCE -> "Some Services Experiencing Elevated Latency"
            ServiceHealthState.PARTIAL_OUTAGE -> "Partial Outage on Core Services"
            ServiceHealthState.MAJOR_OUTAGE -> "Major Outage Under Investigation"
            ServiceHealthState.MAINTENANCE -> "Scheduled Platform Maintenance"
        }

        return PublicStatusOverview(
            overallState = overall,
            message = message,
            services = list
        )
    }
}
