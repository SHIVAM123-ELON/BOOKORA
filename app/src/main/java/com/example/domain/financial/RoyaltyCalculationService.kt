package com.example.domain.financial

import com.example.domain.model.financial.MarketplaceSettings
import com.example.domain.model.financial.Money
import com.example.domain.model.financial.OrderItem
import com.example.domain.model.financial.RoyaltyLedger
import com.example.domain.model.financial.RoyaltyStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Deterministic, server-authoritative service for calculating author royalties and platform commission.
 * Preserves historical calculations on ledger entries.
 */
class RoyaltyCalculationService(
    private val marketplaceSettings: MarketplaceSettings = MarketplaceSettings()
) {

    data class RoyaltyBreakdown(
        val orderItemId: String,
        val authorId: String,
        val bookTitleSnapshot: String,
        val grossAmountMinor: Long,
        val discountMinor: Long,
        val platformFeeMinor: Long,
        val taxMinor: Long,
        val authorRoyaltyMinor: Long,
        val currency: String
    )

    /**
     * Calculates royalty breakdown deterministically for an individual order item.
     */
    fun calculateItemRoyalty(
        orderItem: OrderItem,
        commissionRate: Double = marketplaceSettings.defaultPlatformCommissionRate,
        taxRate: Double = marketplaceSettings.defaultTaxRate,
        currency: String = "INR"
    ): RoyaltyBreakdown {
        val gross = orderItem.finalPriceMinor.coerceAtLeast(0L)
        val discount = orderItem.discountMinor.coerceAtLeast(0L)

        // Platform fee calculation using BigDecimal for zero rounding error
        val platformFee = BigDecimal.valueOf(gross)
            .multiply(BigDecimal.valueOf(commissionRate))
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()

        // Tax calculation if applicable
        val tax = BigDecimal.valueOf(gross)
            .multiply(BigDecimal.valueOf(taxRate))
            .setScale(0, RoundingMode.HALF_EVEN)
            .toLong()

        // Author royalty is the net remainder
        val authorShare = (gross - platformFee - tax).coerceAtLeast(0L)

        return RoyaltyBreakdown(
            orderItemId = orderItem.id,
            authorId = orderItem.sellerId,
            bookTitleSnapshot = orderItem.titleSnapshot,
            grossAmountMinor = gross,
            discountMinor = discount,
            platformFeeMinor = platformFee,
            taxMinor = tax,
            authorRoyaltyMinor = authorShare,
            currency = currency
        )
    }

    /**
     * Generates immutable RoyaltyLedger entries for an entire order.
     */
    fun generateLedgerEntries(
        orderId: String,
        items: List<OrderItem>,
        commissionRate: Double = marketplaceSettings.defaultPlatformCommissionRate,
        currency: String = "INR"
    ): List<RoyaltyLedger> {
        return items.map { item ->
            val breakdown = calculateItemRoyalty(item, commissionRate, marketplaceSettings.defaultTaxRate, currency)
            RoyaltyLedger(
                id = "royalty_${UUID.randomUUID().toString().take(12)}",
                authorId = item.sellerId,
                orderId = orderId,
                orderItemId = item.id,
                bookTitleSnapshot = item.titleSnapshot,
                grossAmountMinor = breakdown.grossAmountMinor,
                discountMinor = breakdown.discountMinor,
                refundMinor = 0L,
                platformFeeMinor = breakdown.platformFeeMinor,
                taxMinor = breakdown.taxMinor,
                royaltyAmountMinor = breakdown.authorRoyaltyMinor,
                currency = currency,
                status = RoyaltyStatus.AVAILABLE,
                createdAt = System.currentTimeMillis()
            )
        }
    }

    /**
     * Reverses royalty entries upon a completed refund.
     */
    fun createReversalEntry(original: RoyaltyLedger, refundAmountMinor: Long): RoyaltyLedger {
        val reversedShare = if (original.grossAmountMinor > 0) {
            (original.royaltyAmountMinor * refundAmountMinor) / original.grossAmountMinor
        } else 0L

        return original.copy(
            id = "royalty_rev_${UUID.randomUUID().toString().take(10)}",
            status = RoyaltyStatus.REVERSED,
            refundMinor = refundAmountMinor,
            royaltyAmountMinor = -reversedShare,
            createdAt = System.currentTimeMillis()
        )
    }
}
