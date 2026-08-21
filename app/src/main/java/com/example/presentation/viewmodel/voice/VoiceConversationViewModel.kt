package com.example.presentation.viewmodel.voice

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.audio.AudioEngine
import com.example.core.ai.voice.GeminiLiveClient
import com.example.core.ai.voice.GeminiLiveEvent
import com.example.domain.model.voice.*
import com.example.domain.repository.BookRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

data class VoiceUiState(
    val connectionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val activePersona: VoicePersona = VoicePersona.DEFAULT,
    val activeMode: VoiceMode = VoiceMode.BOOK_DISCUSS,
    val bookContext: BookVoiceContext? = null,
    val messages: List<VoiceChatMessage> = emptyList(),
    val currentStreamingText: String = "",
    val isMicMuted: Boolean = false,
    val isSpeakerMuted: Boolean = false,
    val audioLevel: Float = 0f,
    val sessionDurationSeconds: Long = 0,
    val errorMessage: String? = null
)

class VoiceConversationViewModel(
    application: Application,
    private val bookRepository: BookRepository
) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine(application)
    private val liveClient = GeminiLiveClient(viewModelScope)

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    private var sessionTimerJob: Job? = null
    private var simulatedWaveJob: Job? = null

    init {
        // Collect live events from Gemini Live API
        viewModelScope.launch {
            liveClient.events.collect { event ->
                when (event) {
                    is GeminiLiveEvent.Connected -> {
                        _uiState.value = _uiState.value.copy(
                            connectionState = LiveConnectionState.CONNECTED
                        )
                        // Speak welcome greeting if transcript is empty
                        if (_uiState.value.messages.isEmpty()) {
                            val greeting = buildInitialGreeting()
                            addAssistantMessage(greeting)
                        }
                    }
                    is GeminiLiveEvent.TextChunk -> {
                        _uiState.value = _uiState.value.copy(
                            connectionState = LiveConnectionState.SPEAKING,
                            currentStreamingText = _uiState.value.currentStreamingText + event.text
                        )
                    }
                    is GeminiLiveEvent.TurnComplete -> {
                        val streamedText = _uiState.value.currentStreamingText.trim()
                        if (streamedText.isNotEmpty()) {
                            addAssistantMessage(streamedText)
                        }
                        _uiState.value = _uiState.value.copy(
                            currentStreamingText = "",
                            connectionState = LiveConnectionState.CONNECTED
                        )
                    }
                    is GeminiLiveEvent.Interrupted -> {
                        audioEngine.stopSpeaking()
                        _uiState.value = _uiState.value.copy(
                            currentStreamingText = "",
                            connectionState = LiveConnectionState.LISTENING
                        )
                    }
                    is GeminiLiveEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            connectionState = LiveConnectionState.ERROR,
                            errorMessage = event.message
                        )
                    }
                    is GeminiLiveEvent.Disconnected -> {
                        _uiState.value = _uiState.value.copy(
                            connectionState = LiveConnectionState.DISCONNECTED
                        )
                    }
                    is GeminiLiveEvent.AudioChunk -> {
                        // Handled in audio playback
                    }
                }
            }
        }

        // Collect mic audio levels from AudioEngine
        viewModelScope.launch {
            audioEngine.audioLevel.collect { level ->
                if (_uiState.value.connectionState == LiveConnectionState.LISTENING) {
                    _uiState.value = _uiState.value.copy(audioLevel = level)
                }
            }
        }
    }

    fun startSession(bookId: String? = null) {
        _uiState.value = _uiState.value.copy(
            connectionState = LiveConnectionState.CONNECTING,
            sessionDurationSeconds = 0,
            errorMessage = null
        )

        // Start timer
        startSessionTimer()

        // Load book context if provided
        if (!bookId.isNullOrBlank()) {
            viewModelScope.launch {
                val book = bookRepository.getBookById(bookId).firstOrNull()
                if (book != null) {
                    val context = BookVoiceContext(
                        bookId = book.id,
                        title = book.title,
                        author = book.authorName,
                        coverUrl = book.coverUrl,
                        currentChapter = "Chapter 1: The Beginning",
                        readingProgressPercent = 25,
                        synopsis = book.description
                    )
                    _uiState.value = _uiState.value.copy(bookContext = context)
                    liveClient.updateBookContext(context)
                }
                connectLiveApi()
            }
        } else {
            connectLiveApi()
        }
    }

    private fun connectLiveApi() {
        liveClient.startSession(
            mode = _uiState.value.activeMode,
            persona = _uiState.value.activePersona,
            bookContext = _uiState.value.bookContext
        )
    }

    private fun buildInitialGreeting(): String {
        val book = _uiState.value.bookContext
        val persona = _uiState.value.activePersona
        return if (book != null) {
            "Hi there! I'm ${persona.name}, your Bookora Voice Companion. I see we're reading \"${book.title}\" by ${book.author}. What would you like to explore or discuss?"
        } else {
            persona.previewGreeting
        }
    }

    /**
     * Toggles continuous speech listening.
     */
    fun toggleListening() {
        if (_uiState.value.connectionState == LiveConnectionState.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        if (_uiState.value.isMicMuted) return

        // If assistant was speaking, interrupt it
        if (_uiState.value.connectionState == LiveConnectionState.SPEAKING) {
            audioEngine.stopSpeaking()
            stopSimulatedWave()
        }

        _uiState.value = _uiState.value.copy(connectionState = LiveConnectionState.LISTENING)

        audioEngine.startListening(
            onResult = { userSpokenText ->
                _uiState.value = _uiState.value.copy(
                    connectionState = LiveConnectionState.THINKING,
                    audioLevel = 0.1f
                )
                addUserMessage(userSpokenText)
                liveClient.sendUserMessage(userSpokenText)
            },
            onPartial = { partialText ->
                // Visual feedback during speech
            },
            onError = { errMsg ->
                _uiState.value = _uiState.value.copy(
                    connectionState = LiveConnectionState.CONNECTED,
                    audioLevel = 0f
                )
            }
        )
    }

    fun stopListening() {
        audioEngine.stopListening()
        _uiState.value = _uiState.value.copy(
            connectionState = LiveConnectionState.CONNECTED,
            audioLevel = 0f
        )
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        addUserMessage(text)
        _uiState.value = _uiState.value.copy(connectionState = LiveConnectionState.THINKING)
        liveClient.sendUserMessage(text)
    }

    private fun addUserMessage(text: String) {
        val msg = VoiceChatMessage(
            id = UUID.randomUUID().toString(),
            sender = VoiceSender.USER,
            text = text
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + msg
        )
    }

    private fun addAssistantMessage(text: String) {
        val msg = VoiceChatMessage(
            id = UUID.randomUUID().toString(),
            sender = VoiceSender.GEMINI_LIVE,
            text = text
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + msg
        )

        // Play audio via TTS if speaker is not muted
        if (!_uiState.value.isSpeakerMuted) {
            startSpeakingWithVisualizer(text)
        }
    }

    private fun startSpeakingWithVisualizer(text: String) {
        _uiState.value = _uiState.value.copy(connectionState = LiveConnectionState.SPEAKING)
        startSimulatedWave()

        val persona = _uiState.value.activePersona
        val pitch = when (persona.id) {
            "charon" -> 0.85f
            "kore" -> 1.15f
            "aoede" -> 1.05f
            "fenrir" -> 0.95f
            "puck" -> 1.25f
            else -> 1.0f
        }

        audioEngine.speak(
            text = text,
            pitch = pitch,
            speechRate = 1.05f,
            onFinished = {
                stopSimulatedWave()
                _uiState.value = _uiState.value.copy(
                    connectionState = LiveConnectionState.CONNECTED,
                    audioLevel = 0f
                )
            }
        )
    }

    private fun startSimulatedWave() {
        simulatedWaveJob?.cancel()
        simulatedWaveJob = viewModelScope.launch {
            var phase = 0f
            while (_uiState.value.connectionState == LiveConnectionState.SPEAKING) {
                phase += 0.3f
                val level = (0.35f + 0.45f * kotlin.math.sin(phase)).coerceIn(0.15f, 0.95f)
                _uiState.value = _uiState.value.copy(audioLevel = level)
                delay(60)
            }
        }
    }

    private fun stopSimulatedWave() {
        simulatedWaveJob?.cancel()
        simulatedWaveJob = null
    }

    fun toggleMicMute() {
        val newMute = !_uiState.value.isMicMuted
        _uiState.value = _uiState.value.copy(isMicMuted = newMute)
        if (newMute) {
            stopListening()
        }
    }

    fun toggleSpeakerMute() {
        val newSpeakerMute = !_uiState.value.isSpeakerMuted
        _uiState.value = _uiState.value.copy(isSpeakerMuted = newSpeakerMute)
        if (newSpeakerMute) {
            audioEngine.stopSpeaking()
            stopSimulatedWave()
        }
    }

    fun selectPersona(persona: VoicePersona) {
        _uiState.value = _uiState.value.copy(activePersona = persona)
        liveClient.updatePersona(persona)
        // Friendly announcement
        addAssistantMessage("Switched voice persona to ${persona.name}. How can I assist you now?")
    }

    fun selectMode(mode: VoiceMode) {
        _uiState.value = _uiState.value.copy(activeMode = mode)
        liveClient.updateMode(mode)
        addAssistantMessage("Mode updated to ${mode.title}. ${mode.subtitle}.")
    }

    fun clearConversation() {
        _uiState.value = _uiState.value.copy(messages = emptyList(), currentStreamingText = "")
        val greeting = buildInitialGreeting()
        addAssistantMessage(greeting)
    }

    fun interruptSpeech() {
        audioEngine.stopSpeaking()
        stopSimulatedWave()
        _uiState.value = _uiState.value.copy(
            connectionState = LiveConnectionState.CONNECTED,
            audioLevel = 0f
        )
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    sessionDurationSeconds = _uiState.value.sessionDurationSeconds + 1
                )
            }
        }
    }

    fun endSession() {
        sessionTimerJob?.cancel()
        stopSimulatedWave()
        audioEngine.stopListening()
        audioEngine.stopSpeaking()
        liveClient.close()
        _uiState.value = _uiState.value.copy(
            connectionState = LiveConnectionState.DISCONNECTED,
            audioLevel = 0f
        )
    }

    override fun onCleared() {
        super.onCleared()
        endSession()
        audioEngine.release()
    }
}
