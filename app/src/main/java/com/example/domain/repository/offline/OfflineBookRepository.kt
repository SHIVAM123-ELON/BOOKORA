package com.example.domain.repository.offline

import com.example.core.result.Resource
import com.example.domain.model.offline.CachedBookContent
import com.example.domain.model.offline.CachedChapter
import com.example.domain.model.offline.OfflineDownloadProgress
import com.example.domain.model.offline.OfflineStorageStats
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository governing local Room-based book content caching,
 * chapter retrieval for offline reading, and storage management.
 */
interface OfflineBookRepository {

    /**
     * Streams cached book metadata and content from Room.
     */
    fun getCachedBook(bookId: String): Flow<CachedBookContent?>

    /**
     * Streams all cached chapters for a specific book sorted by index.
     */
    fun getCachedChapters(bookId: String): Flow<List<CachedChapter>>

    /**
     * Streams a specific cached chapter by its index.
     */
    fun getCachedChapter(bookId: String, chapterIndex: Int): Flow<CachedChapter?>

    /**
     * Streams all books currently stored in the local Room cache.
     */
    fun getAllCachedBooks(): Flow<List<CachedBookContent>>

    /**
     * Streams whether the book is downloaded and available for offline reading.
     */
    fun isBookAvailableOffline(bookId: String): Flow<Boolean>

    /**
     * Downloads/caches book content into Room with progress callbacks.
     */
    fun downloadBookForOffline(bookId: String): Flow<OfflineDownloadProgress>

    /**
     * Removes cached book and all its chapters from Room.
     */
    suspend fun removeCachedBook(bookId: String): Resource<Unit>

    /**
     * Clears all cached books and chapters from Room database.
     */
    suspend fun clearAllOfflineCache(): Resource<Unit>

    /**
     * Streams aggregated offline cache storage metrics (item count, byte size).
     */
    fun getOfflineStorageStats(): Flow<OfflineStorageStats>

    /**
     * Pre-seeds authentic multi-chapter offline contents for sample books.
     */
    suspend fun seedSampleOfflineContentIfEmpty()
}
