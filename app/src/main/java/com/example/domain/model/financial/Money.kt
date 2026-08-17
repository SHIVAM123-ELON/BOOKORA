package com.example.domain.model.financial

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Money representation using integer minor units (e.g., 49900 = ₹499.00).
 * Financial calculations are strictly performed without floating-point errors.
 */
data class Money(
    val amountMinor: Long,
    val currency: String = DEFAULT_CURRENCY
) : Comparable<Money> {

    init {
        require(currency.isNotBlank()) { "Currency code must not be blank" }
    }

    companion object {
        const val DEFAULT_CURRENCY = "INR"

        fun zero(currency: String = DEFAULT_CURRENCY): Money = Money(0L, currency)

        fun fromMajor(majorAmount: Double, currency: String = DEFAULT_CURRENCY): Money {
            val bd = BigDecimal.valueOf(majorAmount).setScale(2, RoundingMode.HALF_EVEN)
            val minor = bd.multiply(BigDecimal.valueOf(100)).toLong()
            return Money(minor, currency)
        }

        fun fromMajor(majorAmount: BigDecimal, currency: String = DEFAULT_CURRENCY): Money {
            val bd = majorAmount.setScale(2, RoundingMode.HALF_EVEN)
            val minor = bd.multiply(BigDecimal.valueOf(100)).toLong()
            return Money(minor, currency)
        }

        fun fromRupees(rupees: Long): Money = Money(rupees * 100L, "INR")
    }

    val amountMajor: BigDecimal
        get() = BigDecimal.valueOf(amountMinor).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_EVEN)

    val currencySymbol: String
        get() = when (currency.uppercase(Locale.US)) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> currency
        }

    val formatted: String
        get() {
            return try {
                val symbol = currencySymbol
                val major = amountMajor
                if (major.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    "$symbol${major.toBigInteger()}"
                } else {
                    String.format(Locale.US, "%s%.2f", symbol, major.toDouble())
                }
            } catch (e: Exception) {
                "$currencySymbol ${amountMinor / 100}"
            }
        }

    val formattedWithDecimals: String
        get() = String.format(Locale.US, "%s%.2f", currencySymbol, amountMajor.toDouble())

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency and ${other.currency}" }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract different currencies: $currency and ${other.currency}" }
        val result = amountMinor - other.amountMinor
        return Money(if (result < 0) 0L else result, currency)
    }

    operator fun times(factor: BigDecimal): Money {
        val bd = BigDecimal.valueOf(amountMinor).multiply(factor).setScale(0, RoundingMode.HALF_EVEN)
        return Money(bd.toLong(), currency)
    }

    operator fun times(quantity: Int): Money {
        return Money(amountMinor * quantity, currency)
    }

    fun applyPercentageDiscount(percentage: Int): Money {
        val clamped = percentage.coerceIn(0, 100)
        val discount = (amountMinor * clamped) / 100L
        return Money(amountMinor - discount, currency)
    }

    fun isZero(): Boolean = amountMinor == 0L
    fun isPositive(): Boolean = amountMinor > 0L

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare different currencies: $currency and ${other.currency}" }
        return amountMinor.compareTo(other.amountMinor)
    }
}
