package com.example.domain.model.financial

import com.example.domain.model.Book

// -------------------------------------------------------------
// ORDER MODELS
// -------------------------------------------------------------

enum class OrderStatus {
    PENDING,
    PAYMENT_PENDING,
    PAID,
    PROCESSING,
    COMPLETED,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED,
    FAILED
}

data class Order(
    val id: String,
    val userId: String,
    val currency: String = "INR",
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long,
    val status: OrderStatus = OrderStatus.PENDING,
    val couponId: String? = null,
    val items: List<OrderItem> = emptyList(),
    val paymentId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val subtotalMoney: Money get() = Money(subtotalMinor, currency)
    val discountMoney: Money get() = Money(discountMinor, currency)
    val taxMoney: Money get() = Money(taxMinor, currency)
    val totalMoney: Money get() = Money(totalMinor, currency)
    val isPaid: Boolean get() = status == OrderStatus.PAID || status == OrderStatus.COMPLETED
}

data class OrderItem(
    val id: String,
    val orderId: String,
    val bookId: String,
    val sellerId: String, // Author or Publisher ID
    val titleSnapshot: String,
    val coverUrlSnapshot: String = "",
    val priceMinor: Long,
    val discountMinor: Long = 0L,
    val finalPriceMinor: Long,
    val quantity: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    val priceMoney: Money get() = Money(priceMinor)
    val discountMoney: Money get() = Money(discountMinor)
    val finalPriceMoney: Money get() = Money(finalPriceMinor)
}

// -------------------------------------------------------------
// PAYMENT MODELS
// -------------------------------------------------------------

enum class PaymentStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    CANCELLED,
    REFUNDED,
    PARTIALLY_REFUNDED
}

enum class PaymentProviderType {
    DEVELOPMENT, // Development simulation only
    RAZORPAY,    // India First Production Gateway
    STRIPE,
    UPI_INTENT
}

data class Payment(
    val id: String,
    val orderId: String,
    val userId: String,
    val provider: String,
    val providerPaymentId: String? = null,
    val providerOrderId: String? = null,
    val amountMinor: Long,
    val currency: String = "INR",
    val status: PaymentStatus = PaymentStatus.CREATED,
    val paymentMethod: String = "UPI / Card",
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val verifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val amountMoney: Money get() = Money(amountMinor, currency)
    val isSuccessful: Boolean get() = status == PaymentStatus.CAPTURED
}

