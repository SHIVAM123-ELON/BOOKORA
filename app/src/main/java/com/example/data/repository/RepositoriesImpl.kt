package com.example.data.repository

import com.example.core.result.Resource
import com.example.core.storage.TokenManager
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.AuthorEntity
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.LibraryEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WishlistEntity
import com.example.domain.model.Author
import com.example.domain.model.Book
import com.example.domain.model.BookFilter
import com.example.domain.model.BookSortOption
import com.example.domain.model.Category
import com.example.domain.model.LibraryItem
import com.example.domain.model.ReadingProgress
import com.example.domain.model.ReadingStatus
import com.example.domain.model.RecentSearch
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.AuthorRepository
import com.example.domain.repository.BookRepository
import com.example.domain.repository.CategoryRepository
import com.example.domain.repository.LibraryRepository
import com.example.domain.repository.SearchRepository
import com.example.domain.repository.WishlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class AuthRepositoryImpl(
    private val db: BookoraDatabase,
    private val tokenManager: TokenManager
) : AuthRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = db.userDao().getCurrentUser().first()
            if (existing == null) {
                val user = UserEntity(
                    id = "u-default-reader-001",
                    email = "alex.mercer@bookora.com",
                    fullName = "Alex Mercer",
                    role = "READER",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                    isVerified = true
                )
                db.userDao().insertUser(user)
                tokenManager.saveTokens("dev_token_001", "dev_refresh_001", user.id, user.role)
            }
        }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val role = if (email.contains("admin", ignoreCase = true)) {
                UserRole.ADMIN
            } else if (email.contains("author", ignoreCase = true)) {
                UserRole.AUTHOR
            } else {
                UserRole.READER
            }

            val user = User(
                id = "u-user-${System.currentTimeMillis()}",
                email = email,
                fullName = email.substringBefore("@").replace(".", " ").split(" ")
                    .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                role = role,
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                isVerified = true
            )

            db.userDao().insertUser(
                UserEntity(
                    id = user.id,
                    email = user.email,
                    fullName = user.fullName,
                    role = user.role.name,
                    avatarUrl = user.avatarUrl,
                    isVerified = user.isVerified
                )
            )

            tokenManager.saveTokens("token_${user.id}", "refresh_${user.id}", user.id, user.role.name)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Authentication failed", e)
        }
    }

    override suspend fun register(fullName: String, email: String, password: String): Resource<User> {
        return try {
            val user = User(
                id = "u-user-${System.currentTimeMillis()}",
                email = email,
                fullName = fullName,
                role = UserRole.READER,
                avatarUrl = null,
                isVerified = true
            )

            db.userDao().insertUser(
                UserEntity(
                    id = user.id,
                    email = user.email,
                    fullName = user.fullName,
                    role = user.role.name,
                    avatarUrl = user.avatarUrl,
                    isVerified = user.isVerified
                )
            )

            tokenManager.saveTokens("token_${user.id}", "refresh_${user.id}", user.id, user.role.name)
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed", e)
        }
    }

    override fun getCurrentUser(): Flow<User?> {
        return db.userDao().getCurrentUser().map { it?.toDomain() }
    }

    override fun isUserLoggedIn(): Flow<Boolean> {
        return tokenManager.accessToken.map { !it.isNullOrBlank() }
    }

    override suspend fun logout(): Resource<Unit> {
        tokenManager.clearTokens()
        db.userDao().clearUsers()
        return Resource.Success(Unit)
    }
}

