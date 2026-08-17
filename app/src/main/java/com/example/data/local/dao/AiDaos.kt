package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.AiUsageEntity
import com.example.data.local.entity.BookSummaryEntity
import com.example.data.local.entity.RecommendationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations WHERE userId = :userId AND bookId = :bookId ORDER BY updatedAt DESC LIMIT 1")
    fun getConversation(userId: String, bookId: String): Flow<AiConversationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)

    @Query("DELETE FROM ai_conversations WHERE userId = :userId AND bookId = :bookId")
    suspend fun deleteConversation(userId: String, bookId: String)
}

@Dao
interface AiMessageDao {
    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<AiMessageEntity>)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
}

@Dao
interface AiUsageDao {
    @Query("SELECT * FROM ai_usage WHERE userId = :userId AND feature = :feature AND date = :date LIMIT 1")
    fun getUsage(userId: String, feature: String, date: String): Flow<AiUsageEntity?>

    @Query("SELECT * FROM ai_usage WHERE userId = :userId AND feature = :feature AND date = :date LIMIT 1")
    suspend fun getUsageDirect(userId: String, feature: String, date: String): AiUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUsage(usage: AiUsageEntity)
}

@Dao
interface RecommendationEventDao {
    @Query("SELECT * FROM recommendation_events WHERE userId = :userId ORDER BY createdAt DESC LIMIT 50")
    fun getRecentEventsForUser(userId: String): Flow<List<RecommendationEventEntity>>

    @Query("SELECT * FROM recommendation_events WHERE userId = :userId AND eventType = :eventType ORDER BY createdAt DESC LIMIT 20")
    fun getEventsByType(userId: String, eventType: String): Flow<List<RecommendationEventEntity>>

    @Query("SELECT * FROM recommendation_events WHERE userId = :userId AND eventType = :eventType ORDER BY createdAt DESC LIMIT 20")
    suspend fun getEventsByTypeDirect(userId: String, eventType: String): List<RecommendationEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: RecommendationEventEntity)
}

@Dao
interface BookSummaryDao {
    @Query("SELECT * FROM book_summaries WHERE bookId = :bookId AND chapterTitle IS NULL LIMIT 1")
    fun getBookSummary(bookId: String): Flow<BookSummaryEntity?>

    @Query("SELECT * FROM book_summaries WHERE bookId = :bookId AND chapterTitle = :chapterTitle LIMIT 1")
    fun getChapterSummary(bookId: String, chapterTitle: String): Flow<BookSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: BookSummaryEntity)
}
