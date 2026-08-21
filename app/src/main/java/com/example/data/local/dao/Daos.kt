package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AuthorEntity
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.LibraryEntity
import com.example.data.local.entity.ReadingProgressEntity
import com.example.data.local.entity.RecentSearchEntity
import com.example.data.local.entity.UserEntity
import com.example.data.local.entity.WishlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
}

@Dao
interface AuthorDao {
    @Query("SELECT * FROM authors ORDER BY rating DESC")
    fun getAllAuthors(): Flow<List<AuthorEntity>>

    @Query("SELECT * FROM authors WHERE id = :id LIMIT 1")
    fun getAuthorById(id: String): Flow<AuthorEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthors(authors: List<AuthorEntity>)
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE isFeatured = 1 ORDER BY rating DESC")
    fun getFeaturedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isTrending = 1 ORDER BY reviewCount DESC")
    fun getTrendingBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isBestSeller = 1 ORDER BY reviewCount DESC")
    fun getBestSellerBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isNewRelease = 1 ORDER BY createdAt DESC")
    fun getNewReleaseBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE discountPrice IS NOT NULL AND discountPrice < price ORDER BY (price - discountPrice) DESC")
    fun getDealBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdDirect(id: String): BookEntity?

    @Query("SELECT * FROM books WHERE authorId = :authorId AND id != :excludeBookId")
    fun getBooksByAuthor(authorId: String, excludeBookId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE categoryId = :categoryId AND id != :excludeBookId")
    fun getSimilarBooks(categoryId: String, excludeBookId: String): Flow<List<BookEntity>>

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE '%' || :query || '%' 
           OR authorName LIKE '%' || :query || '%'
           OR categoryName LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
    """)
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE REPLACE(isbn, '-', '') = REPLACE(:isbn, '-', '') OR isbn = :isbn LIMIT 1")
    fun getBookByIsbn(isbn: String): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE REPLACE(isbn, '-', '') = REPLACE(:isbn, '-', '') OR isbn = :isbn LIMIT 1")
    suspend fun getBookByIsbnDirect(isbn: String): BookEntity?

    @Query("SELECT * FROM books")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Update
    suspend fun updateBook(book: BookEntity)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library_items ORDER BY purchasedAt DESC")
    fun getAllLibraryItems(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library_items WHERE bookId = :bookId LIMIT 1")
    fun getLibraryItem(bookId: String): Flow<LibraryEntity?>

    @Query("SELECT * FROM library_items WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    suspend fun getLibraryItemDirect(userId: String, bookId: String): LibraryEntity?

    @Query("DELETE FROM library_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeLibraryItem(userId: String, bookId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM library_items WHERE bookId = :bookId)")
    suspend fun isBookEntitled(bookId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(item: LibraryEntity)

    @Query("UPDATE library_items SET readingProgress = :progress, lastReadPage = :page, status = :status WHERE bookId = :bookId")
    suspend fun updateProgress(bookId: String, page: Int, progress: Float, status: String)

    @Query("UPDATE library_items SET isDownloaded = :isDownloaded WHERE bookId = :bookId")
    suspend fun updateDownloadState(bookId: String, isDownloaded: Boolean)
}

@Dao
interface ReadingProgressDao {
    @Query("SELECT * FROM reading_progress WHERE userId = :userId AND bookId = :bookId LIMIT 1")
    fun getReadingProgress(userId: String, bookId: String): Flow<ReadingProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReadingProgress(progress: ReadingProgressEntity)
}

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items WHERE userId = :userId ORDER BY addedAt DESC")
    fun getUserWishlist(userId: String): Flow<List<WishlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE userId = :userId AND bookId = :bookId)")
    fun isInWishlist(userId: String, bookId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWishlist(item: WishlistEntity)

    @Query("DELETE FROM wishlist_items WHERE userId = :userId AND bookId = :bookId")
    suspend fun removeFromWishlist(userId: String, bookId: String)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` = :query")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()
}
