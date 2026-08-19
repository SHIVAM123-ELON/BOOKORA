package com.example.data.local.dao.publisher

import androidx.room.*
import com.example.data.local.entity.publisher.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookSubmissionDao {

    @Query("SELECT * FROM book_submissions ORDER BY createdAt DESC")
    fun getAllSubmissions(): Flow<List<BookSubmissionEntity>>

    @Query("SELECT * FROM book_submissions WHERE uploaderUserId = :userId ORDER BY createdAt DESC")
    fun getSubmissionsByUserId(userId: String): Flow<List<BookSubmissionEntity>>

    @Query("SELECT * FROM book_submissions WHERE id = :submissionId LIMIT 1")
    fun getSubmissionById(submissionId: String): Flow<BookSubmissionEntity?>

    @Query("SELECT * FROM book_submissions WHERE id = :submissionId LIMIT 1")
    suspend fun getSubmissionByIdDirect(submissionId: String): BookSubmissionEntity?

    @Query("SELECT * FROM book_submissions WHERE pdfSha256Hash = :hash LIMIT 1")
    suspend fun getSubmissionByHash(hash: String): BookSubmissionEntity?

    @Query("SELECT * FROM book_submissions WHERE status = :status ORDER BY createdAt ASC")
    fun getSubmissionsByStatus(status: String): Flow<List<BookSubmissionEntity>>

    @Query("SELECT COUNT(*) FROM book_submissions WHERE uploaderUserId = :userId AND createdAt >= :sinceTimestamp")
    suspend fun countUserSubmissionsSince(userId: String, sinceTimestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: BookSubmissionEntity)

    @Update
    suspend fun updateSubmission(submission: BookSubmissionEntity)

    @Query("UPDATE book_submissions SET status = :status, moderationFeedback = :feedback, reviewedByAdminId = :adminId, reviewedAt = :reviewedAt, rewardCredited = :rewardCredited, publishedBookId = :publishedBookId, updatedAt = :updatedAt WHERE id = :submissionId")
    suspend fun updateSubmissionModeration(
        submissionId: String,
        status: String,
        feedback: String?,
        adminId: String?,
        reviewedAt: Long?,
        rewardCredited: Boolean,
        publishedBookId: String?,
        updatedAt: Long
    )
}

@Dao
interface CreatorBalanceDao {

    @Query("SELECT * FROM creator_balances WHERE userId = :userId LIMIT 1")
    fun getBalanceByUserId(userId: String): Flow<CreatorBalanceEntity?>

    @Query("SELECT * FROM creator_balances WHERE userId = :userId LIMIT 1")
    suspend fun getBalanceByUserIdDirect(userId: String): CreatorBalanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBalance(balance: CreatorBalanceEntity)

    @Query("UPDATE creator_balances SET isFrozen = :isFrozen, freezeReason = :freezeReason, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateFreezeStatus(userId: String, isFrozen: Boolean, freezeReason: String?, updatedAt: Long)
}

@Dao
interface CreatorTransactionDao {

    @Query("SELECT * FROM creator_transactions WHERE userId = :userId ORDER BY createdAt DESC")
    fun getTransactionsByUserId(userId: String): Flow<List<CreatorTransactionEntity>>

    @Query("SELECT * FROM creator_transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getTransactionByIdempotencyKey(key: String): CreatorTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: CreatorTransactionEntity)
}

@Dao
interface CreatorPayoutRequestDao {

    @Query("SELECT * FROM creator_payout_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getPayoutRequestsByUserId(userId: String): Flow<List<CreatorPayoutRequestEntity>>

    @Query("SELECT * FROM creator_payout_requests ORDER BY requestedAt DESC")
    fun getAllPayoutRequests(): Flow<List<CreatorPayoutRequestEntity>>

    @Query("SELECT * FROM creator_payout_requests WHERE id = :id LIMIT 1")
    suspend fun getPayoutRequestByIdDirect(id: String): CreatorPayoutRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayoutRequest(request: CreatorPayoutRequestEntity)

    @Query("UPDATE creator_payout_requests SET status = :status, processedAt = :processedAt, failureReason = :failureReason, adminNotes = :adminNotes, transactionRef = :transactionRef WHERE id = :id")
    suspend fun updatePayoutStatus(
        id: String,
        status: String,
        processedAt: Long?,
        failureReason: String?,
        adminNotes: String?,
        transactionRef: String?
    )
}

@Dao
interface CopyrightReportDao {

    @Query("SELECT * FROM copyright_reports ORDER BY createdAt DESC")
    fun getAllReports(): Flow<List<CopyrightReportEntity>>

    @Query("SELECT * FROM copyright_reports WHERE submissionId = :submissionId")
    fun getReportsBySubmissionId(submissionId: String): Flow<List<CopyrightReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: CopyrightReportEntity)

    @Query("UPDATE copyright_reports SET isResolved = :isResolved, resolvedAt = :resolvedAt, resolvedByAdminId = :adminId WHERE id = :id")
    suspend fun updateReportResolution(id: String, isResolved: Boolean, resolvedAt: Long?, adminId: String?)
}
