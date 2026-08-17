package com.example.presentation.viewmodel.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.financial.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.financial.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ============================================================================
// CART VIEW MODEL
// ============================================================================

data class CartUiState(
    val cart: Cart = Cart("empty", ""),
    val calculation: CartCalculation = CartCalculation(emptyList(), 0L, 0L, 0L, 0L),
    val couponInput: String = "",
    val appliedCoupon: Coupon? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)

class CartViewModel(
    private val cartRepository: CartRepository,
    private val couponRepository: CouponRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CartUiState(isLoading = true))
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    init {
        loadCart()
    }

    fun loadCart() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val userId = user?.id ?: "u-default-reader-001"
                cartRepository.getCart(userId).collect { cart ->
                    val calcRes = cartRepository.calculateAuthoritativeCart(userId, _uiState.value.appliedCoupon?.code)
                    val calculation = if (calcRes is Resource.Success) calcRes.data else CartCalculation(emptyList(), 0L, 0L, 0L, 0L)
                    _uiState.update {
                        it.copy(
                            cart = cart,
                            calculation = calculation,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun onCouponInputChanged(code: String) {
        _uiState.update { it.copy(couponInput = code, errorMessage = null) }
    }

    fun applyCoupon() {
        val code = _uiState.value.couponInput.trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            val subtotal = _uiState.value.calculation.subtotalMinor

            when (val res = couponRepository.validateCoupon(code, subtotal, userId)) {
                is Resource.Success -> {
                    val coupon = res.data
                    val calcRes = cartRepository.calculateAuthoritativeCart(userId, coupon.code)
                    val calc = if (calcRes is Resource.Success) calcRes.data else _uiState.value.calculation
                    _uiState.update {
                        it.copy(
                            appliedCoupon = coupon,
                            calculation = calc,
                            couponInput = "",
                            isLoading = false,
                            message = "Coupon ${coupon.code} applied successfully!"
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun removeCoupon() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            val calcRes = cartRepository.calculateAuthoritativeCart(userId, null)
            val calc = if (calcRes is Resource.Success) calcRes.data else _uiState.value.calculation
            _uiState.update {
                it.copy(
                    appliedCoupon = null,
                    calculation = calc,
                    message = "Coupon removed"
                )
            }
        }
    }

    fun addToCart(bookId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            cartRepository.addToCart(userId, bookId)
            loadCart()
        }
    }

    fun removeFromCart(bookId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            cartRepository.removeFromCart(userId, bookId)
            loadCart()
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            cartRepository.clearCart(userId)
            loadCart()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }
}

// ============================================================================
// CHECKOUT VIEW MODEL
// ============================================================================

data class CheckoutUiState(
    val order: Order? = null,
    val paymentResult: PaymentInitiationResult? = null,
    val selectedPaymentMethod: String = "UPI_FAST_PAY",
    val isPreparingOrder: Boolean = false,
    val isProcessingPayment: Boolean = false,
    val paymentSuccess: Boolean = false,
    val isDevPaymentModalOpen: Boolean = false,
    val completedPaymentId: String? = null,
    val errorMessage: String? = null
)

class CheckoutViewModel(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun prepareBuyNowOrder(bookId: String, couponCode: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingOrder = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"

            when (val res = orderRepository.createBuyNowOrder(userId, bookId, couponCode)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(order = res.data, isPreparingOrder = false) }
                    initializePayment(res.data.id)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isPreparingOrder = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun prepareCartOrder(couponCode: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingOrder = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"

            when (val res = orderRepository.createCartOrder(userId, couponCode)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(order = res.data, isPreparingOrder = false) }
                    initializePayment(res.data.id)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isPreparingOrder = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    private fun initializePayment(orderId: String) {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            when (val res = paymentRepository.initializePayment(orderId, userId, _uiState.value.selectedPaymentMethod)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(paymentResult = res.data) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun onPaymentMethodSelected(method: String) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun openDevPaymentModal() {
        _uiState.update { it.copy(isDevPaymentModalOpen = true) }
    }

    fun closeDevPaymentModal() {
        _uiState.update { it.copy(isDevPaymentModalOpen = false) }
    }

    fun completePayment(isSuccessSimulation: Boolean = true) {
        val paymentInit = _uiState.value.paymentResult ?: return
        val order = _uiState.value.order ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessingPayment = true, isDevPaymentModalOpen = false, errorMessage = null) }

            val signature = if (isSuccessSimulation) "DEV_SIG_${System.currentTimeMillis()}" else "SIMULATE_FAILURE"
            val providerPaymentId = if (isSuccessSimulation) "dev_pay_${System.currentTimeMillis()}" else "dev_pay_failed"

            when (val res = paymentRepository.verifyAndCapturePayment(
                orderId = order.id,
                providerOrderId = paymentInit.providerOrderId,
                providerPaymentId = providerPaymentId,
                signature = signature
            )) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentSuccess = true,
                            completedPaymentId = res.data.paymentId
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessingPayment = false,
                            paymentSuccess = false,
                            errorMessage = res.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun resetState() {
        _uiState.value = CheckoutUiState()
    }
}

// ============================================================================
// ORDER HISTORY VIEW MODEL
// ============================================================================

data class OrderHistoryUiState(
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val isLoading: Boolean = false,
    val refundSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository,
    private val refundRepository: RefundRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState(isLoading = true))
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val userId = user?.id ?: "u-default-reader-001"
                orderRepository.getUserOrders(userId).collect { orders ->
                    _uiState.update { it.copy(orders = orders, isLoading = false) }
                }
            }
        }
    }

    fun selectOrder(order: Order?) {
        _uiState.update { it.copy(selectedOrder = order) }
    }

    fun requestRefund(orderId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            when (val res = refundRepository.requestRefund(orderId, userId, reason)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            refundSuccessMessage = "Refund request submitted. Review is in progress."
                        )
                    }
                    loadOrders()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(refundSuccessMessage = null, errorMessage = null) }
    }
}

// ============================================================================
// SUBSCRIPTION VIEW MODEL
// ============================================================================

data class SubscriptionUiState(
    val plans: List<SubscriptionPlan> = emptyList(),
    val currentSubscription: Subscription? = null,
    val isProcessing: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState(isProcessing = true))
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptions()
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            val plansFlow = subscriptionRepository.getPlans()
            val user = authRepository.getCurrentUser().first()
            val userId = user?.id ?: "u-default-reader-001"
            val subFlow = subscriptionRepository.getUserSubscription(userId)

            combine(plansFlow, subFlow) { plans, currentSub ->
                _uiState.update {
                    it.copy(
                        plans = plans,
                        currentSubscription = currentSub,
                        isProcessing = false
                    )
                }
            }.collect()
        }
    }

    fun subscribeToPlan(planId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"

            when (val res = subscriptionRepository.createSubscription(userId, planId)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            currentSubscription = res.data,
                            successMessage = "Welcome to ${res.data.planName}! Full library access unlocked."
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isProcessing = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun cancelSubscription(subscriptionId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
            subscriptionRepository.cancelSubscription(userId, subscriptionId)
            _uiState.update { it.copy(isProcessing = false, currentSubscription = null, successMessage = "Subscription cancelled.") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

// ============================================================================
// AUTHOR EARNINGS VIEW MODEL
// ============================================================================

data class AuthorEarningsUiState(
    val summary: AuthorEarningsSummary? = null,
    val wallet: AuthorWallet? = null,
    val ledger: List<RoyaltyLedger> = emptyList(),
    val payoutRequests: List<PayoutRequest> = emptyList(),
    val isSubmittingPayout: Boolean = false,
    val payoutAmountInput: String = "1000",
    val payoutAccountInput: String = "UPI: elena.author@okaxis",
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AuthorEarningsViewModel(
    private val royaltyRepository: RoyaltyRepository,
    private val walletRepository: WalletRepository,
    private val payoutRepository: PayoutRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthorEarningsUiState())
    val uiState: StateFlow<AuthorEarningsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect { user ->
                val authorId = if (user?.role == com.example.domain.model.UserRole.AUTHOR) user.id else "u-author-elena"

                launch {
                    royaltyRepository.getAuthorEarningsSummary(authorId).collect { sum ->
                        _uiState.update { it.copy(summary = sum) }
                    }
                }
                launch {
                    walletRepository.getWallet(authorId).collect { wal ->
                        _uiState.update { it.copy(wallet = wal) }
                    }
                }
                launch {
                    royaltyRepository.getAuthorLedger(authorId).collect { led ->
                        _uiState.update { it.copy(ledger = led) }
                    }
                }
                launch {
                    payoutRepository.getAuthorPayoutRequests(authorId).collect { payouts ->
                        _uiState.update { it.copy(payoutRequests = payouts) }
                    }
                }
            }
        }
    }

    fun onPayoutAmountChanged(amount: String) {
        _uiState.update { it.copy(payoutAmountInput = amount) }
    }

    fun onPayoutAccountChanged(account: String) {
        _uiState.update { it.copy(payoutAccountInput = account) }
    }

    fun submitPayoutRequest() {
        viewModelScope.launch {
            val amountNum = _uiState.value.payoutAmountInput.toDoubleOrNull() ?: 0.0
            val amountMinor = (amountNum * 100L).toLong()
            val account = _uiState.value.payoutAccountInput.trim()

            if (amountMinor <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid payout amount") }
                return@launch
            }

            _uiState.update { it.copy(isSubmittingPayout = true, errorMessage = null) }
            val user = authRepository.getCurrentUser().first()
            val authorId = if (user?.role == com.example.domain.model.UserRole.AUTHOR) user.id else "u-author-elena"

            when (val res = walletRepository.requestPayout(authorId, amountMinor, account)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingPayout = false,
                            successMessage = "Payout of ${res.data.amountMoney.formatted} requested successfully! Under admin review."
                        )
                    }
                    loadData()
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isSubmittingPayout = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}

// ============================================================================
// ADMIN FINANCIAL VIEW MODEL
// ============================================================================

enum class AdminFinanceTab {
    OVERVIEW,
    PAYOUTS,
    REFUNDS,
    COUPONS,
    SETTINGS,
    AUDIT_LOGS
}

data class AdminFinancialUiState(
    val metrics: PlatformFinancialMetrics? = null,
    val settings: MarketplaceSettings = MarketplaceSettings(),
    val recentOrders: List<Order> = emptyList(),
    val payoutRequests: List<PayoutRequest> = emptyList(),
    val refunds: List<Refund> = emptyList(),
    val auditLogs: List<FinancialAuditLog> = emptyList(),
    val riskEvents: List<RiskEvent> = emptyList(),
    val activeTab: AdminFinanceTab = AdminFinanceTab.OVERVIEW,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AdminFinancialViewModel(
    private val adminRepository: FinancialAdminRepository,
    private val payoutRepository: PayoutRepository,
    private val refundRepository: RefundRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminFinancialUiState(isLoading = true))
    val uiState: StateFlow<AdminFinancialUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            launch {
                adminRepository.getPlatformFinancialMetrics().collect { m ->
                    _uiState.update { it.copy(metrics = m, isLoading = false) }
                }
            }
            launch {
                adminRepository.getMarketplaceSettings().collect { s ->
                    _uiState.update { it.copy(settings = s) }
                }
            }
            launch {
                orderRepository.getAllOrders().collect { ords ->
                    _uiState.update { it.copy(recentOrders = ords) }
                }
            }
            launch {
                payoutRepository.getAllPayoutRequests().collect { p ->
                    _uiState.update { it.copy(payoutRequests = p) }
                }
            }
            launch {
                refundRepository.getAllRefunds().collect { r ->
                    _uiState.update { it.copy(refunds = r) }
                }
            }
            launch {
                adminRepository.getAuditLogs().collect { logs ->
                    _uiState.update { it.copy(auditLogs = logs) }
                }
            }
            launch {
                adminRepository.getRiskEvents().collect { events ->
                    _uiState.update { it.copy(riskEvents = events) }
                }
            }
        }
    }

    fun setTab(tab: AdminFinanceTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun approvePayout(payoutId: String) {
        viewModelScope.launch {
            val ref = "bank_ref_tx_${System.currentTimeMillis().toString().takeLast(8)}"
            when (val res = payoutRepository.approvePayout(payoutId, "admin_user_001", ref)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(successMessage = "Payout approved & settled via $ref") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun rejectPayout(payoutId: String, reason: String = "Verification failed or suspicious ledger") {
        viewModelScope.launch {
            when (val res = payoutRepository.rejectPayout(payoutId, "admin_user_001", reason)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(successMessage = "Payout rejected and funds refunded to author wallet.") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun approveRefund(refundId: String) {
        viewModelScope.launch {
            when (val res = refundRepository.approveAndProcessRefund(refundId, "admin_user_001")) {
                is Resource.Success -> {
                    _uiState.update { it.copy(successMessage = "Refund processed, book access revoked, and royalties reversed.") }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun updateCommissionRate(rate: Double) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            val updated = current.copy(defaultPlatformCommissionRate = rate)
            adminRepository.updateMarketplaceSettings(updated)
            _uiState.update { it.copy(settings = updated, successMessage = "Platform commission updated to ${(rate * 100).toInt()}%") }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
