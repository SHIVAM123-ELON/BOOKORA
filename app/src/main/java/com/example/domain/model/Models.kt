package com.example.domain.model

enum class UserRole {
    READER,
    AUTHOR,
    PUBLISHER,
    MODERATOR,
    ADMIN,
    SUPER_ADMIN
}

enum class BookStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    PUBLISHED,
    REJECTED,
    ARCHIVED
}

enum class ReadingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

data class User(
    val id: String,
    val email: String,
    val fullName: String,
    val role: UserRole,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false
)

data class Category(
    val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val iconName: String,
    val bookCount: Int = 0
)

data class Author(
    val id: String,
    val penName: String,
    val bio: String,
    val avatarUrl: String? = null,
    val isVerified: Boolean = true,
    val totalBooks: Int = 0,
    val rating: Double = 4.8,
    val followersCount: Int = 1200
)

data class Book(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val authorId: String,
    val authorName: String,
    val description: String,
    val coverUrl: String,
    val fileUrl: String? = null, // Secure file reference (only provided for entitled/purchased users)
    val previewUrl: String? = null,
    val categoryId: String,
    val categoryName: String,
    val language: String = "English",
    val price: Double,
    val discountPrice: Double? = null,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val pageCount: Int,
    val publicationDate: String = "2024",
    val isbn: String = "",
    val tags: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNewRelease: Boolean = false,
    val status: BookStatus = BookStatus.PUBLISHED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Convenience getter for legacy callers
    val coverImageUrl: String get() = coverUrl
    val averageRating: Double get() = rating
    val totalReviews: Int get() = reviewCount

    val formattedPrice: String
        get() = "₹${price.toInt()}"

    val discountedPrice: String
        get() = if (discountPrice != null && discountPrice < price) {
            "₹${discountPrice.toInt()}"
        } else {
            formattedPrice
        }

    val discountPercentage: Int
        get() = if (discountPrice != null && discountPrice < price && price > 0) {
            (((price - discountPrice) / price) * 100).toInt()
        } else 0
}

data class ReadingProgress(
    val userId: String,
    val bookId: String,
    val currentPage: Int,
    val totalPages: Int,
    val percentage: Float, // 0.0 to 100.0%
    val lastOpenedAt: Long = System.currentTimeMillis()
)

data class LibraryItem(
    val id: String,
    val userId: String,
    val book: Book,
    val readingProgress: Float = 0f, // 0.0 to 100.0%
    val lastReadPage: Int = 1,
    val status: ReadingStatus = ReadingStatus.NOT_STARTED,
    val isDownloaded: Boolean = false,
    val purchasedAt: Long = System.currentTimeMillis()
)

data class Bookmark(
    val id: String,
    val bookId: String,
    val pageNumber: Int,
    val title: String,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class BookFilter(
    val categoryId: String? = null,
    val minRating: Double? = null,
    val maxPrice: Double? = null,
    val language: String? = null,
    val sortOption: BookSortOption = BookSortOption.RELEVANCE
)

enum class BookSortOption(val label: String) {
    RELEVANCE("Relevance"),
    POPULARITY("Most Popular"),
    RATING("Highest Rated"),
    NEWEST("Newest"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low")
}

data class RecentSearch(
    val id: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}