class BookRepositoryImpl(
    private val db: BookoraDatabase
) : BookRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        val sampleBooks = listOf(
            BookEntity(
                id = "b-clean-arch-001",
                title = "Clean Architecture",
                subtitle = "A Craftsman's Guide to Software Structure and Design",
                authorId = "a-martin-001",
                authorName = "Robert C. Martin",
                description = "By applying universal rules of software architecture, you can dramatically improve developer productivity throughout the life of any software system. Uncle Bob presents timeless rules for component design, decoupled boundaries, and testable domain logic.",
                coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3edd4?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/clean_architecture_sample.epub",
                previewUrl = "https://example.com/preview1.pdf",
                categoryId = "c-1",
                categoryName = "Software Engineering",
                language = "English",
                price = 499.0,
                discountPrice = 399.0,
                rating = 4.85,
                reviewCount = 342,
                pageCount = 352,
                publicationDate = "2017-09-20",
                isbn = "978-0134494166",
                tags = "Architecture, Kotlin, Clean Code, Design Patterns, SOLID",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewRelease = false,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-frontier-ai-002",
                title = "Frontier AI & The Agentic Paradigm",
                subtitle = "Architecting Multi-Agent Intelligence Systems",
                authorId = "a-deepmind-002",
                authorName = "DeepMind & AI Research Group",
                description = "A deep exploration of reasoning models, tool-use execution loops, chain-of-thought distillation, and the transition from static LLMs to autonomous cognitive software systems.",
                coverUrl = "https://images.unsplash.com/photo-1620712943543-bcc4688e7485?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/frontier_ai_sample.epub",
                previewUrl = "https://example.com/preview2.pdf",
                categoryId = "c-2",
                categoryName = "Artificial Intelligence",
                language = "English",
                price = 799.0,
                discountPrice = 599.0,
                rating = 4.92,
                reviewCount = 189,
                pageCount = 280,
                publicationDate = "2024-05-12",
                isbn = "978-1954123456",
                tags = "AI, Agents, Machine Learning, Deep Learning, LLM",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewRelease = true,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-atomic-habits-003",
                title = "Atomic Habits",
                subtitle = "An Easy & Proven Way to Build Good Habits & Break Bad Ones",
                authorId = "a-clear-003",
                authorName = "James Clear",
                description = "No matter your goals, Atomic Habits offers a proven framework for improving every day. James Clear reveals practical strategies that teach you exactly how to form good habits, break bad ones, and master tiny behaviors.",
                coverUrl = "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/atomic_habits_sample.epub",
                previewUrl = "https://example.com/preview3.pdf",
                categoryId = "c-4",
                categoryName = "Self Improvement",
                language = "English",
                price = 399.0,
                discountPrice = 299.0,
                rating = 4.90,
                reviewCount = 810,
                pageCount = 320,
                publicationDate = "2018-10-16",
                isbn = "978-0735211292",
                tags = "Habits, Productivity, Psychology, Self Help",
                isFeatured = true,
                isTrending = true,
                isBestSeller = true,
                isNewRelease = false,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-design-systems-004",
                title = "Design Systems & Visual Typography",
                subtitle = "Crafting Scalable Digital User Interfaces",
                authorId = "a-rostova-004",
                authorName = "Elena Rostova",
                description = "Master modern design systems, token architecture, harmonic typography scales, and micro-interactions for modern web and mobile applications with Material 3 principles.",
                coverUrl = "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/design_systems_sample.epub",
                previewUrl = "https://example.com/preview4.pdf",
                categoryId = "c-5",
                categoryName = "Design & UI/UX",
                language = "English",
                price = 549.0,
                discountPrice = 399.0,
                rating = 4.78,
                reviewCount = 94,
                pageCount = 240,
                publicationDate = "2023-11-01",
                isbn = "978-0321987654",
                tags = "Design, UI, UX, Typography, Material Design",
                isFeatured = false,
                isTrending = true,
                isBestSeller = false,
                isNewRelease = true,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-zero-to-one-005",
                title = "Zero to One",
                subtitle = "Notes on Startups, or How to Build the Future",
                authorId = "a-thiel-005",
                authorName = "Peter Thiel",
                description = "The great secret of our time is that there are still uncharted frontiers to explore and new inventions to create. In Zero to One, legendary entrepreneur and investor Peter Thiel shows how we can find singular ways to create those new things.",
                coverUrl = "https://images.unsplash.com/photo-1497633762265-9d179a990aa6?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/zero_to_one_sample.epub",
                previewUrl = "https://example.com/preview5.pdf",
                categoryId = "c-3",
                categoryName = "Startup & Business",
                language = "English",
                price = 450.0,
                discountPrice = 350.0,
                rating = 4.75,
                reviewCount = 520,
                pageCount = 224,
                publicationDate = "2014-09-16",
                isbn = "978-0804139298",
                tags = "Startups, Business, Innovation, Economics",
                isFeatured = false,
                isTrending = true,
                isBestSeller = true,
                isNewRelease = false,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-three-body-problem-006",
                title = "The Three-Body Problem",
                subtitle = "Remembrance of Earth's Past",
                authorId = "a-cixin-006",
                authorName = "Cixin Liu",
                description = "Set against the backdrop of China's Cultural Revolution, a secret military project sends signals into space to establish contact with aliens. An alien civilization on the brink of destruction captures the signal and plans to invade Earth.",
                coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/three_body_sample.epub",
                previewUrl = "https://example.com/preview6.pdf",
                categoryId = "c-6",
                categoryName = "Sci-Fi & Fiction",
                language = "English",
                price = 399.0,
                discountPrice = 279.0,
                rating = 4.88,
                reviewCount = 612,
                pageCount = 400,
                publicationDate = "2014-11-11",
                isbn = "978-0765377067",
                tags = "Sci-Fi, Fiction, Space, Physics, Hugo Award",
                isFeatured = false,
                isTrending = true,
                isBestSeller = true,
                isNewRelease = false,
                status = "PUBLISHED"
            ),
            BookEntity(
                id = "b-kotlin-coroutines-007",
                title = "Kotlin Coroutines: Deep Dive",
                subtitle = "Mastering Asynchronous Flow & Concurrency",
                authorId = "a-martin-001",
                authorName = "Robert C. Martin",
                description = "Comprehensive guide to mastering coroutines, channels, shared flows, structured concurrency, and reactive streams in production Android applications.",
                coverUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=600&auto=format&fit=crop&q=80",
                fileUrl = "samples/coroutines_sample.epub",
                previewUrl = "https://example.com/preview7.pdf",
                categoryId = "c-1",
                categoryName = "Software Engineering",
                language = "English",
                price = 599.0,
                discountPrice = 449.0,
                rating = 4.91,
                reviewCount = 145,
                pageCount = 310,
                publicationDate = "2024-02-18",
                isbn = "978-1804617890",
                tags = "Kotlin, Coroutines, Flow, Android, Reactive",
                isFeatured = true,
                isTrending = true,
                isBestSeller = false,
                isNewRelease = true,
                status = "PUBLISHED"
            )
        )
        db.bookDao().insertBooks(sampleBooks)
    }

    override fun getFeaturedBooks(): Flow<List<Book>> {
        return db.bookDao().getFeaturedBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getTrendingBooks(): Flow<List<Book>> {
        return db.bookDao().getTrendingBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getBestSellers(): Flow<List<Book>> {
        return db.bookDao().getBestSellerBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getNewReleases(): Flow<List<Book>> {
        return db.bookDao().getNewReleaseBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getRecommendedBooks(): Flow<List<Book>> {
        return db.bookDao().getAllBooks().map { list -> list.filter { it.rating >= 4.8 }.map { it.toDomain() } }
    }

    override fun getDeals(): Flow<List<Book>> {
        return db.bookDao().getDealBooks().map { list -> list.map { it.toDomain() } }
    }

    override fun getBookById(id: String): Flow<Book?> {
        return db.bookDao().getBookById(id).map { it?.toDomain() }
    }

    override fun getBooksByAuthor(authorId: String, excludeBookId: String): Flow<List<Book>> {
        return db.bookDao().getBooksByAuthor(authorId, excludeBookId).map { list -> list.map { it.toDomain() } }
    }

    override fun getSimilarBooks(categoryId: String, excludeBookId: String): Flow<List<Book>> {
        return db.bookDao().getSimilarBooks(categoryId, excludeBookId).map { list -> list.map { it.toDomain() } }
    }

    override fun getBooksByCategory(categoryId: String): Flow<List<Book>> {
        return db.bookDao().getAllBooks().map { list ->
            list.filter { it.categoryId == categoryId }.map { it.toDomain() }
        }
    }

    override suspend fun refreshBooks(): Resource<Unit> {
        seedInitialData()
        return Resource.Success(Unit)
    }
}

class CategoryRepositoryImpl(
    private val db: BookoraDatabase
) : CategoryRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedCategories()
        }
    }

    private suspend fun seedCategories() {
        val sampleCategories = listOf(
            CategoryEntity("c-1", "Software Engineering", "software-engineering", "Clean code, architecture & systems", "code", 24),
            CategoryEntity("c-2", "Artificial Intelligence", "artificial-intelligence", "LLMs, reasoning models & machine learning", "psychology", 18),
            CategoryEntity("c-3", "Startup & Business", "startup-business", "Scaling products, strategy & venture", "trending_up", 19),
            CategoryEntity("c-4", "Self Improvement", "self-improvement", "Habits, mental models & focus", "self_improvement", 32),
            CategoryEntity("c-5", "Design & UI/UX", "design-ui-ux", "Design systems, typography & interfaces", "palette", 15),
            CategoryEntity("c-6", "Sci-Fi & Fiction", "sci-fi-fiction", "Speculative futures & novels", "auto_stories", 28)
        )
        db.categoryDao().insertCategories(sampleCategories)
    }

    override fun getCategories(): Flow<List<Category>> {
        return db.categoryDao().getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshCategories(): Resource<Unit> {
        seedCategories()
        return Resource.Success(Unit)
    }
}

class AuthorRepositoryImpl(
    private val db: BookoraDatabase
) : AuthorRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedAuthors()
        }
    }

    private suspend fun seedAuthors() {
        val sampleAuthors = listOf(
            AuthorEntity(
                id = "a-martin-001",
                penName = "Robert C. Martin",
                bio = "Uncle Bob is a legendary software craftsman, author of Clean Code and Clean Architecture.",
                avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 8,
                rating = 4.9,
                followersCount = 45200
            ),
            AuthorEntity(
                id = "a-deepmind-002",
                penName = "DeepMind Research Group",
                bio = "Pioneers of artificial intelligence, transformer reasoning, and cognitive computing agents.",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 5,
                rating = 4.95,
                followersCount = 89000
            ),
            AuthorEntity(
                id = "a-clear-003",
                penName = "James Clear",
                bio = "Writer and speaker focused on habits, decision-making, and continuous self-improvement.",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 3,
                rating = 4.92,
                followersCount = 68400
            ),
            AuthorEntity(
                id = "a-rostova-004",
                penName = "Elena Rostova",
                bio = "Principal Design Architect specializing in typography, micro-interactions, and Material 3 design systems.",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 4,
                rating = 4.82,
                followersCount = 14200
            ),
            AuthorEntity(
                id = "a-thiel-005",
                penName = "Peter Thiel",
                bio = "Technology entrepreneur, venture capitalist, and co-founder of PayPal and Founders Fund.",
                avatarUrl = "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 2,
                rating = 4.75,
                followersCount = 38900
            ),
            AuthorEntity(
                id = "a-cixin-006",
                penName = "Cixin Liu",
                bio = "Leading Chinese science fiction author and Hugo Award winner.",
                avatarUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=200&auto=format&fit=crop&q=80",
                isVerified = true,
                totalBooks = 6,
                rating = 4.89,
                followersCount = 52100
            )
        )
        db.authorDao().insertAuthors(sampleAuthors)
    }

    override fun getPopularAuthors(): Flow<List<Author>> {
        return db.authorDao().getAllAuthors().map { list -> list.map { it.toDomain() } }
    }

    override fun getAuthorById(authorId: String): Flow<Author?> {
        return db.authorDao().getAuthorById(authorId).map { it?.toDomain() }
    }

    override suspend fun refreshAuthors(): Resource<Unit> {
        seedAuthors()
        return Resource.Success(Unit)
    }
}

