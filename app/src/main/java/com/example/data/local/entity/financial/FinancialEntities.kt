package com.example.data.local.entity.financial

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.financial.*

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val currency: String = "INR",
    val subtotalMinor: Long,
    val discountMinor: Long = 0L,
    val taxMinor: Long = 0L,
    val totalMinor: Long,
    val status: String,
    val couponId: String? = null,
    val paymentId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val bookId: String,
    val sellerId: String,
    val titleSnapshot: String,
    val coverUrlSnapshot: String = "",
    val priceMinor: Long,
    val discountMinor: Long = 0L,
    val finalPriceMinor: Long,
    val quantity: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): OrderItem = OrderItem(
        id = id,
        orderId = orderId,
        bookId = bookId,
        sellerId = sellerId,
        titleSnapshot = titleSnapshot,
        coverUrlSnapshot = coverUrlSnapshot,
        priceMinor = priceMinor,
        discountMinor = discountMinor,
        finalPriceMinor = finalPriceMinor,
        quantity = quantity,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: OrderItem): OrderItemEntity = OrderItemEntity(
            id = item.id,
            orderId = item.orderId,
            bookId = item.bookId,
            sellerId = item.sellerId,
            titleSnapshot = item.titleSnapshot,
            coverUrlSnapshot = item.coverUrlSnapshot,
            priceMinor = item.priceMinor,
            discountMinor = item.discountMinor,
            finalPriceMinor = item.finalPriceMinor,
            quantity = item.quantity,
            createdAt = item.createdAt
        )
    }
}

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val userId: String,
    val provider: String,
    val providerPaymentId: String?,
    val providerOrderId: String?,
    val amountMinor: Long,
    val currency: String = "INR",
    val status: String,
    val paymentMethod: String = "UPI / Card",
    val failureCode: String? = null,
    val failureMessage: String? = null,
    val verifiedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Payment = Payment(
        id = id,
        orderId = orderId,
        userId = userId,
        provider = provider,
        providerPaymentId = providerPaymentId,
        providerOrderId = providerOrderId,
        amountMinor = amountMinor,
        currency = currency,
        status = try { PaymentStatus.valueOf(status) } catch (e: Exception) { PaymentStatus.PENDING },
        paymentMethod = paymentMethod,
        failureCode = failureCode,
        failureMessage = failureMessage,
        verifiedAt = verifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(p: Payment): PaymentEntity = PaymentEntity(
            id = p.id,
            orderId = p.orderId,
            userId = p.userId,
            provider = p.provider,
            providerPaymentId = p.providerPaymentId,
            providerOrderId = p.providerOrderId,
            amountMinor = p.amountMinor,
            currency = p.currency,
            status = p.status.name,
            paymentMethod = p.paymentMethod,
            failureCode = p.failureCode,
            failureMessage = p.failureMessage,
            verifiedAt = p.verifiedAt,
            createdAt = p.createdAt,
            updatedAt = p.updatedAt
        )
    }
}

@Entity(tableName = "payment_webhook_events")
data class PaymentWebhookEventEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val eventId: String,
    val eventType: String,
    val payloadHash: String,
    val processed: Boolean = false,
    val processedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "entitlements")
