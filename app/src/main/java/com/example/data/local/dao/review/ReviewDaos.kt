package com.example.data.local.dao.review

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.review.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing Reader Verification lifecycle states.
 */
@Dao
interface ReaderVerificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: ReaderVerificationEntity)

    @Query("SELECT * FROM reader_verifications WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    fun getVerification(userId: String, bookId: String): Flow<ReaderVerificationEntity?>

    @Query("SELECT * FROM reader_verifications WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getVerificationDirect(userId: String, bookId: String): ReaderVerificationEntity?

    @Query("SELECT * FROM reader_verifications WHERE userId = :userId ORDER BY updatedAt DESC")
    fun getVerificationsForUser(userId: String): Flow<List<ReaderVerificationEntity>>

    @Query("UPDATE reader_verifications SET status = :status, isVerified = :isVerified, verificationReason = :reason, revokedAt = :revokedAt, updatedAt = :updatedAt WHERE userId = :userId AND bookId = :bookId")
    suspend fun updateStatus(
        userId: String,
        bookId: String,
        status: String,
        isVerified: Boolean,
        reason: String,
        revokedAt: Long?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM reader_verifications WHERE bookId = :bookId AND isVerified = 1 ORDER BY verifiedAt DESC")
    fun getVerifiedReadersForBook(bookId: String): Flow<List<ReaderVerificationEntity>>

    @Query("SELECT * FROM reader_verifications WHERE bookId = :bookId AND isVerified = 1 ORDER BY verifiedAt DESC")
    suspend fun getVerifiedReadersForBookDirect(bookId: String): List<ReaderVerificationEntity>

    @Query("SELECT COUNT(*) FROM reader_verifications WHERE bookId = :bookId AND isVerified = 1")
    fun getVerifiedReadersCountForBook(bookId: String): Flow<Int>

    @Query("DELETE FROM reader_verifications WHERE userId = :userId AND bookId = :bookId")
    suspend fun deleteVerification(userId: String, bookId: String)
}

/**
 * DAO for managing individual granular Reading Sessions.
 */
@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<ReadingSessionEntity>)

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId AND bookId = :bookId ORDER BY startedAt DESC")
    fun getSessions(userId: String, bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId AND bookId = :bookId ORDER BY startedAt DESC")
    suspend fun getSessionsDirect(userId: String, bookId: String): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId ORDER BY startedAt DESC")
    fun getSessionsForUser(userId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId AND bookId = :bookId ORDER BY startedAt DESC LIMIT 1")
    fun getLatestSession(userId: String, bookId: String): Flow<ReadingSessionEntity?>

    @Query("SELECT SUM(durationSeconds) FROM reading_sessions WHERE userId = :userId AND bookId = :bookId")
    fun getTotalReadingSeconds(userId: String, bookId: String): Flow<Long?>

    @Query("SELECT COUNT(*) FROM reading_sessions WHERE userId = :userId AND bookId = :bookId")
    fun getSessionCount(userId: String, bookId: String): Flow<Int>

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("DELETE FROM reading_sessions WHERE userId = :userId AND bookId = :bookId")
    suspend fun deleteSessionsForBook(userId: String, bookId: String)
}

/**
 * DAO for managing Reading Activity summaries.
 */
@Dao
interface ReadingActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivity(activity: ReadingActivityEntity)

    @Query("SELECT * FROM reading_activities WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    fun getActivity(userId: String, bookId: String): Flow<ReadingActivityEntity?>

    @Query("SELECT * FROM reading_activities WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getActivityDirect(userId: String, bookId: String): ReadingActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId AND bookId = :bookId ORDER BY startedAt DESC")
    fun getSessions(userId: String, bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE userId = :userId AND bookId = :bookId ORDER BY startedAt DESC")
    suspend fun getSessionsDirect(userId: String, bookId: String): List<ReadingSessionEntity>
}

/**
 * DAO for managing Book Reviews and the complete review moderation/verification lifecycle.
 */
@Dao
interface BookReviewDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: BookReviewEntity)

    @Update
    suspend fun updateReview(review: BookReviewEntity)

    @Query("DELETE FROM book_reviews WHERE id = :id")
    suspend fun deleteReview(id: String)

    @Query("DELETE FROM book_reviews WHERE userId = :userId AND bookId = :bookId")
    suspend fun deleteUserReview(userId: String, bookId: String)

    @Query("SELECT * FROM book_reviews WHERE bookId = :bookId AND moderationStatus = 'PUBLISHED' ORDER BY createdAt DESC")
    fun getPublishedReviews(bookId: String): Flow<List<BookReviewEntity>>

    @Query("SELECT * FROM book_reviews WHERE bookId = :bookId AND moderationStatus = 'PUBLISHED' ORDER BY createdAt DESC")
    suspend fun getPublishedReviewsDirect(bookId: String): List<BookReviewEntity>

    @Query("SELECT * FROM book_reviews WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getAllReviewsForBook(bookId: String): Flow<List<BookReviewEntity>>

    @Query("SELECT * FROM book_reviews WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    fun getUserReview(userId: String, bookId: String): Flow<BookReviewEntity?>

    @Query("SELECT * FROM book_reviews WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getUserReviewDirect(userId: String, bookId: String): BookReviewEntity?

    @Query("SELECT * FROM book_reviews WHERE id = :id LIMIT 1")
    fun getReviewById(id: String): Flow<BookReviewEntity?>

    @Query("SELECT * FROM book_reviews WHERE id = :id LIMIT 1")
    suspend fun getReviewByIdDirect(id: String): BookReviewEntity?

    @Query("SELECT * FROM book_reviews WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserReviews(userId: String): Flow<List<BookReviewEntity>>

    @Query("SELECT * FROM book_reviews ORDER BY createdAt DESC")
    fun getAllReviewsForModeration(): Flow<List<BookReviewEntity>>

    @Query("SELECT * FROM book_reviews WHERE moderationStatus = :status ORDER BY createdAt DESC")
    fun getReviewsByModerationStatus(status: String): Flow<List<BookReviewEntity>>

    @Query("UPDATE book_reviews SET moderationStatus = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModerationStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE book_reviews SET helpfulCount = :count WHERE id = :id")
    suspend fun updateHelpfulCount(id: String, count: Int)

    @Query("UPDATE book_reviews SET reportCount = reportCount + 1 WHERE id = :id")
    suspend fun incrementReportCount(id: String)

    @Query("UPDATE book_reviews SET verificationStatus = :status, updatedAt = :updatedAt WHERE userId = :userId AND bookId = :bookId")
    suspend fun updateVerificationStatus(userId: String, bookId: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE book_reviews SET authorReply = :reply, authorRepliedAt = :repliedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun addAuthorReply(id: String, reply: String, repliedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())
}

/**
 * Backward-compatible alias for BookReviewDao.
 */
typealias ReviewDao = BookReviewDao

@Dao
interface ReviewHelpfulVoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVote(vote: ReviewHelpfulVoteEntity)

    @Query("DELETE FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId")
    suspend fun deleteVote(reviewId: String, userId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId)")
    fun hasVoted(reviewId: String, userId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM review_helpful_votes WHERE reviewId = :reviewId AND userId = :userId)")
    suspend fun hasVotedDirect(reviewId: String, userId: String): Boolean

    @Query("SELECT reviewId FROM review_helpful_votes WHERE userId = :userId")
    fun getUserVotedReviewIds(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM review_helpful_votes WHERE reviewId = :reviewId")
    suspend fun getVoteCount(reviewId: String): Int
}

@Dao
interface ReviewReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReviewReportEntity)

    @Query("SELECT * FROM review_reports WHERE reviewId = :reviewId ORDER BY createdAt DESC")
    fun getReportsForReview(reviewId: String): Flow<List<ReviewReportEntity>>

    @Query("SELECT * FROM review_reports WHERE status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingReports(): Flow<List<ReviewReportEntity>>

    @Query("SELECT * FROM review_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<ReviewReportEntity>>

    @Query("UPDATE review_reports SET status = :status, resolvedByAdminId = :adminId, resolvedAt = :resolvedAt WHERE id = :reportId")
    suspend fun resolveReport(
        reportId: String,
        status: String,
        adminId: String,
        resolvedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface ReviewAuditDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(audit: ReviewAuditEntity)

    @Query("SELECT * FROM review_audits WHERE reviewId = :reviewId ORDER BY timestamp DESC")
    fun getAuditsForReview(reviewId: String): Flow<List<ReviewAuditEntity>>

    @Query("SELECT * FROM review_audits ORDER BY timestamp DESC")
    fun getAllAudits(): Flow<List<ReviewAuditEntity>>
}

