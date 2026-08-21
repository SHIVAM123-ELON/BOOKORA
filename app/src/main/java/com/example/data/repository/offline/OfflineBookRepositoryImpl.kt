package com.example.data.repository.offline

import com.example.core.result.Resource
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.offline.CachedBookContentEntity
import com.example.data.local.entity.offline.CachedChapterEntity
import com.example.domain.model.offline.CachedBookContent
import com.example.domain.model.offline.CachedChapter
import com.example.domain.model.offline.DownloadStatus
import com.example.domain.model.offline.OfflineDownloadProgress
import com.example.domain.model.offline.OfflineStorageStats
import com.example.domain.repository.offline.OfflineBookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OfflineBookRepositoryImpl(
    private val db: BookoraDatabase
) : OfflineBookRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedSampleOfflineContentIfEmpty()
        }
    }

    override fun getCachedBook(bookId: String): Flow<CachedBookContent?> {
        return combine(
            db.bookContentCacheDao().getCachedBook(bookId),
            db.bookContentCacheDao().getCachedChapters(bookId)
        ) { bookEntity, chapters ->
            bookEntity?.toDomain(chapters.map { it.toDomain() })
        }.flowOn(Dispatchers.IO)
    }

    override fun getCachedChapters(bookId: String): Flow<List<CachedChapter>> {
        return db.bookContentCacheDao().getCachedChapters(bookId).map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun getCachedChapter(bookId: String, chapterIndex: Int): Flow<CachedChapter?> {
        return db.bookContentCacheDao().getCachedChapter(bookId, chapterIndex).map {
            it?.toDomain()
        }.flowOn(Dispatchers.IO)
    }

    override fun getAllCachedBooks(): Flow<List<CachedBookContent>> {
        return db.bookContentCacheDao().getAllCachedBooks().map { list ->
            list.map { it.toDomain() }
        }.flowOn(Dispatchers.IO)
    }

    override fun isBookAvailableOffline(bookId: String): Flow<Boolean> {
        return db.bookContentCacheDao().isBookAvailableOffline(bookId).flowOn(Dispatchers.IO)
    }

    override fun downloadBookForOffline(bookId: String): Flow<OfflineDownloadProgress> = flow {
        emit(
            OfflineDownloadProgress(
                bookId = bookId,
                progressPercent = 5f,
                status = DownloadStatus.DOWNLOADING,
                statusMessage = "Initiating download package from Bookora Content Delivery..."
            )
        )
        delay(250L)

        val bookEntity = withContext(Dispatchers.IO) {
            db.bookDao().getBookByIdDirect(bookId)
        }

        if (bookEntity == null) {
            emit(
                OfflineDownloadProgress(
                    bookId = bookId,
                    progressPercent = 0f,
                    status = DownloadStatus.FAILED,
                    statusMessage = "Book metadata could not be located."
                )
            )
            return@flow
        }

        emit(
            OfflineDownloadProgress(
                bookId = bookId,
                progressPercent = 30f,
                status = DownloadStatus.DOWNLOADING,
                statusMessage = "Downloading EPUB assets and text streams...",
                bytesDownloaded = 450_000L
            )
        )
        delay(300L)

        emit(
            OfflineDownloadProgress(
                bookId = bookId,
                progressPercent = 65f,
                status = DownloadStatus.PARSING_CHAPTERS,
                statusMessage = "Parsing table of contents, pagination, and formatting...",
                bytesDownloaded = 920_000L
            )
        )
        delay(300L)

        emit(
            OfflineDownloadProgress(
                bookId = bookId,
                progressPercent = 85f,
                status = DownloadStatus.CACHING_TO_ROOM,
                statusMessage = "Persisting chapters into encrypted Room database...",
                bytesDownloaded = 1_250_000L
            )
        )

        val generatedChapters = generateChaptersForBook(bookEntity.id, bookEntity.title, bookEntity.authorName, bookEntity.pageCount)
        val totalBytes = generatedChapters.sumOf { it.content.toByteArray().size.toLong() } + 85_000L

        val cachedBookEntity = CachedBookContentEntity(
            bookId = bookEntity.id,
            title = bookEntity.title,
            subtitle = bookEntity.subtitle,
            authorName = bookEntity.authorName,
            authorId = bookEntity.authorId,
            coverUrl = bookEntity.coverUrl,
            categoryName = bookEntity.categoryName,
            totalPages = bookEntity.pageCount,
            totalChapters = generatedChapters.size,
            fullContent = generatedChapters.joinToString("\n\n---\n\n") { "### ${it.chapterTitle}\n${it.content}" },
            synopsis = bookEntity.description,
            cachedAt = System.currentTimeMillis(),
            sizeBytes = totalBytes,
            isAvailableOffline = true
        )

        withContext(Dispatchers.IO) {
            db.bookContentCacheDao().insertCachedBook(cachedBookEntity)
            db.bookContentCacheDao().insertCachedChapters(generatedChapters)
            db.libraryDao().updateDownloadState(bookId, true)
        }
        delay(200L)

        emit(
            OfflineDownloadProgress(
                bookId = bookId,
                progressPercent = 100f,
                status = DownloadStatus.COMPLETED,
                statusMessage = "Book cached successfully! Ready for offline reading without internet.",
                totalChaptersCached = generatedChapters.size,
                bytesDownloaded = totalBytes
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun removeCachedBook(bookId: String): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.bookContentCacheDao().deleteEntireBookCache(bookId)
                db.libraryDao().updateDownloadState(bookId, false)
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to remove cached book", e)
            }
        }
    }

    override suspend fun clearAllOfflineCache(): Resource<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                db.bookContentCacheDao().clearEntireOfflineCache()
                Resource.Success(Unit)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to clear offline cache", e)
            }
        }
    }

    override fun getOfflineStorageStats(): Flow<OfflineStorageStats> {
        return combine(
            db.bookContentCacheDao().getOfflineBookCount(),
            db.bookContentCacheDao().getTotalCachedSizeBytes()
        ) { count, bytes ->
            OfflineStorageStats(
                totalCachedBooks = count,
                totalStorageBytes = bytes ?: 0L
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun seedSampleOfflineContentIfEmpty() {
        withContext(Dispatchers.IO) {
            val count = db.bookContentCacheDao().getCachedChaptersCount("b-clean-arch-001")
            if (count == 0) {
                // Seed Clean Architecture
                val cleanArchChapters = generateCleanArchitectureChapters()
                val cleanArchSizeBytes = cleanArchChapters.sumOf { it.content.toByteArray().size.toLong() } + 120_000L

                val cleanArchBook = CachedBookContentEntity(
                    bookId = "b-clean-arch-001",
                    title = "Clean Architecture",
                    subtitle = "A Craftsman's Guide to Software Structure and Design",
                    authorName = "Robert C. Martin",
                    authorId = "a-martin-001",
                    coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80",
                    categoryName = "Software Engineering",
                    totalPages = 352,
                    totalChapters = cleanArchChapters.size,
                    fullContent = cleanArchChapters.joinToString("\n\n---\n\n") { "### ${it.chapterTitle}\n${it.content}" },
                    synopsis = "Universal rules of software architecture to decouple domain logic from frameworks and databases.",
                    cachedAt = System.currentTimeMillis(),
                    sizeBytes = cleanArchSizeBytes,
                    isAvailableOffline = true
                )
                db.bookContentCacheDao().insertCachedBook(cleanArchBook)
                db.bookContentCacheDao().insertCachedChapters(cleanArchChapters)

                // Seed Frontier AI
                val frontierAiChapters = generateFrontierAiChapters()
                val frontierSizeBytes = frontierAiChapters.sumOf { it.content.toByteArray().size.toLong() } + 110_000L

                val frontierBook = CachedBookContentEntity(
                    bookId = "b-frontier-ai-002",
                    title = "Frontier AI & The Agentic Paradigm",
                    subtitle = "Architecting Multi-Agent Intelligence Systems",
                    authorName = "DeepMind & AI Research Group",
                    authorId = "a-deepmind-002",
                    coverUrl = "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600&auto=format&fit=crop&q=80",
                    categoryName = "Artificial Intelligence",
                    totalPages = 280,
                    totalChapters = frontierAiChapters.size,
                    fullContent = frontierAiChapters.joinToString("\n\n---\n\n") { "### ${it.chapterTitle}\n${it.content}" },
                    synopsis = "A deep exploration of reasoning models, tool-use execution loops, and autonomous cognitive software.",
                    cachedAt = System.currentTimeMillis(),
                    sizeBytes = frontierSizeBytes,
                    isAvailableOffline = true
                )
                db.bookContentCacheDao().insertCachedBook(frontierBook)
                db.bookContentCacheDao().insertCachedChapters(frontierAiChapters)
            }
        }
    }

    private fun generateCleanArchitectureChapters(): List<CachedChapterEntity> {
        val bookId = "b-clean-arch-001"
        return listOf(
            CachedChapterEntity(
                id = "${bookId}_ch_1",
                bookId = bookId,
                chapterIndex = 1,
                chapterTitle = "Chapter 1: What is Design and Architecture?",
                chapterSubtitle = "The Goal of Software Architecture",
                content = """
There has been a lot of confusion over the years about the difference between design and architecture. Is design small-scale and architecture big-scale? Or is architecture high-level and design low-level?

The answer is simple: There is no difference between them. None whatsoever.

The low-level details and the high-level architecture are part of the very same whole. They form an unbroken continuum of decisions that define the shape of the system. You cannot have one without the other; in fact, the line that separates them is simply an illusion.

The Goal:
The goal of software architecture is to minimize the human resources required to build and maintain the required system.

The measure of design quality is simply the measure of the effort required to satisfy the needs of the customer. If that effort is low and stays low throughout the lifetime of the system, the design is good. If that effort grows with each new release, the design is bad.

The Cost of Bad Code:
When engineering teams rush without architectural boundaries, productivity rapidly asymptotically approaches zero. Within a few releases, 100% of developer effort is spent patching regressional anomalies rather than shipping customer value.
                """.trimIndent(),
                startPage = 1,
                endPage = 25,
                estimatedReadingMinutes = 12,
                wordCount = 280
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_2",
                bookId = bookId,
                chapterIndex = 2,
                chapterTitle = "Chapter 2: A Tale of Two Values",
                chapterSubtitle = "Behavior versus Structure",
                content = """
Every software system provides two different values to the stakeholders: behavior and structure. Software developers are responsible for both, and some developers unfortunately place more focus on one over the other.

1. Behavior:
The first value of software is its behavior. Programmers are hired to make machines behave in ways that make or save money for the stakeholders. We do this by helping stakeholders write a functional specification, and then implementing the code that satisfies those requirements.

2. Structure (Architecture):
The second value of software is related to the word 'software' itself. The word is composed of 'ware', meaning 'product', and 'soft', meaning flexible and changeable. Software was invented to be 'soft'. It was intended to be easy to change the behavior of the machines.

If you give me a program that works perfectly but is impossible to change, then it will cease to work when requirements change, and it will be useless.
If you give me a program that does not work, but is easy to change, then I can make it work and keep it working as requirements evolve.
                """.trimIndent(),
                startPage = 26,
                endPage = 54,
                estimatedReadingMinutes = 14,
                wordCount = 310
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_3",
                bookId = bookId,
                chapterIndex = 3,
                chapterTitle = "Chapter 3: The Component Principles",
                chapterSubtitle = "Cohesion and Coupling across Boundaries",
                content = """
If the SOLID principles tell us how to arrange the bricks into walls and rooms, then the component principles tell us how to arrange the rooms into buildings.

Component Cohesion:
1. REP (Reuse/Release Equivalence Principle): The granule of reuse is the granule of release.
2. CCP (Common Closure Principle): Gather into components those classes that change for the same reasons and at the same times.
3. CRP (Common Reuse Principle): Do not force users of a component to depend on things they don't need.

The Component Coupling Principles:
• The Acyclic Dependencies Principle: Allow no cycles in the component dependency graph.
• The Stable Dependencies Principle: Depend in the direction of stability.
• The Stable Abstractions Principle: A component should be as abstract as it is stable.

By enforcing the Dependency Inversion Principle, source code dependencies always point inwards towards higher-level policies, isolated from SQL, REST, or UI presentation toolkits.
                """.trimIndent(),
                startPage = 55,
                endPage = 98,
                estimatedReadingMinutes = 18,
                wordCount = 350
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_4",
                bookId = bookId,
                chapterIndex = 4,
                chapterTitle = "Chapter 4: The Clean Architecture Core",
                chapterSubtitle = "Entities, Use Cases, and Interface Adapters",
                content = """
Over the last several decades, we have seen a whole range of architectural ideas: Hexagonal Architecture (Ports and Adapters), Onion Architecture, and Screaming Architecture. Though these architectures all vary somewhat in their details, they are very similar. They all produce systems that have the following characteristics:

1. Independent of Frameworks: The architecture does not depend on the existence of some library of feature-laden software.
2. Testable: The business rules can be tested without the UI, Database, Web Server, or any other external element.
3. Independent of UI: The UI can change easily without changing the rest of the system.
4. Independent of Database: You can swap out PostgreSQL for Mongo, CouchDB, or Bigtable. Your business rules are not bound to the database.
5. Independent of any external agency: In fact, your business rules simply don't know anything at all about the outside world.

The Dependency Rule:
The concentric circles represent different areas of software. In general, the further in you go, the higher level the software becomes. The outer circles are mechanisms. The inner circles are policies.
The overriding rule that makes this architecture work is The Dependency Rule: Source code dependencies must point only inward, toward higher-level policies.
                """.trimIndent(),
                startPage = 99,
                endPage = 145,
                estimatedReadingMinutes = 20,
                wordCount = 390
            )
        )
    }

    private fun generateFrontierAiChapters(): List<CachedChapterEntity> {
        val bookId = "b-frontier-ai-002"
        return listOf(
            CachedChapterEntity(
                id = "${bookId}_ch_1",
                bookId = bookId,
                chapterIndex = 1,
                chapterTitle = "Chapter 1: The Transition to Agentic Systems",
                chapterSubtitle = "From Static Inference to Autonomous Execution",
                content = """
The first decade of deep learning was defined by feed-forward pattern recognition: static classification, translation, and next-token prediction. However, true cognitive capability requires more than reflexive token emission. It requires iterative reasoning loops, environment state perception, tool utilization, and self-correcting error recovery.

The Anatomy of an Agent:
An agent is fundamentally a closed cybernetic loop:
1. Environment Observation: Ingesting multimodal sensory inputs, DOM trees, AST code graphs, or database schemas.
2. Deliberation & Thought: Expanding internal reasoning traces (Chain-of-Thought, Tree-of-Thought) to synthesize hypotheses.
3. Action Execution: Emitting structured tool calls (CLI scripts, API invocations, database mutations).
4. Feedback Ingestion: Receiving execution outputs and comparing against expected assertions.
                """.trimIndent(),
                startPage = 1,
                endPage = 30,
                estimatedReadingMinutes = 15,
                wordCount = 290
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_2",
                bookId = bookId,
                chapterIndex = 2,
                chapterTitle = "Chapter 2: Reasoning Models & Multimodal Streams",
                chapterSubtitle = "Architecting Real-Time Audio-Visual Loops",
                content = """
With the advent of frontier multimodal models such as Gemini 3.1 Flash and bidirectional streaming protocols (WebSockets / Bidi-Streams), conversational agents can now interact with ultra-low latency sub-300ms speech synthesis.

Key Design Tenets for Real-Time Voice:
• Chunk-Based PCM Streaming: Audio input is sampled at 16kHz or 24kHz and streamed in 100ms PCM chunks.
• Server-Turn Detection & Barge-In: The client audio visualizer dynamically pauses synthesized playback the microsecond user voice activity is detected.
• Parallel Tool Invocation: While the audio response is streaming, the agent asynchronously issues database queries and returns factual Grounding metadata.
                """.trimIndent(),
                startPage = 31,
                endPage = 68,
                estimatedReadingMinutes = 16,
                wordCount = 320
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_3",
                bookId = bookId,
                chapterIndex = 3,
                chapterTitle = "Chapter 3: Memory Architectures & Graph RAG",
                chapterSubtitle = "Hierarchical Knowledge Graphs for Autonomous Agents",
                content = """
Stateless inference is insufficient for long-running workflows. Frontier systems utilize multi-tier memory hierarchies:
1. Working Memory: Immediate context window holding active scratchpads and tool results.
2. Episodic Memory: Vector-embedded logs of past conversations and task solutions.
3. Semantic Memory: Knowledge graphs capturing entities, ontological relationships, and verified facts.

By combining local Room SQLite persistence with graph embeddings, agents can instantly retrieve grounded context even when operating completely offline.
                """.trimIndent(),
                startPage = 69,
                endPage = 112,
                estimatedReadingMinutes = 18,
                wordCount = 340
            )
        )
    }

    private fun generateChaptersForBook(bookId: String, title: String, author: String, totalPages: Int): List<CachedChapterEntity> {
        val safePages = if (totalPages > 0) totalPages else 120
        val pagesPerChapter = (safePages / 3).coerceAtLeast(10)

        return listOf(
            CachedChapterEntity(
                id = "${bookId}_ch_1",
                bookId = bookId,
                chapterIndex = 1,
                chapterTitle = "Chapter 1: Foundations & Core Concepts",
                chapterSubtitle = "An Introduction to $title",
                content = """
Welcome to Chapter 1 of '$title' by $author.

In this introductory chapter, we explore the essential principles that establish the foundation of our study.

Key Topics Covered:
• Fundamental definitions and historical context.
• Primary methodologies and analytical frameworks.
• Core theoretical underpinnings that inform practical implementation.

As we dive deeper throughout this work, each chapter builds upon these core tenets, equipping the reader with deep conceptual mastery and actionable insights.

"Knowledge in action is the only true measure of understanding."
                """.trimIndent(),
                startPage = 1,
                endPage = pagesPerChapter,
                estimatedReadingMinutes = 10,
                wordCount = 220
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_2",
                bookId = bookId,
                chapterIndex = 2,
                chapterTitle = "Chapter 2: Practical Applications & Strategy",
                chapterSubtitle = "Applying Theory into Real-World Practice",
                content = """
Chapter 2: Deep Dive into Practical Execution.

Bridging the gap between conceptual models and actual execution requires a disciplined approach.

Critical Implementation Pillars:
1. Structured Strategy: Defining clear constraints, metrics, and desired outcomes.
2. Iterative Feedback: Validating assumptions continuously against tangible metrics.
3. Resilience & Scale: Engineering workflows that endure unpredictability and change.

Case studies demonstrate that consistent application of these practices delivers sustainable long-term success.
                """.trimIndent(),
                startPage = pagesPerChapter + 1,
                endPage = pagesPerChapter * 2,
                estimatedReadingMinutes = 12,
                wordCount = 250
            ),
            CachedChapterEntity(
                id = "${bookId}_ch_3",
                bookId = bookId,
                chapterIndex = 3,
                chapterTitle = "Chapter 3: Advanced Frontiers & Future Trajectories",
                chapterSubtitle = "Mastering the Horizon",
                content = """
Chapter 3: The Next Level of Mastery.

In this concluding section, we examine advanced strategies and speculative horizons.

Key Takeaways:
• Synthesis of foundational knowledge and operational execution.
• Anticipating shifts in paradigms, standards, and methodologies.
• Cultivating continuous mastery and innovation.

Thank you for reading '$title'. Continue applying these lessons to craft lasting impact.
                """.trimIndent(),
                startPage = (pagesPerChapter * 2) + 1,
                endPage = safePages,
                estimatedReadingMinutes = 15,
                wordCount = 260
            )
        )
    }
}
