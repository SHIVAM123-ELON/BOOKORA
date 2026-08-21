package com.example.domain.financial

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Server-Side Cryptographic Signature Verifier for Razorpay.
 * Implements standard HMAC-SHA256 signature validation:
 * expected_signature = HMAC-SHA256(order_id + "|" + payment_id, key_secret)
 */
object RazorpaySignatureVerifier {

    private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"

    /**
     * Verifies the Razorpay payment signature against the server-side Key Secret.
     * Uses constant-time equality comparison to protect against timing attacks.
     *
     * @param orderId Razorpay Order ID (e.g., "order_xxx")
     * @param paymentId Razorpay Payment ID (e.g., "pay_xxx")
     * @param signature Signature received from client checkout
     * @param keySecret Razorpay Key Secret stored securely on server
     * @return true if authentic and verified, false otherwise
     */
    fun verifySignature(
        orderId: String,
        paymentId: String,
        signature: String,
        keySecret: String
    ): Boolean {
        if (orderId.isBlank() || paymentId.isBlank() || signature.isBlank() || keySecret.isBlank()) {
            return false
        }

        return try {
            val payload = "$orderId|$paymentId"
            val expectedSignature = calculateHmacSha256(payload, keySecret)
            MessageDigest.isEqual(
                expectedSignature.lowercase().toByteArray(Charsets.UTF_8),
                signature.trim().lowercase().toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Verifies Razorpay Webhook signature against webhook secret.
     */
    fun verifyWebhookSignature(
        payload: String,
        signature: String,
        webhookSecret: String
    ): Boolean {
        if (payload.isBlank() || signature.isBlank() || webhookSecret.isBlank()) {
            return false
        }

        return try {
            val expectedSignature = calculateHmacSha256(payload, webhookSecret)
            MessageDigest.isEqual(
                expectedSignature.lowercase().toByteArray(Charsets.UTF_8),
                signature.trim().lowercase().toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculates authentic HMAC-SHA256 hex string for given payload and secret.
     */
    fun calculateHmacSha256(payload: String, secret: String): String {
        val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        val keySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256_ALGORITHM)
        mac.init(keySpec)
        val hashBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
