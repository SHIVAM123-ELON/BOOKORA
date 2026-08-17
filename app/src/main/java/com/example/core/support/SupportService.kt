package com.example.core.support

import com.example.core.observability.StructuredLogger
import com.example.domain.model.support.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Support & Helpdesk Service for Bookora.
 * Handles ticket triage, SLA prioritization, and resolution workflows.
 */
object SupportService {

    private val tickets = ConcurrentHashMap<String, SupportTicket>()

    init {
        // Seed standard FAQ / demo ticket
        val initialTicket = SupportTicket(
            id = "tkt_welcome1",
            userId = "reader_001",
            userEmail = "reader@example.com",
            category = TicketCategory.GENERAL_INQUIRY,
            subject = "How do I read my books offline?",
            message = "I want to read my purchased books during my flight.",
            status = TicketStatus.RESOLVED,
            priority = TicketPriority.LOW,
            messages = listOf(
                TicketMessage(
                    senderId = "support_agent_01",
                    senderName = "Bookora Support",
                    isStaff = true,
                    message = "Hi! You can tap the download icon on any book in your Library to store it securely for offline reading."
                )
            )
        )
        tickets[initialTicket.id] = initialTicket
    }

    fun createTicket(
        userId: String,
        userEmail: String,
        category: TicketCategory,
        subject: String,
        message: String,
        relatedEntityId: String? = null,
        priority: TicketPriority = TicketPriority.MEDIUM
    ): SupportTicket {
        val ticket = SupportTicket(
            userId = userId,
            userEmail = userEmail,
            category = category,
            subject = subject,
            message = message,
            relatedEntityId = relatedEntityId,
            priority = priority
        )
        tickets[ticket.id] = ticket

        StructuredLogger.info(
            "SUPPORT_TICKET_CREATED",
            mapOf("ticketId" to ticket.id, "userId" to userId, "category" to category.name, "priority" to priority.name)
        )

        return ticket
    }

    fun addReply(
        ticketId: String,
        senderId: String,
        senderName: String,
        isStaff: Boolean,
        message: String
    ): SupportTicket? {
        val ticket = tickets[ticketId] ?: return null
        val newMsg = TicketMessage(
            senderId = senderId,
            senderName = senderName,
            isStaff = isStaff,
            message = message
        )
        val updated = ticket.copy(
            messages = ticket.messages + newMsg,
            status = if (isStaff) TicketStatus.WAITING_FOR_USER else TicketStatus.IN_PROGRESS,
            updatedAt = System.currentTimeMillis()
        )
        tickets[ticketId] = updated
        return updated
    }

    fun updateStatus(ticketId: String, status: TicketStatus): SupportTicket? {
        val ticket = tickets[ticketId] ?: return null
        val updated = ticket.copy(
            status = status,
            updatedAt = System.currentTimeMillis(),
            resolvedAt = if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) System.currentTimeMillis() else null
        )
        tickets[ticketId] = updated
        return updated
    }

    fun getUserTickets(userId: String): List<SupportTicket> {
        return tickets.values.filter { it.userId == userId }.sortedByDescending { it.createdAt }
    }

    fun getAllTickets(): List<SupportTicket> {
        return tickets.values.sortedByDescending { it.createdAt }
    }
}
