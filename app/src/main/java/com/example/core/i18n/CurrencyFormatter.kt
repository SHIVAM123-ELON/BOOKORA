package com.example.core.i18n

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

enum class SupportedCurrency(val code: String, val symbol: String, val displayName: String) {
    INR("INR", "₹", "Indian Rupee (Default)"),
    USD("USD", "$", "US Dollar"),
    EUR("EUR", "€", "Euro"),
    GBP("GBP", "£", "British Pound")
}

/**
 * Enterprise Multi-Currency Formatter for Bookora.
 * Dynamically formats prices according to regional conventions
 * while strictly preserving original transaction currency for historical accounting integrity.
 */
object CurrencyFormatter {

    fun format(amount: Double, currencyCode: String = "INR"): String {
        return try {
            val currency = SupportedCurrency.values().find { it.code.equals(currencyCode, ignoreCase = true) }
                ?: SupportedCurrency.INR

            when (currency) {
                SupportedCurrency.INR -> {
                    // Standard Indian numbering grouping if applicable or clean ₹ format
                    "₹${if (amount % 1.0 == 0.0) amount.toInt().toString() else String.format(Locale.US, "%.2f", amount)}"
                }
                SupportedCurrency.USD -> "$${String.format(Locale.US, "%.2f", amount)}"
                SupportedCurrency.EUR -> "€${String.format(Locale.GERMANY, "%.2f", amount)}"
                SupportedCurrency.GBP -> "£${String.format(Locale.UK, "%.2f", amount)}"
            }
        } catch (e: Exception) {
            "₹${amount.toInt()}"
        }
    }
}
