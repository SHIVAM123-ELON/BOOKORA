package com.example.domain.model.personalization

enum class ReadingGoalDuration(val minutesPerDay: Int, val label: String) {
    CASUAL(15, "15 min / day"),
    REGULAR(30, "30 min / day"),
    AVID(60, "1 hour / day"),
    SCHOLAR(120, "2+ hours / day")
}

data class UserPreferences(
    val userId: String,
    val selectedInterests: Set<String> = emptySet(),
    val favoriteCategories: Set<String> = emptySet(),
    val readingGoal: ReadingGoalDuration = ReadingGoalDuration.REGULAR,
    val preferredLanguages: List<String> = listOf("English", "Hindi"),
    val enablePersonalizedFeed: Boolean = true,
    val enableAiRecommendations: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val ALL_AVAILABLE_INTERESTS = listOf(
            "Technology",
            "Business",
            "Education",
            "Fiction",
            "Self Help",
            "Competitive Exams",
            "Programming",
            "Biography",
            "Finance",
            "Children",
            "Academic"
        )
    }
}
