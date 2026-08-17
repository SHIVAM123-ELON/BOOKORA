package com.example.data.remote

import com.example.data.remote.dto.ApiResponseDto
import com.example.data.remote.dto.AuthResponseDto
import com.example.data.remote.dto.AuthorDto
import com.example.data.remote.dto.BookDto
import com.example.data.remote.dto.CategoryDto
import com.example.data.remote.dto.LoginRequestDto
import com.example.data.remote.dto.ProgressRequestDto
import com.example.data.remote.dto.RegisterRequestDto
import com.example.data.remote.dto.UserDto
import com.example.data.remote.dto.WishlistRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookoraApiService {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): ApiResponseDto<AuthResponseDto>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): ApiResponseDto<AuthResponseDto>

    @GET("api/v1/auth/me")
    suspend fun getCurrentUser(): ApiResponseDto<UserDto>

    @GET("api/v1/books")
    suspend fun getBooks(
        @Query("featured") isFeatured: Boolean? = null,
        @Query("trending") isTrending: Boolean? = null,
        @Query("bestseller") isBestSeller: Boolean? = null,
        @Query("newRelease") isNewRelease: Boolean? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponseDto<List<BookDto>>

    @GET("api/v1/books/{id}")
    suspend fun getBookById(
        @Path("id") id: String
    ): ApiResponseDto<BookDto>

    @GET("api/v1/books/search")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("categoryId") categoryId: String? = null,
        @Query("language") language: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): ApiResponseDto<List<BookDto>>

    @GET("api/v1/categories")
    suspend fun getCategories(): ApiResponseDto<List<CategoryDto>>

    @GET("api/v1/authors/{id}")
    suspend fun getAuthorById(
        @Path("id") authorId: String
    ): ApiResponseDto<AuthorDto>

    @GET("api/v1/library")
    suspend fun getLibrary(): ApiResponseDto<List<BookDto>>

    @POST("api/v1/library/progress")
    suspend fun saveProgress(
        @Body request: ProgressRequestDto
    ): ApiResponseDto<Unit>

    @GET("api/v1/wishlist")
    suspend fun getWishlist(): ApiResponseDto<List<BookDto>>

    @POST("api/v1/wishlist")
    suspend fun addToWishlist(
        @Body request: WishlistRequestDto
    ): ApiResponseDto<Unit>

    @DELETE("api/v1/wishlist/{bookId}")
    suspend fun removeFromWishlist(
        @Path("bookId") bookId: String
    ): ApiResponseDto<Unit>
}
