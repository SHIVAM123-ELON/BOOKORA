package com.example.domain.model.offline

/**
 * Domain model representing fully cached book content stored in the local Room database.
 */
data class CachedBookContent(
    val bookId: String,
    val title: String,
    val subtitle: String?,
    val authorName: String,
    val authorId: String,
    val coverUrl: String,
    val categoryName: String,
    val totalPages: Int,
    val totalChapters: Int,
    val fullContent: String,
    val synopsis: String,
    val cachedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val isAvailableOffline: Boolean = true,
    val chapters: List<CachedChapter> = emptyList()
) {
    val formattedSize: String get() {
        if (sizeBytes <= 0) return "0 KB"
        val kb = sizeBytes / 1024.0
        return if (kb >= 1024) {
            String.format("%.1f MB", kb / 1024.0)
        } else {
            String.format("%.0f KB", kb)
        }
    }
}

/**
 * Domain model representing a distinct chapter available for offline reading.
 */
data class CachedChapter(
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val chapterTitle: String,
    val chapterSubtitle: String? = null,
    val content: String,
    val startPage: Int,
    val endPage: Int,
    val estimatedReadingMinutes: Int,
    val wordCount: Int,
    val cachedAt: Long = System.currentTimeMillis()
)

/**
 * Lifecycle states during offline download and Room database caching.
 */
enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    PARSING_CHAPTERS,
    CACHING_TO_ROOM,
    COMPLETED,
    FAILED
}

/**
 * Progress updates emitted during offline download.
 */
data class OfflineDownloadProgress(
    val bookId: String = "",
    val progressPercent: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val statusMessage: String = "",
    val totalChaptersCached: Int = 0,
    val bytesDownloaded: Long = 0L
)

/**
 * Local on-device storage metrics for cached books.
 */
data class OfflineStorageStats(
    val totalCachedBooks: Int = 0,
    val totalStorageBytes: Long = 0L
) {
    val formattedStorageSize: String get() {
        if (totalStorageBytes <= 0) return "0 KB"
        val kb = totalStorageBytes / 1024.0
        return if (kb >= 1024) {
            String.format("%.1f MB", kb / 1024.0)
        } else {
            String.format("%.0f KB", kb)
        }
    }
}
