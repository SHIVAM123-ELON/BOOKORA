package com.example.data.remote.dto

import com.example.domain.model.Author
import com.example.domain.model.Book
import com.example.domain.model.BookStatus
import com.example.domain.model.Category
import com.example.domain.model.ReadingProgress
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResponseDto<T>(
    @Json(name = "success") val success: Boolean,
    @Json(name = "data") val data: T?,
    @Json(name = "error") val error: ApiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @Json(name = "code") val code: String,
    @Json(name = "message") val message: String
)

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequestDto(
    @Json(name = "fullName") val fullName: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class ProgressRequestDto(
    @Json(name = "bookId") val bookId: String,
    @Json(name = "currentPage") val currentPage: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "percentage") val percentage: Float
)

@JsonClass(generateAdapter = true)
data class WishlistRequestDto(
    @Json(name = "bookId") val bookId: String
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    @Json(name = "user") val user: UserDto,
    @Json(name = "accessToken") val accessToken: String,
    @Json(name = "refreshToken") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "fullName") val fullName: String,
    @Json(name = "role") val role: String,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "isVerified") val isVerified: Boolean = false
) {
    fun toDomain(): User = User(
        id = id,
        email = email,
        fullName = fullName,
        role = try { UserRole.valueOf(role) } catch (e: Exception) { UserRole.READER },
        avatarUrl = avatarUrl,
        isVerified = isVerified
    )
}

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "iconName") val iconName: String = "book",
    @Json(name = "bookCount") val bookCount: Int = 0
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

@JsonClass(generateAdapter = true)
data class AuthorDto(
    @Json(name = "id") val id: String,
    @Json(name = "penName") val penName: String,
    @Json(name = "bio") val bio: String = "",
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "isVerified") val isVerified: Boolean = true,
    @Json(name = "totalBooks") val totalBooks: Int = 0,
    @Json(name = "rating") val rating: Double = 4.8,
    @Json(name = "followersCount") val followersCount: Int = 1000
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

@JsonClass(generateAdapter = true)
data class BookDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "subtitle") val subtitle: String? = null,
    @Json(name = "authorId") val authorId: String,
    @Json(name = "authorName") val authorName: String,
    @Json(name = "description") val description: String = "",
    @Json(name = "coverUrl") val coverUrl: String = "",
    @Json(name = "previewUrl") val previewUrl: String? = null,
    @Json(name = "categoryId") val categoryId: String = "",
    @Json(name = "categoryName") val categoryName: String = "",
    @Json(name = "language") val language: String = "English",
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "discountPrice") val discountPrice: Double? = null,
    @Json(name = "rating") val rating: Double = 0.0,
    @Json(name = "reviewCount") val reviewCount: Int = 0,
    @Json(name = "pageCount") val pageCount: Int = 0,
    @Json(name = "publicationDate") val publicationDate: String = "2024",
    @Json(name = "isbn") val isbn: String = "",
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "isFeatured") val isFeatured: Boolean = false,
    @Json(name = "isTrending") val isTrending: Boolean = false,
    @Json(name = "isBestSeller") val isBestSeller: Boolean = false,
    @Json(name = "isNewRelease") val isNewRelease: Boolean = false,
    @Json(name = "status") val status: String = "PUBLISHED"
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        subtitle = subtitle,
        authorId = authorId,
        authorName = authorName,
        description = description,
        coverUrl = coverUrl,
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
        tags = tags,
        isFeatured = isFeatured,
        isTrending = isTrending,
        isBestSeller = isBestSeller,
        isNewRelease = isNewRelease,
        status = try { BookStatus.valueOf(status) } catch (e: Exception) { BookStatus.PUBLISHED }
    )
}

@JsonClass(generateAdapter = true)
data class ReadingProgressDto(
    @Json(name = "bookId") val bookId: String,
    @Json(name = "currentPage") val currentPage: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "percentage") val percentage: Float,
    @Json(name = "lastOpenedAt") val lastOpenedAt: Long = System.currentTimeMillis()
)
