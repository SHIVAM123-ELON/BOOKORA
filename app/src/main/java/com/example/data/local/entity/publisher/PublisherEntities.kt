package com.example.data.local.entity.publisher

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.publisher.*

@Entity(
    tableName = "book_submissions",
    indices = [
        Index(value = ["uploaderUserId"]),
        Index(value = ["pdfSha256Hash"]),
        Index(value = ["status"])
    ]
)
data class BookSubmissionEntity(
    @PrimaryKey val id: String,
    val uploaderUserId: String,
    val uploaderName: String,
    val uploaderEmail: String,
    val title: String,
    val authorName: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val tags: String, // Comma separated tags
    val pdfFileUri: String,
    val pdfFileSizeBytes: Long,
    val pdfSha256Hash: String,
    val pdfPageCount: Int,
    val coverImageUri: String?,
    val status: String, // SubmissionStatus enum name
    val copyrightDeclarationAccepted: Boolean,
    val copyrightDeclarationTimestamp: Long,
    val copyrightDeclarationVersion: String,
    val moderationFeedback: String?,
    val reviewedByAdminId: String?,
    val reviewedAt: Long?,
    val rewardCredited: Boolean,
    val publishedBookId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): BookSubmission {
        return BookSubmission(
            id = id,
            uploaderUserId = uploaderUserId,
            uploaderName = uploaderName,
            uploaderEmail = uploaderEmail,
            title = title,
            authorName = authorName,
            description = description,
            categoryId = categoryId,
            categoryName = categoryName,
            language = language,
            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
            pdfFileUri = pdfFileUri,
            pdfFileSizeBytes = pdfFileSizeBytes,
            pdfSha256Hash = pdfSha256Hash,
            pdfPageCount = pdfPageCount,
            coverImageUri = coverImageUri,
            status = try { SubmissionStatus.valueOf(status) } catch (e: Exception) { SubmissionStatus.PENDING_REVIEW },
            copyrightDeclarationAccepted = copyrightDeclarationAccepted,
            copyrightDeclarationTimestamp = copyrightDeclarationTimestamp,
            copyrightDeclarationVersion = copyrightDeclarationVersion,
            moderationFeedback = moderationFeedback,
            reviewedByAdminId = reviewedByAdminId,
            reviewedAt = reviewedAt,
            rewardCredited = rewardCredited,
            publishedBookId = publishedBookId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(submission: BookSubmission): BookSubmissionEntity {
            return BookSubmissionEntity(
                id = submission.id,
                uploaderUserId = submission.uploaderUserId,
                uploaderName = submission.uploaderName,
                uploaderEmail = submission.uploaderEmail,
                title = submission.title,
                authorName = submission.authorName,
                description = submission.description,
                categoryId = submission.categoryId,
                categoryName = submission.categoryName,
                language = submission.language,
                tags = submission.tags.joinToString(","),
                pdfFileUri = submission.pdfFileUri,
                pdfFileSizeBytes = submission.pdfFileSizeBytes,
                pdfSha256Hash = submission.pdfSha256Hash,
                pdfPageCount = submission.pdfPageCount,
                coverImageUri = submission.coverImageUri,
                status = submission.status.name,
                copyrightDeclarationAccepted = submission.copyrightDeclarationAccepted,
                copyrightDeclarationTimestamp = submission.copyrightDeclarationTimestamp,
                copyrightDeclarationVersion = submission.copyrightDeclarationVersion,
                moderationFeedback = submission.moderationFeedback,
                reviewedByAdminId = submission.reviewedByAdminId,
                reviewedAt = submission.reviewedAt,
                rewardCredited = submission.rewardCredited,
                publishedBookId = submission.publishedBookId,
                createdAt = submission.createdAt,
                updatedAt = submission.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "creator_balances"
)
data class CreatorBalanceEntity(
    @PrimaryKey val userId: String,
    val availableBalanceMinor: Long,
    val lifetimeEarningsMinor: Long,
    val totalApprovedBooks: Int,
    val totalPendingBooks: Int,
    val totalRejectedBooks: Int,
    val isFrozen: Boolean,
    val freezeReason: String?,
    val updatedAt: Long
) {
    fun toDomain(): CreatorBalance {
        return CreatorBalance(
            userId = userId,
            availableBalanceMinor = availableBalanceMinor,
            lifetimeEarningsMinor = lifetimeEarningsMinor,
            totalApprovedBooks = totalApprovedBooks,
            totalPendingBooks = totalPendingBooks,
            totalRejectedBooks = totalRejectedBooks,
            isFrozen = isFrozen,
            freezeReason = freezeReason,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(balance: CreatorBalance): CreatorBalanceEntity {
            return CreatorBalanceEntity(
                userId = balance.userId,
                availableBalanceMinor = balance.availableBalanceMinor,
                lifetimeEarningsMinor = balance.lifetimeEarningsMinor,
                totalApprovedBooks = balance.totalApprovedBooks,
                totalPendingBooks = balance.totalPendingBooks,
                totalRejectedBooks = balance.totalRejectedBooks,
                isFrozen = balance.isFrozen,
                freezeReason = balance.freezeReason,
                updatedAt = balance.updatedAt
            )
        }
    }
}

@Entity(
    tableName = "creator_transactions",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["idempotencyKey"], unique = true)
    ]
)
data class CreatorTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val idempotencyKey: String,
    val type: String, // CreatorTransactionType enum name
    val amountMinor: Long,
    val submissionId: String?,
    val payoutRequestId: String?,
    val description: String,
    val createdAt: Long
) {
    fun toDomain(): CreatorTransaction {
        return CreatorTransaction(
            id = id,
            userId = userId,
            idempotencyKey = idempotencyKey,
            type = try { CreatorTransactionType.valueOf(type) } catch (e: Exception) { CreatorTransactionType.BOOK_APPROVAL_REWARD },
            amountMinor = amountMinor,
            submissionId = submissionId,
            payoutRequestId = payoutRequestId,
            description = description,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(tx: CreatorTransaction): CreatorTransactionEntity {
            return CreatorTransactionEntity(
                id = tx.id,
                userId = tx.userId,
                idempotencyKey = tx.idempotencyKey,
                type = tx.type.name,
                amountMinor = tx.amountMinor,
                submissionId = tx.submissionId,
                payoutRequestId = tx.payoutRequestId,
                description = tx.description,
                createdAt = tx.createdAt
            )
        }
    }
}

@Entity(
    tableName = "creator_payout_requests",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["status"])
    ]
)
data class CreatorPayoutRequestEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amountMinor: Long,
    val upiId: String,
    val status: String, // CreatorPayoutStatus enum name
    val requestedAt: Long,
    val processedAt: Long?,
    val failureReason: String?,
    val adminNotes: String?,
    val transactionRef: String?
) {
    fun toDomain(): CreatorPayoutRequest {
        return CreatorPayoutRequest(
            id = id,
            userId = userId,
            amountMinor = amountMinor,
            upiId = upiId,
            status = try { CreatorPayoutStatus.valueOf(status) } catch (e: Exception) { CreatorPayoutStatus.REQUESTED },
            requestedAt = requestedAt,
            processedAt = processedAt,
            failureReason = failureReason,
            adminNotes = adminNotes,
            transactionRef = transactionRef
        )
    }

    companion object {
        fun fromDomain(req: CreatorPayoutRequest): CreatorPayoutRequestEntity {
            return CreatorPayoutRequestEntity(
                id = req.id,
                userId = req.userId,
                amountMinor = req.amountMinor,
                upiId = req.upiId,
                status = req.status.name,
                requestedAt = req.requestedAt,
                processedAt = req.processedAt,
                failureReason = req.failureReason,
                adminNotes = req.adminNotes,
                transactionRef = req.transactionRef
            )
        }
    }
}

