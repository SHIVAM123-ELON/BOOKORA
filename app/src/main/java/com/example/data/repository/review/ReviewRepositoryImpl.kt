package com.example.data.repository.review

import com.example.core.result.Resource
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.review.*
import com.example.domain.model.financial.EntitlementStatus
import com.example.domain.model.review.*
import com.example.domain.repository.review.ReviewRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class ReviewRepositoryImpl(
    private val db: BookoraDatabase
) : ReviewRepository {

    companion object {
        const val MINIMUM_READING_PERCENT_FOR_REVIEW = 10.0f
        const val MINIMUM_READING_MINUTES_FOR_REVIEW = 5.0
        const val MIN_REVIEW_LENGTH = 10
        const val MAX_REVIEW_LENGTH = 2000
    }

    override suspend fun checkReviewEligibility(userId: String, bookId: String): ReviewEligibility {
        val book = db.bookDao().getBookByIdDirect(bookId)
            ?: return ReviewEligibility(
                canReview = false,
                isVerifiedReader = false,
                message = "Book not found."
            )

        // Prevent author self-review
        val isAuthor = (book.authorId == userId || book.authorName.equals(userId, ignoreCase = true))
        if (isAuthor) {
            return ReviewEligibility(
                canReview = false,
                isVerifiedReader = false,
                isAuthor = true,
                message = "Authors cannot review their own published books."
            )
        }

        // Check for existing review
        val existingReviewEntity = db.reviewDao().getUserReviewDirect(userId, bookId)
        val existingReview = existingReviewEntity?.toDomain()

        // 1. Check Entitlement
        val entitlement = db.entitlementDao().getEntitlementDirect(userId, bookId)
        val hasActiveEntitlement = entitlement != null &&
                entitlement.status.equals(EntitlementStatus.ACTIVE.name, ignoreCase = true) &&
                entitlement.revokedAt == null

        // 2. Check Reading Activity & Progress
        val activity = db.readingActivityDao().getActivityDirect(userId, bookId)
        val progress = db.readingProgressDao().getReadingProgress(userId, bookId).first()
        val readingPercent = maxOf(activity?.readingPercent ?: 0f, progress?.percentage ?: 0f)
        val readingMinutes = (activity?.totalReadingSeconds ?: 0L) / 60.0
        val isCompleted = (activity?.isCompleted == true) || readingPercent >= 99f

        val meetsReadingCriteria = isCompleted ||
                readingPercent >= MINIMUM_READING_PERCENT_FOR_REVIEW ||
                readingMinutes >= MINIMUM_READING_MINUTES_FOR_REVIEW

        val qualifiesForVerifiedReader = hasActiveEntitlement && meetsReadingCriteria

        // Update/Persist Authoritative ReaderVerification entity
        val verificationStatus = when {
            qualifiesForVerifiedReader -> "VERIFIED"
            !hasActiveEntitlement -> "NO_ENTITLEMENT"
            else -> "PENDING_READING_ACTIVITY"
        }

        val verificationReason = when {
            qualifiesForVerifiedReader -> "Verified reader: Legitimate ${entitlement?.source ?: "entitlement"} with ${"%.1f".format(readingPercent)}% read."
            !hasActiveEntitlement -> "No active book purchase or entitlement found."
            else -> "Reading threshold not yet reached (Current: ${"%.1f".format(readingPercent)}%, requires 10% or 5 minutes)."
        }

        val existingVerification = db.readerVerificationDao().getVerificationDirect(userId, bookId)
        val verificationEntity = ReaderVerificationEntity(
            id = existingVerification?.id ?: "rv-${UUID.randomUUID()}",
            userId = userId,
            bookId = bookId,
            isVerified = qualifiesForVerifiedReader,
            status = verificationStatus,
            entitlementId = entitlement?.id,
            entitlementSource = entitlement?.source,
            readingMinutes = readingMinutes,
            readingPercent = readingPercent,
            verificationReason = verificationReason,
            verifiedAt = if (qualifiesForVerifiedReader) (existingVerification?.verifiedAt ?: System.currentTimeMillis()) else null,
            revokedAt = if (!hasActiveEntitlement && existingVerification?.isVerified == true) System.currentTimeMillis() else null,
            createdAt = existingVerification?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        db.readerVerificationDao().insertVerification(verificationEntity)

        return ReviewEligibility(
            canReview = true,
            isVerifiedReader = qualifiesForVerifiedReader,
            existingReview = existingReview,
            hasEntitlement = hasActiveEntitlement,
            entitlementStatus = entitlement?.status,
            readingPercent = readingPercent,
            readingMinutes = readingMinutes,
            isCompleted = isCompleted,
            isAuthor = false,
            message = if (qualifiesForVerifiedReader) {
                "You are verified as a reader for this book. Your review will display the Verified Reader badge."
            } else if (!hasActiveEntitlement) {
                "You can submit a community review, but it will be marked as unverified (no active entitlement found)."
            } else {
                "You have an entitlement. Read at least 10% or 5 minutes to earn the Verified Reader badge (Current: ${"%.0f".format(readingPercent)}%)."
            }
        )
    }

    override fun getReaderVerification(userId: String, bookId: String): Flow<ReaderVerification?> {
        return db.readerVerificationDao().getVerification(userId, bookId).map { it?.toDomain() }
    }

    override fun getReadingActivity(userId: String, bookId: String): Flow<ReadingActivity?> {
        return db.readingActivityDao().getActivity(userId, bookId).map { it?.toDomain() }
    }

    override fun getReadingSessions(userId: String, bookId: String): Flow<List<ReadingSession>> {
        return db.readingSessionDao().getSessions(userId, bookId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun recordReadingSession(
        userId: String,
        bookId: String,
        durationSeconds: Long,
        startPage: Int,
        endPage: Int,
        totalPages: Int
    ): Resource<ReaderVerification> {
        return try {
            val safeTotalPages = if (totalPages > 0) totalPages else 100
            val safeEndPage = endPage.coerceIn(1, safeTotalPages)
            val startProgress = (startPage.toFloat() / safeTotalPages.toFloat()) * 100f
            val endProgress = (safeEndPage.toFloat() / safeTotalPages.toFloat()) * 100f

            // 1. Insert Session Record
            val session = ReadingSessionEntity(
                id = "rs-${UUID.randomUUID()}",
                userId = userId,
                bookId = bookId,
                startedAt = System.currentTimeMillis() - (durationSeconds * 1000L),
                endedAt = System.currentTimeMillis(),
                durationSeconds = durationSeconds,
                startProgress = startProgress,
                endProgress = endProgress,
                startPage = startPage,
                endPage = safeEndPage
            )
            db.readingActivityDao().insertSession(session)

            // 2. Update/Aggregate Reading Activity
            val existingActivity = db.readingActivityDao().getActivityDirect(userId, bookId)
            val totalSeconds = (existingActivity?.totalReadingSeconds ?: 0L) + durationSeconds
            val furthestPage = maxOf(existingActivity?.furthestPage ?: 1, safeEndPage)
            val maxPercent = maxOf(existingActivity?.readingPercent ?: 0f, endProgress)
            val isCompleted = maxPercent >= 99f || furthestPage >= safeTotalPages
            val sessionCount = (existingActivity?.sessionCount ?: 0) + 1

            val updatedActivity = ReadingActivityEntity(
                id = existingActivity?.id ?: "ra-${UUID.randomUUID()}",
                userId = userId,
                bookId = bookId,
                totalReadingSeconds = totalSeconds,
                furthestPage = furthestPage,
                currentPage = safeEndPage,
                readingPercent = maxPercent,
                isCompleted = isCompleted,
                sessionCount = sessionCount,
                lastSessionAt = System.currentTimeMillis(),
                createdAt = existingActivity?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            db.readingActivityDao().insertOrUpdateActivity(updatedActivity)

            // 3. Update Reading Progress & Library Progress
            db.readingProgressDao().saveReadingProgress(
                com.example.data.local.entity.ReadingProgressEntity(
                    userId = userId,
                    bookId = bookId,
                    currentPage = safeEndPage,
                    totalPages = safeTotalPages,
                    percentage = maxPercent,
                    lastOpenedAt = System.currentTimeMillis()
                )
            )
            db.libraryDao().updateProgress(
                bookId = bookId,
                page = safeEndPage,
                progress = maxPercent,
                status = if (isCompleted) "COMPLETED" else "IN_PROGRESS"
            )

            // 4. Re-evaluate eligibility and update verification entity
            val eligibility = checkReviewEligibility(userId, bookId)
            val verification = db.readerVerificationDao().getVerificationDirect(userId, bookId)?.toDomain()

            // 5. If user already has a review, update its badge status if newly verified
            if (eligibility.isVerifiedReader) {
                db.reviewDao().updateVerificationStatus(
                    userId = userId,
                    bookId = bookId,
                    status = ReviewVerificationStatus.VERIFIED_READER.name
                )
            }

            Resource.Success(verification ?: ReaderVerification(
                id = "rv-fallback",
                userId = userId,
                bookId = bookId,
                isVerified = eligibility.isVerifiedReader,
                status = if (eligibility.isVerifiedReader) "VERIFIED" else "PENDING_READING_ACTIVITY"
            ))
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to record reading session", e)
        }
    }

    override suspend fun submitReview(
        userId: String,
        userName: String,
        userAvatarUrl: String?,
        bookId: String,
        rating: Int,
        title: String,
        reviewText: String
    ): Resource<BookReview> {
        return try {
            // Validation
            if (rating !in 1..5) {
                return Resource.Error("Rating must be between 1 and 5 stars.")
            }
            val trimmedTitle = title.trim()
            val trimmedBody = reviewText.trim()
            if (trimmedTitle.length < 3) {
                return Resource.Error("Review title must be at least 3 characters.")
            }
            if (trimmedBody.length < MIN_REVIEW_LENGTH) {
                return Resource.Error("Review description must be at least $MIN_REVIEW_LENGTH characters.")
            }
            if (trimmedBody.length > MAX_REVIEW_LENGTH) {
                return Resource.Error("Review description cannot exceed $MAX_REVIEW_LENGTH characters.")
            }

            // Check eligibility authoritatively
            val eligibility = checkReviewEligibility(userId, bookId)
            if (!eligibility.canReview) {
                return Resource.Error(eligibility.message)
            }

            // Check single review constraint
            val existing = db.reviewDao().getUserReviewDirect(userId, bookId)
            if (existing != null && existing.moderationStatus != ReviewModerationStatus.REMOVED.name) {
                return Resource.Error("You have already submitted a review for this book. Please edit your existing review.")
            }

            // Anti-Spam heuristic
            val containsSpamKeywords = listOf("http://", "https://", "free crypto", "buy followers", "telegram.me", "whatsapp").any {
                trimmedBody.contains(it, ignoreCase = true) || trimmedTitle.contains(it, ignoreCase = true)
            }
            val moderationStatus = if (containsSpamKeywords) {
                ReviewModerationStatus.PENDING_REVIEW
            } else {
                ReviewModerationStatus.PUBLISHED
            }

            val verificationStatus = if (eligibility.isVerifiedReader) {
                ReviewVerificationStatus.VERIFIED_READER
            } else {
                ReviewVerificationStatus.UNVERIFIED_REVIEWER
            }

            val reviewEntity = BookReviewEntity(
                id = existing?.id ?: "rev-${UUID.randomUUID()}",
                userId = userId,
                bookId = bookId,
                userName = userName.ifBlank { "Reader" },
                userAvatarUrl = userAvatarUrl,
                rating = rating,
                title = trimmedTitle,
                reviewText = trimmedBody,
                verificationStatus = verificationStatus.name,
                moderationStatus = moderationStatus.name,
                helpfulCount = 0,
                reportCount = 0,
                isEdited = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            db.reviewDao().insertReview(reviewEntity)

            // Recalculate book aggregate
            recalculateBookRating(bookId)

            Resource.Success(reviewEntity.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to submit review", e)
        }
    }

    override suspend fun updateReview(
        reviewId: String,
        userId: String,
        rating: Int,
        title: String,
        reviewText: String
    ): Resource<BookReview> {
        return try {
            val existing = db.reviewDao().getReviewByIdDirect(reviewId)
                ?: return Resource.Error("Review not found.")

            if (existing.userId != userId) {
                return Resource.Error("You can only edit your own review.")
            }
            if (rating !in 1..5) {
                return Resource.Error("Rating must be between 1 and 5 stars.")
            }
            val trimmedTitle = title.trim()
            val trimmedBody = reviewText.trim()
            if (trimmedTitle.length < 3) {
                return Resource.Error("Review title must be at least 3 characters.")
            }
            if (trimmedBody.length < MIN_REVIEW_LENGTH) {
                return Resource.Error("Review text must be at least $MIN_REVIEW_LENGTH characters.")
            }

            // Re-evaluate verification status
            val eligibility = checkReviewEligibility(userId, existing.bookId)
            val verificationStatus = if (eligibility.isVerifiedReader) {
                ReviewVerificationStatus.VERIFIED_READER
            } else {
                ReviewVerificationStatus.UNVERIFIED_REVIEWER
            }

            val updated = existing.copy(
                rating = rating,
                title = trimmedTitle,
                reviewText = trimmedBody,
                verificationStatus = verificationStatus.name,
                isEdited = true,
                updatedAt = System.currentTimeMillis()
            )
            db.reviewDao().updateReview(updated)

            recalculateBookRating(existing.bookId)

            Resource.Success(updated.toDomain())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update review", e)
        }
    }

    override suspend fun deleteReview(reviewId: String, userId: String): Resource<Unit> {
        return try {
            val existing = db.reviewDao().getReviewByIdDirect(reviewId)
                ?: return Resource.Error("Review not found.")

            if (existing.userId != userId) {
                return Resource.Error("You are not authorized to delete this review.")
            }

            db.reviewDao().deleteReview(reviewId)
            recalculateBookRating(existing.bookId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete review", e)
        }
    }

    override suspend fun toggleHelpfulVote(reviewId: String, userId: String): Resource<Boolean> {
        return try {
            val review = db.reviewDao().getReviewByIdDirect(reviewId)
                ?: return Resource.Error("Review not found.")

            if (review.userId == userId) {
                return Resource.Error("You cannot vote on your own review.")
            }

            val hasVoted = db.reviewHelpfulVoteDao().hasVotedDirect(reviewId, userId)
            if (hasVoted) {
                db.reviewHelpfulVoteDao().deleteVote(reviewId, userId)
                val newCount = maxOf(0, review.helpfulCount - 1)
                db.reviewDao().updateHelpfulCount(reviewId, newCount)
                Resource.Success(false)
            } else {
                db.reviewHelpfulVoteDao().insertVote(
                    ReviewHelpfulVoteEntity(
                        id = "vote-${UUID.randomUUID()}",
                        reviewId = reviewId,
                        userId = userId,
                        votedAt = System.currentTimeMillis()
                    )
                )
                val newCount = review.helpfulCount + 1
                db.reviewDao().updateHelpfulCount(reviewId, newCount)
                Resource.Success(true)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update helpful vote", e)
        }
    }

    override suspend fun reportReview(
        reviewId: String,
        reporterUserId: String,
        reason: ReportReason,
        details: String
    ): Resource<Unit> {
        return try {
            val review = db.reviewDao().getReviewByIdDirect(reviewId)
                ?: return Resource.Error("Review not found.")

            val report = ReviewReportEntity(
                id = "rep-${UUID.randomUUID()}",
                reviewId = reviewId,
                reporterUserId = reporterUserId,
                reason = reason.name,
                details = details.trim(),
                status = "PENDING",
                createdAt = System.currentTimeMillis()
            )
            db.reviewReportDao().insertReport(report)
            db.reviewDao().incrementReportCount(reviewId)

            // Auto-flag if reports exceed threshold
            if (review.reportCount + 1 >= 3 && review.moderationStatus == ReviewModerationStatus.PUBLISHED.name) {
                db.reviewDao().updateModerationStatus(reviewId, ReviewModerationStatus.PENDING_REVIEW.name)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to submit report", e)
        }
    }

    override suspend fun moderateReview(
        reviewId: String,
        adminUserId: String,
        action: ReviewAuditAction,
        reason: String
    ): Resource<Unit> {
        return try {
            val review = db.reviewDao().getReviewByIdDirect(reviewId)
                ?: return Resource.Error("Review not found.")

            val previousStatus = review.moderationStatus
            val newStatus = when (action) {
                ReviewAuditAction.APPROVE, ReviewAuditAction.RESTORE -> ReviewModerationStatus.PUBLISHED.name
                ReviewAuditAction.HIDE -> ReviewModerationStatus.HIDDEN.name
                ReviewAuditAction.REMOVE -> ReviewModerationStatus.REMOVED.name
                ReviewAuditAction.REJECT -> ReviewModerationStatus.REJECTED.name
                ReviewAuditAction.FLAG_SUSPICIOUS -> ReviewModerationStatus.PENDING_REVIEW.name
            }

            db.reviewDao().updateModerationStatus(reviewId, newStatus)

            // Log immutable audit entry
            val audit = ReviewAuditEntity(
                id = "audit-${UUID.randomUUID()}",
                reviewId = reviewId,
                adminUserId = adminUserId,
                action = action.name,
                reason = reason.trim().ifBlank { "Administrative action: $action" },
                previousState = previousStatus,
                newState = newStatus,
                timestamp = System.currentTimeMillis()
            )
            db.reviewAuditDao().insertAudit(audit)

            // Recalculate rating aggregate
            recalculateBookRating(review.bookId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to moderate review", e)
        }
    }

    override fun getRatingSummary(bookId: String): Flow<RatingSummary> {
        return db.reviewDao().getPublishedReviews(bookId).map { list ->
            if (list.isEmpty()) {
                RatingSummary(bookId = bookId)
            } else {
                val total = list.size
                val verifiedCount = list.count { it.verificationStatus == ReviewVerificationStatus.VERIFIED_READER.name }
                val unverifiedCount = total - verifiedCount
                val avg = list.map { it.rating }.average()
                val one = list.count { it.rating == 1 }
                val two = list.count { it.rating == 2 }
                val three = list.count { it.rating == 3 }
                val four = list.count { it.rating == 4 }
                val five = list.count { it.rating == 5 }

                RatingSummary(
                    bookId = bookId,
                    averageRating = avg,
                    totalReviews = total,
                    verifiedReviewsCount = verifiedCount,
                    unverifiedReviewsCount = unverifiedCount,
                    oneStarCount = one,
                    twoStarCount = two,
                    threeStarCount = three,
                    fourStarCount = four,
                    fiveStarCount = five
                )
            }
        }.distinctUntilChanged()
    }

    override fun getReviewsForBook(
        bookId: String,
        sortOption: ReviewSortOption,
        currentUserId: String?
    ): Flow<List<BookReview>> {
        val publishedReviewsFlow = db.reviewDao().getPublishedReviews(bookId)
        val userVotesFlow = if (currentUserId != null) {
            db.reviewHelpfulVoteDao().getUserVotedReviewIds(currentUserId)
        } else {
            flow { emit(emptyList<String>()) }
        }

        return combine(publishedReviewsFlow, userVotesFlow) { reviews, votedIds ->
            val domainList = reviews.map { r ->
                r.toDomain(isHelpfulByCurrentUser = votedIds.contains(r.id))
            }

            when (sortOption) {
                ReviewSortOption.MOST_HELPFUL -> domainList.sortedWith(
                    compareByDescending<BookReview> { it.helpfulCount }.thenByDescending { it.createdAt }
                )
                ReviewSortOption.NEWEST -> domainList.sortedByDescending { it.createdAt }
                ReviewSortOption.HIGHEST_RATING -> domainList.sortedWith(
                    compareByDescending<BookReview> { it.rating }.thenByDescending { it.helpfulCount }
                )
                ReviewSortOption.LOWEST_RATING -> domainList.sortedWith(
                    compareBy<BookReview> { it.rating }.thenByDescending { it.createdAt }
                )
                ReviewSortOption.VERIFIED_FIRST -> domainList.sortedWith(
                    compareByDescending<BookReview> { it.isVerifiedReader }.thenByDescending { it.helpfulCount }
                )
            }
        }
    }

    override fun getUserReview(userId: String, bookId: String): Flow<BookReview?> {
        return db.reviewDao().getUserReview(userId, bookId).map { it?.toDomain() }
    }

    override fun getAllReviewsForModeration(): Flow<List<BookReview>> {
        return db.reviewDao().getAllReviewsForModeration().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPendingReports(): Flow<List<ReviewReport>> {
        return db.reviewReportDao().getPendingReports().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun recalculateBookRating(bookId: String) {
        val published = db.reviewDao().getPublishedReviewsDirect(bookId)
        val book = db.bookDao().getBookByIdDirect(bookId) ?: return

        val newAvg = if (published.isNotEmpty()) {
            val avg = published.map { it.rating }.average()
            Math.round(avg * 100.0) / 100.0
        } else {
            0.0
        }
        val newCount = published.size

        val updatedBook = book.copy(
            rating = newAvg,
            reviewCount = newCount,
            updatedAt = System.currentTimeMillis()
        )
        db.bookDao().updateBook(updatedBook)
    }
}
