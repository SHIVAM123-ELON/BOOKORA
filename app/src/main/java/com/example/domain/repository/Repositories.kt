package com.example.domain.repository

import android.content.Context
import com.example.core.result.Resource
import com.example.domain.model.Author
import com.example.domain.model.Book
import com.example.domain.model.BookFilter
import com.example.domain.model.Category
import com.example.domain.model.LibraryItem
import com.example.domain.model.ReadingProgress
import com.example.domain.model.RecentSearch
import com.example.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(fullName: String, email: String, password: String): Resource<User>
    suspend fun signInWithGoogleIdToken(idToken: String): Resource<User>
    suspend fun signInWithGoogle(context: Context): Resource<User>
    suspend fun sendPasswordResetEmail(email: String): Resource<Unit>
    fun getCurrentUser(): Flow<User?>
    fun isUserLoggedIn(): Flow<Boolean>
    suspend fun logout(): Resource<Unit>
}

interface BookRepository {
    fun getFeaturedBooks(): Flow<List<Book>>
    fun getTrendingBooks(): Flow<List<Book>>
    fun getBestSellers(): Flow<List<Book>>
    fun getNewReleases(): Flow<List<Book>>
    fun getRecommendedBooks(): Flow<List<Book>>
    fun getDeals(): Flow<List<Book>>
    fun getBookById(id: String): Flow<Book?>
    fun getBooksByAuthor(authorId: String, excludeBookId: String): Flow<List<Book>>
    fun getSimilarBooks(categoryId: String, excludeBookId: String): Flow<List<Book>>
    fun getBooksByCategory(categoryId: String): Flow<List<Book>>
    fun getBookByIsbn(isbn: String): Flow<Book?>
    suspend fun findBookByScannedCode(code: String): Book?
    suspend fun addOrUpdateBook(book: Book): Resource<Unit>
    suspend fun refreshBooks(): Resource<Unit>
}

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun refreshCategories(): Resource<Unit>
}

interface AuthorRepository {
    fun getPopularAuthors(): Flow<List<Author>>
    fun getAuthorById(authorId: String): Flow<Author?>
    suspend fun refreshAuthors(): Resource<Unit>
}

interface SearchRepository {
    fun searchBooks(query: String, filter: BookFilter): Flow<List<Book>>
    fun getRecentSearches(): Flow<List<RecentSearch>>
    suspend fun saveRecentSearch(query: String)
    suspend fun deleteRecentSearch(query: String)
    suspend fun clearRecentSearches()
}

interface WishlistRepository {
    fun getWishlistBooks(): Flow<List<Book>>
    fun isInWishlist(bookId: String): Flow<Boolean>
    suspend fun addToWishlist(bookId: String): Resource<Unit>
    suspend fun addScannedBookToWishlist(book: Book): Resource<Unit>
    suspend fun removeFromWishlist(bookId: String): Resource<Unit>
    suspend fun toggleWishlist(bookId: String): Resource<Boolean>
}

interface LibraryRepository {
    fun getUserLibrary(): Flow<List<LibraryItem>>
    fun isBookEntitled(bookId: String): Flow<Boolean>
    suspend fun addBookToLibrary(bookId: String): Resource<Unit>
    fun getReadingProgress(bookId: String): Flow<ReadingProgress?>
    suspend fun saveProgress(bookId: String, page: Int, totalPages: Int): Resource<Unit>
    suspend fun syncProgress(): Resource<Unit>
    suspend fun toggleDownload(bookId: String, isDownloaded: Boolean): Resource<Unit>
}