data class EntitlementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val source: String,
    val orderId: String? = null,
    val subscriptionId: String? = null,
    val status: String,
    val grantedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val revokedAt: Long? = null
) {
    fun toDomain(): Entitlement = Entitlement(
        id = id,
        userId = userId,
        bookId = bookId,
        source = try { EntitlementSource.valueOf(source) } catch (e: Exception) { EntitlementSource.PURCHASE },
        orderId = orderId,
        subscriptionId = subscriptionId,
        status = try { EntitlementStatus.valueOf(status) } catch (e: Exception) { EntitlementStatus.ACTIVE },
        grantedAt = grantedAt,
        expiresAt = expiresAt,
        revokedAt = revokedAt
    )
}

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val id: String,
    val cartId: String,
    val userId: String,
    val bookId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val code: String,
    val type: String,
    val value: Long,
    val maxDiscountMinor: Long? = null,
    val minimumOrderMinor: Long = 0L,
    val usageLimit: Int = 1000,
    val perUserLimit: Int = 1,
    val timesUsed: Int = 0,
    val startsAt: Long = 0L,
    val expiresAt: Long = Long.MAX_VALUE,
    val applicableBookIds: String = "",
    val applicableCategoryIds: String = "",
    val applicableAuthorIds: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Coupon = Coupon(
        id = id,
        code = code,
        type = try { CouponType.valueOf(type) } catch (e: Exception) { CouponType.PERCENTAGE },
        value = value,
        maxDiscountMinor = maxDiscountMinor,
        minimumOrderMinor = minimumOrderMinor,
        usageLimit = usageLimit,
        perUserLimit = perUserLimit,
        timesUsed = timesUsed,
        startsAt = startsAt,
        expiresAt = expiresAt,
        applicableBookIds = if (applicableBookIds.isBlank()) emptyList() else applicableBookIds.split(","),
        applicableCategoryIds = if (applicableCategoryIds.isBlank()) emptyList() else applicableCategoryIds.split(","),
        applicableAuthorIds = if (applicableAuthorIds.isBlank()) emptyList() else applicableAuthorIds.split(","),
        status = try { CouponStatus.valueOf(status) } catch (e: Exception) { CouponStatus.ACTIVE },
        createdAt = createdAt
    )
}

@Entity(tableName = "bundles")
data class BundleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val priceMinor: Long,
    val originalPriceMinor: Long,
    val currency: String = "INR",
    val bookIds: String,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Bundle = Bundle(
        id = id,
        title = title,
        description = description,
        coverUrl = coverUrl,
        priceMinor = priceMinor,
        originalPriceMinor = originalPriceMinor,
        currency = currency,
        bookIds = if (bookIds.isBlank()) emptyList() else bookIds.split(","),
        status = status,
        createdAt = createdAt
    )
}

@Entity(tableName = "royalty_ledgers")
data class RoyaltyLedgerEntity(
    @PrimaryKey val id: String,
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
    val status: String = "AVAILABLE",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): RoyaltyLedger = RoyaltyLedger(
        id = id,
        authorId = authorId,
        orderId = orderId,
        orderItemId = orderItemId,
        bookTitleSnapshot = bookTitleSnapshot,
        grossAmountMinor = grossAmountMinor,
        discountMinor = discountMinor,
        refundMinor = refundMinor,
        platformFeeMinor = platformFeeMinor,
        taxMinor = taxMinor,
        royaltyAmountMinor = royaltyAmountMinor,
        currency = currency,
        status = try { RoyaltyStatus.valueOf(status) } catch (e: Exception) { RoyaltyStatus.AVAILABLE },
        createdAt = createdAt
    )
}

@Entity(tableName = "author_wallets")
data class AuthorWalletEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val availableBalanceMinor: Long = 0L,
    val pendingBalanceMinor: Long = 0L,
    val lifetimeEarnedMinor: Long = 0L,
    val lifetimePaidMinor: Long = 0L,
    val currency: String = "INR",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): AuthorWallet = AuthorWallet(
        id = id,
        authorId = authorId,
        availableBalanceMinor = availableBalanceMinor,
        pendingBalanceMinor = pendingBalanceMinor,
        lifetimeEarnedMinor = lifetimeEarnedMinor,
        lifetimePaidMinor = lifetimePaidMinor,
        currency = currency,
        updatedAt = updatedAt
    )
}

@Entity(tableName = "payout_requests")
data class PayoutRequestEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val amountMinor: Long,
    val currency: String = "INR",
    val status: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val providerReference: String? = null,
    val payoutAccountMasked: String = "•••• 4821 (UPI/Bank)"
) {
    fun toDomain(): PayoutRequest = PayoutRequest(
        id = id,
        authorId = authorId,
        amountMinor = amountMinor,
        currency = currency,
        status = try { PayoutStatus.valueOf(status) } catch (e: Exception) { PayoutStatus.REQUESTED },
        requestedAt = requestedAt,
        reviewedAt = reviewedAt,
        reviewedBy = reviewedBy,
        rejectionReason = rejectionReason,
        providerReference = providerReference,
        payoutAccountMasked = payoutAccountMasked
    )
}