@Entity(
    tableName = "copyright_reports",
    indices = [
        Index(value = ["submissionId"])
    ]
)
data class CopyrightReportEntity(
    @PrimaryKey val id: String,
    val submissionId: String,
    val bookId: String?,
    val reporterUserId: String,
    val reporterEmail: String,
    val reason: String,
    val proofDetails: String,
    val isResolved: Boolean,
    val resolvedAt: Long?,
    val resolvedByAdminId: String?,
    val createdAt: Long
) {
    fun toDomain(): CopyrightReport {
        return CopyrightReport(
            id = id,
            submissionId = submissionId,
            bookId = bookId,
            reporterUserId = reporterUserId,
            reporterEmail = reporterEmail,
            reason = reason,
            proofDetails = proofDetails,
            isResolved = isResolved,
            resolvedAt = resolvedAt,
            resolvedByAdminId = resolvedByAdminId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(report: CopyrightReport): CopyrightReportEntity {
            return CopyrightReportEntity(
                id = report.id,
                submissionId = report.submissionId,
                bookId = report.bookId,
                reporterUserId = report.reporterUserId,
                reporterEmail = report.reporterEmail,
                reason = report.reason,
                proofDetails = report.proofDetails,
                isResolved = report.isResolved,
                resolvedAt = report.resolvedAt,
                resolvedByAdminId = report.resolvedByAdminId,
                createdAt = report.createdAt
            )
        }
    }
}