class SearchRepositoryImpl(
    private val db: BookoraDatabase
) : SearchRepository {

    override fun searchBooks(query: String, filter: BookFilter): Flow<List<Book>> {
        val rawFlow = if (query.isBlank()) {
            db.bookDao().getAllBooks()
        } else {
            db.bookDao().searchBooks(query.trim())
        }

        return rawFlow.map { entityList ->
            var books = entityList.map { it.toDomain() }

            // Category filter
            if (filter.categoryId != null) {
                books = books.filter { it.categoryId == filter.categoryId }
            }

            // Language filter
            if (filter.language != null) {
                books = books.filter { it.language.equals(filter.language, ignoreCase = true) }
            }

            // Min Rating filter
            if (filter.minRating != null) {
                books = books.filter { it.rating >= filter.minRating }
            }

            // Max Price filter
            if (filter.maxPrice != null) {
                books = books.filter { it.price <= filter.maxPrice }
            }

            // Sorting
            when (filter.sortOption) {
                BookSortOption.RELEVANCE -> books
                BookSortOption.POPULARITY -> books.sortedByDescending { it.reviewCount }
                BookSortOption.NEWEST -> books.sortedByDescending { it.createdAt }
                BookSortOption.RATING -> books.sortedByDescending { it.rating }
                BookSortOption.PRICE_LOW_TO_HIGH -> books.sortedBy { it.discountPrice ?: it.price }
                BookSortOption.PRICE_HIGH_TO_LOW -> books.sortedByDescending { it.discountPrice ?: it.price }
            }
        }.flowOn(Dispatchers.IO)
    }

    override fun getRecentSearches(): Flow<List<RecentSearch>> {
        return db.recentSearchDao().getRecentSearches().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveRecentSearch(query: String) {
        if (query.isNotBlank()) {
            db.recentSearchDao().insertRecentSearch(
                RecentSearchEntity(
                    id = "search-${UUID.randomUUID()}",
                    query = query.trim(),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun deleteRecentSearch(query: String) {
        db.recentSearchDao().deleteRecentSearch(query)
    }

    override suspend fun clearRecentSearches() {
        db.recentSearchDao().clearRecentSearches()
    }
}

class WishlistRepositoryImpl(
    private val db: BookoraDatabase,
    private val authRepository: AuthRepository
) : WishlistRepository {

    private suspend fun getUserId(): String {
        return authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
    }

    override fun getWishlistBooks(): Flow<List<Book>> {
        return combine(
            authRepository.getCurrentUser(),
            db.bookDao().getAllBooks()
        ) { user, books ->
            val userId = user?.id ?: "u-default-reader-001"
            userId to books
        }.combine(db.wishlistDao().getUserWishlist("u-default-reader-001")) { (userId, books), wishlistItems ->
            val bookMap = books.associateBy { it.id }
            wishlistItems.mapNotNull { wish ->
                bookMap[wish.bookId]?.toDomain()
            }
        }.distinctUntilChanged()
    }

    override fun isInWishlist(bookId: String): Flow<Boolean> {
        return authRepository.getCurrentUser().map { user ->
            user?.id ?: "u-default-reader-001"
        }.combine(db.wishlistDao().getUserWishlist("u-default-reader-001")) { _, items ->
            items.any { it.bookId == bookId }
        }
    }

    override suspend fun addToWishlist(bookId: String): Resource<Unit> {
        return try {
            val userId = getUserId()
            db.wishlistDao().addToWishlist(
                WishlistEntity(
                    id = "wish-${UUID.randomUUID()}",
                    userId = userId,
                    bookId = bookId,
                    addedAt = System.currentTimeMillis()
                )
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add to wishlist", e)
        }
    }

    override suspend fun removeFromWishlist(bookId: String): Resource<Unit> {
        return try {
            val userId = getUserId()
            db.wishlistDao().removeFromWishlist(userId, bookId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to remove from wishlist", e)
        }
    }

    override suspend fun toggleWishlist(bookId: String): Resource<Boolean> {
        return try {
            val userId = getUserId()
            val existing = db.wishlistDao().getUserWishlist(userId).first()
            val isInList = existing.any { it.bookId == bookId }
            if (isInList) {
                db.wishlistDao().removeFromWishlist(userId, bookId)
                Resource.Success(false)
            } else {
                db.wishlistDao().addToWishlist(
                    WishlistEntity(
                        id = "wish-${UUID.randomUUID()}",
                        userId = userId,
                        bookId = bookId,
                        addedAt = System.currentTimeMillis()
                    )
                )
                Resource.Success(true)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to toggle wishlist", e)
        }
    }
}

class LibraryRepositoryImpl(
    private val db: BookoraDatabase,
    private val authRepository: AuthRepository
) : LibraryRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialLibrary()
        }
    }

    private suspend fun seedInitialLibrary() {
        val existing = db.libraryDao().getAllLibraryItems().first()
        if (existing.isEmpty()) {
            val item1 = LibraryEntity(
                id = "lib-001",
                userId = "u-default-reader-001",
                bookId = "b-clean-arch-001",
                readingProgress = 42.5f,
                lastReadPage = 148,
                status = "IN_PROGRESS",
                isDownloaded = true,
                purchasedAt = System.currentTimeMillis() - 86400000 * 3
            )
            val item2 = LibraryEntity(
                id = "lib-002",
                userId = "u-default-reader-001",
                bookId = "b-frontier-ai-002",
                readingProgress = 15.0f,
                lastReadPage = 42,
                status = "IN_PROGRESS",
                isDownloaded = false,
                purchasedAt = System.currentTimeMillis() - 86400000 * 7
            )
            db.libraryDao().insertLibraryItem(item1)
            db.libraryDao().insertLibraryItem(item2)

            // Also seed reading progress records
            db.readingProgressDao().saveReadingProgress(
                ReadingProgressEntity(
                    userId = "u-default-reader-001",
                    bookId = "b-clean-arch-001",
                    currentPage = 148,
                    totalPages = 352,
                    percentage = 42.5f,
                    lastOpenedAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun getUserId(): String {
        return authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"
    }

    override fun getUserLibrary(): Flow<List<LibraryItem>> {
        return combine(
            db.libraryDao().getAllLibraryItems(),
            db.bookDao().getAllBooks()
        ) { libItems, books ->
            val bookMap = books.associateBy { it.id }
            libItems.mapNotNull { lib ->
                val bookEntity = bookMap[lib.bookId] ?: return@mapNotNull null
                LibraryItem(
                    id = lib.id,
                    userId = lib.userId,
                    book = bookEntity.toDomain(),
                    readingProgress = lib.readingProgress,
                    lastReadPage = lib.lastReadPage,
                    status = try { ReadingStatus.valueOf(lib.status) } catch (e: Exception) { ReadingStatus.IN_PROGRESS },
                    isDownloaded = lib.isDownloaded,
                    purchasedAt = lib.purchasedAt
                )
            }
        }
    }

    override fun isBookEntitled(bookId: String): Flow<Boolean> {
        return db.libraryDao().getLibraryItem(bookId).map { it != null }
    }

    override suspend fun addBookToLibrary(bookId: String): Resource<Unit> {
        return try {
            val userId = getUserId()
            val item = LibraryEntity(
                id = "lib-${UUID.randomUUID()}",
                userId = userId,
                bookId = bookId,
                readingProgress = 0f,
                lastReadPage = 1,
                status = "NOT_STARTED",
                isDownloaded = false,
                purchasedAt = System.currentTimeMillis()
            )
            db.libraryDao().insertLibraryItem(item)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add book to library", e)
        }
    }

    override fun getReadingProgress(bookId: String): Flow<ReadingProgress?> {
        return authRepository.getCurrentUser().map { user ->
            user?.id ?: "u-default-reader-001"
        }.combine(db.readingProgressDao().getReadingProgress("u-default-reader-001", bookId)) { _, progress ->
            progress?.toDomain()
        }
    }

    override suspend fun saveProgress(bookId: String, page: Int, totalPages: Int): Resource<Unit> {
        return try {
            val userId = getUserId()
            val safeTotal = if (totalPages > 0) totalPages else 100
            val percentage = (page.toFloat() / safeTotal.toFloat()) * 100f
            val status = if (percentage >= 99f) "COMPLETED" else "IN_PROGRESS"

            db.readingProgressDao().saveReadingProgress(
                ReadingProgressEntity(
                    userId = userId,
                    bookId = bookId,
                    currentPage = page,
                    totalPages = safeTotal,
                    percentage = percentage,
                    lastOpenedAt = System.currentTimeMillis()
                )
            )

            db.libraryDao().updateProgress(bookId, page, percentage, status)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to save reading progress", e)
        }
    }

    override suspend fun syncProgress(): Resource<Unit> {
        // Backend synchronization stub ready for remote API endpoint
        return Resource.Success(Unit)
    }

    override suspend fun toggleDownload(bookId: String, isDownloaded: Boolean): Resource<Unit> {
        return try {
            db.libraryDao().updateDownloadState(bookId, isDownloaded)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update download state", e)
        }
    }
}
