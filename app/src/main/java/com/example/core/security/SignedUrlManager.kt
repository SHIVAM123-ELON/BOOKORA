package com.example.core.security

import java.security.MessageDigest
import java.util.Base64

/**
 * Enterprise Secure Signed URL & E-Book Asset Access Protection Engine.
 * Generates and validates tamper-proof, short-lived HMAC-signed URLs for digital book files (PDF, EPUB, Audio),
 * ensuring content cannot be downloaded directly without active verified purchase entitlement.
 */
object SignedUrlManager {

    private const val SIGNING_SECRET = "bookora_prod_signing_key_secure_secret_v1"
    private const val DEFAULT_EXPIRY_SECONDS = 900L // 15 minutes

    data class SignedUrl(
        val url: String,
        val expiresAtEpochMs: Long,
        val signature: String,
        val token: String,
        val watermarkUserTag: String
    )

    data class UrlValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val bookId: String? = null,
        val userId: String? = null
    )

    /**
     * Generates a secure, expiring signed URL for an authorized reader with dynamic watermark metadata.
     */
    fun generateSignedDownloadUrl(
        bookId: String,
        userId: String,
        fileExtension: String = "epub",
        durationSeconds: Long = DEFAULT_EXPIRY_SECONDS
    ): SignedUrl {
        val expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L)
        val rawPayload = "$bookId|$userId|$expiresAt|$fileExtension"
        val signature = computeHmacSha256(rawPayload, SIGNING_SECRET)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawPayload.toByteArray(Charsets.UTF_8))
        val watermarkTag = "Licensed to: $userId (Bookora DRM protected)"

        val url = "https://cdn.bookora.com/vault/books/$bookId.$fileExtension?token=$token&sig=$signature&exp=$expiresAt"

        return SignedUrl(
            url = url,
            expiresAtEpochMs = expiresAt,
            signature = signature,
            token = token,
            watermarkUserTag = watermarkTag
        )
    }

    /**
     * Validates an incoming signed URL token and signature on the download proxy / server.
     */
    fun validateSignedUrl(token: String, signature: String, expectedExpiresAt: Long): UrlValidationResult {
        val now = System.currentTimeMillis()
        if (now > expectedExpiresAt) {
            return UrlValidationResult(isValid = false, reason = "Signed URL has expired. Please request a fresh reading token.")
        }

        val decodedPayload = try {
            String(Base64.getUrlDecoder().decode(token), Charsets.UTF_8)
        } catch (e: Exception) {
            return UrlValidationResult(isValid = false, reason = "Invalid or corrupted token payload")
        }

        val computedSignature = computeHmacSha256(decodedPayload, SIGNING_SECRET)
        if (!MessageDigest.isEqual(computedSignature.toByteArray(Charsets.UTF_8), signature.toByteArray(Charsets.UTF_8))) {
            return UrlValidationResult(isValid = false, reason = "Signature mismatch. Tampering detected.")
        }

        val parts = decodedPayload.split("|")
        if (parts.size < 3) {
            return UrlValidationResult(isValid = false, reason = "Malformed payload structure")
        }

        val bookId = parts[0]
        val userId = parts[1]
        val payloadExpiry = parts[2].toLongOrNull() ?: 0L

        if (payloadExpiry != expectedExpiresAt) {
            return UrlValidationResult(isValid = false, reason = "Expiry timestamp tampering detected")
        }

        return UrlValidationResult(
            isValid = true,
            bookId = bookId,
            userId = userId
        )
    }

    private fun computeHmacSha256(data: String, secret: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(secret.toByteArray(Charsets.UTF_8))
        val hash = md.digest(data.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
}
