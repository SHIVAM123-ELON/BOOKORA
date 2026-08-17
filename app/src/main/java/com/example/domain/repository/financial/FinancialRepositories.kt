package com.example.domain.repository.financial

import com.example.core.result.Resource
import com.example.domain.model.financial.*
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun createBuyNowOrder(
        userId: String,
        bookId: String,
        couponCode: String? = null,
        idempotencyKey: String? = null
    ): Resource<Order>

    suspend fun createCartOrder(
        userId: String,
        couponCode: String? = null,
        idempotencyKey: String? = null
    ): Resource<Order>

    fun getOrderById(orderId: String): Flow<Order?>
    fun getUserOrders(userId: String): Flow<List<Order>>
    fun getAllOrders(): Flow<List<Order>>
    suspend fun cancelOrder(orderId: String, reason: String): Resource<Order>
}

interface PaymentRepository {
    suspend fun initializePayment(
        orderId: String,
        userId: String,
        paymentMethod: String = "UPI_AUTO_PAY"
    ): Resource<PaymentInitiationResult>

    suspend fun verifyAndCapturePayment(
        orderId: String,
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): Resource<PaymentCaptureResultData>

    suspend fun processWebhookEvent(
        provider: String,
        eventId: String,
        eventType: String,
        payloadHash: String,
        orderId: String,
        paymentId: String
    ): Resource<Boolean>

    fun getPaymentByOrderId(orderId: String): Flow<Payment?>
    fun getAllPayments(): Flow<List<Payment>>
}

data class PaymentInitiationResult(
    val paymentId: String,
    val orderId: String,
    val providerOrderId: String,
    val clientToken: String,
    val amountMinor: Long,
    val currency: String,
    val provider: String
)

data class PaymentCaptureResultData(
    val isSuccess: Boolean,
    val orderId: String,
    val paymentId: String,
    val status: PaymentStatus,
    val message: String
)

interface EntitlementRepository {
    fun getUserEntitlements(userId: String): Flow<List<Entitlement>>
    fun isBookEntitled(userId: String, bookId: String): Flow<Boolean>
    suspend fun grantEntitlement(
        userId: String,
        bookId: String,
        source: EntitlementSource,
        orderId: String? = null,
        subscriptionId: String? = null
    ): Resource<Entitlement>

    suspend fun revokeEntitlement(userId: String, bookId: String, reason: String): Resource<Unit>
}

interface CartRepository {
    fun getCart(userId: String): Flow<Cart>
    suspend fun addToCart(userId: String, bookId: String): Resource<Cart>
    suspend fun removeFromCart(userId: String, bookId: String): Resource<Cart>
    suspend fun clearCart(userId: String): Resource<Unit>
    suspend fun calculateAuthoritativeCart(userId: String, couponCode: String? = null): Resource<CartCalculation>
}

data class CartCalculation(
    val items: List<OrderItem>,
    val subtotalMinor: Long,
    val discountMinor: Long,
    val taxMinor: Long,
    val totalMinor: Long,
    val currency: String = "INR",
    val appliedCoupon: Coupon? = null
)

interface CouponRepository {
    fun getActiveCoupons(): Flow<List<Coupon>>
    suspend fun validateCoupon(code: String, orderAmountMinor: Long, userId: String): Resource<Coupon>
    suspend fun createCoupon(coupon: Coupon): Resource<Coupon>
}

interface BundleRepository {
    fun getBundles(): Flow<List<Bundle>>
    fun getBundleById(bundleId: String): Flow<Bundle?>
    suspend fun purchaseBundle(userId: String, bundleId: String): Resource<Order>
}

interface RoyaltyRepository {
    fun getAuthorLedger(authorId: String): Flow<List<RoyaltyLedger>>
    fun getAllRoyaltyLedgers(): Flow<List<RoyaltyLedger>>
    fun getAuthorEarningsSummary(authorId: String): Flow<AuthorEarningsSummary>
}

data class AuthorEarningsSummary(
    val totalSalesCount: Int,
    val grossEarningsMinor: Long,
    val platformFeesMinor: Long,
    val netEarningsMinor: Long,
    val pendingBalanceMinor: Long,
    val availableBalanceMinor: Long,
    val lifetimePaidMinor: Long,
    val currency: String = "INR"
)

interface WalletRepository {
    fun getWallet(authorId: String): Flow<AuthorWallet?>
    suspend fun requestPayout(authorId: String, amountMinor: Long, payoutAccount: String): Resource<PayoutRequest>
    suspend fun recalculateWallet(authorId: String): Resource<AuthorWallet>
}

interface PayoutRepository {
    fun getAuthorPayoutRequests(authorId: String): Flow<List<PayoutRequest>>
    fun getAllPayoutRequests(): Flow<List<PayoutRequest>>
    suspend fun approvePayout(payoutId: String, adminId: String, providerReference: String): Resource<PayoutRequest>
    suspend fun rejectPayout(payoutId: String, adminId: String, reason: String): Resource<PayoutRequest>
}

interface RefundRepository {
    suspend fun requestRefund(orderId: String, userId: String, reason: String): Resource<Refund>
    suspend fun approveAndProcessRefund(refundId: String, adminId: String): Resource<Refund>
    fun getRefundsForOrder(orderId: String): Flow<List<Refund>>
    fun getAllRefunds(): Flow<List<Refund>>
}

interface SubscriptionRepository {
    fun getPlans(): Flow<List<SubscriptionPlan>>
    fun getUserSubscription(userId: String): Flow<Subscription?>
    suspend fun createSubscription(userId: String, planId: String): Resource<Subscription>
    suspend fun cancelSubscription(userId: String, subscriptionId: String): Resource<Subscription>
    fun isBookCoveredBySubscription(userId: String, bookId: String, categoryId: String): Flow<Boolean>
}

interface FinancialAdminRepository {
    fun getMarketplaceSettings(): Flow<MarketplaceSettings>
    suspend fun updateMarketplaceSettings(settings: MarketplaceSettings): Resource<MarketplaceSettings>
    fun getPlatformFinancialMetrics(): Flow<PlatformFinancialMetrics>
    fun getAuditLogs(): Flow<List<FinancialAuditLog>>
    fun getRiskEvents(): Flow<List<RiskEvent>>
}

data class PlatformFinancialMetrics(
    val grossSalesMinor: Long,
    val netSalesMinor: Long,
    val platformRevenueMinor: Long,
    val authorEarningsMinor: Long,
    val totalRefundsMinor: Long,
    val pendingPayoutsMinor: Long,
    val subscriptionRevenueMinor: Long,
    val totalOrdersCount: Int,
    val activeSubscriptionsCount: Int,
    val currency: String = "INR"
)
