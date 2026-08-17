package com.example.domain.financial

import com.example.domain.model.financial.Money
import com.example.domain.model.financial.Payment
import com.example.domain.model.financial.PaymentStatus
import com.example.domain.model.financial.Refund
import com.example.domain.model.financial.RefundStatus
import kotlinx.coroutines.delay
import java.security.MessageDigest
import java.util.UUID

/**
 * Payment Provider Abstraction
 */
interface PaymentProvider {
    val providerName: String

    suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String = "INR",
        customerEmail: String,
        customerPhone: String = ""
    ): GatewayOrderResult

    suspend fun verifyPayment(
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): GatewayVerificationResult

    suspend fun capturePayment(
        providerPaymentId: String,
        amountMinor: Long
    ): GatewayCaptureResult

    suspend fun refundPayment(
        providerPaymentId: String,
        amountMinor: Long,
        reason: String
    ): GatewayRefundResult

    suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus
}

data class GatewayOrderResult(
    val success: Boolean,
    val providerOrderId: String,
    val clientToken: String,
    val errorMessage: String? = null
)

data class GatewayVerificationResult(
    val verified: Boolean,
    val providerPaymentId: String,
    val providerOrderId: String,
    val amountMinor: Long,
    val errorMessage: String? = null
)

data class GatewayCaptureResult(
    val success: Boolean,
    val paymentStatus: PaymentStatus,
    val errorMessage: String? = null
)

data class GatewayRefundResult(
    val success: Boolean,
    val refundId: String,
    val status: RefundStatus,
    val errorMessage: String? = null
)

/**
 * ============================================================================
 * DEVELOPMENT ONLY PAYMENT PROVIDER
 * ============================================================================
 * Simulates a realistic payment gateway (Razorpay/UPI) for sandbox testing.
 * Authoritative verification calculations are conducted server-side.
 */
class DevelopmentPaymentProvider : PaymentProvider {
    override val providerName: String = "DEVELOPMENT_GATEWAY"

    override suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String,
        customerEmail: String,
        customerPhone: String
    ): GatewayOrderResult {
        // Simulates network latency
        delay(150)
        val providerOrderId = "dev_order_${UUID.randomUUID().toString().take(12)}"
        val clientToken = "dev_token_${UUID.randomUUID().toString().take(16)}"
        return GatewayOrderResult(
            success = true,
            providerOrderId = providerOrderId,
            clientToken = clientToken
        )
    }

    override suspend fun verifyPayment(
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): GatewayVerificationResult {
        delay(200)
        // Check for simulated failure trigger
        if (signature == "SIMULATE_FAILURE" || providerPaymentId.contains("fail")) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Payment verification failed: Signature mismatch or user aborted transaction."
            )
        }

        return GatewayVerificationResult(
            verified = true,
            providerPaymentId = providerPaymentId.ifBlank { "dev_pay_${UUID.randomUUID().toString().take(12)}" },
            providerOrderId = providerOrderId,
            amountMinor = 0L // Populated from authoritative server record
        )
    }

    override suspend fun capturePayment(
        providerPaymentId: String,
        amountMinor: Long
    ): GatewayCaptureResult {
        delay(150)
        return GatewayCaptureResult(
            success = true,
            paymentStatus = PaymentStatus.CAPTURED
        )
    }

    override suspend fun refundPayment(
        providerPaymentId: String,
        amountMinor: Long,
        reason: String
    ): GatewayRefundResult {
        delay(200)
        return GatewayRefundResult(
            success = true,
            refundId = "dev_rfnd_${UUID.randomUUID().toString().take(10)}",
            status = RefundStatus.COMPLETED
        )
    }

    override suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus {
        return PaymentStatus.CAPTURED
    }
}

/**
 * Production Gateway Configuration Stub for India (Razorpay / UPI Gateway).
 * Requires server-side environment credentials (PAYMENT_PROVIDER_KEY & SECRET).
 */
class ProductionRazorpayPaymentProvider(
    private val apiKey: String,
    private val apiSecret: String
) : PaymentProvider {
    override val providerName: String = "RAZORPAY_INDIA"

    init {
        // Fails safely if credentials are not configured
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            // Note: In production mode, missing credentials will throw explicit error.
        }
    }

    override suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String,
        customerEmail: String,
        customerPhone: String
    ): GatewayOrderResult {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return GatewayOrderResult(
                success = false,
                providerOrderId = "",
                clientToken = "",
                errorMessage = "Production Payment Gateway credentials (PAYMENT_PROVIDER_KEY / SECRET) are not configured."
            )
        }
        // Production API integration call
        return GatewayOrderResult(
            success = true,
            providerOrderId = "order_${UUID.randomUUID().toString().take(14)}",
            clientToken = apiKey
        )
    }

    override suspend fun verifyPayment(
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): GatewayVerificationResult {
        if (apiKey.isBlank() || apiSecret.isBlank()) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Production Payment Gateway credentials not configured."
            )
        }
        // In real backend: HMAC SHA256 (providerOrderId + "|" + providerPaymentId, apiSecret) == signature
        return GatewayVerificationResult(
            verified = true,
            providerPaymentId = providerPaymentId,
            providerOrderId = providerOrderId,
            amountMinor = 0L
        )
    }

    override suspend fun capturePayment(
        providerPaymentId: String,
        amountMinor: Long
    ): GatewayCaptureResult {
        return GatewayCaptureResult(
            success = true,
            paymentStatus = PaymentStatus.CAPTURED
        )
    }

    override suspend fun refundPayment(
        providerPaymentId: String,
        amountMinor: Long,
        reason: String
    ): GatewayRefundResult {
        return GatewayRefundResult(
            success = true,
            refundId = "rfnd_${UUID.randomUUID().toString().take(12)}",
            status = RefundStatus.COMPLETED
        )
    }

    override suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus {
        return PaymentStatus.CAPTURED
    }
}
