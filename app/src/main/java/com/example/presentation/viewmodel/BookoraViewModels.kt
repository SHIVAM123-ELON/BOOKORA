package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.Author
import com.example.domain.model.Book
import com.example.domain.model.BookFilter
import com.example.domain.model.BookSortOption
import com.example.domain.model.Category
import com.example.domain.model.LibraryItem
import com.example.domain.model.ReadingProgress
import com.example.domain.model.ReadingStatus
import com.example.domain.model.RecentSearch
import com.example.domain.model.UiState
import com.example.domain.model.User
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthorRepository
import com.example.domain.repository.BookRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.WishlistRepository
import com.example.domain.repository.financial.*
import com.example.domain.publisher.PdfValidationService
import com.example.domain.repository.offline.OfflineBookRepository
import com.example.domain.repository.publisher.PublisherRepository
import com.example.domain.repository.review.ReviewRepository
import com.example.presentation.viewmodel.financial.*
import com.example.presentation.viewmodel.offline.OfflineReaderViewModel
import com.example.presentation.viewmodel.publisher.*
import com.example.presentation.viewmodel.review.*
import com.example.presentation.viewmodel.scanner.BookScannerViewModel
import com.example.presentation.viewmodel.voice.VoiceConversationViewModel
import android.app.Application
import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. Auth ViewModel
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: StateFlow<Boolean> = authRepository.isUserLoggedIn()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<Resource<Unit>?>(null)
    val passwordResetState: StateFlow<Resource<Unit>?> = _passwordResetState.asStateFlow()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.login(email, pass)
        }
    }

    fun register(fullName: String, email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.register(fullName, email, pass)
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.signInWithGoogle(context)
        }
    }

    fun signInWithGoogleIdToken(idToken: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            _authState.value = authRepository.signInWithGoogleIdToken(idToken)
        }
    }

    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _passwordResetState.value = Resource.Loading
            _passwordResetState.value = authRepository.sendPasswordResetEmail(email)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = null
        }
    }

    fun resetAuthState() {
        _authState.value = null
        _passwordResetState.value = null
    }
}

// 2. Home ViewModel
class HomeViewModel(
    private val bookRepository: BookRepository,
    private val categoryRepository: CategoryRepository,
    private val authorRepository: AuthorRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val continueReading: StateFlow<LibraryItem?> = libraryRepository.getUserLibrary()
        .map { list -> list.firstOrNull { it.status == ReadingStatus.IN_PROGRESS } ?: list.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val trendingBooks: StateFlow<UiState<List<Book>>> = bookRepository.getTrendingBooks()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load trending books", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val bestSellerBooks: StateFlow<UiState<List<Book>>> = bookRepository.getBestSellers()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load best sellers", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val newReleaseBooks: StateFlow<UiState<List<Book>>> = bookRepository.getNewReleases()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load new releases", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val recommendedBooks: StateFlow<UiState<List<Book>>> = bookRepository.getRecommendedBooks()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load recommendations", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val popularAuthors: StateFlow<UiState<List<Author>>> = authorRepository.getPopularAuthors()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load authors", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val categories: StateFlow<UiState<List<Category>>> = categoryRepository.getCategories()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load categories", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val deals: StateFlow<UiState<List<Book>>> = bookRepository.getDeals()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load deals", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            bookRepository.refreshBooks()
            categoryRepository.refreshCategories()
            authorRepository.refreshAuthors()
            _isRefreshing.value = false
        }
    }
}

// 3. Search ViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(BookFilter())
    val filter: StateFlow<BookFilter> = _filter.asStateFlow()

    val recentSearches: StateFlow<List<RecentSearch>> = searchRepository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<UiState<List<Book>>> = combine(
        _searchQuery.debounce(300).distinctUntilChanged(),
        _filter
    ) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        searchRepository.searchBooks(query, filter)
            .map { list ->
                if (list.isEmpty() && query.isNotBlank()) {
                    UiState.Empty
                } else {
                    UiState.Success(list)
                }
            }
            .catch { emit(UiState.Error(it.message ?: "Search failed", it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isNotBlank()) {
                searchRepository.saveRecentSearch(query)
            }
        }
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.deleteRecentSearch(query)
        }
    }

    fun clearAllRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun updateCategoryFilter(categoryId: String?) {
        _filter.value = _filter.value.copy(
            categoryId = if (_filter.value.categoryId == categoryId) null else categoryId
        )
    }

    fun updateSortOption(sortOption: BookSortOption) {
        _filter.value = _filter.value.copy(sortOption = sortOption)
    }

    fun updateFilter(filter: BookFilter) {
        _filter.value = filter
    }
}

