package com.example.presentation.viewmodel.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.Book
import com.example.domain.repository.BookRepository
import com.example.domain.repository.WishlistRepository
import com.example.presentation.scanner.BarcodeParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isCameraActive: Boolean = true,
    val isTorchOn: Boolean = false,
    val autoAddToWishlist: Boolean = false,
    val currentScannedBook: Book? = null,
    val currentRawCode: String? = null,
    val currentBarcodeFormat: String? = null,
    val isBookInWishlist: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null,
    val sessionScannedBooks: List<Book> = emptyList(),
    val showManualInputDialog: Boolean = false,
    val showHistorySheet: Boolean = false
)

class BookScannerViewModel(
    private val bookRepository: BookRepository,
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeDetected(rawValue: String, format: Int) {
        if (_uiState.value.isProcessing) return
        val normalized = BarcodeParser.normalizeCode(rawValue)
        val formatLabel = BarcodeParser.getFormatLabel(format)

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, currentRawCode = rawValue, currentBarcodeFormat = formatLabel) }
            try {
                val book = bookRepository.findBookByScannedCode(normalized)
                if (book != null) {
                    val inWishlist = wishlistRepository.isInWishlist(book.id).first()
                    
                    val updatedSession = if (_uiState.value.sessionScannedBooks.none { it.id == book.id }) {
                        listOf(book) + _uiState.value.sessionScannedBooks
                    } else {
                        _uiState.value.sessionScannedBooks
                    }

                    _uiState.update {
                        it.copy(
                            currentScannedBook = book,
                            isBookInWishlist = inWishlist,
                            sessionScannedBooks = updatedSession,
                            isProcessing = false,
                            statusMessage = "Book recognized: ${book.title}"
                        )
                    }

                    if (_uiState.value.autoAddToWishlist && !inWishlist) {
                        addCurrentBookToWishlist()
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            statusMessage = "No book found for code: $rawValue"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        statusMessage = "Error resolving code: ${e.message}"
                    )
                }
            }
        }
    }

    fun addCurrentBookToWishlist() {
        val book = _uiState.value.currentScannedBook ?: return
        viewModelScope.launch {
            val result = wishlistRepository.addScannedBookToWishlist(book)
            if (result is Resource.Success) {
                _uiState.update {
                    it.copy(
                        isBookInWishlist = true,
                        statusMessage = "✓ Added '${book.title}' to Wishlist!"
                    )
                }
            } else if (result is Resource.Error) {
                _uiState.update {
                    it.copy(statusMessage = "Failed to add to wishlist: ${result.message}")
                }
            }
        }
    }

    fun addAllSessionBooksToWishlist() {
        val books = _uiState.value.sessionScannedBooks
        if (books.isEmpty()) return

        viewModelScope.launch {
            var addedCount = 0
            books.forEach { book ->
                val res = wishlistRepository.addScannedBookToWishlist(book)
                if (res is Resource.Success) addedCount++
            }
            _uiState.update {
                it.copy(
                    statusMessage = "✓ Added $addedCount scanned physical books to your Wishlist!",
                    isBookInWishlist = true
                )
            }
        }
    }

    fun dismissCurrentScan() {
        _uiState.update {
            it.copy(
                currentScannedBook = null,
                currentRawCode = null,
                currentBarcodeFormat = null,
                isBookInWishlist = false,
                statusMessage = null
            )
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun toggleAutoAdd(enabled: Boolean) {
        _uiState.update { it.copy(autoAddToWishlist = enabled) }
    }

    fun showManualInputDialog(show: Boolean) {
        _uiState.update { it.copy(showManualInputDialog = show) }
    }

    fun showHistorySheet(show: Boolean) {
        _uiState.update { it.copy(showHistorySheet = show) }
    }

    fun searchOrAddManualIsbn(queryOrIsbn: String) {
        if (queryOrIsbn.isBlank()) return
        showManualInputDialog(false)
        onBarcodeDetected(queryOrIsbn.trim(), com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13)
    }

    fun simulateSampleScan(sampleCode: String) {
        val format = if (sampleCode.startsWith("bookora://")) {
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
        } else {
            com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13
        }
        onBarcodeDetected(sampleCode, format)
    }

    fun clearSessionHistory() {
        _uiState.update { it.copy(sessionScannedBooks = emptyList(), statusMessage = "Scan history cleared") }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
