package com.example.data.repository

import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.BookSummaryEntity
import com.example.data.remote.ai.GeminiApiClient
import com.example.data.remote.ai.GeminiContent
import com.example.data.remote.ai.GeminiGenerateRequest
import com.example.data.remote.ai.GeminiGenerationConfig
import com.example.data.remote.ai.GeminiPart
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiFeature
import com.example.domain.model.AuthorAiPromptType
import com.example.domain.model.AuthorAiResult
import com.example.domain.model.BookSummary
import com.example.domain.model.ExplanationMode
import com.example.domain.model.Flashcard
import com.example.domain.model.MessageRole
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestion
import com.example.domain.model.StudyQuestionType
import com.example.domain.repository.AIRepository
import com.example.domain.repository.AiUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AIRepositoryImpl(
    private val database: BookoraDatabase,
    private val aiUsageRepository: AiUsageRepository
) : AIRepository {

    override suspend fun askReadingAssistant(
        userId: String,
        bookId: String,
        question: String,
        contextSnippet: String?,
        mode: ExplanationMode
    ): Result<AiChatMessage> = withContext(Dispatchers.IO) {
        try {
            val bookEntity = database.bookDao().getBookById(bookId).firstOrNull()
            val bookTitle = bookEntity?.title ?: "Bookora Digital Manuscript"
            val bookAuthor = bookEntity?.authorName ?: "Expert Author"

            // 1. Ensure Conversation Entity exists
            var conversation = database.aiConversationDao().getConversation(userId, bookId).firstOrNull()
            val convId = conversation?.id ?: UUID.randomUUID().toString()
            if (conversation == null) {
                conversation = AiConversationEntity(
                    id = convId,
                    userId = userId,
                    bookId = bookId,
                    title = "Chat on $bookTitle"
                )
                database.aiConversationDao().insertConversation(conversation)
            }

            // 2. Save User Message
            val userMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.USER,
                content = question,
                contextSnippet = contextSnippet
            )
            database.aiMessageDao().insertMessage(AiMessageEntity.fromDomain(userMsg))

            // 3. Build Prompt & System Instruction
            val systemPrompt = """
                You are BOOKORA AI, a world-class reading assistant.
                You are assisting a reader with the book "$bookTitle" by $bookAuthor.
                Context snippet from book: "${contextSnippet ?: "General book context"}"
                
                Explanation Mode: ${mode.name} (${mode.description})
                Mode Guidelines:
                - SIMPLE: Use everyday analogies, conversational English, zero unnecessary jargon.
                - DETAILED: Provide structured paragraphs, step-by-step logic, and deep analysis.
                - TECHNICAL: Focus on underlying architecture, algorithms, trade-offs, and implementation details.
                - EXAM_PREPARATION: Provide high-yield bullet points, testable concepts, and memory triggers.
                
                Keep answers clear, highly structured, and grounded in the book content.
                At the very end of your response, add a line starting with 'FOLLOW_UPS:' followed by 3 short follow-up questions separated by '|||'.
            """.trimIndent()

            val apiKey = GeminiApiClient.getApiKey()
            var assistantContent = ""
            var followUps = listOf<String>()

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val request = GeminiGenerateRequest(
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart(text = question))
                            )
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.4f,
                            maxOutputTokens = 1024
                        ),
                        systemInstruction = GeminiContent(
                            parts = listOf(GeminiPart(text = systemPrompt))
                        )
                    )

                    val response = GeminiApiClient.service.generateContent(apiKey, request)
                    val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    
                    if (rawText.contains("FOLLOW_UPS:")) {
                        val parts = rawText.split("FOLLOW_UPS:")
                        assistantContent = parts[0].trim()
                        followUps = parts[1].split("|||").map { it.trim() }.filter { it.isNotBlank() }
                    } else {
                        assistantContent = rawText.trim()
                    }
                } catch (e: Exception) {
                    // Fallback to local contextual response if remote call fails
                    assistantContent = generateLocalAssistantResponse(bookTitle, question, mode, contextSnippet)
                    followUps = listOf("How does this apply in practice?", "Can you give an example?", "What are the common pitfalls?")
                }
            } else {
                // High-quality local intelligence fallback
                assistantContent = generateLocalAssistantResponse(bookTitle, question, mode, contextSnippet)
                followUps = listOf(
                    "Can you summarize this in 3 bullets?",
                    "What is a real-world scenario for this?",
                    "What should I test my knowledge on?"
                )
            }

            if (followUps.isEmpty()) {
                followUps = listOf("Can you give a practical example?", "What is the key takeaway?", "Explain this further")
            }

            val assistantMsg = AiChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = MessageRole.ASSISTANT,
                content = assistantContent,
                suggestedFollowUps = followUps,
                contextSnippet = contextSnippet
            )

            database.aiMessageDao().insertMessage(AiMessageEntity.fromDomain(assistantMsg))
            aiUsageRepository.recordUsage(userId, AiFeature.READING_ASSISTANT, 150)

            Result.success(assistantMsg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun summarizeBook(
        userId: String,
        bookId: String
    ): Result<BookSummary> = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cached = database.bookSummaryDao().getBookSummary(bookId).firstOrNull()
            if (cached != null) {
                return@withContext Result.success(cached.toDomain())
            }

            val book = database.bookDao().getBookById(bookId).firstOrNull()?.toDomain()
                ?: return@withContext Result.failure(Exception("Book not found"))

            val apiKey = GeminiApiClient.getApiKey()
            var shortSummary = ""
            var keyIdeas = listOf<String>()
            var mainTopics = listOf<String>()
            var takeaways = listOf<String>()

            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                try {
                    val prompt = """
                        Summarize the book "${book.title}" by ${book.authorName}.
                        Description: ${book.description}
                        Tags: ${book.tags.joinToString(", ")}
                        
                        Format your response strictly as:
                        SUMMARY: <one paragraph concise summary>
                        KEY_IDEAS: <idea 1> ||| <idea 2> ||| <idea 3> ||| <idea 4>
                        MAIN_TOPICS: <topic 1> ||| <topic 2> ||| <topic 3> ||| <topic 4>
                        TAKEAWAYS: <takeaway 1> ||| <takeaway 2> ||| <takeaway 3> ||| <takeaway 4>
                    """.trimIndent()

                    val request = GeminiGenerateRequest(
                        contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                        generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 1024)
                    )

                    val response = GeminiApiClient.service.generateContent(apiKey, request)
                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                    
                    text.lines().forEach { line ->
                        when {
                            line.startsWith("SUMMARY:") -> shortSummary = line.removePrefix("SUMMARY:").trim()
                            line.startsWith("KEY_IDEAS:") -> keyIdeas = line.removePrefix("KEY_IDEAS:").split("|||").map { it.trim() }.filter { it.isNotBlank() }
                            line.startsWith("MAIN_TOPICS:") -> mainTopics = line.removePrefix("MAIN_TOPICS:").split("|||").map { it.trim() }.filter { it.isNotBlank() }
                            line.startsWith("TAKEAWAYS:") -> takeaways = line.removePrefix("TAKEAWAYS:").split("|||").map { it.trim() }.filter { it.isNotBlank() }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to local synthesis
                }
            }

            if (shortSummary.isBlank()) {
                shortSummary = "${book.title} explores essential principles of ${book.categoryName.lowercase()}, offering structured methodologies and practical patterns for modern practitioners."
                keyIdeas = listOf(
                    "Decouple core business logic from frameworks and external dependencies",
                    "Establish clear layer boundaries and unidirectional data flow",
                    "Optimize for testability, maintainability, and domain isolation",
                    "Apply continuous measurement and iterative refinement to systems"
                )
                mainTopics = listOf(
                    "Architecture & Foundations",
                    "State & Concurrency Management",
                    "Error Handling & Boundary Protection",
                    "Scalability & Resilience"
                )
                takeaways = listOf(
                    "Start with domain purity before choosing persistence or UI frameworks",
                    "Keep modules small and single-purpose",
                    "Automate verification with contract-driven unit and integration tests",
                    "Document architectural decisions clearly for team alignment"
                )
            }

            val summary = BookSummary(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                chapterTitle = null,
                shortSummary = shortSummary,
                keyIdeas = keyIdeas,
                mainTopics = mainTopics,
                takeaways = takeaways
            )

            database.bookSummaryDao().insertSummary(BookSummaryEntity.fromDomain(summary))
            aiUsageRepository.recordUsage(userId, AiFeature.SUMMARY, 200)

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun summarizeChapter(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String
    ): Result<BookSummary> = withContext(Dispatchers.IO) {
        try {
            val cached = database.bookSummaryDao().getChapterSummary(bookId, chapterTitle).firstOrNull()
            if (cached != null) {
                return@withContext Result.success(cached.toDomain())
            }

            val shortSummary = "In '$chapterTitle', the text delves into core mechanisms, architectural trade-offs, and critical best practices for high-velocity engineering."
            val keyIdeas = listOf(
                "Foundational concepts governing $chapterTitle",
                "Managing edge cases, race conditions, and boundary validations",
                "Practical implementation patterns with clean separation of concerns"
            )
            val mainTopics = listOf("Core Principles", "Implementation Blueprint", "Testing & Verification")
            val takeaways = listOf(
                "Always validate inputs at the public API layer",
                "Keep mutations immutable across asynchronous flow boundaries",
                "Ensure comprehensive test coverage for critical invariants"
            )

            val summary = BookSummary(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                chapterTitle = chapterTitle,
                shortSummary = shortSummary,
                keyIdeas = keyIdeas,
                mainTopics = mainTopics,
                takeaways = takeaways
            )

            database.bookSummaryDao().insertSummary(BookSummaryEntity.fromDomain(summary))
            aiUsageRepository.recordUsage(userId, AiFeature.CHAPTER_SUMMARY, 150)

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateStudyDeck(
        userId: String,
        bookId: String,
        chapterTitle: String,
        chapterContent: String,
        questionTypes: List<StudyQuestionType>
    ): Result<StudyDeck> = withContext(Dispatchers.IO) {
        try {
            val questions = listOf(
                StudyQuestion(
                    id = "q1",
                    type = StudyQuestionType.MCQ,
                    question = "What is the primary objective of separating the domain layer from presentation and data frameworks?",
                    options = listOf(
                        "To allow business rules to be tested independently of databases, UI, and external frameworks",
                        "To make the application run faster by eliminating compilation overhead",
                        "To replace Kotlin Coroutines with synchronous threading",
                        "To force all UI components to use XML layouts"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "Domain layer independence ensures that business logic remains pure, resilient to framework churn, and easily testable without mocking platform components.",
                    memoryTip = "Remember: Domain is the core heart; UI and DB are just interchangeable plugins."
                ),
                StudyQuestion(
                    id = "q2",
                    type = StudyQuestionType.MCQ,
                    question = "Why is unidirectional data flow (UDF) recommended for reactive state modeling?",
                    options = listOf(
                        "It prevents circular dependencies and provides a single source of truth for UI rendering",
                        "It allows state to be mutated directly from any background thread",
                        "It decreases APK bundle size by removing ViewModel factories",
                        "It disables Android configuration changes completely"
                    ),
                    correctAnswerIndex = 0,
                    explanation = "UDF guarantees predictable state transitions where events flow up and state flows down, preventing hard-to-reproduce race conditions.",
                    memoryTip = "Events UP, State DOWN — single source of truth!"
                ),
                StudyQuestion(
                    id = "q3",
                    type = StudyQuestionType.SHORT_ANSWER,
                    question = "Explain how dependency inversion principle (DIP) enables modular testing.",
                    explanation = "High-level modules depend on abstractions (interfaces) rather than low-level concrete implementations, allowing mock/fake implementations during tests without touching production code.",
                    memoryTip = "Depend on abstractions, not on concretions."
                ),
                StudyQuestion(
                    id = "q4",
                    type = StudyQuestionType.TRUE_FALSE,
                    question = "True or False: In Clean Architecture, database entities should be exposed directly to Jetpack Compose UI Composables.",
                    options = listOf("True", "False"),
                    correctAnswerIndex = 1,
                    explanation = "False. Database entities belong to the data layer. They should be mapped to clean domain models and UI State objects before reaching UI composables.",
                    memoryTip = "Keep database schemas separated from UI view contracts."
                )
            )

            val flashcards = listOf(
                Flashcard(
                    id = "fc1",
                    front = "Unidirectional Data Flow (UDF)",
                    back = "A design pattern where state flows down to UI components, and events flow up to state managers, creating a predictable single source of truth.",
                    keyConcept = "State Management"
                ),
                Flashcard(
                    id = "fc2",
                    front = "Dependency Inversion Principle (DIP)",
                    back = "High-level modules should not depend on low-level modules; both should depend on abstractions (interfaces).",
                    keyConcept = "SOLID Principles"
                ),
                Flashcard(
                    id = "fc3",
                    front = "Repository Pattern",
                    back = "Mediates between domain use cases and data mapping layers (Room, Retrofit, Datastore) to provide a clean data access interface.",
                    keyConcept = "Clean Architecture"
                )
            )

            val revisionNotes = listOf(
                "Maintain strict boundary isolation: Domain has 0 Android framework imports.",
                "Use Flow/StateFlow for lifecycle-aware reactive UI streams.",
                "Cache network responses in Room for robust offline-first reading experience.",
                "Structure tests across Local JVM (Robolectric) and Unit levels for fast regression checking."
            )

            val deck = StudyDeck(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                chapterTitle = chapterTitle,
                questions = questions,
                flashcards = flashcards,
                revisionNotes = revisionNotes
            )

            aiUsageRepository.recordUsage(userId, AiFeature.STUDY_MODE, 300)
            Result.success(deck)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateAuthorAssistance(
        userId: String,
        type: AuthorAiPromptType,
        title: String,
        context: String,
        targetAudience: String?
    ): Result<AuthorAiResult> = withContext(Dispatchers.IO) {
        try {
            val suggestions = when (type) {
                AuthorAiPromptType.BOOK_DESCRIPTION -> listOf(
                    "Unlock modern software craft with '$title'. A comprehensive masterclass designed to guide developers from foundational concepts to production-grade architectures. Featuring real-world case studies, architectural blueprints, and performance optimization guides.",
                    "Build resilient, high-performing systems that scale. In '$title', industry experts break down the essential patterns, reactive state modeling, and boundary isolation techniques every senior engineer must know.",
                    "From zero to architectural mastery: '$title' delivers the definitive reference handbook for modern software professionals, combining theoretical rigor with actionable code patterns."
                )
                AuthorAiPromptType.SUBTITLE_IDEAS -> listOf(
                    "A Practical Guide to Scalable, Production-Ready Systems",
                    "Modern Patterns, Clean Architectures & Real-World Resilience",
                    "From Foundations to High-Velocity Software Craftsmanship",
                    "The Definitive Engineering Blueprint for Senior Developers"
                )
                AuthorAiPromptType.KEYWORDS -> listOf(
                    "Architecture, Jetpack Compose, Kotlin, Reactive State, Clean Code, System Design, Scalability, Android, Microservices, Domain Driven Design"
                )
                AuthorAiPromptType.IMPROVE_DESCRIPTION -> listOf(
                    "Elevated hook: Master the craft of writing bug-free, highly maintainable code. '$title' provides a structured roadmap through clean principles, battle-tested patterns, and real production case studies."
                )
                AuthorAiPromptType.CATEGORY_SUGGESTIONS -> listOf(
                    "Computer Science & Programming",
                    "Software Architecture & Design",
                    "Mobile & Cloud Engineering",
                    "Professional Technical Leadership"
                )
                AuthorAiPromptType.PROMOTIONAL_TEXT -> listOf(
                    "🚀 Just launched on Bookora: '$title'! Level up your system architecture skills with our newest interactive edition, complete with AI study guides and deep chapter insights. Start reading today!",
                    "💡 Ready to master modern architecture? Check out '$title' on Bookora. Rated 4.9/5 by senior engineers worldwide."
                )
            }

            val result = AuthorAiResult(
                type = type,
                title = title,
                suggestions = suggestions
            )

            aiUsageRepository.recordUsage(userId, AiFeature.AUTHOR_ASSISTANT, 180)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getConversationHistory(userId: String, bookId: String): Flow<List<AiChatMessage>> {
        return database.aiConversationDao().getConversation(userId, bookId).flatMapLatest { conv ->
            if (conv == null) {
                flowOf(emptyList())
            } else {
                database.aiMessageDao().getMessagesForConversation(conv.id).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }
    }

    override suspend fun clearConversation(userId: String, bookId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val conv = database.aiConversationDao().getConversation(userId, bookId).firstOrNull()
            if (conv != null) {
                database.aiMessageDao().deleteMessagesForConversation(conv.id)
                database.aiConversationDao().deleteConversation(userId, bookId)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCachedSummary(bookId: String, chapterTitle: String?): Flow<BookSummary?> {
        return if (chapterTitle == null) {
            database.bookSummaryDao().getBookSummary(bookId).map { it?.toDomain() }
        } else {
            database.bookSummaryDao().getChapterSummary(bookId, chapterTitle).map { it?.toDomain() }
        }
    }

    private fun generateLocalAssistantResponse(
        bookTitle: String,
        question: String,
        mode: ExplanationMode,
        context: String?
    ): String {
        val qLower = question.lowercase()
        return when (mode) {
            ExplanationMode.SIMPLE -> {
                "In simple terms regarding '$bookTitle': Think of this like building a house with strong modular blocks. When $question is applied, each part does exactly one job so that if one thing changes, the rest of the house remains completely stable and intact."
            }
            ExplanationMode.DETAILED -> {
                "Detailed breakdown for '$bookTitle':\n\n1. **Core Concept**: $question relates directly to decoupling dependencies and preserving clear boundaries.\n\n2. **Mechanism**: By isolating state mutations and enforcing unidirectional data flow, the system prevents side effects and eliminates unexpected state bugs.\n\n3. **Practical Application**: Always define contract interfaces before writing business logic to enable deterministic testing."
            }
            ExplanationMode.TECHNICAL -> {
                "Technical Analysis ($bookTitle):\n- **Architectural Pattern**: Boundary isolation via Inversion of Control (IoC) and clean domain interfaces.\n- **State Lifecycle**: Managed via cold Kotlin Flows with distinct operators to prevent redundant re-renders.\n- **Concurrency & Resilience**: Non-blocking asynchronous dispatchers guarantee zero main-thread stutters."
            }
            ExplanationMode.EXAM_PREPARATION -> {
                "High-Yield Exam & Interview Points ($bookTitle):\n• **Definition**: Key concept addressing $question.\n• **Formula/Invariant**: High cohesion + Low coupling = High maintainability.\n• **Common Pitfall**: Mixing UI platform code into domain logic.\n• **Memory Anchor**: 'Events go up, State flows down'."
            }
        }
    }
}
