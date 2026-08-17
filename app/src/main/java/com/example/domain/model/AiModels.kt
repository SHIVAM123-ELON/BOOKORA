package com.example.domain.model

enum class AiFeature(val displayName: String, val dailyLimitFree: Int, val dailyLimitPlus: Int) {
    READING_ASSISTANT("Reading Assistant", 15, 100),
    SUMMARY("Book Summary", 10, 50),
    CHAPTER_SUMMARY("Chapter Summary", 15, 100),
    STUDY_MODE("AI Study Mode", 10, 50),
    RECOMMENDATION("AI Recommendations", 30, 200),
    AUTHOR_ASSISTANT("Author Writing AI", 10, 50),
    SEMANTIC_SEARCH("Semantic Search", 30, 200)
}

enum class ExplanationMode(val label: String, val description: String) {
    SIMPLE("Explain Simply", "Easy analogies, plain English, jargon-free"),
    DETAILED("Detailed", "In-depth breakdown with step-by-step reasoning"),
    TECHNICAL("Technical", "Engineering precision, axioms, and architectural depth"),
    EXAM_PREPARATION("Exam Prep", "Key takeaways, testable points, and memory anchors")
}

enum class StudyQuestionType(val label: String) {
    MCQ("Multiple Choice"),
    SHORT_ANSWER("Short Answer"),
    TRUE_FALSE("True / False"),
    FLASHCARD("Flashcards")
}

data class StudyQuestion(
    val id: String,
    val type: StudyQuestionType,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val memoryTip: String = "",
    val userAnswerIndex: Int? = null
)

data class Flashcard(
    val id: String,
    val front: String,
    val back: String,
    val keyConcept: String = ""
)

data class StudyDeck(
    val id: String,
    val bookId: String,
    val chapterTitle: String,
    val questions: List<StudyQuestion> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val revisionNotes: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class BookSummary(
    val id: String,
    val bookId: String,
    val chapterTitle: String? = null,
    val shortSummary: String,
    val keyIdeas: List<String>,
    val mainTopics: List<String>,
    val takeaways: List<String>,
    val generatedAt: Long = System.currentTimeMillis()
)

data class AiChatMessage(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedFollowUps: List<String> = emptyList(),
    val contextSnippet: String? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class AiConversation(
    val id: String,
    val userId: String,
    val bookId: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class AuthorAiPromptType(val label: String) {
    BOOK_DESCRIPTION("Generate Description"),
    SUBTITLE_IDEAS("Subtitle Ideas"),
    KEYWORDS("Generate Keywords & Tags"),
    IMPROVE_DESCRIPTION("Polish & Improve Description"),
    CATEGORY_SUGGESTIONS("Category Suggestions"),
    PROMOTIONAL_TEXT("Social & Store Promo Copy")
}

data class AuthorAiResult(
    val type: AuthorAiPromptType,
    val title: String,
    val suggestions: List<String>,
    val metadata: Map<String, String> = emptyMap()
)

enum class RecommendationEventType {
    VIEW,
    SEARCH,
    WISHLIST,
    PURCHASE,
    READ,
    COMPLETE,
    RATE
}

data class RecommendationEvent(
    val id: String,
    val userId: String,
    val bookId: String,
    val eventType: RecommendationEventType,
    val metadata: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class RecommendationType(val title: String) {
    RECOMMENDED_FOR_YOU("Recommended For You"),
    BECAUSE_YOU_READ("Because You Read"),
    SIMILAR_BOOKS("Similar Books"),
    CONTINUE_LEARNING("Continue Your Learning"),
    POPULAR_IN_INTERESTS("Popular in Your Interests"),
    GENERAL_POPULAR("Trending across Bookora")
}

data class BookRecommendation(
    val book: Book,
    val type: RecommendationType,
    val reason: String?,
    val score: Float = 1.0f
)

data class AiUsageRecord(
    val id: String,
    val userId: String,
    val feature: AiFeature,
    val requestCount: Int,
    val tokenUsage: Int,
    val date: String, // YYYY-MM-DD
    val createdAt: Long = System.currentTimeMillis()
)

data class SemanticQueryAnalysis(
    val rawQuery: String,
    val topic: String,
    val level: String? = null, // Beginner, Intermediate, Advanced
    val intent: String? = null, // Learning, Quick Reference, Comprehensive
    val keywords: List<String> = emptyList()
)

data class SemanticSearchResult(
    val book: Book,
    val relevanceScore: Float,
    val matchedReason: String,
    val matchedConcepts: List<String> = emptyList()
)
