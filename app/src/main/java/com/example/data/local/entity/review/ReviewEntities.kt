package com.example.data.local.entity.review

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.review.*

@Entity(
    tableName = "reader_verifications",
    indices = [
        Index(value = ["userId", "bookId"], unique = true),
        Index(value = ["bookId"]),
        Index(value = ["userId"])
    ]
)
data class ReaderVerificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val isVerified: Boolean,
    val status: String,
    val entitlementId: String? = null,
    val entitlementSource: String? = null,
    val readingMinutes: Double = 0.0,
    val readingPercent: Float = 0f,
    val verificationReason: String = "",
    val verifiedAt: Long? = null,
    val revokedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ReaderVerification = ReaderVerification(
        id = id,
        userId = userId,
        bookId = bookId,
        isVerified = isVerified,
        status = status,
        entitlementSource = entitlementSource,
        readingMinutes = readingMinutes,
        readingPercent = readingPercent,
        verificationReason = verificationReason,
        verifiedAt = verifiedAt,
        revokedAt = revokedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(d: ReaderVerification): ReaderVerificationEntity = ReaderVerificationEntity(
            id = d.id,
            userId = d.userId,
            bookId = d.bookId,
            isVerified = d.isVerified,
            status = d.status,
            entitlementSource = d.entitlementSource,
            readingMinutes = d.readingMinutes,
            readingPercent = d.readingPercent,
            verificationReason = d.verificationReason,
            verifiedAt = d.verifiedAt,
            revokedAt = d.revokedAt,
            createdAt = d.createdAt,
            updatedAt = d.updatedAt
        )
    }
}

@Entity(
    tableName = "reading_activities",
    indices = [
        Index(value = ["userId", "bookId"], unique = true),
        Index(value = ["bookId"]),
        Index(value = ["userId"])
    ]
)
data class ReadingActivityEntity(
    @PrimaryKey val id: String,
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
    fun toDomain(): ReadingActivity = ReadingActivity(
        id = id,
        userId = userId,
        bookId = bookId,
        totalReadingSeconds = totalReadingSeconds,
        furthestPage = furthestPage,
        currentPage = currentPage,
        readingPercent = readingPercent,
        isCompleted = isCompleted,
        sessionCount = sessionCount,
        lastSessionAt = lastSessionAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Entity(
    tableName = "reading_sessions",
    indices = [
        Index(value = ["userId", "bookId"]),
        Index(value = ["bookId"]),
        Index(value = ["userId"])
    ]
)
data class ReadingSessionEntity(
    @PrimaryKey val id: String,
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
) {
    fun toDomain(): ReadingSession = ReadingSession(
        id = id,
        userId = userId,
        bookId = bookId,
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        startProgress = startProgress,
        endProgress = endProgress,
        startPage = startPage,
        endPage = endPage,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(s: ReadingSession): ReadingSessionEntity = ReadingSessionEntity(
            id = s.id,
            userId = s.userId,
            bookId = s.bookId,
            startedAt = s.startedAt,
            endedAt = s.endedAt,
            durationSeconds = s.durationSeconds,
            startProgress = s.startProgress,
            endProgress = s.endProgress,
            startPage = s.startPage,
            endPage = s.endPage,
            createdAt = s.createdAt
        )
    }
}

@Entity(
    tableName = "book_reviews",
    indices = [
        Index(value = ["userId", "bookId"], unique = true),
        Index(value = ["bookId"]),
        Index(value = ["userId"]),
        Index(value = ["moderationStatus"])
    ]
)
data class BookReviewEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val rating: Int,
    val title: String,
    val reviewText: String,
    val verificationStatus: String, // VERIFIED_READER, UNVERIFIED_REVIEWER
    val moderationStatus: String,   // PUBLISHED, PENDING_REVIEW, HIDDEN, REMOVED, REJECTED
    val helpfulCount: Int = 0,
    val reportCount: Int = 0,
    val isEdited: Boolean = false,
    val authorReply: String? = null,
    val authorRepliedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(isHelpfulByCurrentUser: Boolean = false): BookReview = BookReview(
        id = id,
        userId = userId,
        bookId = bookId,
        userName = userName,
        userAvatarUrl = userAvatarUrl,
        rating = rating,
        title = title,
        reviewText = reviewText,
        verificationStatus = try { ReviewVerificationStatus.valueOf(verificationStatus) } catch (e: Exception) { ReviewVerificationStatus.UNVERIFIED_REVIEWER },
        moderationStatus = try { ReviewModerationStatus.valueOf(moderationStatus) } catch (e: Exception) { ReviewModerationStatus.PUBLISHED },
        helpfulCount = helpfulCount,
        reportCount = reportCount,
        isEdited = isEdited,
        isHelpfulByCurrentUser = isHelpfulByCurrentUser,
        authorReply = authorReply,
        authorRepliedAt = authorRepliedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(r: BookReview): BookReviewEntity = BookReviewEntity(
            id = r.id,
            userId = r.userId,
            bookId = r.bookId,
            userName = r.userName,
            userAvatarUrl = r.userAvatarUrl,
            rating = r.rating,
            title = r.title,
            reviewText = r.reviewText,
            verificationStatus = r.verificationStatus.name,
            moderationStatus = r.moderationStatus.name,
            helpfulCount = r.helpfulCount,
            reportCount = r.reportCount,
            isEdited = r.isEdited,
            authorReply = r.authorReply,
            authorRepliedAt = r.authorRepliedAt,
            createdAt = r.createdAt,
            updatedAt = r.updatedAt
        )
    }
}

@Entity(
    tableName = "review_helpful_votes",
    indices = [
        Index(value = ["reviewId", "userId"], unique = true),
        Index(value = ["reviewId"]),
        Index(value = ["userId"])
    ]
)
data class ReviewHelpfulVoteEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val userId: String,
    val votedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "review_reports",
    indices = [
        Index(value = ["reviewId", "reporterUserId"]),
        Index(value = ["reviewId"]),
        Index(value = ["reporterUserId"]),
        Index(value = ["status"])
    ]
)
data class ReviewReportEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val reporterUserId: String,
    val reason: String,
    val details: String,
    val status: String = "PENDING",
    val resolvedByAdminId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
) {
    fun toDomain(): ReviewReport = ReviewReport(
        id = id,
        reviewId = reviewId,
        reporterUserId = reporterUserId,
        reason = try { ReportReason.valueOf(reason) } catch (e: Exception) { ReportReason.OTHER },
        details = details,
        status = status,
        resolvedByAdminId = resolvedByAdminId,
        createdAt = createdAt,
        resolvedAt = resolvedAt
    )
}

@Entity(
    tableName = "review_audits",
    indices = [
        Index(value = ["reviewId"]),
        Index(value = ["adminUserId"])
    ]
)
data class ReviewAuditEntity(
    @PrimaryKey val id: String,
    val reviewId: String,
    val adminUserId: String,
    val action: String,
    val reason: String,
    val previousState: String,
    val newState: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): ReviewAudit = ReviewAudit(
        id = id,
        reviewId = reviewId,
        adminUserId = adminUserId,
        action = try { ReviewAuditAction.valueOf(action) } catch (e: Exception) { ReviewAuditAction.APPROVE },
        reason = reason,
        previousState = previousState,
        newState = newState,
        timestamp = timestamp
    )
}
