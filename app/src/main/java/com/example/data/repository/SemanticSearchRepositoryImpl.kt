package com.example.data.repository

import com.example.data.local.BookoraDatabase
import com.example.domain.model.Book
import com.example.domain.model.BookFilter
import com.example.domain.model.BookSortOption
import com.example.domain.model.SemanticQueryAnalysis
import com.example.domain.model.SemanticSearchResult
import com.example.domain.repository.SemanticSearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SemanticSearchRepositoryImpl(
    private val database: BookoraDatabase
) : SemanticSearchRepository {

    override fun analyzeQuery(query: String): SemanticQueryAnalysis {
        val lower = query.lowercase().trim()

        val level = when {
            lower.contains("beginner") || lower.contains("starter") || lower.contains("intro") || lower.contains("basics") -> "Beginner"
            lower.contains("advanced") || lower.contains("expert") || lower.contains("deep dive") || lower.contains("mastery") -> "Advanced"
            lower.contains("intermediate") || lower.contains("production") -> "Intermediate"
            else -> null
        }

        val intent = when {
            lower.contains("learn") || lower.contains("how to") || lower.contains("guide") || lower.contains("tutorial") -> "Learning & Tutorials"
            lower.contains("architecture") || lower.contains("design") || lower.contains("scale") || lower.contains("patterns") -> "Architecture & Systems"
            lower.contains("best") || lower.contains("top") || lower.contains("recommended") -> "Top Recommendations"
            else -> "Catalog Exploration"
        }

        val stopWords = setOf("i", "want", "a", "an", "the", "book", "books", "for", "on", "about", "to", "in", "and", "or", "of", "with")
        val keywords = lower.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.length > 2 && !stopWords.contains(it) }

        val topic = keywords.firstOrNull() ?: query

        return SemanticQueryAnalysis(
            rawQuery = query,
            topic = topic,
            level = level,
            intent = intent,
            keywords = keywords
        )
    }

    override fun semanticSearch(query: String, filter: BookFilter): Flow<List<SemanticSearchResult>> {
        val analysis = analyzeQuery(query)

        return database.bookDao().getAllBooks().map { entities ->
            val books = entities.map { it.toDomain() }

            if (query.isBlank()) {
                return@map books.map {
                    SemanticSearchResult(
                        book = it,
                        relevanceScore = it.rating.toFloat(),
                        matchedReason = "Recommended title in ${it.categoryName}",
                        matchedConcepts = it.tags
                    )
                }
            }

            val scoredResults = books.mapNotNull { book ->
                // Apply category filter if specified
                if (filter.categoryId != null && book.categoryId != filter.categoryId) {
                    return@mapNotNull null
                }
                // Apply rating filter
                if (filter.minRating != null && book.rating < filter.minRating) {
                    return@mapNotNull null
                }
                // Apply language filter
                if (filter.language != null && !book.language.equals(filter.language, ignoreCase = true)) {
                    return@mapNotNull null
                }

                var score = 0f
                val matchedConcepts = mutableListOf<String>()
                val lowerTitle = book.title.lowercase()
                val lowerDesc = book.description.lowercase()
                val lowerAuthor = book.authorName.lowercase()
                val lowerCategory = book.categoryName.lowercase()
                val bookTags = book.tags.map { it.lowercase() }

                // Check exact keywords
                analysis.keywords.forEach { keyword ->
                    if (lowerTitle.contains(keyword)) {
                        score += 8.0f
                        matchedConcepts.add("Title match: $keyword")
                    }
                    if (lowerAuthor.contains(keyword)) {
                        score += 7.0f
                        matchedConcepts.add("Author: ${book.authorName}")
                    }
                    if (lowerCategory.contains(keyword)) {
                        score += 6.0f
                        matchedConcepts.add("Category: ${book.categoryName}")
                    }
                    if (bookTags.any { it.contains(keyword) }) {
                        score += 5.0f
                        matchedConcepts.add("Tag: $keyword")
                    }
                    if (lowerDesc.contains(keyword)) {
                        score += 3.0f
                        matchedConcepts.add("Content match: $keyword")
                    }
                }

                // Check level matching
                if (analysis.level != null) {
                    if (lowerTitle.contains(analysis.level.lowercase()) || lowerDesc.contains(analysis.level.lowercase()) || bookTags.contains(analysis.level.lowercase())) {
                        score += 4.0f
                        matchedConcepts.add("Level: ${analysis.level}")
                    }
                }

                // Boost by rating & popularity
                score += (book.rating.toFloat() * 0.5f)
                if (book.isTrending) score += 1.0f
                if (book.isBestSeller) score += 1.5f

                if (score > 1.0f) {
                    val reason = when {
                        matchedConcepts.isNotEmpty() -> matchedConcepts.take(2).joinToString(" • ")
                        else -> "Relevant to ${analysis.topic}"
                    }

                    SemanticSearchResult(
                        book = book,
                        relevanceScore = score,
                        matchedReason = reason,
                        matchedConcepts = matchedConcepts.distinct()
                    )
                } else {
                    null
                }
            }

            val sorted = when (filter.sortOption) {
                BookSortOption.POPULARITY -> scoredResults.sortedByDescending { it.book.reviewCount }
                BookSortOption.RATING -> scoredResults.sortedByDescending { it.book.rating }
                BookSortOption.NEWEST -> scoredResults.sortedByDescending { it.book.createdAt }
                BookSortOption.PRICE_LOW_TO_HIGH -> scoredResults.sortedBy { it.book.discountPrice ?: it.book.price }
                BookSortOption.PRICE_HIGH_TO_LOW -> scoredResults.sortedByDescending { it.book.discountPrice ?: it.book.price }
                else -> scoredResults.sortedByDescending { it.relevanceScore }
            }

            sorted
        }
    }
}
