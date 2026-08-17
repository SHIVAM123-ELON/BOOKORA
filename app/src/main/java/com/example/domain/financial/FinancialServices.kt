package com.example.domain.financial

import com.example.domain.model.financial.FinancialAuditLog
import com.example.domain.model.financial.Order
import com.example.domain.model.financial.OrderStatus
import com.example.domain.model.financial.Payment
import com.example.domain.model.financial.PaymentStatus
import com.example.domain.model.financial.RiskEvent
import com.example.domain.model.financial.RiskSeverity
import java.util.UUID

/**
 * Audit Logger for Financial Events.
 * Never stores sensitive payment credentials or raw secrets.
 */
class FinancialAuditLogger {

    fun createLog(
        actor: String,
        action: String,
        entity: String,
        entityId: String,
        metadata: Map<String, Any?> = emptyMap()
    ): FinancialAuditLog {
        val safeMetadata = metadata.filterKeys { key ->
            !key.contains("secret", ignoreCase = true) &&
            !key.contains("password", ignoreCase = true) &&
            !key.contains("card", ignoreCase = true) &&
            !key.contains("cvv", ignoreCase = true)
        }.entries.joinToString(prefix = "{", postfix = "}") { "\"${it.key}\": \"${it.value}\"" }

        return FinancialAuditLog(
            id = "audit_${UUID.randomUUID().toString().take(12)}",
            actor = actor,
            action = action,
            entity = entity,
            entityId = entityId,
            timestamp = System.currentTimeMillis(),
            metadata = safeMetadata
        )
    }
}

/**
 * Fraud & Risk Detection Foundation.
 * Flags suspicious activity for admin review without automatic irreversible bans.
 */
class FraudDetectionService {

    private val userPaymentAttempts = mutableMapOf<String, MutableList<Long>>()
    private val processedIdempotencyKeys = mutableSetOf<String>()

    fun checkPaymentRisk(
        userId: String,
        orderAmountMinor: Long,
        idempotencyKey: String?
    ): RiskEvent? {
        val now = System.currentTimeMillis()

        // 1. Idempotency duplicate check
        if (idempotencyKey != null) {
            if (processedIdempotencyKeys.contains(idempotencyKey)) {
                return RiskEvent(
                    id = "risk_${UUID.randomUUID().toString().take(10)}",
                    userId = userId,
                    type = "DUPLICATE_IDEMPOTENT_TRANSACTION",
                    severity = RiskSeverity.LOW,
                    referenceId = idempotencyKey,
                    metadata = "{\"reason\": \"Repeated idempotency key for payment creation\"}"
                )
            }
            processedIdempotencyKeys.add(idempotencyKey)
        }

        // 2. Velocity check (more than 5 attempts within 60 seconds)
        val attempts = userPaymentAttempts.getOrPut(userId) { mutableListOf() }
        attempts.removeAll { now - it > 60_000 }
        attempts.add(now)

        if (attempts.size > 5) {
            return RiskEvent(
                id = "risk_${UUID.randomUUID().toString().take(10)}",
                userId = userId,
                type = "HIGH_VELOCITY_PAYMENT_ATTEMPTS",
                severity = RiskSeverity.HIGH,
                referenceId = userId,
                metadata = "{\"attempts\": ${attempts.size}, \"timeframe\": \"60s\"}"
            )
        }

        return null
    }

    fun checkCouponAbuse(userId: String, couponCode: String, timesUsedByUser: Int, perUserLimit: Int): RiskEvent? {
        if (timesUsedByUser >= perUserLimit) {
            return RiskEvent(
                id = "risk_${UUID.randomUUID().toString().take(10)}",
                userId = userId,
                type = "COUPON_USAGE_LIMIT_EXCEEDED",
                severity = RiskSeverity.MEDIUM,
                referenceId = couponCode,
                metadata = "{\"timesUsed\": $timesUsedByUser, \"limit\": $perUserLimit}"
            )
        }
        return null
    }
}

/**
 * Reconciliation Service to inspect consistency between Payments, Orders, Entitlements and Ledgers.
 */
class FinancialReconciliationService {

    data class ReconciliationReport(
        val totalOrdersChecked: Int,
        val mismatchedOrders: List<String>,
        val missingEntitlements: List<String>,
        val uncapturedPayments: List<String>,
        val isHealthy: Boolean
    )

    fun reconcile(
        orders: List<Order>,
        payments: List<Payment>,
        entitledBookIdsByUser: Map<String, Set<String>>
    ): ReconciliationReport {
        val mismatched = mutableListOf<String>()
        val missingEntitlements = mutableListOf<String>()
        val uncaptured = mutableListOf<String>()

        val paymentMap = payments.associateBy { it.orderId }

        for (order in orders) {
            val payment = paymentMap[order.id]
            if (order.status == OrderStatus.PAID || order.status == OrderStatus.COMPLETED) {
                if (payment == null || payment.status != PaymentStatus.CAPTURED) {
                    mismatched.add("Order ${order.id} marked PAID but payment is not CAPTURED")
                }

                // Check entitlements
                val userEntitlements = entitledBookIdsByUser[order.userId] ?: emptySet()
                for (item in order.items) {
                    if (!userEntitlements.contains(item.bookId)) {
                        missingEntitlements.add("User ${order.userId} missing entitlement for book ${item.bookId} in order ${order.id}")
                    }
                }
            }
        }

        return ReconciliationReport(
            totalOrdersChecked = orders.size,
            mismatchedOrders = mismatched,
            missingEntitlements = missingEntitlements,
            uncapturedPayments = uncaptured,
            isHealthy = mismatched.isEmpty() && missingEntitlements.isEmpty()
        )
    }
}
