package com.example.domain.repository

import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiFeature
import com.example.domain.model.AuthorAiPromptType
import com.example.domain.model.AuthorAiResult
import com.example.domain.model.BookFilter
import com.example.domain.model.BookRecommendation
import com.example.domain.model.BookSummary
import com.example.domain.model.ExplanationMode
import com.example.domain.model.RecommendationEvent
import com.example.domain.model.SemanticQueryAnalysis
import com.example.domain.model.SemanticSearchResult
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestionType
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    suspend fun askReadingAssistant(
        userId: String,
        bookId: String,
        question: String,
        contextSnippet: String?,
        mode: ExplanationMode = ExplanationMode.SIMPLE
    ): Result<AiChatMessage>

    suspend fun summarizeBook(
        userId: String,
        bookId: String
    ): Result<BookSummary>

    suspend fun summarizeChapter(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String
    ): Result<BookSummary>

    suspend fun generateStudyDeck(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String,
        questionTypes: List<StudyQuestionType> = listOf(StudyQuestionType.MCQ, StudyQuestionType.SHORT_ANSWER, StudyQuestionType.FLASHCARD)
    ): Result<StudyDeck>

    suspend fun generateAuthorAssistance(
        userId: String,
        type: AuthorAiPromptType,
        title: String,
        context: String,
        targetAudience: String? = null
    ): Result<AuthorAiResult>

    fun getConversationHistory(userId: String, bookId: String): Flow<List<AiChatMessage>>

    suspend fun clearConversation(userId: String, bookId: String): Result<Unit>

    fun getCachedSummary(bookId: String, chapterTitle: String?): Flow<BookSummary?>
}

interface RecommendationRepository {
    suspend fun recordEvent(event: RecommendationEvent)
    fun getPersonalizedRecommendations(userId: String, limit: Int = 10): Flow<List<BookRecommendation>>
    fun getBecauseYouReadRecommendations(userId: String): Flow<List<BookRecommendation>>
    fun getSimilarToWishlistRecommendations(userId: String): Flow<List<BookRecommendation>>
    fun getContinueLearningRecommendations(userId: String): Flow<List<BookRecommendation>>
    fun getPopularInInterestsRecommendations(userId: String): Flow<List<BookRecommendation>>
}

interface SemanticSearchRepository {
    fun semanticSearch(query: String, filter: BookFilter): Flow<List<SemanticSearchResult>>
    fun analyzeQuery(query: String): SemanticQueryAnalysis
}

interface AiUsageRepository {
    suspend fun recordUsage(userId: String, feature: AiFeature, tokens: Int = 0)
    fun getTodayUsage(userId: String, feature: AiFeature): Flow<Int>
    fun canUseFeature(userId: String, feature: AiFeature, isPlusUser: Boolean): Flow<Boolean>
}
