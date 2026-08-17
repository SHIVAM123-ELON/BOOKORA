package com.example.core.i18n

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SupportedLanguage(val code: String, val displayName: String, val isRtl: Boolean = false) {
    ENGLISH("en", "English", false),
    HINDI("hi", "हिन्दी (Hindi)", false),
    SPANISH("es", "Español", false),
    FRENCH("fr", "Français", false),
    ARABIC("ar", "العربية (Arabic)", true)
}

/**
 * Localization Manager for Bookora.
 * Manages locale switches, date formatting, number formatting, and RTL readiness.
 */
object LocalizationManager {

    private var currentLanguage = SupportedLanguage.ENGLISH

    fun setLanguage(language: SupportedLanguage) {
        currentLanguage = language
    }

    fun getCurrentLanguage(): SupportedLanguage = currentLanguage

    fun isRtl(): Boolean = currentLanguage.isRtl

    fun formatDate(timestampMs: Long): String {
        val locale = when (currentLanguage) {
            SupportedLanguage.HINDI -> Locale("hi", "IN")
            SupportedLanguage.SPANISH -> Locale("es", "ES")
            SupportedLanguage.FRENCH -> Locale("fr", "FR")
            SupportedLanguage.ARABIC -> Locale("ar", "SA")
            else -> Locale.US
        }
        val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", locale)
        return formatter.format(Date(timestampMs))
    }
}
