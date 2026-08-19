package com.example.domain.repository.review

import com.example.core.result.Resource
import com.example.domain.model.review.*
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    /**
     * Authoritatively check whether the user is eligible to write a review,
     * whether they qualify for the Verified Reader badge, or if they are blocked.
     */
    suspend fun checkReviewEligibility(userId: String, bookId: String): ReviewEligibility

    /**
     * Authoritatively evaluate and retrieve reader verification status for a given user and book.
     */
    fun getReaderVerification(userId: String, bookId: String): Flow<ReaderVerification?>

    /**
     * Records a granular reading session, updating reading activity, progress, and re-evaluating verification eligibility.
     */
    suspend fun recordReadingSession(
        userId: String,
        bookId: String,
        durationSeconds: Long,
        startPage: Int,
        endPage: Int,
        totalPages: Int
    ): Resource<ReaderVerification>

    /**
     * Submits a book review with authoritative verification check, anti-spam validation, and dynamic rating recalculation.
     */
    suspend fun submitReview(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        bookId: String,
        rating: Int,
        title: String,
        reviewText: String
    ): Resource<BookReview>

    /**
     * Updates an existing review written by the user.
     */
    suspend fun updateReview(
        reviewId: String,
        userId: String,
        rating: Int,
        title: String,
        reviewText: String
    ): Resource<BookReview>

    /**
     * Deletes an existing review and recalculates the book rating aggregate.
     */
    suspend fun deleteReview(reviewId: String, userId: String): Resource<Unit>

    /**
     * Toggles a Helpful vote on a review. Prevents self-voting.
     */
    suspend fun toggleHelpfulVote(reviewId: String, userId: String): Resource<Boolean>

    /**
     * Reports a review for spam, spoilers, harassment, or manipulation.
     */
    suspend fun reportReview(
        reviewId: String,
        reporterUserId: String,
        reason: ReportReason,
        details: String
    ): Resource<Unit>

    /**
     * Admin moderation action on a review (APPROVE, HIDE, REMOVE, RESTORE, REJECT).
     */
    suspend fun moderateReview(
        reviewId: String,
        adminUserId: String,
        action: ReviewAuditAction,
        reason: String
    ): Resource<Unit>

    /**
     * Authoritatively calculates and streams rating distribution & summary for a book.
     */
    fun getRatingSummary(bookId: String): Flow<RatingSummary>

    /**
     * Retrieves published reviews for a book, sorted and with current user's helpful vote status.
     */
    fun getReviewsForBook(
        bookId: String,
        sortOption: ReviewSortOption,
        currentUserId: String?
    ): Flow<List<BookReview>>

    /**
     * Retrieves current user's review for a book if any.
     */
    fun getUserReview(userId: String, bookId: String): Flow<BookReview?>

    /**
     * Retrieves all reviews for admin moderation.
     */
    fun getAllReviewsForModeration(): Flow<List<BookReview>>

    /**
     * Retrieves all pending reports for admin review.
     */
    fun getPendingReports(): Flow<List<ReviewReport>>

    /**
     * Authoritatively recalculates aggregate rating for a book and writes to BookEntity.
     */
    suspend fun recalculateBookRating(bookId: String)
}
