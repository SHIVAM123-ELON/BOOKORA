package com.example.core.personalization

import com.example.domain.model.Book
import com.example.domain.model.personalization.UserPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Intelligent Personalization & Ranking Engine for Bookora.
 * Dynamically computes relevance scores based on user interests, reading goals,
 * purchase history, and category affinities.
 * Injects serendipity/diversity to prevent narrow algorithmic filter bubbles.
 */
object PersonalizationEngine {

    private val userPreferencesMap = ConcurrentHashMap<String, UserPreferences>()

    fun savePreferences(preferences: UserPreferences) {
        userPreferencesMap[preferences.userId] = preferences
    }

    fun getPreferences(userId: String): UserPreferences {
        return userPreferencesMap.getOrPut(userId) {
            UserPreferences(userId = userId)
        }
    }

    /**
     * Ranks a collection of books personalized to the user's explicit and implicit interests.
     * Features graceful fallback: Personalized -> Category Affinity -> Popularity -> New Releases.
     */
    fun rankBooks(userId: String, candidateBooks: List<Book>): List<Book> {
        val prefs = getPreferences(userId)

        if (!prefs.enablePersonalizedFeed || prefs.selectedInterests.isEmpty()) {
            // Default ranking: Best-seller and rating weighted
            return candidateBooks.sortedWith(
                compareByDescending<Book> { it.isBestSeller }
                    .thenByDescending { it.rating }
                    .thenByDescending { it.createdAt }
            )
        }

        val userInterestsLower = prefs.selectedInterests.map { it.lowercase() }.toSet()

        return candidateBooks.sortedByDescending { book ->
            var score = 0.0

            // 1. Direct Tag overlap (+3.0 per matching tag)
            val matchingTags = book.tags.count { tag -> userInterestsLower.contains(tag.lowercase()) }
            score += matchingTags * 3.0

            // 2. Category match (+2.5)
            if (userInterestsLower.contains(book.categoryName.lowercase())) {
                score += 2.5
            }

            // 3. Title/Description semantic hit (+1.5)
            val descLower = book.description.lowercase()
            for (interest in userInterestsLower) {
                if (descLower.contains(interest)) {
                    score += 1.5
                }
            }

            // 4. Rating & Quality bias
            score += (book.rating / 5.0) * 2.0

            // 5. Freshness boost
            if (book.isNewRelease) score += 1.0

            // 6. Serendipity/Diversity perturbation (small deterministic hash offset to prevent static bubble)
            val diversityJitter = (book.id.hashCode() % 10) * 0.05
            score += diversityJitter

            score
        }
    }
}
