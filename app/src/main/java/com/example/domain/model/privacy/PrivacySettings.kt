package com.example.domain.model.privacy

data class PrivacySettings(
    val userId: String,
    val analyticsOptOut: Boolean = false,
    val personalizedRecommendations: Boolean = true,
    val allowAiInteractionStorage: Boolean = true,
    val readingProgressSync: Boolean = true,
    val emailMarketingOptIn: Boolean = false,
    val pushNotificationsOptIn: Boolean = true,
    val shareReadingActivityWithFollowers: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DataExportPackage(
    val userId: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val accountInfo: Map<String, Any?>,
    val libraryBookIds: List<String>,
    val wishlistBookIds: List<String>,
    val orderHistorySummaries: List<Map<String, Any?>>,
    val reviewsCount: Int,
    val privacySettings: PrivacySettings
)
