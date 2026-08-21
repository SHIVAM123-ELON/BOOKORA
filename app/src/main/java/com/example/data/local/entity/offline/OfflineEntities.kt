package com.example.data.local.entity.offline

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.offline.CachedBookContent
import com.example.domain.model.offline.CachedChapter

/**
 * Room Entity storing full book metadata and overall offline availability state.
 */
@Entity(
    tableName = "cached_book_contents",
    indices = [
        Index(value = ["bookId"], unique = true),
        Index(value = ["isAvailableOffline"])
    ]
)
data class CachedBookContentEntity(
    @PrimaryKey
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
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val offlineFormat: String = "ROOM_EPUB_V1"
) {
    fun toDomain(chapters: List<CachedChapter> = emptyList()): CachedBookContent = CachedBookContent(
        bookId = bookId,
        title = title,
        subtitle = subtitle,
        authorName = authorName,
        authorId = authorId,
        coverUrl = coverUrl,
        categoryName = categoryName,
        totalPages = totalPages,
        totalChapters = totalChapters,
        fullContent = fullContent,
        synopsis = synopsis,
        cachedAt = cachedAt,
        sizeBytes = sizeBytes,
        isAvailableOffline = isAvailableOffline,
        chapters = chapters
    )

    companion object {
        fun fromDomain(domain: CachedBookContent): CachedBookContentEntity = CachedBookContentEntity(
            bookId = domain.bookId,
            title = domain.title,
            subtitle = domain.subtitle,
            authorName = domain.authorName,
            authorId = domain.authorId,
            coverUrl = domain.coverUrl,
            categoryName = domain.categoryName,
            totalPages = domain.totalPages,
            totalChapters = domain.totalChapters,
            fullContent = domain.fullContent,
            synopsis = domain.synopsis,
            cachedAt = domain.cachedAt,
            sizeBytes = domain.sizeBytes,
            isAvailableOffline = domain.isAvailableOffline
        )
    }
}

/**
 * Room Entity storing granular individual chapters for smooth offline pagination and reading.
 */
@Entity(
    tableName = "cached_chapters",
    indices = [
        Index(value = ["bookId", "chapterIndex"], unique = true),
        Index(value = ["bookId"])
    ]
)
data class CachedChapterEntity(
    @PrimaryKey
    val id: String, // e.g. "${bookId}_ch_${chapterIndex}"
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
) {
    fun toDomain(): CachedChapter = CachedChapter(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        chapterTitle = chapterTitle,
        chapterSubtitle = chapterSubtitle,
        content = content,
        startPage = startPage,
        endPage = endPage,
        estimatedReadingMinutes = estimatedReadingMinutes,
        wordCount = wordCount,
        cachedAt = cachedAt
    )

    companion object {
        fun fromDomain(domain: CachedChapter): CachedChapterEntity = CachedChapterEntity(
            id = domain.id,
            bookId = domain.bookId,
            chapterIndex = domain.chapterIndex,
            chapterTitle = domain.chapterTitle,
            chapterSubtitle = domain.chapterSubtitle,
            content = domain.content,
            startPage = domain.startPage,
            endPage = domain.endPage,
            estimatedReadingMinutes = domain.estimatedReadingMinutes,
            wordCount = domain.wordCount,
            cachedAt = domain.cachedAt
        )
    }
}
