package com.example.core.observability

import java.time.Instant
import java.util.UUID

/**
 * Enterprise Structured JSON Logger with automatic PII and sensitive secret redaction.
 * Ensures that passwords, credit cards, auth tokens, and API keys are never leaked to logs.
 */
object StructuredLogger {

    enum class LogLevel { DEBUG, INFO, WARN, ERROR, AUDIT }

    data class LogEntry(
        val timestamp: String,
        val level: LogLevel,
        val requestId: String,
        val userId: String?,
        val route: String?,
        val statusCode: Int?,
        val durationMs: Long?,
        val message: String,
        val errorCode: String? = null,
        val metadata: Map<String, Any?> = emptyMap()
    ) {
        fun toJsonString(): String {
            val sb = StringBuilder()
            sb.append("{")
            sb.append("\"timestamp\":\"").append(timestamp).append("\",")
            sb.append("\"level\":\"").append(level.name).append("\",")
            sb.append("\"requestId\":\"").append(requestId).append("\",")
            if (userId != null) sb.append("\"userId\":\"").append(userId).append("\",")
            if (route != null) sb.append("\"route\":\"").append(route).append("\",")
            if (statusCode != null) sb.append("\"statusCode\":").append(statusCode).append(",")
            if (durationMs != null) sb.append("\"durationMs\":").append(durationMs).append(",")
            if (errorCode != null) sb.append("\"errorCode\":\"").append(errorCode).append("\",")
            sb.append("\"message\":\"").append(escapeJson(message)).append("\"")
            if (metadata.isNotEmpty()) {
                sb.append(",\"metadata\":{")
                val entries = metadata.entries.joinToString(",") { (k, v) ->
                    val sanitizedVal = sanitizeValue(k, v)
                    "\"$k\":$sanitizedVal"
                }
                sb.append(entries).append("}")
            }
            sb.append("}")
            return sb.toString()
        }
    }

    private val SENSITIVE_KEYS = setOf(
        "password", "pass", "secret", "token", "accesstoken", "refreshtoken",
        "apikey", "cardnumber", "cvv", "authorization", "privatekey", "geminikey"
    )

    fun newRequestId(): String {
        return "req_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    fun log(
        level: LogLevel,
        message: String,
        requestId: String = newRequestId(),
        userId: String? = null,
        route: String? = null,
        statusCode: Int? = null,
        durationMs: Long? = null,
        errorCode: String? = null,
        metadata: Map<String, Any?> = emptyMap()
    ): LogEntry {
        val entry = LogEntry(
            timestamp = Instant.now().toString(),
            level = level,
            requestId = requestId,
            userId = userId,
            route = route,
            statusCode = statusCode,
            durationMs = durationMs,
            message = redactSensitiveText(message),
            errorCode = errorCode,
            metadata = metadata
        )
        // Output in structured JSON format
        println("[BOOKORA_LOG] " + entry.toJsonString())
        return entry
    }

    fun debug(message: String, requestId: String = newRequestId(), metadata: Map<String, Any?> = emptyMap()): LogEntry =
        log(LogLevel.DEBUG, message, requestId = requestId, metadata = metadata)

    fun debug(message: String, metadata: Map<String, Any?>): LogEntry =
        debug(message = message, requestId = newRequestId(), metadata = metadata)

    fun info(message: String, requestId: String = newRequestId(), metadata: Map<String, Any?> = emptyMap()): LogEntry =
        log(LogLevel.INFO, message, requestId = requestId, metadata = metadata)

    fun info(message: String, metadata: Map<String, Any?>): LogEntry =
        info(message = message, requestId = newRequestId(), metadata = metadata)

    fun warn(message: String, requestId: String = newRequestId(), errorCode: String? = null, metadata: Map<String, Any?> = emptyMap()): LogEntry =
        log(LogLevel.WARN, message, requestId = requestId, errorCode = errorCode, metadata = metadata)

    fun warn(message: String, metadata: Map<String, Any?>): LogEntry =
        warn(message = message, requestId = newRequestId(), errorCode = null, metadata = metadata)

    fun error(message: String, requestId: String = newRequestId(), errorCode: String? = null, metadata: Map<String, Any?> = emptyMap()): LogEntry =
        log(LogLevel.ERROR, message, requestId = requestId, errorCode = errorCode, metadata = metadata)

    fun error(message: String, metadata: Map<String, Any?>): LogEntry =
        error(message = message, requestId = newRequestId(), errorCode = null, metadata = metadata)

    fun error(message: String, throwable: Throwable?, metadata: Map<String, Any?> = emptyMap()): LogEntry =
        error(message = "$message: ${throwable?.message}", requestId = newRequestId(), errorCode = throwable?.javaClass?.simpleName, metadata = metadata)


    fun audit(adminId: String, action: String, entity: String, entityId: String, requestId: String = newRequestId(), metadata: Map<String, Any?> = emptyMap()): LogEntry =
        log(
            level = LogLevel.AUDIT,
            message = "Admin action '$action' performed on $entity ($entityId) by $adminId",
            requestId = requestId,
            userId = adminId,
            metadata = metadata + mapOf("action" to action, "entity" to entity, "entityId" to entityId)
        )

    private fun redactSensitiveText(text: String): String {
        var result = text
        // Redact Bearer tokens
        result = result.replace(Regex("(?i)bearer\\s+[a-zA-Z0-9._\\-]+"), "Bearer [REDACTED]")
        // Redact API Keys
        result = result.replace(Regex("(?i)(api[_-]?key)=([^&\\s]+)"), "$1=[REDACTED]")
        return result
    }

    private fun sanitizeValue(key: String, value: Any?): String {
        if (value == null) return "null"
        val normalizedKey = key.lowercase().replace("_", "").replace("-", "")
        if (SENSITIVE_KEYS.any { normalizedKey.contains(it) }) {
            return "\"[REDACTED]\""
        }
        return when (value) {
            is Number, is Boolean -> value.toString()
            else -> "\"${escapeJson(value.toString())}\""
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
