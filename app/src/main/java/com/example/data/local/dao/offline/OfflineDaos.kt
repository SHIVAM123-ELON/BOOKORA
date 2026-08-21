package com.example.data.local.dao.offline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.local.entity.offline.CachedBookContentEntity
import com.example.data.local.entity.offline.CachedChapterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookContentCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedBook(book: CachedBookContentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedChapters(chapters: List<CachedChapterEntity>)

    @Query("SELECT * FROM cached_book_contents WHERE bookId = :bookId LIMIT 1")
    fun getCachedBook(bookId: String): Flow<CachedBookContentEntity?>

    @Query("SELECT * FROM cached_book_contents WHERE bookId = :bookId LIMIT 1")
    suspend fun getCachedBookDirect(bookId: String): CachedBookContentEntity?

    @Query("SELECT * FROM cached_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun getCachedChapters(bookId: String): Flow<List<CachedChapterEntity>>

    @Query("SELECT * FROM cached_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getCachedChaptersDirect(bookId: String): List<CachedChapterEntity>

    @Query("SELECT * FROM cached_chapters WHERE bookId = :bookId AND chapterIndex = :index LIMIT 1")
    fun getCachedChapter(bookId: String, index: Int): Flow<CachedChapterEntity?>

    @Query("SELECT * FROM cached_chapters WHERE bookId = :bookId AND chapterIndex = :index LIMIT 1")
    suspend fun getCachedChapterDirect(bookId: String, index: Int): CachedChapterEntity?

    @Query("SELECT * FROM cached_book_contents ORDER BY cachedAt DESC")
    fun getAllCachedBooks(): Flow<List<CachedBookContentEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_book_contents WHERE bookId = :bookId AND isAvailableOffline = 1)")
    fun isBookAvailableOffline(bookId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_book_contents WHERE bookId = :bookId AND isAvailableOffline = 1)")
    suspend fun isBookAvailableOfflineDirect(bookId: String): Boolean

    @Query("DELETE FROM cached_book_contents WHERE bookId = :bookId")
    suspend fun deleteCachedBook(bookId: String)

    @Query("DELETE FROM cached_chapters WHERE bookId = :bookId")
    suspend fun deleteCachedChapters(bookId: String)

    @Transaction
    suspend fun deleteEntireBookCache(bookId: String) {
        deleteCachedBook(bookId)
        deleteCachedChapters(bookId)
    }

    @Query("DELETE FROM cached_book_contents")
    suspend fun clearAllCachedBooks()

    @Query("DELETE FROM cached_chapters")
    suspend fun clearAllCachedChapters()

    @Transaction
    suspend fun clearEntireOfflineCache() {
        clearAllCachedBooks()
        clearAllCachedChapters()
    }

    @Query("SELECT COUNT(*) FROM cached_book_contents WHERE isAvailableOffline = 1")
    fun getOfflineBookCount(): Flow<Int>

    @Query("SELECT SUM(sizeBytes) FROM cached_book_contents WHERE isAvailableOffline = 1")
    fun getTotalCachedSizeBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM cached_chapters WHERE bookId = :bookId")
    suspend fun getCachedChaptersCount(bookId: String): Int
}
