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
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.StudySession.ARG_DECK_ID])
    private val modeArg: String = checkNotNull(savedStateHandle[Screen.StudySession.ARG_MODE])
    val mode: StudyMode = runCatching { StudyMode.valueOf(modeArg) }.getOrDefault(StudyMode.SPACED_REPETITION)

    private var queue: StudyQueue? = null

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalCount = result.data.totalCount,
                        )
                    }
                    loadNextCard()
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
        val next = q.peek()
        if (next == null) {
            _uiState.update { it.copy(isComplete = true) }
            return
        }
        _uiState.update { state ->
            state.copy(
                currentCard = next,
                currentIndex = q.reviewedCount,
                progress = q.progress,
                isFlipped = false,
                isAnswerRevealed = false,
                selectedAnswer = null,
                isCorrect = null,
                writeAnswer = "",
                isWriteCorrect = null,
                quizOptions = if (mode == StudyMode.QUIZ) emptyList() else state.quizOptions,
            )
        }
        if (mode == StudyMode.QUIZ) generateQuizOptions(next)
        if (mode == StudyMode.MATCH) { /* handled separately */ }
    }

    // ===== SWIPE / LEARN ACTIONS =====

    fun onFlipCard() {
        _uiState.update { it.copy(isFlipped = !it.isFlipped) }
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
            // Requeue if failed in SM-2 mode
            if (rating < 3 && mode == StudyMode.SPACED_REPETITION) {
                queue?.markFailed(card, requeue = true)
            }
            val result = CardResult(card, rating, sm2Result)
            _uiState.update {
                it.copy(
                    sessionResults = it.sessionResults + result,
                    reviewedCount = (queue?.reviewedCount ?: 0),
                )
            }
            loadNextCard()
        }
    }

    // ===== QUIZ ACTIONS =====

    private fun generateQuizOptions(card: Flashcard) {
        viewModelScope.launch {
            // Get all other cards for distractors
            val allBackTexts = queue?.getReviewedCards()?.map { it.backText } ?: emptyList()
            val distractors = allBackTexts
                .filter { it != card.backText }
                .shuffled()
                .take(3)

            val options = (distractors + card.backText).shuffled()
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
        val take = minOf(allCards.size, 6) // max 6 pairs (12 items)
        val selected = allCards.shuffled().take(take)
        val items = mutableListOf<MatchItem>()
        selected.forEach { card ->
            val pairId = card.id
            items.add(MatchItem(UUID.randomUUID().toString(), card.frontText, MatchItemType.FRONT, pairId))
            items.add(MatchItem(UUID.randomUUID().toString(), card.backText, MatchItemType.BACK, pairId))
        }
        _uiState.update { it.copy(matchItems = items.shuffled()) }
    }

    fun onMatchItemClick(itemId: String) {
        val state = _uiState.value
        val item = state.matchItems.find { it.id == itemId } ?: return
        if (item.isMatched) return

        val selected = state.matchItems.find { it.isSelected }

        if (selected == null) {
            // First selection
            _uiState.update {
                it.copy(matchItems = it.matchItems.map { m ->
                    m.copy(isSelected = m.id == itemId, isError = false)
                })
            }
        } else if (selected.id == itemId) {
            // Deselect
            _uiState.update {
                it.copy(matchItems = it.matchItems.map { m -> m.copy(isSelected = false) })
            }
        } else {
            // Check match
            val isMatch = selected.cardId == item.cardId && selected.type != item.type
            if (isMatch) {
                _uiState.update {
                    it.copy(matchItems = it.matchItems.map { m ->
                        if (m.id == itemId || m.id == selected.id)
                            m.copy(isSelected = false, isMatched = true, isError = false)
                        else m
                    })
                }
                // Check if all matched
                if (_uiState.value.matchItems.all { it.isMatched }) {
                    viewModelScope.launch {
                        delay(500)
                        _uiState.update { it.copy(isComplete = true) }
                    }
                }
            } else {
                // Error flash
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
}
