package com.example.domain.model.publisher

/**
 * Moderation and Lifecycle States for Open Publisher Submissions.
 */
enum class SubmissionStatus {
    DRAFT,
    UPLOADING,
    PROCESSING,
    PENDING_REVIEW,
    CHANGES_REQUESTED,
    APPROVED,
    REJECTED,
    TAKEN_DOWN
}

/**
 * Verification & Integrity checks conducted during file validation.
 */
data class FileValidationResult(
    val isValid: Boolean,
    val mimeType: String,
    val sha256Hash: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val isPasswordProtected: Boolean,
    val isCorrupted: Boolean,
    val isDuplicate: Boolean,
    val duplicateBookId: String? = null,
    val errorMessage: String? = null,
    val safetyScanPassed: Boolean = true
)

/**
 * Open Publisher Book Submission Entity (Domain Model).
 */
data class BookSubmission(
    val id: String,
    val uploaderUserId: String,
    val uploaderName: String,
    val uploaderEmail: String,
    val title: String,
    val authorName: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val tags: List<String>,
    val pdfFileUri: String,
    val pdfFileSizeBytes: Long,
    val pdfSha256Hash: String,
    val pdfPageCount: Int,
    val coverImageUri: String? = null,
    val status: SubmissionStatus = SubmissionStatus.PENDING_REVIEW,
    val copyrightDeclarationAccepted: Boolean,
    val copyrightDeclarationTimestamp: Long,
    val copyrightDeclarationVersion: String = "v1.0",
    val moderationFeedback: String? = null,
    val reviewedByAdminId: String? = null,
    val reviewedAt: Long? = null,
    val rewardCredited: Boolean = false,
    val publishedBookId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Copyright Takedown & Infringement Report
 */
data class CopyrightReport(
    val id: String,
    val submissionId: String,
    val bookId: String?,
    val reporterUserId: String,
    val reporterEmail: String,
    val reason: String,
    val proofDetails: String,
    val isResolved: Boolean = false,
    val resolvedAt: Long? = null,
    val resolvedByAdminId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Creator Balance Model (1 Approved Book = ₹1 Reward).
 */
data class CreatorBalance(
    val userId: String,
    val availableBalanceMinor: Long = 0L, // in paise (₹1.00 = 100 paise)
    val lifetimeEarningsMinor: Long = 0L,
    val totalApprovedBooks: Int = 0,
    val totalPendingBooks: Int = 0,
    val totalRejectedBooks: Int = 0,
    val isFrozen: Boolean = false,
    val freezeReason: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val availableBalanceRupees: Double get() = availableBalanceMinor / 100.0
    val lifetimeEarningsRupees: Double get() = lifetimeEarningsMinor / 100.0
    val formattedAvailable: String get() = "₹%.2f".format(availableBalanceRupees)
    val formattedLifetime: String get() = "₹%.2f".format(lifetimeEarningsRupees)
}

/**
 * Creator Transaction Type
 */
enum class CreatorTransactionType {
    BOOK_APPROVAL_REWARD, // ₹1 credit per validated & approved book
    PAYOUT_WITHDRAWAL,    // UPI payout withdrawal debit
    ADMIN_ADJUSTMENT,     // Authoritative admin balance adjustment
    REVERSAL              // Reversal upon confirmed copyright takedown
}

/**
 * Immutable Creator Reward / Payout Transaction.
 */
data class CreatorTransaction(
    val id: String,
    val userId: String,
    val idempotencyKey: String,
    val type: CreatorTransactionType,
    val amountMinor: Long, // e.g., +100 paise for ₹1 reward, or -5000 paise for ₹50 payout
    val submissionId: String? = null,
    val payoutRequestId: String? = null,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    val amountRupees: Double get() = amountMinor / 100.0
    val formattedAmount: String get() = (if (amountMinor >= 0) "+₹%.2f" else "-₹%.2f").format(Math.abs(amountRupees))
}

/**
 * Creator Payout Request Status
 */
enum class CreatorPayoutStatus {
    REQUESTED,
    PROCESSING,
    PAID,
    FAILED,
    REJECTED
}

/**
 * Creator Withdrawal Payout Request Model
 */
data class CreatorPayoutRequest(
    val id: String,
    val userId: String,
    val amountMinor: Long,
    val upiId: String,
    val status: CreatorPayoutStatus = CreatorPayoutStatus.REQUESTED,
    val requestedAt: Long = System.currentTimeMillis(),
    val processedAt: Long? = null,
    val failureReason: String? = null,
    val adminNotes: String? = null,
    val transactionRef: String? = null
) {
    val amountRupees: Double get() = amountMinor / 100.0
    val formattedAmount: String get() = "₹%.2f".format(amountRupees)
}
