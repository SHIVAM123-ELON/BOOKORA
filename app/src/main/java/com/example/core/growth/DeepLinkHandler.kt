package com.example.core.growth

import android.net.Uri
import com.example.core.observability.StructuredLogger

sealed class DeepLinkTarget {
    data class Book(val bookId: String) : DeepLinkTarget()
    data class Author(val authorId: String) : DeepLinkTarget()
    data class Promo(val promoId: String) : DeepLinkTarget()
    data class Bundle(val bundleId: String) : DeepLinkTarget()
    data class Subscription(val planId: String) : DeepLinkTarget()
    data class ReferralInvite(val code: String) : DeepLinkTarget()
    object Unknown : DeepLinkTarget()
}

/**
 * Enterprise Deep Link Parser and Router for Bookora.
 * Handles custom schemes (bookora://) and verified App Links (https://bookora.com).
 */
object DeepLinkHandler {

    private const val SCHEME_CUSTOM = "bookora"
    private const val HOST_HTTPS = "bookora.com"
    private const val HOST_WWW_HTTPS = "www.bookora.com"

    fun parse(uriString: String?): DeepLinkTarget {
        if (uriString.isNullOrBlank()) return DeepLinkTarget.Unknown

        try {
            val uri = Uri.parse(uriString)
            val scheme = uri.scheme?.lowercase() ?: return DeepLinkTarget.Unknown
            val host = uri.host?.lowercase() ?: ""
            val pathSegments = uri.pathSegments ?: emptyList()

            StructuredLogger.info(
                "DEEP_LINK_PARSED",
                metadata = mapOf("uri" to uriString, "scheme" to scheme, "host" to host)
            )

            if (scheme == SCHEME_CUSTOM) {
                return when (host) {
                    "book" -> {
                        val bookId = pathSegments.firstOrNull() ?: uri.getQueryParameter("id") ?: ""
                        if (bookId.isNotBlank()) DeepLinkTarget.Book(bookId) else DeepLinkTarget.Unknown
                    }
                    "author" -> {
                        val authorId = pathSegments.firstOrNull() ?: uri.getQueryParameter("id") ?: ""
                        if (authorId.isNotBlank()) DeepLinkTarget.Author(authorId) else DeepLinkTarget.Unknown
                    }
                    "promo" -> {
                        val promoId = pathSegments.firstOrNull() ?: uri.getQueryParameter("code") ?: ""
                        if (promoId.isNotBlank()) DeepLinkTarget.Promo(promoId) else DeepLinkTarget.Unknown
                    }
                    "bundle" -> {
                        val bundleId = pathSegments.firstOrNull() ?: uri.getQueryParameter("id") ?: ""
                        if (bundleId.isNotBlank()) DeepLinkTarget.Bundle(bundleId) else DeepLinkTarget.Unknown
                    }
                    "subscription" -> {
                        val planId = pathSegments.firstOrNull() ?: uri.getQueryParameter("plan") ?: ""
                        if (planId.isNotBlank()) DeepLinkTarget.Subscription(planId) else DeepLinkTarget.Unknown
                    }
                    "invite", "ref" -> {
                        val code = pathSegments.firstOrNull() ?: uri.getQueryParameter("code") ?: ""
                        if (code.isNotBlank()) DeepLinkTarget.ReferralInvite(code) else DeepLinkTarget.Unknown
                    }
                    else -> DeepLinkTarget.Unknown
                }
            }

            if ((scheme == "https" || scheme == "http") && (host == HOST_HTTPS || host == HOST_WWW_HTTPS)) {
                if (pathSegments.isEmpty()) return DeepLinkTarget.Unknown
                val firstSegment = pathSegments[0].lowercase()
                val param = if (pathSegments.size > 1) pathSegments[1] else ""

                return when (firstSegment) {
                    "books", "book" -> if (param.isNotBlank()) DeepLinkTarget.Book(param) else DeepLinkTarget.Unknown
                    "authors", "author" -> if (param.isNotBlank()) DeepLinkTarget.Author(param) else DeepLinkTarget.Unknown
                    "promos", "promo" -> if (param.isNotBlank()) DeepLinkTarget.Promo(param) else DeepLinkTarget.Unknown
                    "bundles", "bundle" -> if (param.isNotBlank()) DeepLinkTarget.Bundle(param) else DeepLinkTarget.Unknown
                    "subscriptions", "plans" -> if (param.isNotBlank()) DeepLinkTarget.Subscription(param) else DeepLinkTarget.Unknown
                    "ref", "invite" -> if (param.isNotBlank()) DeepLinkTarget.ReferralInvite(param) else DeepLinkTarget.Unknown
                    else -> DeepLinkTarget.Unknown
                }
            }

        } catch (e: Exception) {
            StructuredLogger.warn("DEEP_LINK_PARSE_ERROR", metadata = mapOf("uri" to uriString, "error" to (e.message ?: "unknown")))
        }

        return DeepLinkTarget.Unknown
    }
}
