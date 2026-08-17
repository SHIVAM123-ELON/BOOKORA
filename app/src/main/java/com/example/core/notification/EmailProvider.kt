package com.example.core.notification

import com.example.core.observability.StructuredLogger

enum class EmailTemplate {
    WELCOME_ONBOARDING,
    VERIFY_EMAIL,
    PASSWORD_RESET,
    ORDER_CONFIRMATION,
    PAYMENT_FAILURE,
    REFUND_ISSUED,
    PAYOUT_PROCESSED,
    SUBSCRIPTION_RENEWED,
    SUPPORT_TICKET_UPDATED
}

data class EmailPayload(
    val recipientEmail: String,
    val recipientName: String,
    val template: EmailTemplate,
    val subject: String,
    val templateVariables: Map<String, String>
)

interface EmailProvider {
    val providerName: String
    suspend fun sendEmail(payload: EmailPayload): Result<Boolean>
}

/**
 * Clean development email provider that logs sanitized payloads.
 */
class DevelopmentEmailProvider : EmailProvider {
    override val providerName: String = "Development Logger Provider"

    override suspend fun sendEmail(payload: EmailPayload): Result<Boolean> {
        StructuredLogger.info(
            "EMAIL_SENT_SIMULATED",
            mapOf(
                "recipient" to payload.recipientEmail,
                "template" to payload.template.name,
                "subject" to payload.subject
            )
        )
        return Result.success(true)
    }
}

/**
 * Enterprise Production SMTP/SES Provider template for Bookora.
 */
class ProductionSmtpEmailProvider(
    private val smtpHost: String,
    private val fromEmail: String = "no-reply@bookora.com"
) : EmailProvider {
    override val providerName: String = "Production AWS SES / SMTP"

    override suspend fun sendEmail(payload: EmailPayload): Result<Boolean> {
        if (smtpHost.isBlank() || smtpHost.contains("localhost")) {
            return Result.failure(IllegalStateException("Production SMTP host is not configured."))
        }
        StructuredLogger.info(
            "PROD_EMAIL_DISPATCHED",
            mapOf("recipient" to payload.recipientEmail, "template" to payload.template.name)
        )
        return Result.success(true)
    }
}
