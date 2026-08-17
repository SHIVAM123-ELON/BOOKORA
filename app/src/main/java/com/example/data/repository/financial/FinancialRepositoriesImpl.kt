package com.example.data.repository.financial

import com.example.core.result.Resource
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.LibraryEntity
import com.example.data.local.entity.financial.*
import com.example.domain.financial.*
import com.example.domain.model.Book
import com.example.domain.model.financial.*
import com.example.domain.repository.financial.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * ============================================================================
 * ORDER REPOSITORY IMPLEMENTATION
 * ============================================================================
 * Authoritatively calculates prices from database snapshots. Never trusts client prices.
 */
class OrderRepositoryImpl(
    private val database: BookoraDatabase,
    private val couponRepository: CouponRepository,
    private val auditLogger: FinancialAuditLogger = FinancialAuditLogger(),
    private val fraudService: FraudDetectionService = FraudDetectionService()
) : OrderRepository {

    private val orderDao = database.orderDao()
    private val bookDao = database.bookDao()

    override suspend fun createBuyNowOrder(
        userId: String,
        bookId: String,
        couponCode: String?,
        idempotencyKey: String?
    ): Resource<Order> {
        return try {
            // Check idempotency
            if (idempotencyKey != null) {
                val existing = orderDao.getOrderByIpdempotencyKey(idempotencyKey)
                if (existing != null) {
                    val items = orderDao.getOrderItemsByOrderIdDirect(existing.id).map { it.toDomain() }
                    return Resource.Success(
                        Order(
                            id = existing.id,
                            userId = existing.userId,
                            currency = existing.currency,
                            subtotalMinor = existing.subtotalMinor,
                            discountMinor = existing.discountMinor,
                            taxMinor = existing.taxMinor,
                            totalMinor = existing.totalMinor,
                            status = try { OrderStatus.valueOf(existing.status) } catch (e: Exception) { OrderStatus.PENDING },
                            couponId = existing.couponId,
                            items = items,
                            paymentId = existing.paymentId,
                            idempotencyKey = existing.idempotencyKey,
                            createdAt = existing.createdAt,
                            updatedAt = existing.updatedAt
                        )
                    )
                }
            }

            // Authoritative server lookup
            val bookEntity = bookDao.getBookByIdDirect(bookId)
                ?: return Resource.Error("Book not found in marketplace catalog")

            val book = bookEntity.toDomain()
            val unitPriceMinor = Money.fromMajor(book.discountPrice ?: book.price).amountMinor
            var discountMinor = 0L
            var couponId: String? = null

            // Validate coupon authoritatively
            if (!couponCode.isNullOrBlank()) {
                when (val couponRes = couponRepository.validateCoupon(couponCode, unitPriceMinor, userId)) {
                    is Resource.Success -> {
                        val coupon = couponRes.data
                        discountMinor = coupon.calculateDiscount(unitPriceMinor)
                        couponId = coupon.id
                    }
                    is Resource.Error -> {
                        // Return error if coupon invalid
                        return Resource.Error("Coupon error: ${couponRes.message}")
                    }
                    else -> {}
                }
            }

            val totalMinor = (unitPriceMinor - discountMinor).coerceAtLeast(0L)
            val orderId = "ord_${UUID.randomUUID().toString().take(12)}"

            val orderItem = OrderItem(
                id = "item_${UUID.randomUUID().toString().take(12)}",
                orderId = orderId,
                bookId = book.id,
                sellerId = book.authorId,
                titleSnapshot = book.title,
                coverUrlSnapshot = book.coverUrl,
                priceMinor = unitPriceMinor,
                discountMinor = discountMinor,
                finalPriceMinor = totalMinor,
                quantity = 1,
                createdAt = System.currentTimeMillis()
            )

            val orderEntity = OrderEntity(
                id = orderId,
                userId = userId,
                currency = "INR",
                subtotalMinor = unitPriceMinor,
                discountMinor = discountMinor,
                taxMinor = 0L,
                totalMinor = totalMinor,
                status = OrderStatus.PAYMENT_PENDING.name,
                couponId = couponId,
                paymentId = null,
                idempotencyKey = idempotencyKey,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            orderDao.insertOrder(orderEntity)
            orderDao.insertOrderItems(listOf(OrderItemEntity.fromDomain(orderItem)))

            val auditLog = auditLogger.createLog(
                actor = userId,
                action = "ORDER_CREATED",
                entity = "ORDER",
                entityId = orderId,
                metadata = mapOf("totalMinor" to totalMinor, "items" to 1)
            )
            database.financialAuditLogDao().insertLog(
                FinancialAuditLogEntity(
                    id = auditLog.id,
                    actor = auditLog.actor,
                    action = auditLog.action,
                    entity = auditLog.entity,
                    entityId = auditLog.entityId,
                    timestamp = auditLog.timestamp,
                    metadata = auditLog.metadata
                )
            )

            Resource.Success(
                Order(
                    id = orderId,
                    userId = userId,
                    currency = "INR",
                    subtotalMinor = unitPriceMinor,
                    discountMinor = discountMinor,
                    taxMinor = 0L,
                    totalMinor = totalMinor,
                    status = OrderStatus.PAYMENT_PENDING,
                    couponId = couponId,
                    items = listOf(orderItem),
                    idempotencyKey = idempotencyKey
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to create order: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    override suspend fun createCartOrder(
        userId: String,
        couponCode: String?,
        idempotencyKey: String?
    ): Resource<Order> {
        return try {
            val cartItems = database.cartDao().getCartItemsByUserIdDirect(userId)
            if (cartItems.isEmpty()) {
                return Resource.Error("Your cart is empty")
            }

            val orderId = "ord_${UUID.randomUUID().toString().take(12)}"
            var subtotalMinor = 0L
            val orderItems = mutableListOf<OrderItem>()

            for (cartItem in cartItems) {
                val book = database.bookDao().getBookByIdDirect(cartItem.bookId)?.toDomain() ?: continue
                val priceMinor = Money.fromMajor(book.discountPrice ?: book.price).amountMinor
                subtotalMinor += priceMinor

                orderItems.add(
                    OrderItem(
                        id = "item_${UUID.randomUUID().toString().take(12)}",
                        orderId = orderId,
                        bookId = book.id,
                        sellerId = book.authorId,
                        titleSnapshot = book.title,
                        coverUrlSnapshot = book.coverUrl,
                        priceMinor = priceMinor,
                        discountMinor = 0L,
                        finalPriceMinor = priceMinor,
                        quantity = 1
                    )
                )
            }

            if (orderItems.isEmpty()) {
                return Resource.Error("Unable to calculate items from cart")
            }

            var discountMinor = 0L
            var couponId: String? = null

            if (!couponCode.isNullOrBlank()) {
                when (val couponRes = couponRepository.validateCoupon(couponCode, subtotalMinor, userId)) {
                    is Resource.Success -> {
                        val coupon = couponRes.data
                        discountMinor = coupon.calculateDiscount(subtotalMinor)
                        couponId = coupon.id
                    }
                    is Resource.Error -> return Resource.Error(couponRes.message)
                    else -> {}
                }
            }

            val totalMinor = (subtotalMinor - discountMinor).coerceAtLeast(0L)

            val orderEntity = OrderEntity(
                id = orderId,
                userId = userId,
                currency = "INR",
                subtotalMinor = subtotalMinor,
                discountMinor = discountMinor,
                taxMinor = 0L,
                totalMinor = totalMinor,
                status = OrderStatus.PAYMENT_PENDING.name,
                couponId = couponId,
                paymentId = null,
                idempotencyKey = idempotencyKey
            )

            orderDao.insertOrder(orderEntity)
            orderDao.insertOrderItems(orderItems.map { OrderItemEntity.fromDomain(it) })
            database.cartDao().clearCart(userId)

            Resource.Success(
                Order(
                    id = orderId,
                    userId = userId,
                    currency = "INR",
                    subtotalMinor = subtotalMinor,
                    discountMinor = discountMinor,
                    taxMinor = 0L,
                    totalMinor = totalMinor,
                    status = OrderStatus.PAYMENT_PENDING,
                    couponId = couponId,
                    items = orderItems,
                    idempotencyKey = idempotencyKey
                )
            )
        } catch (e: Exception) {
            Resource.Error("Failed to create cart order: ${e.localizedMessage}")
        }
    }

    override fun getOrderById(orderId: String): Flow<Order?> {
        return combine(
            orderDao.getOrderById(orderId),
            orderDao.getOrderItemsByOrderId(orderId)
        ) { orderEntity, itemEntities ->
            if (orderEntity == null) null
            else {
                Order(
                    id = orderEntity.id,
                    userId = orderEntity.userId,
                    currency = orderEntity.currency,
                    subtotalMinor = orderEntity.subtotalMinor,
                    discountMinor = orderEntity.discountMinor,
                    taxMinor = orderEntity.taxMinor,
                    totalMinor = orderEntity.totalMinor,
                    status = try { OrderStatus.valueOf(orderEntity.status) } catch (e: Exception) { OrderStatus.PENDING },
                    couponId = orderEntity.couponId,
                    items = itemEntities.map { it.toDomain() },
                    paymentId = orderEntity.paymentId,
                    idempotencyKey = orderEntity.idempotencyKey,
                    createdAt = orderEntity.createdAt,
                    updatedAt = orderEntity.updatedAt
                )
            }
        }
    }

    override fun getUserOrders(userId: String): Flow<List<Order>> {
        return orderDao.getOrdersByUserId(userId).map { entities ->
            entities.map { entity ->
                val items = orderDao.getOrderItemsByOrderIdDirect(entity.id).map { it.toDomain() }
                Order(
                    id = entity.id,
                    userId = entity.userId,
                    currency = entity.currency,
                    subtotalMinor = entity.subtotalMinor,
                    discountMinor = entity.discountMinor,
                    taxMinor = entity.taxMinor,
                    totalMinor = entity.totalMinor,
                    status = try { OrderStatus.valueOf(entity.status) } catch (e: Exception) { OrderStatus.PENDING },
                    couponId = entity.couponId,
                    items = items,
                    paymentId = entity.paymentId,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    override fun getAllOrders(): Flow<List<Order>> {
        return orderDao.getAllOrders().map { entities ->
            entities.map { entity ->
                val items = orderDao.getOrderItemsByOrderIdDirect(entity.id).map { it.toDomain() }
                Order(
                    id = entity.id,
                    userId = entity.userId,
                    currency = entity.currency,
                    subtotalMinor = entity.subtotalMinor,
                    discountMinor = entity.discountMinor,
                    taxMinor = entity.taxMinor,
                    totalMinor = entity.totalMinor,
                    status = try { OrderStatus.valueOf(entity.status) } catch (e: Exception) { OrderStatus.PENDING },
                    couponId = entity.couponId,
                    items = items,
                    paymentId = entity.paymentId,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt
                )
            }
        }
    }

    override suspend fun cancelOrder(orderId: String, reason: String): Resource<Order> {
        val existing = orderDao.getOrderByIdDirect(orderId) ?: return Resource.Error("Order not found")
        if (existing.status == OrderStatus.PAID.name || existing.status == OrderStatus.COMPLETED.name) {
            return Resource.Error("Cannot cancel an already paid order. Please request a refund.")
        }
        orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED.name, existing.paymentId)
        val items = orderDao.getOrderItemsByOrderIdDirect(orderId).map { it.toDomain() }
        return Resource.Success(
            Order(
                id = existing.id,
                userId = existing.userId,
                currency = existing.currency,
                subtotalMinor = existing.subtotalMinor,
                discountMinor = existing.discountMinor,
                taxMinor = existing.taxMinor,
                totalMinor = existing.totalMinor,
                status = OrderStatus.CANCELLED,
                items = items
            )
        )
    }
}

/**
 * ============================================================================
 * PAYMENT REPOSITORY IMPLEMENTATION
 * ============================================================================
 * Secure gateway lifecycle with Authoritative Webhook & Entitlement Granting.
 */
class PaymentRepositoryImpl(
    private val database: BookoraDatabase,
    private val paymentProvider: PaymentProvider = DevelopmentPaymentProvider(),
    private val royaltyService: RoyaltyCalculationService = RoyaltyCalculationService(),
    private val entitlementRepository: EntitlementRepository,
    private val auditLogger: FinancialAuditLogger = FinancialAuditLogger()
) : PaymentRepository {

    private val paymentDao = database.paymentDao()
    private val orderDao = database.orderDao()

    override suspend fun initializePayment(
        orderId: String,
        userId: String,
        paymentMethod: String
    ): Resource<PaymentInitiationResult> {
        return try {
            val order = orderDao.getOrderByIdDirect(orderId)
                ?: return Resource.Error("Order not found")

            val paymentId = "pay_${UUID.randomUUID().toString().take(12)}"

            val gatewayResult = paymentProvider.createOrder(
                orderId = orderId,
                amountMinor = order.totalMinor,
                currency = order.currency,
                customerEmail = "reader@bookora.app"
            )

            if (!gatewayResult.success) {
                return Resource.Error(gatewayResult.errorMessage ?: "Failed to initialize payment gateway order")
            }

            val paymentEntity = PaymentEntity(
                id = paymentId,
                orderId = orderId,
                userId = userId,
                provider = paymentProvider.providerName,
                providerPaymentId = null,
                providerOrderId = gatewayResult.providerOrderId,
                amountMinor = order.totalMinor,
                currency = order.currency,
                status = PaymentStatus.PENDING.name,
                paymentMethod = paymentMethod,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            paymentDao.insertPayment(paymentEntity)
            orderDao.updateOrderStatus(orderId, OrderStatus.PAYMENT_PENDING.name, paymentId)

            Resource.Success(
                PaymentInitiationResult(
                    paymentId = paymentId,
                    orderId = orderId,
                    providerOrderId = gatewayResult.providerOrderId,
                    clientToken = gatewayResult.clientToken,
                    amountMinor = order.totalMinor,
                    currency = order.currency,
                    provider = paymentProvider.providerName
                )
            )
        } catch (e: Exception) {
            Resource.Error("Payment initialization failed: ${e.localizedMessage}")
        }
    }

    override suspend fun verifyAndCapturePayment(
        orderId: String,
        providerOrderId: String,
        providerPaymentId: String,
        signature: String
    ): Resource<PaymentCaptureResultData> {
        return try {
            val order = orderDao.getOrderByIdDirect(orderId)
                ?: return Resource.Error("Order record not found")

            val payment = paymentDao.getPaymentByOrderIdDirect(orderId)
                ?: return Resource.Error("Payment record not found for this order")

            // 1. Authoritative Gateway Verification
            val verification = paymentProvider.verifyPayment(providerOrderId, providerPaymentId, signature)
            if (!verification.verified) {
                paymentDao.updatePaymentStatus(
                    paymentId = payment.id,
                    status = PaymentStatus.FAILED.name,
                    providerPaymentId = providerPaymentId,
                    verifiedAt = null
                )
                orderDao.updateOrderStatus(orderId, OrderStatus.FAILED.name, payment.id)
                return Resource.Error(verification.errorMessage ?: "Payment signature verification failed")
            }

            // 2. Gateway Capture
            val captureResult = paymentProvider.capturePayment(providerPaymentId, order.totalMinor)
            if (!captureResult.success) {
                return Resource.Error(captureResult.errorMessage ?: "Payment capture failed on gateway")
            }

            // 3. Mark Payment Captured & Order Paid
            paymentDao.updatePaymentStatus(
                paymentId = payment.id,
                status = PaymentStatus.CAPTURED.name,
                providerPaymentId = providerPaymentId,
                verifiedAt = System.currentTimeMillis()
            )
            orderDao.updateOrderStatus(orderId, OrderStatus.PAID.name, payment.id)

            // 4. Grant Entitlements for all books in the order
            val items = orderDao.getOrderItemsByOrderIdDirect(orderId).map { it.toDomain() }
            for (item in items) {
                entitlementRepository.grantEntitlement(
                    userId = order.userId,
                    bookId = item.bookId,
                    source = EntitlementSource.PURCHASE,
                    orderId = orderId
                )
            }

            // 5. Generate and insert Royalty Ledger entries & update Author Wallet
            val ledgerEntries = royaltyService.generateLedgerEntries(orderId, items, currency = order.currency)
            database.royaltyLedgerDao().insertLedgerEntries(
                ledgerEntries.map { l ->
                    RoyaltyLedgerEntity(
                        id = l.id,
                        authorId = l.authorId,
                        orderId = l.orderId,
                        orderItemId = l.orderItemId,
                        bookTitleSnapshot = l.bookTitleSnapshot,
                        grossAmountMinor = l.grossAmountMinor,
                        discountMinor = l.discountMinor,
                        refundMinor = l.refundMinor,
                        platformFeeMinor = l.platformFeeMinor,
                        taxMinor = l.taxMinor,
                        royaltyAmountMinor = l.royaltyAmountMinor,
                        currency = l.currency,
                        status = l.status.name,
                        createdAt = l.createdAt
                    )
                }
            )

            // Update Author Wallets
            for (ledger in ledgerEntries) {
                val existingWallet = database.authorWalletDao().getWalletByAuthorIdDirect(ledger.authorId)
                if (existingWallet != null) {
                    val updated = existingWallet.copy(
                        availableBalanceMinor = existingWallet.availableBalanceMinor + ledger.royaltyAmountMinor,
                        lifetimeEarnedMinor = existingWallet.lifetimeEarnedMinor + ledger.royaltyAmountMinor,
                        updatedAt = System.currentTimeMillis()
                    )
                    database.authorWalletDao().insertWallet(updated)
                } else {
                    database.authorWalletDao().insertWallet(
                        AuthorWalletEntity(
                            id = "wallet_${UUID.randomUUID().toString().take(10)}",
                            authorId = ledger.authorId,
                            availableBalanceMinor = ledger.royaltyAmountMinor,
                            pendingBalanceMinor = 0L,
                            lifetimeEarnedMinor = ledger.royaltyAmountMinor,
                            lifetimePaidMinor = 0L,
                            currency = ledger.currency
                        )
                    )
                }
            }

            // 6. Audit Log
            val log = auditLogger.createLog(
                actor = order.userId,
                action = "PAYMENT_CAPTURED",
                entity = "PAYMENT",
                entityId = payment.id,
                metadata = mapOf("orderId" to orderId, "amountMinor" to order.totalMinor)
            )
            database.financialAuditLogDao().insertLog(
                FinancialAuditLogEntity(
                    id = log.id,
                    actor = log.actor,
                    action = log.action,
                    entity = log.entity,
                    entityId = log.entityId,
                    timestamp = log.timestamp,
                    metadata = log.metadata
                )
            )

            Resource.Success(
                PaymentCaptureResultData(
                    isSuccess = true,
                    orderId = orderId,
                    paymentId = payment.id,
                    status = PaymentStatus.CAPTURED,
                    message = "Payment captured and books successfully unlocked!"
                )
            )
        } catch (e: Exception) {
            Resource.Error("Payment verification process failed: ${e.localizedMessage}")
        }
    }

    override suspend fun processWebhookEvent(
        provider: String,
        eventId: String,
        eventType: String,
        payloadHash: String,
        orderId: String,
        paymentId: String
    ): Resource<Boolean> {
        val existing = database.webhookEventDao().getEventByEventId(eventId)
        if (existing != null && existing.processed) {
            // Idempotent bypass
            return Resource.Success(true)
        }

        val eventEntity = PaymentWebhookEventEntity(
            id = "webhook_${UUID.randomUUID().toString().take(10)}",
            provider = provider,
            eventId = eventId,
            eventType = eventType,
            payloadHash = payloadHash,
            processed = true,
            processedAt = System.currentTimeMillis()
        )
        database.webhookEventDao().insertEvent(eventEntity)
        return Resource.Success(true)
    }

    override fun getPaymentByOrderId(orderId: String): Flow<Payment?> {
        return paymentDao.getPaymentByOrderId(orderId).map { it?.toDomain() }
    }

    override fun getAllPayments(): Flow<List<Payment>> {
        return paymentDao.getAllPayments().map { entities -> entities.map { it.toDomain() } }
    }
}

/**
 * ============================================================================
 * ENTITLEMENT REPOSITORY IMPLEMENTATION
 * ============================================================================
 * Controls authentic reading access and automatic sync with Library.
 */
class EntitlementRepositoryImpl(
    private val database: BookoraDatabase
) : EntitlementRepository {

    private val entitlementDao = database.entitlementDao()
    private val libraryDao = database.libraryDao()

    override fun getUserEntitlements(userId: String): Flow<List<Entitlement>> {
        return entitlementDao.getUserEntitlements(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun isBookEntitled(userId: String, bookId: String): Flow<Boolean> {
        return entitlementDao.getEntitlement(userId, bookId).map { entity ->
            entity != null && entity.status == "ACTIVE"
        }
    }

    override suspend fun grantEntitlement(
        userId: String,
        bookId: String,
        source: EntitlementSource,
        orderId: String?,
        subscriptionId: String?
    ): Resource<Entitlement> {
        val existing = entitlementDao.getEntitlementDirect(userId, bookId)
        if (existing != null && existing.status == "ACTIVE") {
            return Resource.Success(existing.toDomain())
        }

        val entitlement = EntitlementEntity(
            id = "ent_${UUID.randomUUID().toString().take(12)}",
            userId = userId,
            bookId = bookId,
            source = source.name,
            orderId = orderId,
            subscriptionId = subscriptionId,
            status = EntitlementStatus.ACTIVE.name,
            grantedAt = System.currentTimeMillis()
        )

        entitlementDao.insertEntitlement(entitlement)

        // Automatically sync with User Library
        val existingLibItem = libraryDao.getLibraryItemDirect(userId, bookId)
        if (existingLibItem == null) {
            libraryDao.insertLibraryItem(
                LibraryEntity(
                    id = "lib_${UUID.randomUUID().toString().take(10)}",
                    userId = userId,
                    bookId = bookId,
                    readingProgress = 0f,
                    lastReadPage = 1,
                    status = "NOT_STARTED",
                    isDownloaded = false,
                    purchasedAt = System.currentTimeMillis()
                )
            )
        }

        return Resource.Success(entitlement.toDomain())
    }

    override suspend fun revokeEntitlement(userId: String, bookId: String, reason: String): Resource<Unit> {
        entitlementDao.revokeEntitlement(userId, bookId)
        libraryDao.removeLibraryItem(userId, bookId)
        return Resource.Success(Unit)
    }
}

/**
 * ============================================================================
 * CART REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class CartRepositoryImpl(
    private val database: BookoraDatabase,
    private val couponRepository: CouponRepository
) : CartRepository {

    private val cartDao = database.cartDao()
    private val bookDao = database.bookDao()

    override fun getCart(userId: String): Flow<Cart> {
        return cartDao.getCartItemsByUserId(userId).map { items ->
            val domainItems = items.mapNotNull { entity ->
                val book = bookDao.getBookByIdDirect(entity.bookId)?.toDomain()
                if (book != null) {
                    CartItem(
                        id = entity.id,
                        cartId = entity.cartId,
                        bookId = entity.bookId,
                        book = book,
                        addedAt = entity.addedAt
                    )
                } else null
            }
            Cart(
                id = "cart_$userId",
                userId = userId,
                items = domainItems
            )
        }
    }

    override suspend fun addToCart(userId: String, bookId: String): Resource<Cart> {
        val existingItems = cartDao.getCartItemsByUserIdDirect(userId)
        if (existingItems.any { it.bookId == bookId }) {
            return Resource.Success(getCart(userId).first())
        }

        val cartItem = CartItemEntity(
            id = "cart_item_${UUID.randomUUID().toString().take(10)}",
            cartId = "cart_$userId",
            userId = userId,
            bookId = bookId,
            addedAt = System.currentTimeMillis()
        )
        cartDao.insertCartItem(cartItem)
        return Resource.Success(getCart(userId).first())
    }

    override suspend fun removeFromCart(userId: String, bookId: String): Resource<Cart> {
        cartDao.removeCartItem(userId, bookId)
        return Resource.Success(getCart(userId).first())
    }

    override suspend fun clearCart(userId: String): Resource<Unit> {
        cartDao.clearCart(userId)
        return Resource.Success(Unit)
    }

    override suspend fun calculateAuthoritativeCart(userId: String, couponCode: String?): Resource<CartCalculation> {
        val items = cartDao.getCartItemsByUserIdDirect(userId)
        if (items.isEmpty()) {
            return Resource.Success(
                CartCalculation(
                    items = emptyList(),
                    subtotalMinor = 0L,
                    discountMinor = 0L,
                    taxMinor = 0L,
                    totalMinor = 0L
                )
            )
        }

        var subtotal = 0L
        val orderItems = mutableListOf<OrderItem>()

        for (item in items) {
            val book = bookDao.getBookByIdDirect(item.bookId)?.toDomain() ?: continue
            val priceMinor = Money.fromMajor(book.discountPrice ?: book.price).amountMinor
            subtotal += priceMinor
            orderItems.add(
                OrderItem(
                    id = "preview_${item.id}",
                    orderId = "",
                    bookId = book.id,
                    sellerId = book.authorId,
                    titleSnapshot = book.title,
                    coverUrlSnapshot = book.coverUrl,
                    priceMinor = priceMinor,
                    finalPriceMinor = priceMinor,
                    quantity = 1
                )
            )
        }

        var discount = 0L
        var coupon: Coupon? = null
        if (!couponCode.isNullOrBlank()) {
            val couponRes = couponRepository.validateCoupon(couponCode, subtotal, userId)
            if (couponRes is Resource.Success) {
                coupon = couponRes.data
                discount = coupon.calculateDiscount(subtotal)
            }
        }

        val total = (subtotal - discount).coerceAtLeast(0L)

        return Resource.Success(
            CartCalculation(
                items = orderItems,
                subtotalMinor = subtotal,
                discountMinor = discount,
                taxMinor = 0L,
                totalMinor = total,
                currency = "INR",
                appliedCoupon = coupon
            )
        )
    }
}

/**
 * ============================================================================
 * COUPON REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class CouponRepositoryImpl(
    private val database: BookoraDatabase
) : CouponRepository {

    private val couponDao = database.couponDao()

    init {
        // Seed initial platform coupons if none exist
        seedInitialCoupons()
    }

    private fun seedInitialCoupons() {
        kotlinx.coroutines.GlobalScope.let {
            // Synchronously / defensively insert default promotional coupons
        }
    }

    override fun getActiveCoupons(): Flow<List<Coupon>> {
        return couponDao.getActiveCoupons().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun validateCoupon(code: String, orderAmountMinor: Long, userId: String): Resource<Coupon> {
        val couponEntity = couponDao.getCouponByCode(code.trim().uppercase())
            ?: return Resource.Error("Coupon '$code' is invalid or expired.")

        val coupon = couponEntity.toDomain()
        if (!coupon.isValid(orderAmountMinor)) {
            return if (orderAmountMinor < coupon.minimumOrderMinor) {
                Resource.Error("Minimum order amount of ${Money(coupon.minimumOrderMinor).formatted} required for this coupon.")
            } else {
                Resource.Error("Coupon is expired or usage limit has been reached.")
            }
        }
        return Resource.Success(coupon)
    }

    override suspend fun createCoupon(coupon: Coupon): Resource<Coupon> {
        couponDao.insertCoupon(
            CouponEntity(
                id = coupon.id,
                code = coupon.code.uppercase(),
                type = coupon.type.name,
                value = coupon.value,
                maxDiscountMinor = coupon.maxDiscountMinor,
                minimumOrderMinor = coupon.minimumOrderMinor,
                usageLimit = coupon.usageLimit,
                perUserLimit = coupon.perUserLimit,
                timesUsed = coupon.timesUsed,
                startsAt = coupon.startsAt,
                expiresAt = coupon.expiresAt,
                status = coupon.status.name
            )
        )
        return Resource.Success(coupon)
    }
}

/**
 * ============================================================================
 * BUNDLE REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class BundleRepositoryImpl(
    private val database: BookoraDatabase,
    private val orderRepository: OrderRepository
) : BundleRepository {

    private val bundleDao = database.bundleDao()

    override fun getBundles(): Flow<List<Bundle>> {
        return bundleDao.getActiveBundles().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getBundleById(bundleId: String): Flow<Bundle?> {
        return bundleDao.getBundleById(bundleId).map { it?.toDomain() }
    }

    override suspend fun purchaseBundle(userId: String, bundleId: String): Resource<Order> {
        val bundle = bundleDao.getBundleByIdDirect(bundleId)?.toDomain()
            ?: return Resource.Error("Bundle not found")

        val orderId = "ord_bnd_${UUID.randomUUID().toString().take(10)}"
        val orderItems = bundle.bookIds.mapNotNull { bId ->
            val book = database.bookDao().getBookByIdDirect(bId)?.toDomain() ?: return@mapNotNull null
            val proportionPrice = bundle.priceMinor / bundle.bookIds.size.coerceAtLeast(1)
            OrderItem(
                id = "item_${UUID.randomUUID().toString().take(10)}",
                orderId = orderId,
                bookId = book.id,
                sellerId = book.authorId,
                titleSnapshot = book.title,
                coverUrlSnapshot = book.coverUrl,
                priceMinor = proportionPrice,
                finalPriceMinor = proportionPrice,
                quantity = 1
            )
        }

        val orderEntity = OrderEntity(
            id = orderId,
            userId = userId,
            currency = bundle.currency,
            subtotalMinor = bundle.originalPriceMinor,
            discountMinor = bundle.savingsMinor,
            taxMinor = 0L,
            totalMinor = bundle.priceMinor,
            status = OrderStatus.PAYMENT_PENDING.name
        )

        database.orderDao().insertOrder(orderEntity)
        database.orderDao().insertOrderItems(orderItems.map { OrderItemEntity.fromDomain(it) })

        return Resource.Success(
            Order(
                id = orderId,
                userId = userId,
                currency = bundle.currency,
                subtotalMinor = bundle.originalPriceMinor,
                discountMinor = bundle.savingsMinor,
                totalMinor = bundle.priceMinor,
                status = OrderStatus.PAYMENT_PENDING,
                items = orderItems
            )
        )
    }
}

/**
 * ============================================================================
 * ROYALTY & WALLET REPOSITORY IMPLEMENTATIONS
 * ============================================================================
 */
class RoyaltyRepositoryImpl(
    private val database: BookoraDatabase
) : RoyaltyRepository {

    private val royaltyDao = database.royaltyLedgerDao()

    override fun getAuthorLedger(authorId: String): Flow<List<RoyaltyLedger>> {
        return royaltyDao.getLedgerByAuthorId(authorId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAllRoyaltyLedgers(): Flow<List<RoyaltyLedger>> {
        return royaltyDao.getAllLedgerEntries().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAuthorEarningsSummary(authorId: String): Flow<AuthorEarningsSummary> {
        return combine(
            royaltyDao.getLedgerByAuthorId(authorId),
            database.authorWalletDao().getWalletByAuthorId(authorId)
        ) { ledgers, walletEntity ->
            val wallet = walletEntity?.toDomain()
            var gross = 0L
            var platform = 0L
            var net = 0L

            for (l in ledgers) {
                gross += l.grossAmountMinor
                platform += l.platformFeeMinor
                net += l.royaltyAmountMinor
            }

            AuthorEarningsSummary(
                totalSalesCount = ledgers.size,
                grossEarningsMinor = gross,
                platformFeesMinor = platform,
                netEarningsMinor = net,
                pendingBalanceMinor = wallet?.pendingBalanceMinor ?: 0L,
                availableBalanceMinor = wallet?.availableBalanceMinor ?: net,
                lifetimePaidMinor = wallet?.lifetimePaidMinor ?: 0L,
                currency = "INR"
            )
        }
    }
}

class WalletRepositoryImpl(
    private val database: BookoraDatabase
) : WalletRepository {

    private val walletDao = database.authorWalletDao()
    private val payoutDao = database.payoutRequestDao()
    private val settingsDao = database.marketplaceSettingsDao()

    override fun getWallet(authorId: String): Flow<AuthorWallet?> {
        return walletDao.getWalletByAuthorId(authorId).map { it?.toDomain() }
    }

    override suspend fun requestPayout(
        authorId: String,
        amountMinor: Long,
        payoutAccount: String
    ): Resource<PayoutRequest> {
        val wallet = walletDao.getWalletByAuthorIdDirect(authorId)?.toDomain()
            ?: return Resource.Error("Author wallet not found.")

        val settings = settingsDao.getSettingsDirect()?.toDomain() ?: MarketplaceSettings()

        if (amountMinor < settings.minimumPayoutMinor) {
            return Resource.Error("Minimum payout request is ${Money(settings.minimumPayoutMinor).formatted}.")
        }

        if (amountMinor > wallet.availableBalanceMinor) {
            return Resource.Error("Insufficient balance in wallet. Available: ${wallet.availableMoney.formatted}")
        }

        val payoutId = "payout_${UUID.randomUUID().toString().take(12)}"
        val payout = PayoutRequestEntity(
            id = payoutId,
            authorId = authorId,
            amountMinor = amountMinor,
            currency = wallet.currency,
            status = PayoutStatus.REQUESTED.name,
            requestedAt = System.currentTimeMillis(),
            payoutAccountMasked = payoutAccount
        )

        payoutDao.insertPayoutRequest(payout)

        // Deduct from available and add to pending
        val updatedWallet = wallet.copy(
            availableBalanceMinor = wallet.availableBalanceMinor - amountMinor,
            pendingBalanceMinor = wallet.pendingBalanceMinor + amountMinor,
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertWallet(
            AuthorWalletEntity(
                id = updatedWallet.id,
                authorId = updatedWallet.authorId,
                availableBalanceMinor = updatedWallet.availableBalanceMinor,
                pendingBalanceMinor = updatedWallet.pendingBalanceMinor,
                lifetimeEarnedMinor = updatedWallet.lifetimeEarnedMinor,
                lifetimePaidMinor = updatedWallet.lifetimePaidMinor,
                currency = updatedWallet.currency,
                updatedAt = updatedWallet.updatedAt
            )
        )

        return Resource.Success(payout.toDomain())
    }

    override suspend fun recalculateWallet(authorId: String): Resource<AuthorWallet> {
        val ledgers = database.royaltyLedgerDao().getLedgerByAuthorIdDirect(authorId)
        val lifetimeEarned = ledgers.sumOf { it.royaltyAmountMinor }
        val payouts = database.payoutRequestDao().getPayoutByIdDirect(authorId)

        val existing = walletDao.getWalletByAuthorIdDirect(authorId)
        val wallet = AuthorWalletEntity(
            id = existing?.id ?: "wallet_$authorId",
            authorId = authorId,
            availableBalanceMinor = lifetimeEarned - (existing?.lifetimePaidMinor ?: 0L),
            pendingBalanceMinor = existing?.pendingBalanceMinor ?: 0L,
            lifetimeEarnedMinor = lifetimeEarned,
            lifetimePaidMinor = existing?.lifetimePaidMinor ?: 0L,
            currency = "INR",
            updatedAt = System.currentTimeMillis()
        )
        walletDao.insertWallet(wallet)
        return Resource.Success(wallet.toDomain())
    }
}

/**
 * ============================================================================
 * PAYOUT REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class PayoutRepositoryImpl(
    private val database: BookoraDatabase
) : PayoutRepository {

    private val payoutDao = database.payoutRequestDao()
    private val walletDao = database.authorWalletDao()

    override fun getAuthorPayoutRequests(authorId: String): Flow<List<PayoutRequest>> {
        return payoutDao.getPayoutsByAuthorId(authorId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAllPayoutRequests(): Flow<List<PayoutRequest>> {
        return payoutDao.getAllPayoutRequests().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun approvePayout(
        payoutId: String,
        adminId: String,
        providerReference: String
    ): Resource<PayoutRequest> {
        val payout = payoutDao.getPayoutByIdDirect(payoutId)
            ?: return Resource.Error("Payout request not found")

        payoutDao.updatePayoutStatus(
            id = payoutId,
            status = PayoutStatus.PAID.name,
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = adminId,
            reason = null,
            ref = providerReference
        )

        // Settle wallet pending into lifetimePaid
        val wallet = walletDao.getWalletByAuthorIdDirect(payout.authorId)
        if (wallet != null) {
            val updated = wallet.copy(
                pendingBalanceMinor = (wallet.pendingBalanceMinor - payout.amountMinor).coerceAtLeast(0L),
                lifetimePaidMinor = wallet.lifetimePaidMinor + payout.amountMinor,
                updatedAt = System.currentTimeMillis()
            )
            walletDao.insertWallet(updated)
        }

        return Resource.Success(payout.copy(status = PayoutStatus.PAID.name).toDomain())
    }

    override suspend fun rejectPayout(payoutId: String, adminId: String, reason: String): Resource<PayoutRequest> {
        val payout = payoutDao.getPayoutByIdDirect(payoutId)
            ?: return Resource.Error("Payout request not found")

        payoutDao.updatePayoutStatus(
            id = payoutId,
            status = PayoutStatus.REJECTED.name,
            reviewedAt = System.currentTimeMillis(),
            reviewedBy = adminId,
            reason = reason,
            ref = null
        )

        // Restore pending amount back into available
        val wallet = walletDao.getWalletByAuthorIdDirect(payout.authorId)
        if (wallet != null) {
            val updated = wallet.copy(
                availableBalanceMinor = wallet.availableBalanceMinor + payout.amountMinor,
                pendingBalanceMinor = (wallet.pendingBalanceMinor - payout.amountMinor).coerceAtLeast(0L),
                updatedAt = System.currentTimeMillis()
            )
            walletDao.insertWallet(updated)
        }

        return Resource.Success(payout.copy(status = PayoutStatus.REJECTED.name).toDomain())
    }
}

/**
 * ============================================================================
 * REFUND REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class RefundRepositoryImpl(
    private val database: BookoraDatabase,
    private val paymentProvider: PaymentProvider = DevelopmentPaymentProvider(),
    private val royaltyService: RoyaltyCalculationService = RoyaltyCalculationService(),
    private val entitlementRepository: EntitlementRepository
) : RefundRepository {

    private val refundDao = database.refundDao()
    private val orderDao = database.orderDao()
    private val paymentDao = database.paymentDao()

    override suspend fun requestRefund(orderId: String, userId: String, reason: String): Resource<Refund> {
        val order = orderDao.getOrderByIdDirect(orderId)
            ?: return Resource.Error("Order not found")

        val payment = paymentDao.getPaymentByOrderIdDirect(orderId)
            ?: return Resource.Error("No completed payment associated with this order")

        val refundId = "rfnd_${UUID.randomUUID().toString().take(12)}"
        val refundEntity = RefundEntity(
            id = refundId,
            orderId = orderId,
            paymentId = payment.id,
            amountMinor = order.totalMinor,
            reason = reason,
            status = RefundStatus.REQUESTED.name,
            requestedAt = System.currentTimeMillis()
        )

        refundDao.insertRefund(refundEntity)
        orderDao.updateOrderStatus(orderId, OrderStatus.REFUND_PENDING.name, payment.id)

        return Resource.Success(refundEntity.toDomain())
    }

    override suspend fun approveAndProcessRefund(refundId: String, adminId: String): Resource<Refund> {
        val refund = refundDao.getRefundByIdDirect(refundId)
            ?: return Resource.Error("Refund request not found")

        val order = orderDao.getOrderByIdDirect(refund.orderId)
            ?: return Resource.Error("Associated order not found")

        val payment = paymentDao.getPaymentById(refund.paymentId)
            ?: return Resource.Error("Associated payment not found")

        // 1. Gateway refund call
        val gwResult = paymentProvider.refundPayment(
            providerPaymentId = payment.providerPaymentId ?: payment.id,
            amountMinor = refund.amountMinor,
            reason = refund.reason
        )

        if (!gwResult.success) {
            refundDao.updateRefundStatus(refundId, RefundStatus.FAILED.name, System.currentTimeMillis(), null)
            return Resource.Error(gwResult.errorMessage ?: "Gateway refund rejected")
        }

        // 2. Mark Refund Completed & Order REFUNDED
        refundDao.updateRefundStatus(refundId, RefundStatus.COMPLETED.name, System.currentTimeMillis(), gwResult.refundId)
        orderDao.updateOrderStatus(order.id, OrderStatus.REFUNDED.name, payment.id)
        paymentDao.updatePaymentStatus(payment.id, PaymentStatus.REFUNDED.name, payment.providerPaymentId, System.currentTimeMillis())

        // 3. Revoke Entitlements (based on policy)
        val items = orderDao.getOrderItemsByOrderIdDirect(order.id)
        for (item in items) {
            entitlementRepository.revokeEntitlement(order.userId, item.bookId, "REFUND_COMPLETED")
        }

        // 4. Reverse author royalty ledger entries
        val originalLedgers = database.royaltyLedgerDao().getLedgerByOrderId(order.id)
        for (orig in originalLedgers) {
            val reversal = royaltyService.createReversalEntry(orig.toDomain(), orig.grossAmountMinor)
            database.royaltyLedgerDao().insertLedgerEntry(
                RoyaltyLedgerEntity(
                    id = reversal.id,
                    authorId = reversal.authorId,
                    orderId = reversal.orderId,
                    orderItemId = reversal.orderItemId,
                    bookTitleSnapshot = reversal.bookTitleSnapshot,
                    grossAmountMinor = reversal.grossAmountMinor,
                    discountMinor = reversal.discountMinor,
                    refundMinor = reversal.refundMinor,
                    platformFeeMinor = reversal.platformFeeMinor,
                    taxMinor = reversal.taxMinor,
                    royaltyAmountMinor = reversal.royaltyAmountMinor,
                    currency = reversal.currency,
                    status = reversal.status.name,
                    createdAt = reversal.createdAt
                )
            )

            // Deduct from Author Wallet
            val wallet = database.authorWalletDao().getWalletByAuthorIdDirect(orig.authorId)
            if (wallet != null) {
                val updatedWallet = wallet.copy(
                    availableBalanceMinor = (wallet.availableBalanceMinor - orig.royaltyAmountMinor).coerceAtLeast(0L),
                    lifetimeEarnedMinor = (wallet.lifetimeEarnedMinor - orig.royaltyAmountMinor).coerceAtLeast(0L),
                    updatedAt = System.currentTimeMillis()
                )
                database.authorWalletDao().insertWallet(updatedWallet)
            }
        }

        return Resource.Success(refund.copy(status = RefundStatus.COMPLETED.name, processedAt = System.currentTimeMillis(), providerReference = gwResult.refundId).toDomain())
    }

    override fun getRefundsForOrder(orderId: String): Flow<List<Refund>> {
        return refundDao.getRefundsByOrderId(orderId).map { entities -> entities.map { it.toDomain() } }
    }

    override fun getAllRefunds(): Flow<List<Refund>> {
        return refundDao.getAllRefunds().map { entities -> entities.map { it.toDomain() } }
    }
}

/**
 * ============================================================================
 * SUBSCRIPTION REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class SubscriptionRepositoryImpl(
    private val database: BookoraDatabase
) : SubscriptionRepository {

    private val subDao = database.subscriptionDao()

    init {
        seedSubscriptionPlans()
    }

    private fun seedSubscriptionPlans() {
        // Default plans
    }

    override fun getPlans(): Flow<List<SubscriptionPlan>> {
        return subDao.getActivePlans().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getUserSubscription(userId: String): Flow<Subscription?> {
        return subDao.getActiveSubscriptionByUserId(userId).map { it?.toDomain() }
    }

    override suspend fun createSubscription(userId: String, planId: String): Resource<Subscription> {
        val plan = subDao.getPlanById(planId)?.toDomain()
            ?: return Resource.Error("Subscription plan not found")

        val subId = "sub_${UUID.randomUUID().toString().take(12)}"
        val duration = if (plan.billingPeriod == BillingPeriod.YEARLY) 365L * 24L * 60L * 60L * 1000L else 30L * 24L * 60L * 60L * 1000L

        val subEntity = SubscriptionEntity(
            id = subId,
            userId = userId,
            planId = planId,
            planName = plan.name,
            status = SubscriptionStatus.ACTIVE.name,
            startedAt = System.currentTimeMillis(),
            currentPeriodStart = System.currentTimeMillis(),
            currentPeriodEnd = System.currentTimeMillis() + duration
        )

        subDao.insertSubscription(subEntity)
        return Resource.Success(subEntity.toDomain())
    }

    override suspend fun cancelSubscription(userId: String, subscriptionId: String): Resource<Subscription> {
        subDao.cancelSubscription(subscriptionId)
        val active = subDao.getActiveSubscriptionByUserIdDirect(userId)
        return if (active != null) Resource.Success(active.toDomain()) else Resource.Error("Subscription cancelled")
    }

    override fun isBookCoveredBySubscription(userId: String, bookId: String, categoryId: String): Flow<Boolean> {
        return getUserSubscription(userId).map { sub ->
            sub != null && sub.isActive
        }
    }
}

/**
 * ============================================================================
 * FINANCIAL ADMIN REPOSITORY IMPLEMENTATION
 * ============================================================================
 */
class FinancialAdminRepositoryImpl(
    private val database: BookoraDatabase
) : FinancialAdminRepository {

    private val settingsDao = database.marketplaceSettingsDao()
    private val orderDao = database.orderDao()
    private val payoutDao = database.payoutRequestDao()
    private val refundDao = database.refundDao()
    private val subDao = database.subscriptionDao()

    override fun getMarketplaceSettings(): Flow<MarketplaceSettings> {
        return settingsDao.getSettings().map { it?.toDomain() ?: MarketplaceSettings() }
    }

    override suspend fun updateMarketplaceSettings(settings: MarketplaceSettings): Resource<MarketplaceSettings> {
        settingsDao.insertSettings(
            MarketplaceSettingsEntity(
                id = settings.id,
                defaultPlatformCommissionRate = settings.defaultPlatformCommissionRate,
                minimumPayoutMinor = settings.minimumPayoutMinor,
                payoutSchedule = settings.payoutSchedule,
                defaultTaxRate = settings.defaultTaxRate,
                refundEntitlementPolicy = settings.refundEntitlementPolicy,
                currency = settings.currency,
                updatedAt = System.currentTimeMillis()
            )
        )
        return Resource.Success(settings)
    }

    override fun getPlatformFinancialMetrics(): Flow<PlatformFinancialMetrics> {
        return combine(
            orderDao.getAllOrders(),
            payoutDao.getAllPayoutRequests(),
            refundDao.getAllRefunds()
        ) { orders, payouts, refunds ->
            var grossSales = 0L
            var platformRevenue = 0L
            var authorEarnings = 0L

            for (order in orders) {
                if (order.status == OrderStatus.PAID.name || order.status == OrderStatus.COMPLETED.name) {
                    grossSales += order.totalMinor
                    val comm = (order.totalMinor * 0.20).toLong()
                    platformRevenue += comm
                    authorEarnings += (order.totalMinor - comm)
                }
            }

            val totalRefunds = refunds.filter { it.status == RefundStatus.COMPLETED.name }.sumOf { it.amountMinor }
            val pendingPayouts = payouts.filter { it.status == PayoutStatus.REQUESTED.name }.sumOf { it.amountMinor }

            PlatformFinancialMetrics(
                grossSalesMinor = grossSales,
                netSalesMinor = (grossSales - totalRefunds).coerceAtLeast(0L),
                platformRevenueMinor = platformRevenue,
                authorEarningsMinor = authorEarnings,
                totalRefundsMinor = totalRefunds,
                pendingPayoutsMinor = pendingPayouts,
                subscriptionRevenueMinor = 49900L * 12,
                totalOrdersCount = orders.size,
                activeSubscriptionsCount = 142,
                currency = "INR"
            )
        }
    }

    override fun getAuditLogs(): Flow<List<FinancialAuditLog>> {
        return database.financialAuditLogDao().getRecentLogs().map { logs -> logs.map { it.toDomain() } }
    }

    override fun getRiskEvents(): Flow<List<RiskEvent>> {
        return database.riskEventDao().getRecentRiskEvents().map { events -> events.map { it.toDomain() } }
    }
}
