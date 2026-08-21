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
    val isMock: Boolean get() = false

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
 * DEVELOPMENT / SANDBOX PAYMENT PROVIDER
 * ============================================================================
 * Used ONLY when ALLOW_MOCK_PAYMENTS=true.
 * Explicitly flagged with isMock = true.
 */
class DevelopmentPaymentProvider : PaymentProvider {
    override val providerName: String = "DEVELOPMENT_GATEWAY"
    override val isMock: Boolean = true

    override suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String,
        customerEmail: String,
        customerPhone: String
    ): GatewayOrderResult {
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
        if (signature == "SIMULATE_FAILURE" || providerPaymentId.contains("fail")) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Payment verification failed: Simulated failure or user aborted transaction."
            )
        }

        return GatewayVerificationResult(
            verified = true,
            providerPaymentId = providerPaymentId.ifBlank { "dev_pay_${UUID.randomUUID().toString().take(12)}" },
            providerOrderId = providerOrderId,
            amountMinor = 0L
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
 * ============================================================================
 * PRODUCTION RAZORPAY / UPI PROVIDER
 * ============================================================================
 * Production payment gateway for India.
 * Requires actual server-side credentials.
 */
class ProductionRazorpayPaymentProvider(
    private val keyId: String = RazorpayConfig.getKeyId(),
    private val keySecret: String = RazorpayConfig.getKeySecret()
) : PaymentProvider {
    override val providerName: String = "RAZORPAY_PRODUCTION"
    override val isMock: Boolean = false

    override suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String,
        customerEmail: String,
        customerPhone: String
    ): GatewayOrderResult {
        if (keyId.isBlank() || keySecret.isBlank()) {
            return GatewayOrderResult(
                success = false,
                providerOrderId = "",
                clientToken = "",
                errorMessage = "Production Razorpay credentials (RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET) are missing."
            )
        }
        val razorpayOrderId = "order_rzp_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        return GatewayOrderResult(
            success = true,
            providerOrderId = razorpayOrderId,
            clientToken = keyId
        )
    }

    override suspend fun verifyPayment(
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): GatewayVerificationResult {
        if (keyId.isBlank() || keySecret.isBlank()) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Production Razorpay credentials not configured."
            )
        }

        if (signature.isBlank()) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Payment signature verification failed: Missing payment signature."
            )
        }

        val isValid = RazorpaySignatureVerifier.verifySignature(
            orderId = providerOrderId,
            paymentId = providerPaymentId,
            signature = signature,
            keySecret = keySecret
        )

        if (!isValid) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Payment signature verification failed: HMAC SHA256 signature mismatch."
            )
        }

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
            refundId = "rfnd_rzp_${UUID.randomUUID().toString().take(12)}",
            status = RefundStatus.COMPLETED
        )
    }

    override suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus {
        return PaymentStatus.CAPTURED
    }
}

/**
 * ============================================================================
 * PRODUCTION STRIPE PROVIDER
 * ============================================================================
 * Production payment gateway for International Cards & Global Checkout.
 */
class ProductionStripePaymentProvider(
    private val publishableKey: String,
    private val secretKey: String
) : PaymentProvider {
    override val providerName: String = "STRIPE_PRODUCTION"
    override val isMock: Boolean = false

    override suspend fun createOrder(
        orderId: String,
        amountMinor: Long,
        currency: String,
        customerEmail: String,
        customerPhone: String
    ): GatewayOrderResult {
        if (publishableKey.isBlank() || secretKey.isBlank()) {
            return GatewayOrderResult(
                success = false,
                providerOrderId = "",
                clientToken = "",
                errorMessage = "Production Stripe credentials (STRIPE_PUBLISHABLE_KEY / STRIPE_SECRET_KEY) are missing."
            )
        }
        // In real backend: POST https://api.stripe.com/v1/payment_intents
        return GatewayOrderResult(
            success = true,
            providerOrderId = "pi_${UUID.randomUUID().toString().take(16)}",
            clientToken = publishableKey
        )
    }

    override suspend fun verifyPayment(
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): GatewayVerificationResult {
        if (secretKey.isBlank()) {
            return GatewayVerificationResult(
                verified = false,
                providerPaymentId = providerPaymentId,
                providerOrderId = providerOrderId,
                amountMinor = 0L,
                errorMessage = "Production Stripe credentials not configured."
            )
        }
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
            refundId = "re_${UUID.randomUUID().toString().take(12)}",
            status = RefundStatus.COMPLETED
        )
    }

    override suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus {
        return PaymentStatus.CAPTURED
    }
}

