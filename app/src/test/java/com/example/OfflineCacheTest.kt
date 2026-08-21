package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.local.BookoraDatabase
import com.example.data.local.entity.BookEntity
import com.example.data.local.entity.offline.CachedBookContentEntity
import com.example.data.local.entity.offline.CachedChapterEntity
import com.example.data.repository.offline.OfflineBookRepositoryImpl
import com.example.domain.model.offline.DownloadStatus
import com.example.presentation.viewmodel.offline.OfflineReaderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineCacheTest {

    private lateinit var db: BookoraDatabase
    private lateinit var offlineRepo: OfflineBookRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BookoraDatabase::class.java
        ).allowMainThreadQueries().build()

        offlineRepo = OfflineBookRepositoryImpl(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndRetrieveCachedBookAndChapters() = runBlocking {
        val bookId = "b-test-offline-01"
        val bookEntity = CachedBookContentEntity(
            bookId = bookId,
            title = "Kotlin in Action Offline",
            subtitle = "Mastering Kotlin Coroutines",
            authorName = "Dmitry Jemerov",
            authorId = "a-jemerov",
            coverUrl = "https://example.com/cover.jpg",
            categoryName = "Programming",
            totalPages = 360,
            totalChapters = 2,
            fullContent = "Full book content cached in Room",
            synopsis = "A comprehensive guide to Kotlin",
            cachedAt = System.currentTimeMillis(),
            sizeBytes = 150_000L,
            isAvailableOffline = true
        )

        val chapter1 = CachedChapterEntity(
            id = "${bookId}_ch_1",
            bookId = bookId,
            chapterIndex = 1,
            chapterTitle = "Chapter 1: Kotlin Basics",
            chapterSubtitle = "Syntax and Types",
            content = "Kotlin is a modern statically typed programming language.",
            startPage = 1,
            endPage = 20,
            estimatedReadingMinutes = 10,
            wordCount = 300
        )

        val chapter2 = CachedChapterEntity(
            id = "${bookId}_ch_2",
            bookId = bookId,
            chapterIndex = 2,
            chapterTitle = "Chapter 2: Coroutines & Flow",
            chapterSubtitle = "Asynchronous Streams",
            content = "Flow builders and channel pipelines in Kotlin.",
            startPage = 21,
            endPage = 50,
            estimatedReadingMinutes = 15,
            wordCount = 450
        )

        db.bookContentCacheDao().insertCachedBook(bookEntity)
        db.bookContentCacheDao().insertCachedChapters(listOf(chapter1, chapter2))

        val retrievedBook = db.bookContentCacheDao().getCachedBookDirect(bookId)
        assertNotNull(retrievedBook)
        assertEquals("Kotlin in Action Offline", retrievedBook?.title)
        assertTrue(retrievedBook?.isAvailableOffline == true)

        val retrievedChapters = db.bookContentCacheDao().getCachedChaptersDirect(bookId)
        assertEquals(2, retrievedChapters.size)
        assertEquals("Chapter 1: Kotlin Basics", retrievedChapters[0].chapterTitle)
        assertEquals("Chapter 2: Coroutines & Flow", retrievedChapters[1].chapterTitle)

        val isOffline = db.bookContentCacheDao().isBookAvailableOfflineDirect(bookId)
        assertTrue(isOffline)
    }

    @Test
    fun testDownloadBookForOfflineFlow() = runBlocking {
        val bookId = "b-sample-dl-01"
        // Seed book in bookDao
        db.bookDao().insertBooks(
            listOf(
                BookEntity(
                    id = bookId,
                    title = "Offline Caching Mastery",
                    subtitle = "Room DB Best Practices",
                    authorId = "a-author-01",
                    authorName = "Jane Doe",
                    description = "How to build bulletproof offline Android apps",
                    coverUrl = "https://example.com/cover.jpg",
                    fileUrl = "samples/offline.epub",
                    previewUrl = "https://example.com/preview.pdf",
                    categoryId = "c-tech",
                    categoryName = "Technology",
                    language = "English",
                    price = 499.0,
                    discountPrice = 399.0,
                    rating = 4.8,
                    reviewCount = 50,
                    pageCount = 180,
                    publicationDate = "2024-01-01",
                    isbn = "978-0000000000",
                    tags = "Android, Room, Kotlin",
                    status = "PUBLISHED"
                )
            )
        )

        val progressUpdates = offlineRepo.downloadBookForOffline(bookId).toList()
        assertTrue(progressUpdates.isNotEmpty())

        val lastProgress = progressUpdates.last()
        assertEquals(DownloadStatus.COMPLETED, lastProgress.status)
        assertEquals(100f, lastProgress.progressPercent, 0.01f)

        // Verify book is now in Room cache
        val cached = offlineRepo.getCachedBook(bookId).first()
        assertNotNull(cached)
        assertEquals("Offline Caching Mastery", cached?.title)
        assertEquals(3, cached?.chapters?.size)
    }

    @Test
    fun testOfflineReaderViewModelNavigation() = runBlocking {
        val viewModel = OfflineReaderViewModel(offlineRepo)
        viewModel.initializeForBook("b-clean-arch-001")

        // Initial chapter index is 1
        assertEquals(1, viewModel.selectedChapterIndex.value)

        // Navigate forward
        viewModel.nextChapter()
        assertEquals(2, viewModel.selectedChapterIndex.value)

        // Navigate backward
        viewModel.previousChapter()
        assertEquals(1, viewModel.selectedChapterIndex.value)

        // Toggle simulated offline mode
        assertFalse(viewModel.isOfflineSimulated.value)
        viewModel.toggleOfflineSimulated()
        assertTrue(viewModel.isOfflineSimulated.value)
    }
}
