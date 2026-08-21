package com.example.presentation.viewmodel.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.offline.CachedBookContent
import com.example.domain.model.offline.CachedChapter
import com.example.domain.model.offline.DownloadStatus
import com.example.domain.model.offline.OfflineDownloadProgress
import com.example.domain.model.offline.OfflineStorageStats
import com.example.domain.repository.offline.OfflineBookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineReaderViewModel(
    private val offlineBookRepository: OfflineBookRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow("")
    val currentBookId: StateFlow<String> = _currentBookId.asStateFlow()

    private val _selectedChapterIndex = MutableStateFlow(1)
    val selectedChapterIndex: StateFlow<Int> = _selectedChapterIndex.asStateFlow()

    private val _isOfflineSimulated = MutableStateFlow(false)
    val isOfflineSimulated: StateFlow<Boolean> = _isOfflineSimulated.asStateFlow()

    private val _downloadProgress = MutableStateFlow(OfflineDownloadProgress())
    val downloadProgress: StateFlow<OfflineDownloadProgress> = _downloadProgress.asStateFlow()

    val cachedBook: StateFlow<CachedBookContent?> = _currentBookId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(null) else offlineBookRepository.getCachedBook(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chapters: StateFlow<List<CachedChapter>> = _currentBookId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList()) else offlineBookRepository.getCachedChapters(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentChapter: StateFlow<CachedChapter?> = kotlinx.coroutines.flow.combine(
        chapters,
        _selectedChapterIndex
    ) { chapterList, index ->
        chapterList.find { it.chapterIndex == index } ?: chapterList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isBookCached: StateFlow<Boolean> = _currentBookId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(false) else offlineBookRepository.isBookAvailableOffline(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val storageStats: StateFlow<OfflineStorageStats> = offlineBookRepository.getOfflineStorageStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OfflineStorageStats())

    fun initializeForBook(bookId: String) {
        if (_currentBookId.value != bookId) {
            _currentBookId.value = bookId
            _selectedChapterIndex.value = 1
        }
    }

    fun selectChapter(chapterIndex: Int) {
        _selectedChapterIndex.value = chapterIndex
    }

    fun nextChapter() {
        val total = chapters.value.size
        if (_selectedChapterIndex.value < total) {
            _selectedChapterIndex.value += 1
        }
    }

    fun previousChapter() {
        if (_selectedChapterIndex.value > 1) {
            _selectedChapterIndex.value -= 1
        }
    }

    fun toggleOfflineSimulated() {
        _isOfflineSimulated.value = !_isOfflineSimulated.value
    }

    fun downloadBookForOffline(bookId: String = _currentBookId.value) {
        if (bookId.isBlank()) return
        viewModelScope.launch {
            offlineBookRepository.downloadBookForOffline(bookId).collect { progress ->
                _downloadProgress.value = progress
            }
        }
    }

    fun removeBookFromOffline(bookId: String = _currentBookId.value) {
        if (bookId.isBlank()) return
        viewModelScope.launch {
            offlineBookRepository.removeCachedBook(bookId)
            _downloadProgress.value = OfflineDownloadProgress(
                bookId = bookId,
                status = DownloadStatus.IDLE,
                statusMessage = "Offline cache removed."
            )
        }
    }

    fun clearAllOfflineCache() {
        viewModelScope.launch {
            offlineBookRepository.clearAllOfflineCache()
        }
    }
}