data class PaymentWebhookEvent(
    val id: String,
    val provider: String,
    val eventId: String,
    val eventType: String,
    val payloadHash: String,
    val processed: Boolean = false,
    val processedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// ENTITLEMENT MODELS
// -------------------------------------------------------------

enum class EntitlementSource {
    PURCHASE,
    SUBSCRIPTION,
    BUNDLE,
    PROMOTION,
    ADMIN_GRANT
}

enum class EntitlementStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

data class Entitlement(
    val id: String,
    val userId: String,
    val bookId: String,
    val source: EntitlementSource,
    val orderId: String? = null,
    val subscriptionId: String? = null,
    val status: EntitlementStatus = EntitlementStatus.ACTIVE,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val revokedAt: Long? = null
) {
    val isActive: Boolean
        get() = status == EntitlementStatus.ACTIVE && (expiresAt == null || expiresAt > System.currentTimeMillis())
}

// -------------------------------------------------------------
// CART MODELS
// -------------------------------------------------------------

data class Cart(
    val id: String,
    val userId: String,
    val items: List<CartItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val itemCount: Int get() = items.size
}

data class CartItem(
    val id: String,
    val cartId: String,
    val bookId: String,
    val book: Book? = null,
    val addedAt: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// COUPONS & PROMOTIONS
// -------------------------------------------------------------

enum class CouponType {
    PERCENTAGE,
    FIXED_AMOUNT
}

enum class CouponStatus {
    ACTIVE,
    EXPIRED,
    DISABLED
}

data class Coupon(
    val id: String,
    val code: String,
    val type: CouponType,
    val value: Long, // Percentage (e.g. 20 for 20%) or Fixed Minor units (e.g. 10000 for ₹100)
    val maxDiscountMinor: Long? = null,
    val minimumOrderMinor: Long = 0L,
    val usageLimit: Int = 1000,
    val perUserLimit: Int = 1,
    val timesUsed: Int = 0,
    val startsAt: Long = 0L,
    val expiresAt: Long = Long.MAX_VALUE,
    val applicableBookIds: List<String> = emptyList(),
    val applicableCategoryIds: List<String> = emptyList(),
    val applicableAuthorIds: List<String> = emptyList(),
    val status: CouponStatus = CouponStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isValid(orderAmountMinor: Long, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (status != CouponStatus.ACTIVE) return false
        if (currentTime < startsAt || currentTime > expiresAt) return false
        if (timesUsed >= usageLimit) return false
        if (orderAmountMinor < minimumOrderMinor) return false
        return true
    }

    fun calculateDiscount(orderAmountMinor: Long): Long {
        if (!isValid(orderAmountMinor)) return 0L
        val rawDiscount = when (type) {
            CouponType.PERCENTAGE -> (orderAmountMinor * value) / 100L
            CouponType.FIXED_AMOUNT -> value
        }
        val maxDiscount = maxDiscountMinor ?: Long.MAX_VALUE
        return rawDiscount.coerceAtMost(maxDiscount).coerceAtMost(orderAmountMinor)
    }
}

enum class PromotionType {
    BOOK_DISCOUNT,
    CATEGORY_DISCOUNT,
    AUTHOR_PROMOTION,
    FEATURED_DEAL,
    FLASH_SALE
}

data class Promotion(
    val id: String,
    val name: String,
    val description: String,
    val type: PromotionType,
    val discountPercentage: Int = 0,
    val startsAt: Long,
    val endsAt: Long,
    val targetIds: List<String> = emptyList(),
    val status: String = "ACTIVE"
)

// -------------------------------------------------------------
// BOOK BUNDLES
// -------------------------------------------------------------

data class Bundle(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val priceMinor: Long,
    val originalPriceMinor: Long,
    val currency: String = "INR",
    val bookIds: List<String> = emptyList(),
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    val priceMoney: Money get() = Money(priceMinor, currency)
    val originalPriceMoney: Money get() = Money(originalPriceMinor, currency)
    val savingsMinor: Long get() = (originalPriceMinor - priceMinor).coerceAtLeast(0L)
    val savingsMoney: Money get() = Money(savingsMinor, currency)
}

// -------------------------------------------------------------
// ROYALTY & COMMISSION
// -------------------------------------------------------------

enum class RoyaltyStatus {
    PENDING,
    AVAILABLE,
    PAID,
    REVERSED
}

data class RoyaltyLedger(
    val id: String,
    val authorId: String,
    val orderId: String,
    val orderItemId: String,
    val bookTitleSnapshot: String,
    val grossAmountMinor: Long,
    val discountMinor: Long = 0L,
    val refundMinor: Long = 0L,
    val platformFeeMinor: Long,
    val taxMinor: Long = 0L,
    val royaltyAmountMinor: Long,
    val currency: String = "INR",
    val status: RoyaltyStatus = RoyaltyStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis()
) {
    val grossMoney: Money get() = Money(grossAmountMinor, currency)
    val platformFeeMoney: Money get() = Money(platformFeeMinor, currency)
    val royaltyMoney: Money get() = Money(royaltyAmountMinor, currency)
}

data class MarketplaceSettings(
    val id: String = "default_settings",
    val defaultPlatformCommissionRate: Double = 0.20, // 20% platform share
    val minimumPayoutMinor: Long = 100000L, // ₹1,000.00
    val payoutSchedule: String = "WEEKLY_ON_MONDAY",
    val defaultTaxRate: Double = 0.00, // Configurable tax
    val refundEntitlementPolicy: String = "REFUND_REVOKES_ACCESS",
    val currency: String = "INR",
    val updatedAt: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// AUTHOR WALLET & PAYOUT
// -------------------------------------------------------------

data class AuthorWallet(
    val id: String,
    val authorId: String,
    val availableBalanceMinor: Long = 0L,
    val pendingBalanceMinor: Long = 0L,
    val lifetimeEarnedMinor: Long = 0L,
    val lifetimePaidMinor: Long = 0L,
    val currency: String = "INR",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val availableMoney: Money get() = Money(availableBalanceMinor, currency)
    val pendingMoney: Money get() = Money(pendingBalanceMinor, currency)
    val lifetimeEarnedMoney: Money get() = Money(lifetimeEarnedMinor, currency)
    val lifetimePaidMoney: Money get() = Money(lifetimePaidMinor, currency)
}

enum class PayoutStatus {
    REQUESTED,
    UNDER_REVIEW,
    APPROVED,
    PROCESSING,
    PAID,
    REJECTED,
    FAILED
}

data class PayoutRequest(
    val id: String,
    val authorId: String,
    val amountMinor: Long,
    val currency: String = "INR",
    val status: PayoutStatus = PayoutStatus.REQUESTED,
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val providerReference: String? = null,
    val payoutAccountMasked: String = "•••• 4821 (UPI/Bank)"
) {
    val amountMoney: Money get() = Money(amountMinor, currency)
}

enum class VerificationStatus {
    NOT_STARTED,
    PENDING,
    VERIFIED,
    REJECTED
}

data class AuthorVerification(
    val id: String,
    val authorId: String,
    val status: VerificationStatus = VerificationStatus.NOT_STARTED,
    val verificationProvider: String = "BOOKORA_KYC_SECURE",
    val providerReference: String? = null,
    val verifiedAt: Long? = null,
    val rejectionReason: String? = null
)

// -------------------------------------------------------------
// REFUNDS
// -------------------------------------------------------------

enum class RefundStatus {
    REQUESTED,
    APPROVED,
    PROCESSING,
    COMPLETED,
    FAILED,
    REJECTED
}

data class Refund(
    val id: String,
    val orderId: String,
    val paymentId: String,
    val amountMinor: Long,
    val reason: String,
    val status: RefundStatus = RefundStatus.REQUESTED,
    val providerReference: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
) {
    val amountMoney: Money get() = Money(amountMinor)
}

// -------------------------------------------------------------
// SUBSCRIPTIONS
// -------------------------------------------------------------

enum class BillingPeriod {
    MONTHLY,
    YEARLY
}

enum class SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELLED,
    EXPIRED
}

data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String,
    val priceMinor: Long,
    val currency: String = "INR",
    val billingPeriod: BillingPeriod = BillingPeriod.MONTHLY,
    val status: String = "ACTIVE",
    val features: List<String> = emptyList()
) {
    val priceMoney: Money get() = Money(priceMinor, currency)
}

data class Subscription(
    val id: String,
    val userId: String,
    val planId: String,
    val planName: String = "Bookora Unlimited",
    val provider: String = "BOOKORA_SUB",
    val providerSubscriptionId: String? = null,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    val startedAt: Long = System.currentTimeMillis(),
    val currentPeriodStart: Long = System.currentTimeMillis(),
    val currentPeriodEnd: Long = System.currentTimeMillis() + (30L * 24L * 60L * 60L * 1000L),
    val cancelledAt: Long? = null
) {
    val isActive: Boolean
        get() = (status == SubscriptionStatus.ACTIVE || status == SubscriptionStatus.TRIALING) &&
                currentPeriodEnd > System.currentTimeMillis()
}

data class SubscriptionCatalogRule(
    val id: String,
    val planId: String,
    val includedCategoryIds: List<String> = emptyList(),
    val includedBookIds: List<String> = emptyList(),
    val allowAllNonPremium: Boolean = true
)

// -------------------------------------------------------------
// AUDIT LOG & RISK EVENTS
// -------------------------------------------------------------

data class FinancialAuditLog(
    val id: String,
    val actor: String, // userId or "SYSTEM" or "ADMIN"
    val action: String, // e.g. "ORDER_CREATED", "PAYMENT_CAPTURED"
    val entity: String, // "ORDER", "PAYMENT", "ROYALTY", "PAYOUT"
    val entityId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "{}"
)

enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class RiskEvent(
    val id: String,
    val userId: String,
    val type: String, // "DUPLICATE_PAYMENT", "RAPID_FAILED_PAYMENTS", "COUPON_ABUSE"
    val severity: RiskSeverity = RiskSeverity.LOW,
    val referenceId: String? = null,
    val metadata: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

// -------------------------------------------------------------
// TAX RULES
// -------------------------------------------------------------

data class TaxRule(
    val id: String,
    val country: String = "IN",
    val region: String? = null,
    val taxType: String = "GST",
    val rate: Double = 0.0, // 0.0 for digital books under exemption or configurable GST
    val effectiveFrom: Long = 0L,
    val effectiveTo: Long = Long.MAX_VALUE,
    val status: String = "ACTIVE"
)

// -------------------------------------------------------------
// PAYMENT LINKS (WHATSAPP, SMS, EMAIL, DIRECT)
// -------------------------------------------------------------

enum class PaymentLinkStatus {
    CREATED,
    SENT,
    PAID,
    EXPIRED,
    CANCELLED,
    FAILED
}

enum class PaymentLinkDeliveryMethod {
    WHATSAPP,
    SMS,
    EMAIL,
    COPY_LINK,
    DIRECT_LINK
}

data class PaymentLink(
    val id: String,
    val userId: String,
    val orderId: String,
    val razorpayPaymentLinkId: String,
    val paymentLinkUrl: String,
    val amountMinor: Long,
    val currency: String = "INR",
    val status: PaymentLinkStatus = PaymentLinkStatus.CREATED,
    val deliveryMethod: PaymentLinkDeliveryMethod = PaymentLinkDeliveryMethod.COPY_LINK,
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val booksSummary: String = "",
    val expiresAt: Long = System.currentTimeMillis() + (72 * 60 * 60 * 1000L), // 72 hours
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val amountMoney: Money get() = Money(amountMinor, currency)
    val amountMajor: Double get() = amountMinor / 100.0
    val isExpired: Boolean get() = status != PaymentLinkStatus.PAID && System.currentTimeMillis() > expiresAt
}


