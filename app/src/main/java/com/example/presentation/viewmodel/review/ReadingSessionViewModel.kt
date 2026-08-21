package com.example.presentation.viewmodel.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.result.Resource
import com.example.domain.model.review.ReaderVerification
import com.example.domain.model.review.ReadingActivity
import com.example.domain.model.review.ReadingSession
import com.example.domain.model.review.ReviewEligibility
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.review.ReviewRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * State representing an in-progress reading session in the reader UI.
 */
data class ActiveReadingSessionState(
    val bookId: String = "",
    val userId: String = "",
    val startPage: Int = 1,
    val currentPage: Int = 1,
    val totalPages: Int = 100,
    val durationSeconds: Long = 0L,
    val sessionStartTime: Long = System.currentTimeMillis(),
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val currentProgressPercent: Float = 0f,
    val sessionDeltaPercent: Float = 0f,
    val pagesReadThisSession: Int = 0
) {
    val durationMinutes: Double get() = durationSeconds / 60.0

    val formattedDuration: String get() {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remMin = minutes % 60
            "${hours}h ${remMin}m"
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    val readingSpeedPagesPerHour: Double get() {
        if (durationSeconds <= 10 || pagesReadThisSession <= 0) return 0.0
        val hours = durationSeconds / 3600.0
        return pagesReadThisSession / hours
    }

    val estimatedMinutesRemaining: Int get() {
        val remainingPages = maxOf(0, totalPages - currentPage)
        val speed = readingSpeedPagesPerHour
        return if (speed > 0) {
            ((remainingPages / speed) * 60).toInt()
        } else {
            0
        }
    }
}

/**
 * Events dispatched by ReadingSessionViewModel for UI feedback (toasts, snackbars, badges).
 */
sealed interface ReadingSessionEvent {
    data class SessionSaved(
        val durationSeconds: Long,
        val pagesRead: Int,
        val newProgressPercent: Float,
        val isVerified: Boolean
    ) : ReadingSessionEvent

    data class VerificationStatusUpdated(
        val isVerified: Boolean,
        val reason: String
    ) : ReadingSessionEvent

    data class ReadingMilestoneReached(
        val milestonePercent: Int,
        val message: String
    ) : ReadingSessionEvent

    data class Error(val message: String) : ReadingSessionEvent
}

/**
 * ViewModel managing the business logic for tracking active reading sessions,
 * periodically syncing reading metrics to Room database, and dynamically evaluating
 * reader verification status between the UI and Room DAOs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadingSessionViewModel(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentBookId = MutableStateFlow("")
    val currentBookId: StateFlow<String> = _currentBookId.asStateFlow()

    private val _activeSession = MutableStateFlow(ActiveReadingSessionState())
    val activeSession: StateFlow<ActiveReadingSessionState> = _activeSession.asStateFlow()

    private val _events = MutableSharedFlow<ReadingSessionEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<ReadingSessionEvent> = _events.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var timerJob: Job? = null
    private var autoSaveJob: Job? = null
    private var lastRecordedSeconds: Long = 0L
    private var lastRecordedPage: Int = 1

    val currentUserId: StateFlow<String> = authRepository.getCurrentUser()
        .map { it?.id ?: "u-default-reader-001" }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "u-default-reader-001")

    /**
     * Reactive Stream: Active Reader Verification State for the current book from Room DB.
     */
    val readerVerification: StateFlow<ReaderVerification?> = combine(
        currentUserId,
        _currentBookId
    ) { userId, bookId ->
        userId to bookId
    }.flatMapLatest { (userId, bookId) ->
        if (bookId.isNotBlank()) {
            reviewRepository.getReaderVerification(userId, bookId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Reactive Stream: Aggregate Reading Activity summary from Room DB.
     */
    val readingActivity: StateFlow<ReadingActivity?> = combine(
        currentUserId,
        _currentBookId
    ) { userId, bookId ->
        userId to bookId
    }.flatMapLatest { (userId, bookId) ->
        if (bookId.isNotBlank()) {
            reviewRepository.getReadingActivity(userId, bookId)
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Reactive Stream: Granular list of past reading sessions for the current book from Room DB.
     */
    val sessionHistory: StateFlow<List<ReadingSession>> = combine(
        currentUserId,
        _currentBookId
    ) { userId, bookId ->
        userId to bookId
    }.flatMapLatest { (userId, bookId) ->
        if (bookId.isNotBlank()) {
            reviewRepository.getReadingSessions(userId, bookId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Reactive Stream: Authoritative review eligibility & verified reader status from Room DB.
     */
    private val _reviewEligibility = MutableStateFlow<ReviewEligibility?>(null)
    val reviewEligibility: StateFlow<ReviewEligibility?> = _reviewEligibility.asStateFlow()

    /**
     * Sets or switches the target book context.
     */
    fun setBookContext(bookId: String) {
        if (_currentBookId.value != bookId) {
            // If another session is tracking, flush and finish it first
            if (_activeSession.value.isTracking && _activeSession.value.bookId.isNotBlank()) {
                stopAndSaveSession()
            }
            _currentBookId.value = bookId
            refreshEligibility(bookId)
        }
    }

    /**
     * Begins an active reading session timer and progress tracker.
     */
    fun startSession(
        bookId: String,
        startPage: Int = 1,
        totalPages: Int = 100
    ) {
        if (bookId.isBlank()) return

        // If switching books or restarting
        if (_activeSession.value.isTracking && _activeSession.value.bookId == bookId) {
            // Already tracking current book, ensure not paused
            if (_activeSession.value.isPaused) {
                resumeSession()
            }
            return
        }

        _currentBookId.value = bookId
        val safeTotal = if (totalPages > 0) totalPages else 100
        val safeStart = startPage.coerceIn(1, safeTotal)
        val initialProgress = (safeStart.toFloat() / safeTotal.toFloat()) * 100f

        _activeSession.value = ActiveReadingSessionState(
            bookId = bookId,
            userId = currentUserId.value,
            startPage = safeStart,
            currentPage = safeStart,
            totalPages = safeTotal,
            durationSeconds = 0L,
            sessionStartTime = System.currentTimeMillis(),
            isTracking = true,
            isPaused = false,
            currentProgressPercent = initialProgress,
            sessionDeltaPercent = 0f,
            pagesReadThisSession = 0
        )
        lastRecordedSeconds = 0L
        lastRecordedPage = safeStart

        startTimerCoroutines()
        refreshEligibility(bookId)
    }

    /**
     * Updates the reader's current page in real time and computes progress.
     */
    fun updateCurrentPage(newPage: Int) {
        val current = _activeSession.value
        if (!current.isTracking) return

        val safePage = newPage.coerceIn(1, current.totalPages)
        val newProgress = (safePage.toFloat() / current.totalPages.toFloat()) * 100f
        val deltaProgress = (newProgress - ((current.startPage.toFloat() / current.totalPages.toFloat()) * 100f)).coerceAtLeast(0f)
        val pagesRead = maxOf(0, safePage - current.startPage)

        _activeSession.value = current.copy(
            currentPage = safePage,
            currentProgressPercent = newProgress,
            sessionDeltaPercent = deltaProgress,
            pagesReadThisSession = pagesRead
        )

        // Check milestones
        checkMilestones(newProgress)
    }

    /**
     * Pauses active reading tracking timer (e.g. when app goes into background or menu opens).
     */
    fun pauseSession() {
        val current = _activeSession.value
        if (current.isTracking && !current.isPaused) {
            _activeSession.value = current.copy(isPaused = true)
        }
    }

    /**
     * Resumes active reading tracking timer.
     */
    fun resumeSession() {
        val current = _activeSession.value
        if (current.isTracking && current.isPaused) {
            _activeSession.value = current.copy(isPaused = false)
        }
    }

    /**
     * Toggles play/pause state of the active reading session.
     */
    fun togglePause() {
        val current = _activeSession.value
        if (current.isTracking) {
            _activeSession.value = current.copy(isPaused = !current.isPaused)
        }
    }

    /**
     * Stops the active reading session, calculates duration, and saves the session
     * into the Room database via the repository, triggering verification evaluation.
     */
    fun stopAndSaveSession(onComplete: (ReaderVerification?) -> Unit = {}) {
        val session = _activeSession.value
        if (!session.isTracking || session.durationSeconds < 2) {
            // Reset state if too brief (< 2 seconds)
            timerJob?.cancel()
            autoSaveJob?.cancel()
            _activeSession.value = ActiveReadingSessionState()
            onComplete(null)
            return
        }

        timerJob?.cancel()
        autoSaveJob?.cancel()

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val durationToRecord = session.durationSeconds
                val startPage = session.startPage
                val endPage = session.currentPage
                val totalPages = session.totalPages
                val userId = currentUserId.value
                val bookId = session.bookId

                val result = withContext(Dispatchers.IO) {
                    reviewRepository.recordReadingSession(
                        userId = userId,
                        bookId = bookId,
                        durationSeconds = durationToRecord,
                        startPage = startPage,
                        endPage = endPage,
                        totalPages = totalPages
                    )
                }

                when (result) {
                    is Resource.Success -> {
                        val verification = result.data
                        _events.emit(
                            ReadingSessionEvent.SessionSaved(
                                durationSeconds = durationToRecord,
                                pagesRead = maxOf(1, endPage - startPage),
                                newProgressPercent = session.currentProgressPercent,
                                isVerified = verification.isVerified
                            )
                        )
                        if (verification.isVerified) {
                            _events.emit(
                                ReadingSessionEvent.VerificationStatusUpdated(
                                    isVerified = true,
                                    reason = verification.verificationReason
                                )
                            )
                        }
                        refreshEligibility(bookId)
                        onComplete(verification)
                    }
                    is Resource.Error -> {
                        _events.emit(ReadingSessionEvent.Error(result.message ?: "Failed to save reading session"))
                        onComplete(null)
                    }
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _events.emit(ReadingSessionEvent.Error(e.message ?: "Error saving reading session"))
                }
                onComplete(null)
            } finally {
                _isSaving.value = false
                _activeSession.value = ActiveReadingSessionState()
            }
        }
    }

    /**
     * Flushes incremental progress to database without stopping the timer session.
     * Useful for periodic background sync or when reader changes chapters.
     */
    fun flushIncrementalProgress() {
        val session = _activeSession.value
        if (!session.isTracking) return

        val unrecordedDuration = session.durationSeconds - lastRecordedSeconds
        val currentPage = session.currentPage

        if (unrecordedDuration >= 15 || currentPage != lastRecordedPage) {
            val start = lastRecordedPage
            lastRecordedSeconds = session.durationSeconds
            lastRecordedPage = currentPage

            viewModelScope.launch(Dispatchers.IO) {
                reviewRepository.recordReadingSession(
                    userId = currentUserId.value,
                    bookId = session.bookId,
                    durationSeconds = maxOf(1L, unrecordedDuration),
                    startPage = start,
                    endPage = currentPage,
                    totalPages = session.totalPages
                )
                refreshEligibility(session.bookId)
            }
        }
    }

    /**
     * Refreshes reader verification and review eligibility status from the database.
     */
    fun refreshEligibility(bookId: String = _currentBookId.value) {
        if (bookId.isBlank()) return
        viewModelScope.launch {
            val userId = currentUserId.value
            val result = reviewRepository.checkReviewEligibility(userId, bookId)
            _reviewEligibility.value = result
        }
    }

    private fun startTimerCoroutines() {
        timerJob?.cancel()
        autoSaveJob?.cancel()

        // 1. Seconds Increment Timer Job
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val current = _activeSession.value
                if (current.isTracking && !current.isPaused) {
                    _activeSession.value = current.copy(
                        durationSeconds = current.durationSeconds + 1
                    )
                }
            }
        }

        // 2. Periodic Auto-Save Heartbeat Job (every 45 seconds)
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(45_000L)
                flushIncrementalProgress()
            }
        }
    }

    private var lastMilestoneAnnounced = 0
    private fun checkMilestones(progress: Float) {
        val currentMilestone = when {
            progress >= 100f -> 100
            progress >= 75f -> 75
            progress >= 50f -> 50
            progress >= 25f -> 25
            progress >= 10f -> 10
            else -> 0
        }

        if (currentMilestone > lastMilestoneAnnounced && currentMilestone > 0) {
            lastMilestoneAnnounced = currentMilestone
            viewModelScope.launch {
                val message = when (currentMilestone) {
                    10 -> "Reached 10%! Verified Reader status is being evaluated."
                    25 -> "Quarter of the way through! 25% completed."
                    50 -> "Halfway milestone achieved! 50% completed."
                    75 -> "Almost there! 75% completed."
                    100 -> "Congratulations! You finished the book!"
                    else -> "Progress milestone: $currentMilestone%"
                }
                _events.emit(ReadingSessionEvent.ReadingMilestoneReached(currentMilestone, message))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Save any ongoing session before ViewModel destruction
        val session = _activeSession.value
        if (session.isTracking && session.durationSeconds >= 5) {
            timerJob?.cancel()
            autoSaveJob?.cancel()
            // Launch on IO to ensure final session is stored
            viewModelScope.launch(Dispatchers.IO) {
                reviewRepository.recordReadingSession(
                    userId = currentUserId.value,
                    bookId = session.bookId,
                    durationSeconds = session.durationSeconds,
                    startPage = session.startPage,
                    endPage = session.currentPage,
                    totalPages = session.totalPages
                )
            }
        }
    }
}
