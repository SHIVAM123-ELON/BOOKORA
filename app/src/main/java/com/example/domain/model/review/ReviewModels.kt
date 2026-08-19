package com.example.domain.model.review

/**
 * Reader Verification Status for Reviews and Profiles.
 */
enum class ReviewVerificationStatus {
    VERIFIED_READER,      // Valid active entitlement + verified reading threshold met
    UNVERIFIED_REVIEWER   // No verified entitlement or reading threshold not yet reached
}

/**
 * Moderation and Visibility Lifecycle for Book Reviews.
 */
enum class ReviewModerationStatus {
    PUBLISHED,       // Visible to all readers
    PENDING_REVIEW,  // Awaiting moderator review (spam flag or flagged user)
    HIDDEN,          // Hidden from public view by moderation
    REMOVED,         // Soft-removed by admin or user
    REJECTED         // Explicitly rejected by moderation policy
}

/**
 * Sorting criteria for Book Reviews.
 */
enum class ReviewSortOption(val displayName: String) {
    MOST_HELPFUL("Most Helpful"),
    NEWEST("Newest"),
    HIGHEST_RATING("Highest Rating"),
    LOWEST_RATING("Lowest Rating"),
    VERIFIED_FIRST("Verified Readers First")
}

/**
 * Reason for reporting a review.
 */
enum class ReportReason(val displayName: String) {
    SPAM("Spam / Commercial advertising"),
    INAPPROPRIATE("Inappropriate or abusive language"),
    SPOILER("Unmarked major plot spoilers"),
    FAKE_REVIEW("Fake / Manipulated review"),
    HARASSMENT("Targeted harassment or hate speech"),
    OFF_TOPIC("Not relevant to the book"),
    OTHER("Other violation")
}

/**
 * Moderation Actions for Audit Trail.
 */
enum class ReviewAuditAction {
    APPROVE,
    HIDE,
    REMOVE,
    RESTORE,
    FLAG_SUSPICIOUS,
    REJECT
}

/**
 * Domain Model for Reader Verification State.
 */
data class ReaderVerification(
    val id: String,
    val userId: String,
    val bookId: String,
    val isVerified: Boolean,
    val status: String, // VERIFIED, UNVERIFIED, REVOKED, NO_ENTITLEMENT, PENDING_READING_ACTIVITY
    val entitlementSource: String? = null,
    val readingMinutes: Double = 0.0,
    val readingPercent: Float = 0f,
    val verificationReason: String = "",
    val verifiedAt: Long? = null,
    val revokedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Domain Model for Individual Reading Session.
 */
data class ReadingSession(
    val id: String,
    val userId: String,
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val startProgress: Float,
    val endProgress: Float,
    val startPage: Int,
    val endPage: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Domain Model for Aggregate Reading Activity.
 */
data class ReadingActivity(
    val id: String,
    val userId: String,
    val bookId: String,
    val totalReadingSeconds: Long = 0L,
    val furthestPage: Int = 1,
    val currentPage: Int = 1,
    val readingPercent: Float = 0f,
    val isCompleted: Boolean = false,
    val sessionCount: Int = 0,
    val lastSessionAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalReadingMinutes: Double get() = totalReadingSeconds / 60.0
}

/**
 * Domain Model for a Book Review.
 */
data class BookReview(
    val id: String,
    val userId: String,
    val bookId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val rating: Int, // 1 to 5
    val title: String,
    val reviewText: String,
    val verificationStatus: ReviewVerificationStatus = ReviewVerificationStatus.UNVERIFIED_REVIEWER,
    val moderationStatus: ReviewModerationStatus = ReviewModerationStatus.PUBLISHED,
    val helpfulCount: Int = 0,
    val reportCount: Int = 0,
    val isEdited: Boolean = false,
    val isHelpfulByCurrentUser: Boolean = false,
    val authorReply: String? = null,
    val authorRepliedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isVerifiedReader: Boolean get() = verificationStatus == ReviewVerificationStatus.VERIFIED_READER
}

/**
 * Authoritative Aggregate Rating Summary for a Book.
 */
data class RatingSummary(
    val bookId: String,
    val averageRating: Double = 0.0,
    val totalReviews: Int = 0,
    val verifiedReviewsCount: Int = 0,
    val unverifiedReviewsCount: Int = 0,
    val oneStarCount: Int = 0,
    val twoStarCount: Int = 0,
    val threeStarCount: Int = 0,
    val fourStarCount: Int = 0,
    val fiveStarCount: Int = 0
) {
    val fiveStarPercent: Float get() = if (totalReviews > 0) fiveStarCount.toFloat() / totalReviews else 0f
    val fourStarPercent: Float get() = if (totalReviews > 0) fourStarCount.toFloat() / totalReviews else 0f
    val threeStarPercent: Float get() = if (totalReviews > 0) threeStarCount.toFloat() / totalReviews else 0f
    val twoStarPercent: Float get() = if (totalReviews > 0) twoStarCount.toFloat() / totalReviews else 0f
    val oneStarPercent: Float get() = if (totalReviews > 0) oneStarCount.toFloat() / totalReviews else 0f
    val formattedAverage: String get() = "%.1f".format(averageRating)
}

/**
 * User Review Eligibility and Entitlement Check Result.
 */
data class ReviewEligibility(
    val canReview: Boolean,
    val isVerifiedReader: Boolean,
    val existingReview: BookReview? = null,
    val hasEntitlement: Boolean = false,
    val entitlementStatus: String? = null,
    val readingPercent: Float = 0f,
    val readingMinutes: Double = 0.0,
    val isCompleted: Boolean = false,
    val isAuthor: Boolean = false,
    val message: String = ""
)

/**
 * Review Report Record for Moderation.
 */
data class ReviewReport(
    val id: String,
    val reviewId: String,
    val reporterUserId: String,
    val reason: ReportReason,
    val details: String,
    val status: String = "PENDING", // PENDING, RESOLVED_DISMISSED, RESOLVED_REMOVED
    val resolvedByAdminId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

/**
 * Review Moderation Audit Entry.
 */
data class ReviewAudit(
    val id: String,
    val reviewId: String,
    val adminUserId: String,
    val action: ReviewAuditAction,
    val reason: String,
    val previousState: String,
    val newState: String,
    val timestamp: Long = System.currentTimeMillis()
)