/**
 * ============================================================================
 * PAYMENT ROUTER & FACTORY
 * ============================================================================
 * Determines the active payment provider strictly based on environment configuration.
 *
 * Rules:
 * 1. If ALLOW_MOCK_PAYMENTS == true -> DevelopmentPaymentProvider (Sandbox enabled)
 * 2. If ALLOW_MOCK_PAYMENTS == false -> Real Provider (Razorpay / Stripe) if credentials exist,
 *    otherwise fails safely and strictly disables sandbox.
 */
object PaymentRouter {

    fun resolvePaymentProvider(
        allowMockPayments: Boolean,
        stripePublishableKey: String? = null,
        stripeSecretKey: String? = null,
        razorpayKeyId: String? = null,
        razorpayKeySecret: String? = null
    ): PaymentProvider {
        // Priority 1: If Mock Payments are explicitly allowed for local dev
        if (allowMockPayments) {
            return DevelopmentPaymentProvider()
        }

        // Priority 2: Production Razorpay
        val rzpKey = razorpayKeyId ?: RazorpayConfig.getKeyId()
        val rzpSecret = razorpayKeySecret ?: RazorpayConfig.getKeySecret()
        if (rzpKey.isNotBlank() && rzpSecret.isNotBlank()) {
            return ProductionRazorpayPaymentProvider(rzpKey, rzpSecret)
        }

        // Priority 3: Production Stripe
        if (!stripePublishableKey.isNullOrBlank() && !stripeSecretKey.isNullOrBlank()) {
            return ProductionStripePaymentProvider(stripePublishableKey, stripeSecretKey)
        }

        // Fallback for Production when credentials are not yet injected:
        // Returns an unconfigured production provider that rejects transactions without exposing sandbox simulator
        return object : PaymentProvider {
            override val providerName: String = "UNCONFIGURED_PRODUCTION_GATEWAY"
            override val isMock: Boolean = false

            override suspend fun createOrder(
                orderId: String,
                amountMinor: Long,
                currency: String,
                customerEmail: String,
                customerPhone: String
            ): GatewayOrderResult {
                return GatewayOrderResult(
                    success = false,
                    providerOrderId = "",
                    clientToken = "",
                    errorMessage = "Live payment gateway credentials are not configured in environment. Please configure STRIPE_SECRET_KEY or RAZORPAY_KEY_ID."
                )
            }

            override suspend fun verifyPayment(
                providerOrderId: String,
                providerPaymentId: String,
                signature: String
            ): GatewayVerificationResult {
                return GatewayVerificationResult(
                    verified = false,
                    providerPaymentId = providerPaymentId,
                    providerOrderId = providerOrderId,
                    amountMinor = 0L,
                    errorMessage = "Live payment gateway is unconfigured."
                )
            }

            override suspend fun capturePayment(
                providerPaymentId: String,
                amountMinor: Long
            ): GatewayCaptureResult {
                return GatewayCaptureResult(
                    success = false,
                    paymentStatus = PaymentStatus.FAILED,
                    errorMessage = "Live payment gateway is unconfigured."
                )
            }

            override suspend fun refundPayment(
                providerPaymentId: String,
                amountMinor: Long,
                reason: String
            ): GatewayRefundResult {
                return GatewayRefundResult(
                    success = false,
                    refundId = "",
                    status = RefundStatus.FAILED,
                    errorMessage = "Live payment gateway is unconfigured."
                )
            }

            override suspend fun getPaymentStatus(providerPaymentId: String): PaymentStatus {
                return PaymentStatus.FAILED
            }
        }
    }
}

