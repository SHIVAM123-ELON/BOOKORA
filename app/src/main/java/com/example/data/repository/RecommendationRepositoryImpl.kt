package com.example.data.repository

import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.RecommendationEventEntity
import com.example.domain.model.Book
import com.example.domain.model.BookRecommendation
import com.example.domain.model.RecommendationEvent
import com.example.domain.model.RecommendationEventType
import com.example.domain.model.RecommendationType
import com.example.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RecommendationRepositoryImpl(
    private val database: BookoraDatabase
) : RecommendationRepository {

    override suspend fun recordEvent(event: RecommendationEvent) {
        database.recommendationEventDao().insertEvent(RecommendationEventEntity.fromDomain(event))
    }

    override fun getPersonalizedRecommendations(userId: String, limit: Int): Flow<List<BookRecommendation>> {
        return combine(
            database.bookDao().getAllBooks(),
            database.recommendationEventDao().getRecentEventsForUser(userId),
            database.libraryDao().getAllLibraryItems(),
            database.wishlistDao().getUserWishlist(userId)
        ) { books, events, libraryItems, wishlistItems ->
            val domainBooks = books.map { it.toDomain() }
            val readBookIds = libraryItems.map { it.bookId }.toSet()
            val wishlistedBookIds = wishlistItems.map { it.bookId }.toSet()

            // Find categories user engaged with most
            val categoryAffinity = mutableMapOf<String, Int>()
            val authorAffinity = mutableMapOf<String, Int>()

            events.forEach { event ->
                val book = domainBooks.firstOrNull { it.id == event.bookId }
                if (book != null) {
                    val weight = when (event.eventType) {
                        "PURCHASE" -> 5
                        "READ" -> 4
                        "COMPLETE" -> 5
                        "RATE" -> 4
                        "WISHLIST" -> 3
                        "VIEW" -> 1
                        "SEARCH" -> 2
                        else -> 1
                    }
                    categoryAffinity[book.categoryId] = (categoryAffinity[book.categoryId] ?: 0) + weight
                    authorAffinity[book.authorId] = (authorAffinity[book.authorId] ?: 0) + weight
                }
            }

            val recommendations = mutableListOf<BookRecommendation>()

            // If user has read books, recommend from top category
            val topCategory = categoryAffinity.maxByOrNull { it.value }?.key
            val topAuthor = authorAffinity.maxByOrNull { it.value }?.key

            val lastReadBookId = events.firstOrNull { it.eventType == "READ" || it.eventType == "PURCHASE" }?.bookId
            val lastReadBook = domainBooks.firstOrNull { it.id == lastReadBookId }

            domainBooks.forEach { book ->
                if (!readBookIds.contains(book.id)) {
                    var score = book.rating.toFloat()
                    var reason: String? = null
                    var type = RecommendationType.RECOMMENDED_FOR_YOU

                    if (lastReadBook != null && book.categoryId == lastReadBook.categoryId) {
                        score += 5.0f
                        reason = "Because you read ${lastReadBook.title}"
                        type = RecommendationType.BECAUSE_YOU_READ
                    } else if (wishlistedBookIds.contains(book.id)) {
                        score += 4.0f
                        reason = "From your reading wishlist"
                        type = RecommendationType.SIMILAR_BOOKS
                    } else if (topCategory != null && book.categoryId == topCategory) {
                        score += 3.0f
                        reason = "Matches your interest in ${book.categoryName}"
                        type = RecommendationType.POPULAR_IN_INTERESTS
                    } else if (topAuthor != null && book.authorId == topAuthor) {
                        score += 3.5f
                        reason = "More from author ${book.authorName}"
                        type = RecommendationType.SIMILAR_BOOKS
                    } else if (book.isTrending || book.isBestSeller) {
                        reason = "Trending across Bookora readers"
                        type = RecommendationType.GENERAL_POPULAR
                    }

                    if (reason != null) {
                        recommendations.add(
                            BookRecommendation(
                                book = book,
                                type = type,
                                reason = reason,
                                score = score
                            )
                        )
                    }
                }
            }

            // If list is empty (cold start), add best rated and trending books with clear fallback reasons
            if (recommendations.isEmpty()) {
                domainBooks.sortedByDescending { it.rating }.take(limit).forEach { book ->
                    recommendations.add(
                        BookRecommendation(
                            book = book,
                            type = RecommendationType.GENERAL_POPULAR,
                            reason = "Top rated in ${book.categoryName}",
                            score = book.rating.toFloat()
                        )
                    )
                }
            }

            recommendations.sortedByDescending { it.score }.take(limit)
        }
    }

    override fun getBecauseYouReadRecommendations(userId: String): Flow<List<BookRecommendation>> {
        return getPersonalizedRecommendations(userId).map { list ->
            list.filter { it.type == RecommendationType.BECAUSE_YOU_READ }
        }
    }

    override fun getSimilarToWishlistRecommendations(userId: String): Flow<List<BookRecommendation>> {
        return getPersonalizedRecommendations(userId).map { list ->
            list.filter { it.type == RecommendationType.SIMILAR_BOOKS }
        }
    }

    override fun getContinueLearningRecommendations(userId: String): Flow<List<BookRecommendation>> {
        return getPersonalizedRecommendations(userId).map { list ->
            list.filter { it.type == RecommendationType.CONTINUE_LEARNING || it.type == RecommendationType.POPULAR_IN_INTERESTS }
        }
    }

    override fun getPopularInInterestsRecommendations(userId: String): Flow<List<BookRecommendation>> {
        return getPersonalizedRecommendations(userId).map { list ->
            list.filter { it.type == RecommendationType.POPULAR_IN_INTERESTS }
        }
    }
}
