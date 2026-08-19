package com.example.presentation.viewmodel.publisher

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.publisher.*
import com.example.domain.publisher.PdfValidationService
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.publisher.PublisherRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UploadBookUiState(
    val selectedPdfUri: Uri? = null,
    val pdfFileName: String = "",
    val isAnalyzingPdf: Boolean = false,
    val validationResult: FileValidationResult? = null,
    val title: String = "",
    val authorName: String = "",
    val description: String = "",
    val categoryId: String = "cat-fiction",
    val categoryName: String = "Fiction",
    val language: String = "English",
    val tags: String = "",
    val coverImageUri: String? = null,
    val copyrightAccepted: Boolean = false,
    val isSubmitting: Boolean = false,
    val uploadProgress: Float = 0f,
    val submissionSuccess: Boolean = false,
    val submittedBook: BookSubmission? = null,
    val errorMessage: String? = null
)

class UploadBookViewModel(
    private val publisherRepository: PublisherRepository,
    private val authRepository: AuthRepository,
    private val pdfValidationService: PdfValidationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(UploadBookUiState())
    val uiState: StateFlow<UploadBookUiState> = _uiState.asStateFlow()

    fun onPdfSelected(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedPdfUri = uri,
                    pdfFileName = fileName,
                    isAnalyzingPdf = true,
                    errorMessage = null,
                    validationResult = null
                )
            }

            // Retrieve existing hashes for duplicate checks
            val allSubmissions = publisherRepository.getAllSubmissions().first()
            val existingHashes = allSubmissions.map { it.pdfSha256Hash }.toSet()

            val result = pdfValidationService.validatePdf(uri, existingHashes)

            _uiState.update {
                it.copy(
                    isAnalyzingPdf = false,
                    validationResult = result,
                    errorMessage = if (!result.isValid) result.errorMessage else null,
                    title = if (it.title.isBlank()) fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ").capitalizeWords() else it.title
                )
            }
        }
    }

    fun onTitleChanged(v: String) = _uiState.update { it.copy(title = v) }
    fun onAuthorNameChanged(v: String) = _uiState.update { it.copy(authorName = v) }
    fun onDescriptionChanged(v: String) = _uiState.update { it.copy(description = v) }
    fun onCategorySelected(id: String, name: String) = _uiState.update { it.copy(categoryId = id, categoryName = name) }
    fun onLanguageChanged(v: String) = _uiState.update { it.copy(language = v) }
    fun onTagsChanged(v: String) = _uiState.update { it.copy(tags = v) }
    fun onCoverImageSelected(uri: String?) = _uiState.update { it.copy(coverImageUri = uri) }
    fun onCopyrightToggled(accepted: Boolean) = _uiState.update { it.copy(copyrightAccepted = accepted) }

    fun submitBook() {
        val state = _uiState.value
        val validation = state.validationResult

        if (validation == null || !validation.isValid) {
            _uiState.update { it.copy(errorMessage = "Please select a valid, non-corrupted PDF file before submitting.") }
            return
        }

        if (state.title.isBlank() || state.authorName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Title and Author Name are required.") }
            return
        }

        if (!state.copyrightAccepted) {
            _uiState.update { it.copy(errorMessage = "You must accept the copyright and content ownership declaration.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, uploadProgress = 0.2f, errorMessage = null) }

            val currentUser = authRepository.getCurrentUser().first()
            val userId = currentUser?.id ?: "u-default-reader-001"
            val userName = currentUser?.fullName ?: "Bookora Creator"
            val userEmail = currentUser?.email ?: "creator@bookora.app"

            _uiState.update { it.copy(uploadProgress = 0.6f) }

            val tagList = state.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            val result = publisherRepository.submitBook(
                userId = userId,
                userName = userName,
                userEmail = userEmail,
                title = state.title,
                authorName = state.authorName,
                description = state.description,
                categoryId = state.categoryId,
                categoryName = state.categoryName,
                language = state.language,
                tags = tagList,
                pdfUri = state.selectedPdfUri.toString(),
                pdfSizeBytes = validation.fileSizeBytes,
                pdfSha256 = validation.sha256Hash,
                pdfPageCount = validation.pageCount,
                coverImageUri = state.coverImageUri,
                copyrightAccepted = state.copyrightAccepted
            )

            when (result) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            uploadProgress = 1.0f,
                            submissionSuccess = true,
                            submittedBook = result.data
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            uploadProgress = 0f,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {}
            }
        }
    }

    fun reset() {
        _uiState.value = UploadBookUiState()
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

data class CreatorEarningsUiState(
    val balance: CreatorBalance? = null,
    val transactions: List<CreatorTransaction> = emptyList(),
    val payoutRequests: List<CreatorPayoutRequest> = emptyList(),
    val mySubmissions: List<BookSubmission> = emptyList(),
    val isWithdrawalDialogOpen: Boolean = false,
    val withdrawalAmountText: String = "50",
    val upiIdText: String = "",
    val isRequestingWithdrawal: Boolean = false,
    val withdrawalSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class CreatorEarningsViewModel(
    private val publisherRepository: PublisherRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatorEarningsUiState())
    val uiState: StateFlow<CreatorEarningsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser().first()
            val userId = user?.id ?: "u-default-reader-001"

            launch {
                publisherRepository.getCreatorBalance(userId).collect { bal ->
                    _uiState.update { it.copy(balance = bal) }
                }
            }

            launch {
                publisherRepository.getCreatorTransactions(userId).collect { txs ->
                    _uiState.update { it.copy(transactions = txs) }
                }
            }

            launch {
                publisherRepository.getUserPayoutRequests(userId).collect { reqs ->
                    _uiState.update { it.copy(payoutRequests = reqs) }
                }
            }

            launch {
                publisherRepository.getSubmissionsForUser(userId).collect { subs ->
                    _uiState.update { it.copy(mySubmissions = subs) }
                }
            }
        }
    }

    fun openWithdrawalDialog() = _uiState.update { it.copy(isWithdrawalDialogOpen = true, withdrawalSuccessMessage = null, errorMessage = null) }
    fun closeWithdrawalDialog() = _uiState.update { it.copy(isWithdrawalDialogOpen = false) }
    fun onWithdrawalAmountChanged(v: String) = _uiState.update { it.copy(withdrawalAmountText = v) }
    fun onUpiIdChanged(v: String) = _uiState.update { it.copy(upiIdText = v) }

    fun submitWithdrawalRequest() {
        val state = _uiState.value
        val amountRupees = state.withdrawalAmountText.toDoubleOrNull() ?: 0.0
        val amountPaise = (amountRupees * 100).toLong()

        if (amountPaise < 5000L) {
            _uiState.update { it.copy(errorMessage = "Minimum payout withdrawal is ₹50.00.") }
            return
        }

        if (state.upiIdText.isBlank() || !state.upiIdText.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid UPI ID (e.g. user@bank).") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isRequestingWithdrawal = true, errorMessage = null) }
            val userId = authRepository.getCurrentUser().first()?.id ?: "u-default-reader-001"

            when (val res = publisherRepository.requestPayout(userId, amountPaise, state.upiIdText)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isRequestingWithdrawal = false,
                            isWithdrawalDialogOpen = false,
                            withdrawalSuccessMessage = "Withdrawal request of ₹%.2f submitted successfully!".format(amountRupees)
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isRequestingWithdrawal = false,
                            errorMessage = res.message
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

data class AdminModerationUiState(
    val submissions: List<BookSubmission> = emptyList(),
    val selectedStatusFilter: SubmissionStatus? = null,
    val selectedSubmission: BookSubmission? = null,
    val isReviewing: Boolean = false,
    val feedbackText: String = "",
    val payoutRequests: List<CreatorPayoutRequest> = emptyList(),
    val copyrightReports: List<CopyrightReport> = emptyList(),
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class AdminModerationViewModel(
    private val publisherRepository: PublisherRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminModerationUiState())
    val uiState: StateFlow<AdminModerationUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            launch {
                publisherRepository.getAllSubmissions().collect { subs ->
                    _uiState.update { it.copy(submissions = subs) }
                }
            }
            launch {
                publisherRepository.getAllPayoutRequests().collect { reqs ->
                    _uiState.update { it.copy(payoutRequests = reqs) }
                }
            }
            launch {
                publisherRepository.getAllCopyrightReports().collect { reports ->
                    _uiState.update { it.copy(copyrightReports = reports) }
                }
            }
        }
    }

    fun onFilterSelected(status: SubmissionStatus?) = _uiState.update { it.copy(selectedStatusFilter = status) }
    fun onSelectSubmission(sub: BookSubmission?) = _uiState.update { it.copy(selectedSubmission = sub, feedbackText = "") }
    fun onFeedbackChanged(v: String) = _uiState.update { it.copy(feedbackText = v) }

    fun reviewSelectedSubmission(status: SubmissionStatus) {
        val sub = _uiState.value.selectedSubmission ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewing = true, errorMessage = null) }
            val adminUser = authRepository.getCurrentUser().first()
            val adminId = adminUser?.id ?: "admin_authoritative_root"

            when (val res = publisherRepository.reviewSubmission(
                submissionId = sub.id,
                adminUserId = adminId,
                status = status,
                feedback = _uiState.value.feedbackText.ifBlank { null }
            )) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isReviewing = false,
                            selectedSubmission = null,
                            feedbackText = "",
                            successMessage = "Submission '${sub.title}' marked as ${status.name}." +
                                    if (status == SubmissionStatus.APPROVED) " ₹1 reward credited to creator." else ""
                        )
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isReviewing = false, errorMessage = res.message) }
                }
                else -> {}
            }
        }
    }

    fun updatePayoutRequest(payoutId: String, status: CreatorPayoutStatus, transactionRef: String? = null) {
        viewModelScope.launch {
            val ref = transactionRef ?: "UPI_DISBURSE_${System.currentTimeMillis()}"
            publisherRepository.updatePayoutStatus(
                payoutId = payoutId,
                status = status,
                adminNotes = "Reviewed by Admin",
                transactionRef = if (status == CreatorPayoutStatus.PAID) ref else null
            )
        }
    }

    fun freezeCreator(userId: String, freeze: Boolean, reason: String?) {
        viewModelScope.launch {
            publisherRepository.freezeCreatorAccount(userId, freeze, reason)
        }
    }
}
