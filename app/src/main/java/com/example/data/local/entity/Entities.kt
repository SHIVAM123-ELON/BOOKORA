package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Author
import com.example.domain.model.Book
import com.example.domain.model.BookStatus
import com.example.domain.model.Category
import com.example.domain.model.ReadingProgress
import com.example.domain.model.RecentSearch
import com.example.domain.model.User
import com.example.domain.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val fullName: String,
    val role: String,
    val avatarUrl: String?,
    val isVerified: Boolean = false
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        fullName = fullName,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.READER },
        avatarUrl = avatarUrl,
        isVerified = isVerified
    )

    companion object {
        fun fromDomain(user: User): UserEntity = UserEntity(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            role = user.role.name,
            avatarUrl = user.avatarUrl,
            isVerified = user.isVerified
        )
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val slug: String,
    val description: String,
    val iconName: String,
    val bookCount: Int = 0
) {
    fun toDomain(): Category = Category(
        id = id,
        name = name,
        slug = slug,
        description = description,
        iconName = iconName,
        bookCount = bookCount
    )
}

@Entity(tableName = "authors")
data class AuthorEntity(
    @PrimaryKey val id: String,
    val penName: String,
    val bio: String,
    val avatarUrl: String?,
    val isVerified: Boolean = true,
    val totalBooks: Int = 0,
    val rating: Double = 4.8,
    val followersCount: Int = 1000
) {
    fun toDomain(): Author = Author(
        id = id,
        penName = penName,
        bio = bio,
        avatarUrl = avatarUrl,
        isVerified = isVerified,
        totalBooks = totalBooks,
        rating = rating,
        followersCount = followersCount
    )
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String?,
    val authorId: String,
    val authorName: String,
    val description: String,
    val coverUrl: String,
    val fileUrl: String?,
    val previewUrl: String?,
    val categoryId: String,
    val categoryName: String,
    val language: String,
    val price: Double,
    val discountPrice: Double?,
    val rating: Double,
    val reviewCount: Int,
    val pageCount: Int,
    val publicationDate: String,
    val isbn: String,
    val tags: String,
    val isFeatured: Boolean = false,
    val isTrending: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNewRelease: Boolean = false,
    val status: String = "PUBLISHED",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        subtitle = subtitle,
        authorId = authorId,
        authorName = authorName,
        description = description,
        coverUrl = coverUrl,
        fileUrl = fileUrl,
        previewUrl = previewUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        language = language,
        price = price,
        discountPrice = discountPrice,
        rating = rating,
        reviewCount = reviewCount,
        pageCount = pageCount,
        publicationDate = publicationDate,
        isbn = isbn,
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        isFeatured = isFeatured,
        isTrending = isTrending,
        isBestSeller = isBestSeller,
        isNewRelease = isNewRelease,
        status = try { BookStatus.valueOf(status) } catch (e: Exception) { BookStatus.PUBLISHED },
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            subtitle = book.subtitle,
            authorId = book.authorId,
            authorName = book.authorName,
            description = book.description,
            coverUrl = book.coverUrl,
            fileUrl = book.fileUrl,
            previewUrl = book.previewUrl,
            categoryId = book.categoryId,
            categoryName = book.categoryName,
            language = book.language,
            price = book.price,
            discountPrice = book.discountPrice,
            rating = book.rating,
            reviewCount = book.reviewCount,
            pageCount = book.pageCount,
            publicationDate = book.publicationDate,
            isbn = book.isbn,
            tags = book.tags.joinToString(","),
            isFeatured = book.isFeatured,
            isTrending = book.isTrending,
            isBestSeller = book.isBestSeller,
            isNewRelease = book.isNewRelease,
            status = book.status.name,
            createdAt = book.createdAt,
            updatedAt = book.updatedAt
        )
    }
}

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: String,
    val userId: String,
    val currentPage: Int,
    val totalPages: Int,
    val percentage: Float,
    val lastOpenedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): ReadingProgress = ReadingProgress(
        userId = userId,
        bookId = bookId,
        currentPage = currentPage,
        totalPages = totalPages,
        percentage = percentage,
        lastOpenedAt = lastOpenedAt
    )
}

@Entity(tableName = "library_items")
data class LibraryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val readingProgress: Float = 0f,
    val lastReadPage: Int = 1,
    val status: String = "NOT_STARTED",
    val isDownloaded: Boolean = false,
    val purchasedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlist_items")
data class WishlistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val bookId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val id: String,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): RecentSearch = RecentSearch(
        id = id,
        query = query,
        timestamp = timestamp
    )
}
