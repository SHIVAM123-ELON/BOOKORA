package com.example.domain.usecase

import com.example.domain.model.AiChatMessage
import com.example.domain.model.AuthorAiPromptType
import com.example.domain.model.AuthorAiResult
import com.example.domain.model.BookFilter
import com.example.domain.model.BookRecommendation
import com.example.domain.model.BookSummary
import com.example.domain.model.ExplanationMode
import com.example.domain.model.RecommendationEvent
import com.example.domain.model.RecommendationEventType
import com.example.domain.model.SemanticSearchResult
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestionType
import com.example.domain.repository.AIRepository
import com.example.domain.repository.AiUsageRepository
import com.example.domain.repository.RecommendationRepository
import com.example.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AskReadingAssistantUseCase(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(
        userId: String,
        bookId: String,
        question: String,
        contextSnippet: String?,
        mode: ExplanationMode = ExplanationMode.SIMPLE
    ): Result<AiChatMessage> {
        return aiRepository.askReadingAssistant(userId, bookId, question, contextSnippet, mode)
    }

    fun getHistory(userId: String, bookId: String): Flow<List<AiChatMessage>> {
        return aiRepository.getConversationHistory(userId, bookId)
    }

    suspend fun clearHistory(userId: String, bookId: String): Result<Unit> {
        return aiRepository.clearConversation(userId, bookId)
    }
}

class GetBookSummaryUseCase(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(userId: String, bookId: String): Result<BookSummary> {
        return aiRepository.summarizeBook(userId, bookId)
    }

    fun getCached(bookId: String): Flow<BookSummary?> {
        return aiRepository.getCachedSummary(bookId, null)
    }
}

class GetChapterSummaryUseCase(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String
    ): Result<BookSummary> {
        return aiRepository.summarizeChapter(userId, bookId, chapterTitle, chapterContent)
    }

    fun getCached(bookId: String, chapterTitle: String): Flow<BookSummary?> {
        return aiRepository.getCachedSummary(bookId, chapterTitle)
    }
}

class GenerateStudyDeckUseCase(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String,
        questionTypes: List<StudyQuestionType> = listOf(StudyQuestionType.MCQ, StudyQuestionType.SHORT_ANSWER, StudyQuestionType.FLASHCARD)
    ): Result<StudyDeck> {
        return aiRepository.generateStudyDeck(userId, bookId, chapterTitle, chapterContent, questionTypes)
    }
}

class GetPersonalizedRecommendationsUseCase(
    private val recommendationRepository: RecommendationRepository
) {
    operator fun invoke(userId: String, limit: Int = 10): Flow<List<BookRecommendation>> {
        return recommendationRepository.getPersonalizedRecommendations(userId, limit)
    }

    suspend fun recordEvent(userId: String, bookId: String, type: RecommendationEventType, metadata: String? = null) {
        val event = RecommendationEvent(
            id = UUID.randomUUID().toString(),
            userId = userId,
            bookId = bookId,
            eventType = type,
            metadata = metadata
        )
        recommendationRepository.recordEvent(event)
    }
}

class SemanticSearchUseCase(
    private val semanticSearchRepository: SemanticSearchRepository
) {
    operator fun invoke(query: String, filter: BookFilter = BookFilter()): Flow<List<SemanticSearchResult>> {
        return semanticSearchRepository.semanticSearch(query, filter)
    }
}

class AuthorWritingAiUseCase(
    private val aiRepository: AIRepository
) {
    suspend operator fun invoke(
        userId: String,
        type: AuthorAiPromptType,
        title: String,
        context: String,
        targetAudience: String? = null
    ): Result<AuthorAiResult> {
        return aiRepository.generateAuthorAssistance(userId, type, title, context, targetAudience)
    }
}
