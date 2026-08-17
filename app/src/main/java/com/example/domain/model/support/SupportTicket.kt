package com.example.domain.model.support

import java.util.UUID

enum class TicketCategory {
    GENERAL_INQUIRY,
    PAYMENT_ISSUE,
    BOOK_ACCESS_ISSUE,
    SUBSCRIPTION_ISSUE,
    COPYRIGHT_REPORT,
    AUTHOR_PAYOUT_QUERY,
    ACCOUNT_SECURITY,
    BUG_REPORT
}

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_USER,
    RESOLVED,
    CLOSED
}

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class TicketMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderId: String,
    val senderName: String,
    val isStaff: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SupportTicket(
    val id: String = "tkt_" + UUID.randomUUID().toString().take(10),
    val userId: String,
    val userEmail: String,
    val category: TicketCategory,
    val subject: String,
    val message: String,
    val status: TicketStatus = TicketStatus.OPEN,
    val priority: TicketPriority = TicketPriority.MEDIUM,
    val assignedTo: String? = null,
    val messages: List<TicketMessage> = emptyList(),
    val relatedEntityId: String? = null, // e.g. orderId, bookId
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

enum class CopyrightClaimStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ACTION_REQUIRED,
    RESOLVED,
    REJECTED
}

data class CopyrightClaim(
    val id: String = "cpr_" + UUID.randomUUID().toString().take(10),
    val bookId: String,
    val bookTitle: String,
    val claimantName: String,
    val claimantEmail: String,
    val organizationName: String? = null,
    val copyrightRegistrationNumber: String? = null,
    val reason: String,
    val evidenceUrl: String? = null,
    val statementOfGoodFaith: Boolean = true,
    val status: CopyrightClaimStatus = CopyrightClaimStatus.SUBMITTED,
    val moderatorNotes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long? = null
)

enum class IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class IncidentStatus {
    OPEN,
    INVESTIGATING,
    MITIGATED,
    RESOLVED
}

data class Incident(
    val id: String = "inc_" + UUID.randomUUID().toString().take(8),
    val severity: IncidentSeverity,
    val title: String,
    val description: String,
    val affectedServices: List<String>,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val startedAt: Long = System.currentTimeMillis(),
    val mitigatedAt: Long? = null,
    val resolvedAt: Long? = null,
    val postMortemUrl: String? = null
)
