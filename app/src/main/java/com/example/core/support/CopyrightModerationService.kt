package com.example.core.support

import com.example.core.observability.StructuredLogger
import com.example.domain.model.support.CopyrightClaim
import com.example.domain.model.support.CopyrightClaimStatus
import java.util.concurrent.ConcurrentHashMap

/**
 * Copyright & DMCA Compliance Moderation Service for Bookora.
 * Collects, verifies, and triages intellectual property claims from copyright holders.
 * Protects claimant privacy and ensures due process before taking action on authored books.
 */
object CopyrightModerationService {

    private val claims = ConcurrentHashMap<String, CopyrightClaim>()

    fun submitClaim(
        bookId: String,
        bookTitle: String,
        claimantName: String,
        claimantEmail: String,
        organizationName: String?,
        copyrightRegistrationNumber: String?,
        reason: String,
        evidenceUrl: String?
    ): Result<CopyrightClaim> {
        if (claimantName.isBlank() || claimantEmail.isBlank() || reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Claimant name, contact email, and infringement grounds are mandatory."))
        }

        val claim = CopyrightClaim(
            bookId = bookId,
            bookTitle = bookTitle,
            claimantName = claimantName,
            claimantEmail = claimantEmail,
            organizationName = organizationName,
            copyrightRegistrationNumber = copyrightRegistrationNumber,
            reason = reason,
            evidenceUrl = evidenceUrl,
            status = CopyrightClaimStatus.SUBMITTED
        )
        claims[claim.id] = claim

        StructuredLogger.warn(
            "COPYRIGHT_CLAIM_SUBMITTED",
            mapOf("claimId" to claim.id, "bookId" to bookId, "claimant" to claimantName)
        )

        return Result.success(claim)
    }

    fun reviewClaim(
        claimId: String,
        newStatus: CopyrightClaimStatus,
        moderatorNotes: String
    ): CopyrightClaim? {
        val claim = claims[claimId] ?: return null
        val updated = claim.copy(
            status = newStatus,
            moderatorNotes = moderatorNotes,
            reviewedAt = System.currentTimeMillis()
        )
        claims[claimId] = updated

        StructuredLogger.info(
            "COPYRIGHT_CLAIM_REVIEWED",
            mapOf("claimId" to claimId, "newStatus" to newStatus.name)
        )

        return updated
    }

    fun getClaims(): List<CopyrightClaim> {
        return claims.values.sortedByDescending { it.createdAt }
    }
}
