package com.example.presentation.viewmodel.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.UiState
import com.example.domain.model.review.*
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.review.ReviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BookReviewsViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow("")
    val currentBookId: StateFlow<String> = _currentBookId.asStateFlow()

    private val _sortOption = MutableStateFlow(ReviewSortOption.MOST_HELPFUL)
    val sortOption: StateFlow<ReviewSortOption> = _sortOption.asStateFlow()

    private val _reviewActionState = MutableStateFlow<UiState<String>>(UiState.Empty)
    val reviewActionState: StateFlow<UiState<String>> = _reviewActionState.asStateFlow()

    private val _eligibilityState = MutableStateFlow<ReviewEligibility?>(null)
    val eligibilityState: StateFlow<ReviewEligibility?> = _eligibilityState.asStateFlow()

    val currentUserId: StateFlow<String> = authRepository.getCurrentUser()
        .map { it?.id ?: "u-default-reader-001" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "u-default-reader-001")

    val currentUserName: StateFlow<String> = authRepository.getCurrentUser()
        .map { it?.fullName ?: "Bookora Reader" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Bookora Reader")

    fun setBookId(bookId: String) {
        if (_currentBookId.value != bookId) {
            _currentBookId.value = bookId
            refreshEligibility(bookId)
        }
    }

    fun setSortOption(option: ReviewSortOption) {
        _sortOption.value = option
    }

    fun refreshEligibility(bookId: String = _currentBookId.value) {
        if (bookId.isBlank()) return
        viewModelScope.launch {
            val userId = currentUserId.value
            val result = reviewRepository.checkReviewEligibility(userId, bookId)
            _eligibilityState.value = result
        }
    }

    fun getRatingSummary(bookId: String): StateFlow<RatingSummary> {
        return reviewRepository.getRatingSummary(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RatingSummary(bookId))
    }

    fun getReviews(bookId: String): StateFlow<UiState<List<BookReview>>> {
        return combine(_sortOption, currentUserId) { sort, userId ->
            sort to userId
        }.flatMapLatest { (sort, userId) ->
            reviewRepository.getReviewsForBook(bookId, sort, userId)
                .map { list ->
                    if (list.isEmpty()) UiState.Empty else UiState.Success(list)
                }
                .catch { emit(UiState.Error(it.message ?: "Failed to load reviews", it)) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    }

    fun getUserReview(bookId: String): StateFlow<BookReview?> {
        return currentUserId.flatMapLatest { userId ->
            reviewRepository.getUserReview(userId, bookId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    fun submitReview(
        bookId: String,
        rating: Int,
        title: String,
        reviewText: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _reviewActionState.value = UiState.Loading
            val userId = currentUserId.value
            val userName = currentUserName.value
            val userAvatar = authRepository.getCurrentUser().first()?.avatarUrl

            when (val res = reviewRepository.submitReview(
                userId = userId,
                userName = userName,
                userAvatarUrl = userAvatar,
                bookId = bookId,
                rating = rating,
                title = title,
                reviewText = reviewText
            )) {
                is Resource.Success -> {
                    _reviewActionState.value = UiState.Success("Review submitted successfully!")
                    refreshEligibility(bookId)
                    onSuccess()
                }
                is Resource.Error -> {
                    _reviewActionState.value = UiState.Error(res.message ?: "Failed to submit review")
                }
                is Resource.Loading -> {
                    _reviewActionState.value = UiState.Loading
                }
            }
        }
    }

    fun updateReview(
        reviewId: String,
        rating: Int,
        title: String,
        reviewText: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _reviewActionState.value = UiState.Loading
            val userId = currentUserId.value
            when (val res = reviewRepository.updateReview(
                reviewId = reviewId,
                userId = userId,
                rating = rating,
                title = title,
                reviewText = reviewText
            )) {
                is Resource.Success -> {
                    _reviewActionState.value = UiState.Success("Review updated successfully!")
                    refreshEligibility(_currentBookId.value)
                    onSuccess()
                }
                is Resource.Error -> {
                    _reviewActionState.value = UiState.Error(res.message ?: "Failed to update review")
                }
                is Resource.Loading -> {
                    _reviewActionState.value = UiState.Loading
                }
            }
        }
    }

    fun deleteReview(reviewId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _reviewActionState.value = UiState.Loading
            val userId = currentUserId.value
            when (val res = reviewRepository.deleteReview(reviewId, userId)) {
                is Resource.Success -> {
                    _reviewActionState.value = UiState.Success("Review removed.")
                    refreshEligibility(_currentBookId.value)
                    onSuccess()
                }
                is Resource.Error -> {
                    _reviewActionState.value = UiState.Error(res.message ?: "Failed to delete review")
                }
                is Resource.Loading -> {
                    _reviewActionState.value = UiState.Loading
                }
            }
        }
    }

    fun toggleHelpfulVote(reviewId: String) {
        viewModelScope.launch {
            val userId = currentUserId.value
            reviewRepository.toggleHelpfulVote(reviewId, userId)
        }
    }

    fun reportReview(
        reviewId: String,
        reason: ReportReason,
        details: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val userId = currentUserId.value
            when (val res = reviewRepository.reportReview(reviewId, userId, reason, details)) {
                is Resource.Success -> onComplete(true, "Report submitted. Our moderation team will investigate.")
                is Resource.Error -> onComplete(false, res.message ?: "Failed to submit report.")
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetActionState() {
        _reviewActionState.value = UiState.Empty
    }
}

class AdminReviewModerationViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedTab = MutableStateFlow("ALL") // ALL, REPORTED, HIDDEN, PUBLISHED
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    private val _moderationActionState = MutableStateFlow<UiState<String>>(UiState.Empty)
    val moderationActionState: StateFlow<UiState<String>> = _moderationActionState.asStateFlow()

    val reviewsList: StateFlow<UiState<List<BookReview>>> = combine(
        reviewRepository.getAllReviewsForModeration(),
        _selectedTab
    ) { reviews, tab ->
        val filtered = when (tab) {
            "REPORTED" -> reviews.filter { it.reportCount > 0 || it.moderationStatus == ReviewModerationStatus.PENDING_REVIEW }
            "HIDDEN" -> reviews.filter { it.moderationStatus == ReviewModerationStatus.HIDDEN || it.moderationStatus == ReviewModerationStatus.REMOVED || it.moderationStatus == ReviewModerationStatus.REJECTED }
            "PUBLISHED" -> reviews.filter { it.moderationStatus == ReviewModerationStatus.PUBLISHED }
            else -> reviews
        }
        if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
    }.catch {
        emit(UiState.Error(it.message ?: "Failed to load reviews for moderation", it))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val pendingReports: StateFlow<List<ReviewReport>> = reviewRepository.getPendingReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    fun performAction(
        reviewId: String,
        action: ReviewAuditAction,
        reason: String
    ) {
        viewModelScope.launch {
            _moderationActionState.value = UiState.Loading
            val adminId = authRepository.getCurrentUser().first()?.id ?: "admin-root"
            when (val res = reviewRepository.moderateReview(reviewId, adminId, action, reason)) {
                is Resource.Success -> {
                    _moderationActionState.value = UiState.Success("Review updated to ${action.name}")
                }
                is Resource.Error -> {
                    _moderationActionState.value = UiState.Error(res.message ?: "Moderation action failed")
                }
                is Resource.Loading -> {
                    _moderationActionState.value = UiState.Loading
                }
            }
        }
    }

    fun resetActionState() {
        _moderationActionState.value = UiState.Empty
    }
}