@Entity(tableName = "author_verifications")
data class AuthorVerificationEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val status: String,
    val verificationProvider: String = "BOOKORA_KYC_SECURE",
    val providerReference: String? = null,
    val verifiedAt: Long? = null,
    val rejectionReason: String? = null
)

@Entity(tableName = "refunds")
data class RefundEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val paymentId: String,
    val amountMinor: Long,
    val reason: String,
    val status: String,
    val providerReference: String? = null,
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null
) {
    fun toDomain(): Refund = Refund(
        id = id,
        orderId = orderId,
        paymentId = paymentId,
        amountMinor = amountMinor,
        reason = reason,
        status = try { RefundStatus.valueOf(status) } catch (e: Exception) { RefundStatus.REQUESTED },
        providerReference = providerReference,
        requestedAt = requestedAt,
        processedAt = processedAt
    )
}

@Entity(tableName = "subscription_plans")
data class SubscriptionPlanEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val priceMinor: Long,
    val currency: String = "INR",
    val billingPeriod: String = "MONTHLY",
    val status: String = "ACTIVE",
    val features: String = ""
) {
    fun toDomain(): SubscriptionPlan = SubscriptionPlan(
        id = id,
        name = name,
        description = description,
        priceMinor = priceMinor,
        currency = currency,
        billingPeriod = try { BillingPeriod.valueOf(billingPeriod) } catch (e: Exception) { BillingPeriod.MONTHLY },
        status = status,
        features = if (features.isBlank()) emptyList() else features.split(";;;")
    )
}

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val planId: String,
    val planName: String = "Bookora Unlimited",
    val provider: String = "BOOKORA_SUB",
    val providerSubscriptionId: String? = null,
    val status: String,
    val startedAt: Long = System.currentTimeMillis(),
    val currentPeriodStart: Long = System.currentTimeMillis(),
    val currentPeriodEnd: Long = System.currentTimeMillis(),
    val cancelledAt: Long? = null
) {
    fun toDomain(): Subscription = Subscription(
        id = id,
        userId = userId,
        planId = planId,
        planName = planName,
        provider = provider,
        providerSubscriptionId = providerSubscriptionId,
        status = try { SubscriptionStatus.valueOf(status) } catch (e: Exception) { SubscriptionStatus.ACTIVE },
        startedAt = startedAt,
        currentPeriodStart = currentPeriodStart,
        currentPeriodEnd = currentPeriodEnd,
        cancelledAt = cancelledAt
    )
}

@Entity(tableName = "financial_audit_logs")
data class FinancialAuditLogEntity(
    @PrimaryKey val id: String,
    val actor: String,
    val action: String,
    val entity: String,
    val entityId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "{}"
) {
    fun toDomain(): FinancialAuditLog = FinancialAuditLog(
        id = id,
        actor = actor,
        action = action,
        entity = entity,
        entityId = entityId,
        timestamp = timestamp,
        metadata = metadata
    )
}

@Entity(tableName = "risk_events")
data class RiskEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val severity: String,
    val referenceId: String? = null,
    val metadata: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): RiskEvent = RiskEvent(
        id = id,
        userId = userId,
        type = type,
        severity = try { RiskSeverity.valueOf(severity) } catch (e: Exception) { RiskSeverity.LOW },
        referenceId = referenceId,
        metadata = metadata,
        createdAt = createdAt
    )
}

@Entity(tableName = "marketplace_settings")
data class MarketplaceSettingsEntity(
    @PrimaryKey val id: String = "default_settings",
    val defaultPlatformCommissionRate: Double = 0.20,
    val minimumPayoutMinor: Long = 100000L,
    val payoutSchedule: String = "WEEKLY_ON_MONDAY",
    val defaultTaxRate: Double = 0.00,
    val refundEntitlementPolicy: String = "REFUND_REVOKES_ACCESS",
    val currency: String = "INR",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): MarketplaceSettings = MarketplaceSettings(
        id = id,
        defaultPlatformCommissionRate = defaultPlatformCommissionRate,
        minimumPayoutMinor = minimumPayoutMinor,
        payoutSchedule = payoutSchedule,
        defaultTaxRate = defaultTaxRate,
        refundEntitlementPolicy = refundEntitlementPolicy,
        currency = currency,
        updatedAt = updatedAt
    )
}
