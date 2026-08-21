package com.example.data.local.dao.financial

import androidx.room.*
import com.example.data.local.entity.financial.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM orders WHERE id = :orderId")
    fun getOrderById(orderId: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderByIdDirect(orderId: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsByOrderId(orderId: String): Flow<List<OrderItemEntity>>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsByOrderIdDirect(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY createdAt DESC")
    fun getOrdersByUserId(userId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    suspend fun getAllOrdersDirect(): List<OrderEntity>

    @Query("UPDATE orders SET status = :status, paymentId = :paymentId, updatedAt = :updatedAt WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String, paymentId: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM orders WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getOrderByIpdempotencyKey(key: String): OrderEntity?
}

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE providerPaymentId = :providerPaymentId LIMIT 1")
    suspend fun getPaymentByProviderPaymentId(providerPaymentId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE providerOrderId = :providerOrderId LIMIT 1")
    suspend fun getPaymentByProviderOrderId(providerOrderId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    fun getPaymentByOrderId(orderId: String): Flow<PaymentEntity?>

    @Query("SELECT * FROM payments WHERE orderId = :orderId")
    suspend fun getPaymentByOrderIdDirect(orderId: String): PaymentEntity?

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    suspend fun getAllPaymentsDirect(): List<PaymentEntity>

    @Query("UPDATE payments SET status = :status, providerPaymentId = :providerPaymentId, verifiedAt = :verifiedAt, updatedAt = :updatedAt WHERE id = :paymentId")
    suspend fun updatePaymentStatus(
        paymentId: String,
        status: String,
        providerPaymentId: String?,
        verifiedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface WebhookEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: PaymentWebhookEventEntity): Long

    @Query("SELECT * FROM payment_webhook_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventByEventId(eventId: String): PaymentWebhookEventEntity?

    @Query("UPDATE payment_webhook_events SET processed = 1, processedAt = :processedAt WHERE id = :id")
    suspend fun markProcessed(id: String, processedAt: Long = System.currentTimeMillis())
}

@Dao
interface EntitlementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntitlement(entitlement: EntitlementEntity)

    @Query("SELECT * FROM entitlements WHERE userId = :userId AND status = 'ACTIVE'")
    fun getUserEntitlements(userId: String): Flow<List<EntitlementEntity>>

    @Query("SELECT * FROM entitlements WHERE userId = :userId AND bookId = :bookId AND status = 'ACTIVE' LIMIT 1")
    fun getEntitlement(userId: String, bookId: String): Flow<EntitlementEntity?>

    @Query("SELECT * FROM entitlements WHERE userId = :userId AND bookId = :bookId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getEntitlementDirect(userId: String, bookId: String): EntitlementEntity?

    @Query("UPDATE entitlements SET status = 'REVOKED', revokedAt = :revokedAt WHERE userId = :userId AND bookId = :bookId")
    suspend fun revokeEntitlement(userId: String, bookId: String, revokedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM entitlements WHERE status = 'ACTIVE'")
    suspend fun getAllActiveEntitlementsDirect(): List<EntitlementEntity>
}

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItemEntity)

    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY addedAt DESC")
    fun getCartItemsByUserId(userId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY addedAt DESC")
    suspend fun getCartItemsByUserIdDirect(userId: String): List<CartItemEntity>

    @Query("DELETE FROM cart_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeCartItem(userId: String, bookId: String)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCart(userId: String)
}

@Dao
interface CouponDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Query("SELECT * FROM coupons WHERE code = :code LIMIT 1")
    suspend fun getCouponByCode(code: String): CouponEntity?

    @Query("SELECT * FROM coupons WHERE status = 'ACTIVE'")
    fun getActiveCoupons(): Flow<List<CouponEntity>>

    @Query("UPDATE coupons SET timesUsed = timesUsed + 1 WHERE id = :id")
    suspend fun incrementUsage(id: String)
}

@Dao
interface BundleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBundle(bundle: BundleEntity)

    @Query("SELECT * FROM bundles WHERE status = 'ACTIVE'")
    fun getActiveBundles(): Flow<List<BundleEntity>>

    @Query("SELECT * FROM bundles WHERE id = :bundleId LIMIT 1")
    fun getBundleById(bundleId: String): Flow<BundleEntity?>

    @Query("SELECT * FROM bundles WHERE id = :bundleId LIMIT 1")
    suspend fun getBundleByIdDirect(bundleId: String): BundleEntity?
}

@Dao
interface RoyaltyLedgerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<RoyaltyLedgerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: RoyaltyLedgerEntity)

    @Query("SELECT * FROM royalty_ledgers WHERE authorId = :authorId ORDER BY createdAt DESC")
    fun getLedgerByAuthorId(authorId: String): Flow<List<RoyaltyLedgerEntity>>

    @Query("SELECT * FROM royalty_ledgers WHERE authorId = :authorId ORDER BY createdAt DESC")
    suspend fun getLedgerByAuthorIdDirect(authorId: String): List<RoyaltyLedgerEntity>

    @Query("SELECT * FROM royalty_ledgers ORDER BY createdAt DESC")
    fun getAllLedgerEntries(): Flow<List<RoyaltyLedgerEntity>>

    @Query("SELECT * FROM royalty_ledgers WHERE orderId = :orderId")
    suspend fun getLedgerByOrderId(orderId: String): List<RoyaltyLedgerEntity>
}

@Dao
interface AuthorWalletDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: AuthorWalletEntity)

    @Query("SELECT * FROM author_wallets WHERE authorId = :authorId LIMIT 1")
    fun getWalletByAuthorId(authorId: String): Flow<AuthorWalletEntity?>

    @Query("SELECT * FROM author_wallets WHERE authorId = :authorId LIMIT 1")
    suspend fun getWalletByAuthorIdDirect(authorId: String): AuthorWalletEntity?
}

@Dao
interface PayoutRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutRequest(request: PayoutRequestEntity)

    @Query("SELECT * FROM payout_requests WHERE authorId = :authorId ORDER BY requestedAt DESC")
    fun getPayoutsByAuthorId(authorId: String): Flow<List<PayoutRequestEntity>>

    @Query("SELECT * FROM payout_requests ORDER BY requestedAt DESC")
    fun getAllPayoutRequests(): Flow<List<PayoutRequestEntity>>

    @Query("SELECT * FROM payout_requests WHERE id = :payoutId LIMIT 1")
    suspend fun getPayoutByIdDirect(payoutId: String): PayoutRequestEntity?

    @Query("UPDATE payout_requests SET status = :status, reviewedAt = :reviewedAt, reviewedBy = :reviewedBy, rejectionReason = :reason, providerReference = :ref WHERE id = :id")
    suspend fun updatePayoutStatus(
        id: String,
        status: String,
        reviewedAt: Long?,
        reviewedBy: String?,
        reason: String?,
        ref: String?
    )
}