// 4. Explore ViewModel
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExploreViewModel(
    private val searchRepository: SearchRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(BookFilter())
    val filter: StateFlow<BookFilter> = _filter.asStateFlow()

    val categories: StateFlow<UiState<List<Category>>> = categoryRepository.getCategories()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load categories", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val exploreBooks: StateFlow<UiState<List<Book>>> = combine(
        _searchQuery.debounce(300).distinctUntilChanged(),
        _filter
    ) { query, filter ->
        query to filter
    }.flatMapLatest { (query, filter) ->
        searchRepository.searchBooks(query, filter)
            .map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { emit(UiState.Error(it.message ?: "Failed to load books", it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _filter.value = _filter.value.copy(
            categoryId = if (_filter.value.categoryId == categoryId) null else categoryId
        )
    }

    fun selectSortOption(sortOption: BookSortOption) {
        _filter.value = _filter.value.copy(sortOption = sortOption)
    }

    fun setRatingFilter(rating: Double?) {
        _filter.value = _filter.value.copy(minRating = rating)
    }

    fun setLanguageFilter(lang: String?) {
        _filter.value = _filter.value.copy(language = lang)
    }

    fun resetFilters() {
        _filter.value = BookFilter()
        _searchQuery.value = ""
    }
}

// 5. Book Details ViewModel
class BookDetailsViewModel(
    private val bookRepository: BookRepository,
    private val wishlistRepository: WishlistRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    fun getBookState(bookId: String): StateFlow<UiState<Book>> {
        return bookRepository.getBookById(bookId)
            .map { book ->
                if (book != null) UiState.Success(book) else UiState.Empty
            }
            .catch { emit(UiState.Error(it.message ?: "Failed to load book details", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    }

    fun getSimilarBooks(categoryId: String, excludeBookId: String): StateFlow<List<Book>> {
        return bookRepository.getSimilarBooks(categoryId, excludeBookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getAuthorBooks(authorId: String, excludeBookId: String): StateFlow<List<Book>> {
        return bookRepository.getBooksByAuthor(authorId, excludeBookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun isInWishlist(bookId: String): StateFlow<Boolean> {
        return wishlistRepository.isInWishlist(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    fun isBookEntitled(bookId: String): StateFlow<Boolean> {
        return libraryRepository.isBookEntitled(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    }

    fun getReadingProgress(bookId: String): StateFlow<ReadingProgress?> {
        return libraryRepository.getReadingProgress(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun toggleWishlist(bookId: String) {
        viewModelScope.launch {
            wishlistRepository.toggleWishlist(bookId)
        }
    }

    fun buyOrClaimBook(bookId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            libraryRepository.addBookToLibrary(bookId)
            onComplete()
        }
    }

    fun updateProgress(bookId: String, page: Int, totalPages: Int) {
        viewModelScope.launch {
            libraryRepository.saveProgress(bookId, page, totalPages)
        }
    }
}

// 6. Wishlist ViewModel
class WishlistViewModel(
    private val wishlistRepository: WishlistRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    val wishlistItems: StateFlow<UiState<List<Book>>> = wishlistRepository.getWishlistBooks()
        .map { if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "Failed to load wishlist", it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(bookId)
        }
    }

    fun addToLibrary(bookId: String) {
        viewModelScope.launch {
            libraryRepository.addBookToLibrary(bookId)
            wishlistRepository.removeFromWishlist(bookId)
        }
    }
}

// 7. Library ViewModel
class LibraryViewModel(
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow("ALL") // ALL, READING, COMPLETED, DOWNLOADED
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    val libraryItems: StateFlow<UiState<List<LibraryItem>>> = combine(
        libraryRepository.getUserLibrary(),
        _selectedTab
    ) { items, tab ->
        val filtered = when (tab) {
            "READING" -> items.filter { it.status == ReadingStatus.IN_PROGRESS }
            "COMPLETED" -> items.filter { it.status == ReadingStatus.COMPLETED || it.readingProgress >= 99f }
            "DOWNLOADED" -> items.filter { it.isDownloaded }
            else -> items
        }
        if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }.catch {
        emit(UiState.Error(it.message ?: "Failed to load library items", it))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun updateProgress(bookId: String, page: Int, totalPages: Int) {
        viewModelScope.launch {
            libraryRepository.saveProgress(bookId, page, totalPages)
        }
    }

    fun toggleDownload(bookId: String, currentDownload: Boolean) {
        viewModelScope.launch {
            libraryRepository.toggleDownload(bookId, !currentDownload)
        }
    }
}

// Factory for Dependency Injection
class ViewModelFactory(
    private val authRepo: AuthRepository,
    private val bookRepo: BookRepository,
    private val catRepo: CategoryRepository,
    private val authorRepo: AuthorRepository,
    private val searchRepo: SearchRepository,
    private val wishlistRepo: WishlistRepository,
    private val libRepo: LibraryRepository,
    private val cartRepo: CartRepository? = null,
    private val couponRepo: CouponRepository? = null,
    private val orderRepo: OrderRepository? = null,
    private val paymentRepo: PaymentRepository? = null,
    private val refundRepo: RefundRepository? = null,
    private val subRepo: SubscriptionRepository? = null,
    private val royaltyRepo: RoyaltyRepository? = null,
    private val walletRepo: WalletRepository? = null,
    private val payoutRepo: PayoutRepository? = null,
    private val adminRepo: FinancialAdminRepository? = null,
    private val publisherRepo: PublisherRepository? = null,
    private val pdfValidationService: PdfValidationService? = null,
    private val reviewRepo: ReviewRepository? = null,
    private val offlineBookRepo: OfflineBookRepository? = null,
    private val paymentLinkRepo: PaymentLinkRepository? = null,
    private val application: Application? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(authRepo) as T
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(bookRepo, catRepo, authorRepo, libRepo) as T
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(searchRepo) as T
            modelClass.isAssignableFrom(ExploreViewModel::class.java) -> ExploreViewModel(searchRepo, catRepo) as T
            modelClass.isAssignableFrom(BookDetailsViewModel::class.java) -> BookDetailsViewModel(bookRepo, wishlistRepo, libRepo) as T
            modelClass.isAssignableFrom(WishlistViewModel::class.java) -> WishlistViewModel(wishlistRepo, libRepo) as T
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> LibraryViewModel(libRepo) as T
            modelClass.isAssignableFrom(CartViewModel::class.java) -> {
                if (cartRepo != null && couponRepo != null) CartViewModel(cartRepo, couponRepo, authRepo) as T
                else throw IllegalArgumentException("CartRepository and CouponRepository required")
            }
            modelClass.isAssignableFrom(CheckoutViewModel::class.java) -> {
                if (orderRepo != null && paymentRepo != null) CheckoutViewModel(orderRepo, paymentRepo, authRepo, paymentLinkRepo) as T
                else throw IllegalArgumentException("OrderRepository and PaymentRepository required")
            }
            modelClass.isAssignableFrom(OrderHistoryViewModel::class.java) -> {
                if (orderRepo != null && refundRepo != null) OrderHistoryViewModel(orderRepo, refundRepo, authRepo) as T
                else throw IllegalArgumentException("OrderRepository and RefundRepository required")
            }
            modelClass.isAssignableFrom(SubscriptionViewModel::class.java) -> {
                if (subRepo != null) SubscriptionViewModel(subRepo, authRepo) as T
                else throw IllegalArgumentException("SubscriptionRepository required")
            }
            modelClass.isAssignableFrom(AuthorEarningsViewModel::class.java) -> {
                if (royaltyRepo != null && walletRepo != null && payoutRepo != null) AuthorEarningsViewModel(royaltyRepo, walletRepo, payoutRepo, authRepo) as T
                else throw IllegalArgumentException("Royalty, Wallet and Payout repositories required")
            }
            modelClass.isAssignableFrom(AdminFinancialViewModel::class.java) -> {
                if (adminRepo != null && payoutRepo != null && refundRepo != null && orderRepo != null) AdminFinancialViewModel(adminRepo, payoutRepo, refundRepo, orderRepo, paymentLinkRepo) as T
                else throw IllegalArgumentException("Admin, Payout, Refund, Order repositories required")
            }
            modelClass.isAssignableFrom(PaymentLinkViewModel::class.java) -> {
                if (paymentLinkRepo != null) PaymentLinkViewModel(paymentLinkRepo, authRepo) as T
                else throw IllegalArgumentException("PaymentLinkRepository required")
            }
            modelClass.isAssignableFrom(UploadBookViewModel::class.java) -> {

                if (publisherRepo != null && pdfValidationService != null) UploadBookViewModel(publisherRepo, authRepo, pdfValidationService) as T
                else throw IllegalArgumentException("PublisherRepository and PdfValidationService required")
            }
            modelClass.isAssignableFrom(CreatorEarningsViewModel::class.java) -> {
                if (publisherRepo != null) CreatorEarningsViewModel(publisherRepo, authRepo) as T
                else throw IllegalArgumentException("PublisherRepository required")
            }
            modelClass.isAssignableFrom(AdminModerationViewModel::class.java) -> {
                if (publisherRepo != null) AdminModerationViewModel(publisherRepo, authRepo) as T
                else throw IllegalArgumentException("PublisherRepository required")
            }
            modelClass.isAssignableFrom(BookReviewsViewModel::class.java) -> {
                if (reviewRepo != null) BookReviewsViewModel(reviewRepo, authRepo) as T
                else throw IllegalArgumentException("ReviewRepository required")
            }
            modelClass.isAssignableFrom(ReadingSessionViewModel::class.java) -> {
                if (reviewRepo != null) ReadingSessionViewModel(reviewRepo, authRepo) as T
                else throw IllegalArgumentException("ReviewRepository required")
            }
            modelClass.isAssignableFrom(AdminReviewModerationViewModel::class.java) -> {
                if (reviewRepo != null) AdminReviewModerationViewModel(reviewRepo, authRepo) as T
                else throw IllegalArgumentException("ReviewRepository required")
            }
            modelClass.isAssignableFrom(OfflineReaderViewModel::class.java) -> {
                if (offlineBookRepo != null) OfflineReaderViewModel(offlineBookRepo) as T
                else throw IllegalArgumentException("OfflineBookRepository required")
            }
            modelClass.isAssignableFrom(VoiceConversationViewModel::class.java) -> {
                if (application != null) VoiceConversationViewModel(application, bookRepo) as T
                else throw IllegalArgumentException("Application context required for VoiceConversationViewModel")
            }
            modelClass.isAssignableFrom(BookScannerViewModel::class.java) -> {
                BookScannerViewModel(bookRepo, wishlistRepo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
