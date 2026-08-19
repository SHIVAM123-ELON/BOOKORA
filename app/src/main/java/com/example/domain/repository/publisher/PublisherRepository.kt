package com.example.domain.repository.publisher

import com.example.core.result.Resource
import com.example.domain.model.publisher.*
import kotlinx.coroutines.flow.Flow

interface PublisherRepository {

    // Submission CRUD & Lifecycle
    fun getSubmissionsForUser(userId: String): Flow<List<BookSubmission>>
    fun getAllSubmissions(): Flow<List<BookSubmission>>
    fun getSubmissionsByStatus(status: SubmissionStatus): Flow<List<BookSubmission>>
    fun getSubmissionById(submissionId: String): Flow<BookSubmission?>

    suspend fun submitBook(
        userId: String,
        userName: String,
        userEmail: String,
        title: String,
        authorName: String,
        description: String,
        categoryId: String,
        categoryName: String,
        language: String,
        tags: List<String>,
        pdfUri: String,
        pdfSizeBytes: Long,
        pdfSha256: String,
        pdfPageCount: Int,
        coverImageUri: String?,
        copyrightAccepted: Boolean
    ): Resource<BookSubmission>

    // Authoritative Admin Moderation Actions
    suspend fun reviewSubmission(
        submissionId: String,
        adminUserId: String,
        status: SubmissionStatus,
        feedback: String? = null
    ): Resource<BookSubmission>

    // Creator Balance & Ledger
    fun getCreatorBalance(userId: String): Flow<CreatorBalance?>
    fun getCreatorTransactions(userId: String): Flow<List<CreatorTransaction>>
    suspend fun freezeCreatorAccount(userId: String, isFrozen: Boolean, reason: String?): Resource<Unit>

    // Payout Requests
    fun getUserPayoutRequests(userId: String): Flow<List<CreatorPayoutRequest>>
    fun getAllPayoutRequests(): Flow<List<CreatorPayoutRequest>>
    suspend fun requestPayout(
        userId: String,
        amountMinor: Long,
        upiId: String
    ): Resource<CreatorPayoutRequest>

    suspend fun updatePayoutStatus(
        payoutId: String,
        status: CreatorPayoutStatus,
        adminNotes: String? = null,
        transactionRef: String? = null,
        failureReason: String? = null
    ): Resource<CreatorPayoutRequest>

    // Copyright Reports & Takedown
    fun getAllCopyrightReports(): Flow<List<CopyrightReport>>
    suspend fun submitCopyrightReport(
        submissionId: String,
        bookId: String?,
        reporterUserId: String,
        reporterEmail: String,
        reason: String,
        proofDetails: String
    ): Resource<CopyrightReport>

    suspend fun resolveCopyrightReport(
        reportId: String,
        adminUserId: String,
        takeDownSubmission: Boolean
    ): Resource<Unit>
}
