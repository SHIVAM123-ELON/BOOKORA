package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AuthorAiPromptType
import com.example.domain.model.AuthorAiResult
import com.example.domain.model.BookFilter
import com.example.domain.model.BookRecommendation
import com.example.domain.model.BookSummary
import com.example.domain.model.ExplanationMode
import com.example.domain.model.RecommendationEventType
import com.example.domain.model.SemanticSearchResult
import com.example.domain.model.StudyDeck
import com.example.domain.model.StudyQuestion
import com.example.domain.model.UiState
import com.example.domain.usecase.AskReadingAssistantUseCase
import com.example.domain.usecase.AuthorWritingAiUseCase
import com.example.domain.usecase.GenerateStudyDeckUseCase
import com.example.domain.usecase.GetBookSummaryUseCase
import com.example.domain.usecase.GetChapterSummaryUseCase
import com.example.domain.usecase.GetPersonalizedRecommendationsUseCase
import com.example.domain.usecase.SemanticSearchUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 1. Reading Assistant ViewModel
class AiReadingAssistantViewModel(
    private val askReadingAssistantUseCase: AskReadingAssistantUseCase
) : ViewModel() {

    private val _explanationMode = MutableStateFlow(ExplanationMode.SIMPLE)
    val explanationMode: StateFlow<ExplanationMode> = _explanationMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _contextSnippet = MutableStateFlow<String?>(null)
    val contextSnippet: StateFlow<String?> = _contextSnippet.asStateFlow()

    fun setExplanationMode(mode: ExplanationMode) {
        _explanationMode.value = mode
    }

    fun setContextSnippet(snippet: String?) {
        _contextSnippet.value = snippet
    }

    fun getConversationHistory(userId: String, bookId: String): StateFlow<List<AiChatMessage>> {
        return askReadingAssistantUseCase.getHistory(userId, bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun sendMessage(
        userId: String,
        bookId: String,
        prompt: String,
        onSuccess: () -> Unit = {}
    ) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = askReadingAssistantUseCase(
                userId = userId,
                bookId = bookId,
                question = prompt,
                contextSnippet = _contextSnippet.value,
                mode = _explanationMode.value
            )
            _isLoading.value = false
            if (result.isSuccess) {
                onSuccess()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to get AI response"
            }
        }
    }

    fun clearChat(userId: String, bookId: String) {
        viewModelScope.launch {
            askReadingAssistantUseCase.clearHistory(userId, bookId)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

// 2. Book & Chapter Summary ViewModel
class AiSummaryViewModel(
    private val getBookSummaryUseCase: GetBookSummaryUseCase,
    private val getChapterSummaryUseCase: GetChapterSummaryUseCase
) : ViewModel() {

    private val _summaryState = MutableStateFlow<UiState<BookSummary>>(UiState.Loading)
    val summaryState: StateFlow<UiState<BookSummary>> = _summaryState.asStateFlow()

    fun loadBookSummary(userId: String, bookId: String) {
        viewModelScope.launch {
            _summaryState.value = UiState.Loading
            val result = getBookSummaryUseCase(userId, bookId)
            result.onSuccess {
                _summaryState.value = UiState.Success(it)
            }.onFailure {
                _summaryState.value = UiState.Error(it.message ?: "Failed to generate book summary", it)
            }
        }
    }

    fun loadChapterSummary(userId: String, bookId: String, chapterTitle: String, content: String) {
        viewModelScope.launch {
            _summaryState.value = UiState.Loading
            val result = getChapterSummaryUseCase(userId, bookId, chapterTitle, content)
            result.onSuccess {
                _summaryState.value = UiState.Success(it)
            }.onFailure {
                _summaryState.value = UiState.Error(it.message ?: "Failed to generate chapter summary", it)
            }
        }
    }
}

// 3. AI Study Mode ViewModel
class StudyModeViewModel(
    private val generateStudyDeckUseCase: GenerateStudyDeckUseCase
) : ViewModel() {

    private val _deckState = MutableStateFlow<UiState<StudyDeck>>(UiState.Loading)
    val deckState: StateFlow<UiState<StudyDeck>> = _deckState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, Int>> = _userAnswers.asStateFlow()

    private val _isAnswerSubmitted = MutableStateFlow(false)
    val isAnswerSubmitted: StateFlow<Boolean> = _isAnswerSubmitted.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _currentFlashcardIndex = MutableStateFlow(0)
    val currentFlashcardIndex: StateFlow<Int> = _currentFlashcardIndex.asStateFlow()

    private val _isFlashcardFlipped = MutableStateFlow(false)
    val isFlashcardFlipped: StateFlow<Boolean> = _isFlashcardFlipped.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Quiz, 1: Flashcards, 2: Revision Notes
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun loadDeck(userId: String, bookId: String, chapterTitle: String, content: String) {
        viewModelScope.launch {
            _deckState.value = UiState.Loading
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _isAnswerSubmitted.value = false
            _score.value = 0
            _currentFlashcardIndex.value = 0
            _isFlashcardFlipped.value = false

            val result = generateStudyDeckUseCase(userId, bookId, chapterTitle, content)
            result.onSuccess {
                _deckState.value = UiState.Success(it)
            }.onFailure {
                _deckState.value = UiState.Error(it.message ?: "Failed to generate study deck", it)
            }
        }
    }

    fun selectOption(questionIndex: Int, optionIndex: Int) {
        if (!_isAnswerSubmitted.value) {
            _userAnswers.value = _userAnswers.value.toMutableMap().apply {
                put(questionIndex, optionIndex)
            }
        }
    }

    fun submitAnswer(deck: StudyDeck) {
        if (_isAnswerSubmitted.value) return
        val currentQ = deck.questions.getOrNull(_currentQuestionIndex.value) ?: return
        val chosen = _userAnswers.value[_currentQuestionIndex.value] ?: return
        _isAnswerSubmitted.value = true
        if (chosen == currentQ.correctAnswerIndex) {
            _score.value += 1
        }
    }

    fun nextQuestion(deck: StudyDeck) {
        if (_currentQuestionIndex.value < deck.questions.size - 1) {
            _currentQuestionIndex.value += 1
            _isAnswerSubmitted.value = false
        }
    }

    fun flipCard() {
        _isFlashcardFlipped.value = !_isFlashcardFlipped.value
    }

    fun nextFlashcard(deck: StudyDeck) {
        if (_currentFlashcardIndex.value < deck.flashcards.size - 1) {
            _currentFlashcardIndex.value += 1
            _isFlashcardFlipped.value = false
        }
    }

    fun prevFlashcard() {
        if (_currentFlashcardIndex.value > 0) {
            _currentFlashcardIndex.value -= 1
            _isFlashcardFlipped.value = false
        }
    }

    fun resetQuiz() {
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _isAnswerSubmitted.value = false
        _score.value = 0
    }
}

// 4. AI Recommendations ViewModel
class AiRecommendationsViewModel(
    private val recommendationsUseCase: GetPersonalizedRecommendationsUseCase
) : ViewModel() {

    fun getRecommendations(userId: String): StateFlow<UiState<List<BookRecommendation>>> {
        return recommendationsUseCase(userId)
            .map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { emit(UiState.Error(it.message ?: "Failed to load recommendations", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)
    }

    fun trackEvent(userId: String, bookId: String, type: RecommendationEventType, metadata: String? = null) {
        viewModelScope.launch {
            recommendationsUseCase.recordEvent(userId, bookId, type, metadata)
        }
    }
}

// 5. Author AI Assistant ViewModel
class AuthorAiAssistantViewModel(
    private val authorWritingAiUseCase: AuthorWritingAiUseCase
) : ViewModel() {

    private val _selectedType = MutableStateFlow(AuthorAiPromptType.BOOK_DESCRIPTION)
    val selectedType: StateFlow<AuthorAiPromptType> = _selectedType.asStateFlow()

    private val _aiResultState = MutableStateFlow<UiState<AuthorAiResult>?>(null)
    val aiResultState: StateFlow<UiState<AuthorAiResult>?> = _aiResultState.asStateFlow()

    fun selectType(type: AuthorAiPromptType) {
        _selectedType.value = type
    }

    fun generate(userId: String, title: String, context: String, audience: String?) {
        viewModelScope.launch {
            _aiResultState.value = UiState.Loading
            val result = authorWritingAiUseCase(
                userId = userId,
                type = _selectedType.value,
                title = title,
                context = context,
                targetAudience = audience
            )
            result.onSuccess {
                _aiResultState.value = UiState.Success(it)
            }.onFailure {
                _aiResultState.value = UiState.Error(it.message ?: "Failed to generate author assistance", it)
            }
        }
    }

    fun clearResult() {
        _aiResultState.value = null
    }
}

// 6. Semantic Search ViewModel
class SemanticSearchViewModel(
    private val semanticSearchUseCase: SemanticSearchUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(BookFilter())
    val filter: StateFlow<BookFilter> = _filter.asStateFlow()

    val searchResults: StateFlow<UiState<List<SemanticSearchResult>>> = kotlinx.coroutines.flow.combine(
        _searchQuery,
        _filter
    ) { query, filter ->
        query to filter
    }.kotlinx.coroutines.flow.flatMapLatest { (query, filter) ->
        semanticSearchUseCase(query, filter)
            .map { list ->
                if (list.isEmpty()) UiState.Empty else UiState.Success(list)
            }
            .catch { emit(UiState.Error(it.message ?: "Semantic search failed", it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: BookFilter) {
        _filter.value = filter
    }

    fun clearQuery() {
        _searchQuery.value = ""
    }
}