@Dao
interface AuthorVerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: AuthorVerificationEntity)

    @Query("SELECT * FROM author_verifications WHERE authorId = :authorId LIMIT 1")
    fun getVerificationByAuthorId(authorId: String): Flow<AuthorVerificationEntity?>

    @Query("SELECT * FROM author_verifications WHERE authorId = :authorId LIMIT 1")
    suspend fun getVerificationByAuthorIdDirect(authorId: String): AuthorVerificationEntity?
}

@Dao
interface RefundDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRefund(refund: RefundEntity)

    @Query("SELECT * FROM refunds WHERE orderId = :orderId")
    fun getRefundsByOrderId(orderId: String): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds ORDER BY requestedAt DESC")
    fun getAllRefunds(): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds WHERE id = :refundId LIMIT 1")
    suspend fun getRefundByIdDirect(refundId: String): RefundEntity?

    @Query("UPDATE refunds SET status = :status, processedAt = :processedAt, providerReference = :ref WHERE id = :id")
    suspend fun updateRefundStatus(id: String, status: String, processedAt: Long?, ref: String?)
}

@Dao
interface SubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscription_plans WHERE status = 'ACTIVE'")
    fun getActivePlans(): Flow<List<SubscriptionPlanEntity>>

    @Query("SELECT * FROM subscription_plans WHERE id = :id LIMIT 1")
    suspend fun getPlanById(id: String): SubscriptionPlanEntity?

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND status IN ('ACTIVE', 'TRIALING') LIMIT 1")
    fun getActiveSubscriptionByUserId(userId: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE userId = :userId AND status IN ('ACTIVE', 'TRIALING') LIMIT 1")
    suspend fun getActiveSubscriptionByUserIdDirect(userId: String): SubscriptionEntity?

    @Query("UPDATE subscriptions SET status = 'CANCELLED', cancelledAt = :cancelledAt WHERE id = :id")
    suspend fun cancelSubscription(id: String, cancelledAt: Long = System.currentTimeMillis())
}

@Dao
interface FinancialAuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FinancialAuditLogEntity)

    @Query("SELECT * FROM financial_audit_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<FinancialAuditLogEntity>>
}

@Dao
interface RiskEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskEvent(event: RiskEventEntity)

    @Query("SELECT * FROM risk_events ORDER BY createdAt DESC LIMIT 100")
    fun getRecentRiskEvents(): Flow<List<RiskEventEntity>>
}

@Dao
interface MarketplaceSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: MarketplaceSettingsEntity)

    @Query("SELECT * FROM marketplace_settings WHERE id = 'default_settings' LIMIT 1")
    fun getSettings(): Flow<MarketplaceSettingsEntity?>

    @Query("SELECT * FROM marketplace_settings WHERE id = 'default_settings' LIMIT 1")
    suspend fun getSettingsDirect(): MarketplaceSettingsEntity?
}
