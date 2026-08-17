package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiFeature
import com.example.domain.model.AiUsageRecord
import com.example.domain.model.BookSummary
import com.example.domain.model.Flashcard
import com.example.domain.model.MessageRole
import com.example.domain.model.RecommendationEvent
import com.example.domain.model.RecommendationEventType
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestion
import com.example.domain.model.StudyQuestionType

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_messages")
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // USER, ASSISTANT, SYSTEM
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedFollowUps: String = "", // Comma-separated or line-separated
    val contextSnippet: String? = null
) {
    fun toDomain(): AiChatMessage = AiChatMessage(
        id = id,
        conversationId = conversationId,
        role = try { MessageRole.valueOf(role) } catch (e: Exception) { MessageRole.ASSISTANT },
        content = content,
        timestamp = timestamp,
        suggestedFollowUps = if (suggestedFollowUps.isBlank()) emptyList() else suggestedFollowUps.split("|||"),
        contextSnippet = contextSnippet
    )

    companion object {
        fun fromDomain(msg: AiChatMessage): AiMessageEntity = AiMessageEntity(
            id = msg.id,
            conversationId = msg.conversationId,
            role = msg.role.name,
            content = msg.content,
            timestamp = msg.timestamp,
            suggestedFollowUps = msg.suggestedFollowUps.joinToString("|||"),
            contextSnippet = msg.contextSnippet
        )
    }
}

@Entity(tableName = "ai_usage")
data class AiUsageEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val feature: String, // READING_ASSISTANT, SUMMARY, etc.
    val requestCount: Int,
    val tokenUsage: Int,
    val date: String, // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): AiUsageRecord = AiUsageRecord(
        id = id,
        userId = userId,
        feature = try { AiFeature.valueOf(feature) } catch (e: Exception) { AiFeature.READING_ASSISTANT },
        requestCount = requestCount,
        tokenUsage = tokenUsage,
        date = date,
        createdAt = createdAt
    )
}

@Entity(tableName = "recommendation_events")
data class RecommendationEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val eventType: String, // VIEW, SEARCH, WISHLIST, PURCHASE, READ, COMPLETE, RATE
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): RecommendationEvent = RecommendationEvent(
        id = id,
        userId = userId,
        bookId = bookId,
        eventType = try { RecommendationEventType.valueOf(eventType) } catch (e: Exception) { RecommendationEventType.VIEW },
        metadata = metadata,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(event: RecommendationEvent): RecommendationEventEntity = RecommendationEventEntity(
            id = event.id,
            userId = event.userId,
            bookId = event.bookId,
            eventType = event.eventType.name,
            metadata = event.metadata,
            createdAt = event.createdAt
        )
    }
}

@Entity(tableName = "book_summaries")
data class BookSummaryEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterTitle: String?,
    val shortSummary: String,
    val keyIdeas: String, // Delimited by |||
    val mainTopics: String, // Delimited by |||
    val takeaways: String, // Delimited by |||
    val generatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): BookSummary = BookSummary(
        id = id,
        bookId = bookId,
        chapterTitle = chapterTitle,
        shortSummary = shortSummary,
        keyIdeas = if (keyIdeas.isBlank()) emptyList() else keyIdeas.split("|||"),
        mainTopics = if (mainTopics.isBlank()) emptyList() else mainTopics.split("|||"),
        takeaways = if (takeaways.isBlank()) emptyList() else takeaways.split("|||"),
        generatedAt = generatedAt
    )

    companion object {
        fun fromDomain(summary: BookSummary): BookSummaryEntity = BookSummaryEntity(
            id = summary.id,
            bookId = summary.bookId,
            chapterTitle = summary.chapterTitle,
            shortSummary = summary.shortSummary,
            keyIdeas = summary.keyIdeas.joinToString("|||"),
            mainTopics = summary.mainTopics.joinToString("|||"),
            takeaways = summary.takeaways.joinToString("|||"),
            generatedAt = summary.generatedAt
        )
    }
}
