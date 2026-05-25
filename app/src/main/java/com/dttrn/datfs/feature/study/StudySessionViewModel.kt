package com.dttrn.datfs.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.study.SM2Algorithm
import com.dttrn.datfs.core.domain.study.StudyQueue
import com.dttrn.datfs.core.domain.usecase.study.GetStudyQueueUseCase
import com.dttrn.datfs.core.domain.usecase.study.SubmitReviewUseCase
import com.dttrn.datfs.core.tts.TtsManager
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class StudySessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val getStudyQueueUseCase: GetStudyQueueUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
    val ttsManager: TtsManager,
) : ViewModel() {

    companion object {
        /** Temporary holder for passing results to StudyResultScreen */
        internal var pendingResults: List<CardResult> = emptyList()
        internal var pendingDeckTitle: String = ""
    }

    val deckId: String = checkNotNull(savedStateHandle[Screen.StudySession.ARG_DECK_ID])
    private val modeArg: String = checkNotNull(savedStateHandle[Screen.StudySession.ARG_MODE])
    val mode: StudyMode = runCatching { StudyMode.valueOf(modeArg) }.getOrDefault(StudyMode.SPACED_REPETITION)

    private var queue: StudyQueue? = null
    private var allOriginalCards: List<Flashcard> = emptyList()  // All cards before range filter

    private val _uiState = MutableStateFlow(StudySessionUiState())
    val uiState: StateFlow<StudySessionUiState> = _uiState.asStateFlow()

    init {
        loadDeckAndQueue()
    }

    private fun loadDeckAndQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load deck title
            deckRepository.getDeckById(deckId).first()?.let { deck ->
                _uiState.update { it.copy(deckTitle = deck.title, mode = mode) }
            }

            // Build study queue
            when (val result = getStudyQueueUseCase(deckId, mode)) {
                is com.dttrn.datfs.core.domain.common.Result.Success -> {
                    queue = result.data
                    allOriginalCards = result.data.allCards
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalCount = result.data.totalCount,
                            originalTotalCount = result.data.totalCount,
                        )
                    }
                    if (mode == StudyMode.MATCH) {
                        // MATCH: setup all cards at once, don't use card-by-card flow
                        setupMatchMode(result.data.allCards)
                    } else {
                        loadNextCard()
                    }
                }
                is com.dttrn.datfs.core.domain.common.Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.exception.message ?: "Có lỗi xảy ra")
                    }
                }
                else -> Unit
            }
        }
    }

    private fun loadNextCard() {
        val q = queue ?: return
        if (mode == StudyMode.MATCH) return  // MATCH handles its own state
        val next = q.peek()
        if (next == null) {
            // Store results before completing so StudyResult can read them
            pendingResults = _uiState.value.sessionResults
            pendingDeckTitle = _uiState.value.deckTitle
            _uiState.update { it.copy(isComplete = true) }
            return
        }
        _uiState.update { state ->
            state.copy(
                currentCard = next,
                currentIndex = q.masteredCount,
                progress = q.progress,
                isFlipped = false, // always start unflipped; SwipeableCard handles content order via showFrontFirst
                isAnswerRevealed = false,
                selectedAnswer = null,
                isCorrect = null,
                writeAnswer = "",
                isWriteCorrect = null,
                quizOptions = if (mode == StudyMode.QUIZ) emptyList() else state.quizOptions,
            )
        }
        if (mode == StudyMode.QUIZ) generateQuizOptions(next)
    }

    // ===== SWIPE / LEARN ACTIONS =====

    fun onFlipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
    }

    fun onToggleFrontFirst() {
        _uiState.update {
            it.copy(
                showFrontFirst = !it.showFrontFirst,
                isFlipped = false, // reset current card to show correct primary face
            )
        }
    }

    fun onShuffleCards() {
        queue?.shuffle()
        _uiState.update { it.copy(isShuffled = true) }
        loadNextCard()
    }

    /**
     * Called from swipe gesture or rating buttons.
     * rating: SM2Algorithm.Ratings.*
     */
    fun onRateCard(rating: Int) {
        val card = queue?.next() ?: return
        viewModelScope.launch {
            val sm2Result = when (val r = submitReviewUseCase(card, rating)) {
                is com.dttrn.datfs.core.domain.common.Result.Success -> r.data
                else -> SM2Algorithm.calculate(rating, card.easeFactor, card.intervalDays, card.repetitionCount)
            }
            // Requeue if failed — applies to ALL modes (học tới khi nhớ)
            if (rating < 3) {
                queue?.markFailed(card, requeue = true)
            }
            val result = CardResult(card, rating, sm2Result)
            _uiState.update {
                // Deduplicate: keep latest result per card (in case of re-review)
                val newResults = it.sessionResults.toMutableList()
                val idx = newResults.indexOfFirst { r -> r.card.id == card.id }
                if (idx >= 0) newResults[idx] = result else newResults.add(result)
                it.copy(
                    sessionResults = newResults,
                    reviewedCount = (queue?.masteredCount ?: 0),
                )
            }
            loadNextCard()
        }
    }

    // ===== QUIZ ACTIONS =====

    private fun generateQuizOptions(card: Flashcard) {
        viewModelScope.launch {
            // Use full card pool for better distractors (not just reviewed cards)
            val allBackTexts = queue?.allCards?.map { it.backText } ?: emptyList()
            val distractors = allBackTexts
                .filter { it != card.backText }
                .shuffled()
                .take(3)
            // If not enough distractors, pad with dummy options
            val padded = if (distractors.size < 3) {
                distractors + List(3 - distractors.size) { "—" }
            } else distractors
            val options = (padded + card.backText).shuffled()
            _uiState.update { it.copy(quizOptions = options) }
        }
    }

    fun onSelectQuizAnswer(answer: String) {
        if (_uiState.value.isAnswerRevealed) return
        val card = _uiState.value.currentCard ?: return
        val correct = answer.equals(card.backText, ignoreCase = true)
        _uiState.update {
            it.copy(
                selectedAnswer = answer,
                isAnswerRevealed = true,
                isCorrect = correct,
            )
        }
        // Auto-advance after delay
        viewModelScope.launch {
            delay(1200)
            onRateCard(SM2Algorithm.Ratings.fromQuizAnswer(correct))
        }
    }

    // ===== WRITE ACTIONS =====

    fun onWriteAnswerChange(answer: String) {
        _uiState.update { it.copy(writeAnswer = answer) }
    }

    fun onSubmitWriteAnswer() {
        if (_uiState.value.isAnswerRevealed) return
        val card = _uiState.value.currentCard ?: return
        val answer = _uiState.value.writeAnswer.trim()
        val correct = answer.equals(card.backText.trim(), ignoreCase = true)
        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                isWriteCorrect = correct,
                isCorrect = correct,
            )
        }
    }

    fun onWriteAdvance() {
        val correct = _uiState.value.isWriteCorrect ?: false
        onRateCard(SM2Algorithm.Ratings.fromQuizAnswer(correct))
    }

    // ===== MATCH MODE =====

    fun setupMatchMode(allCards: List<Flashcard>) {
        if (allCards.isEmpty()) {
            _uiState.update { it.copy(error = "Deck không có thẻ nào để ghép đôi") }
            return
        }
        val take = minOf(allCards.size, 6) // max 6 pairs = 12 items
        val selected = allCards.shuffled().take(take)
        val items = mutableListOf<MatchItem>()
        selected.forEach { card ->
            val pairId = card.id
            items.add(MatchItem(UUID.randomUUID().toString(), card.frontText, MatchItemType.FRONT, pairId))
            items.add(MatchItem(UUID.randomUUID().toString(), card.backText, MatchItemType.BACK, pairId))
        }
        _uiState.update {
            it.copy(
                matchItems = items.shuffled(),
                totalCount = take,    // number of pairs
                isLoading = false,
            )
        }
    }

    fun onMatchItemClick(itemId: String) {
        val state = _uiState.value
        val item = state.matchItems.find { it.id == itemId } ?: return
        if (item.isMatched || item.isError) return

        val selected = state.matchItems.find { it.isSelected }

        if (selected == null) {
            // First selection — highlight
            _uiState.update {
                it.copy(matchItems = it.matchItems.map { m ->
                    m.copy(isSelected = m.id == itemId, isError = false)
                })
            }
        } else if (selected.id == itemId) {
            // Tap same item — deselect
            _uiState.update {
                it.copy(matchItems = it.matchItems.map { m -> m.copy(isSelected = false) })
            }
        } else {
            // Second selection — check match
            val isMatch = selected.cardId == item.cardId && selected.type != item.type
            if (isMatch) {
                val newItems = state.matchItems.map { m ->
                    if (m.id == itemId || m.id == selected.id)
                        m.copy(isSelected = false, isMatched = true, isError = false)
                    else m
                }
                val allMatched = newItems.all { it.isMatched }
                _uiState.update { it.copy(matchItems = newItems) }

                if (allMatched) {
                    viewModelScope.launch {
                        delay(600) // brief pause to show all-green state
                        // Record results for each matched pair
                        val matchedCards = queue?.allCards
                            ?.filter { c -> newItems.any { m -> m.cardId == c.id && m.isMatched } }
                            ?: emptyList()
                        val results = matchedCards.map { c ->
                            CardResult(
                                card = c,
                                rating = SM2Algorithm.Ratings.GOOD,
                                sm2Result = SM2Algorithm.calculate(
                                    SM2Algorithm.Ratings.GOOD, c.easeFactor, c.intervalDays, c.repetitionCount
                                ),
                            )
                        }
                        // Store results before completing
                        pendingResults = results
                        pendingDeckTitle = _uiState.value.deckTitle
                        _uiState.update {
                            it.copy(
                                sessionResults = results,
                                reviewedCount = matchedCards.size,
                                isComplete = true,
                            )
                        }
                    }
                }
            } else {
                // Wrong pair — flash error
                _uiState.update {
                    it.copy(matchItems = it.matchItems.map { m ->
                        if (m.id == itemId || m.id == selected.id)
                            m.copy(isSelected = false, isError = true)
                        else m
                    })
                }
                viewModelScope.launch {
                    delay(600)
                    _uiState.update {
                        it.copy(matchItems = it.matchItems.map { m -> m.copy(isError = false) })
                    }
                }
            }
        }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }

    // ===== TEXT TO SPEECH =====

    /**
     * Phát âm từ tiếng Anh hiện tại.
     * Ưu tiên đọc frontText (từ gốc tiếng Anh) vì TTS đọc IPA không chuẩn.
     */
    fun onSpeakWord() {
        val card = _uiState.value.currentCard ?: return
        ttsManager.speak(card.frontText)
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.stop()
    }

    // ===== RANGE SELECTION =====

    fun onShowRangeDialog() {
        _uiState.update {
            it.copy(
                showRangeDialog = true,
                rangeFrom = it.rangeFrom ?: 1,
                rangeTo = it.rangeTo ?: it.originalTotalCount,
            )
        }
    }

    fun onDismissRangeDialog() {
        _uiState.update { it.copy(showRangeDialog = false) }
    }

    fun onApplyRange(from: Int, to: Int) {
        if (allOriginalCards.isEmpty()) return
        val safeFrom = from.coerceIn(1, allOriginalCards.size)
        val safeTo = to.coerceIn(safeFrom, allOriginalCards.size)
        val rangedCards = allOriginalCards.subList(safeFrom - 1, safeTo) // 1-indexed to 0-indexed

        queue = StudyQueue(rangedCards, mode, shuffled = false)
        _uiState.update {
            it.copy(
                showRangeDialog = false,
                totalCount = rangedCards.size,
                rangeFrom = safeFrom,
                rangeTo = safeTo,
                isRangeApplied = true,
                reviewedCount = 0,
                sessionResults = emptyList(),
                isComplete = false,
                isShuffled = false,
            )
        }
        if (mode == StudyMode.MATCH) {
            setupMatchMode(rangedCards)
        } else {
            loadNextCard()
        }
    }

    fun onClearRange() {
        if (allOriginalCards.isEmpty()) return
        queue = StudyQueue(allOriginalCards, mode, shuffled = mode != StudyMode.LEARN)
        _uiState.update {
            it.copy(
                showRangeDialog = false,
                totalCount = allOriginalCards.size,
                rangeFrom = null,
                rangeTo = null,
                isRangeApplied = false,
                reviewedCount = 0,
                sessionResults = emptyList(),
                isComplete = false,
                isShuffled = false,
            )
        }
        if (mode == StudyMode.MATCH) {
            setupMatchMode(allOriginalCards)
        } else {
            loadNextCard()
        }
    }
}
