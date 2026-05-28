package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.model.ReviewSession
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import com.dttrn.datfs.core.domain.study.SM2Algorithm
import com.dttrn.datfs.core.domain.study.StudyQueue
import com.dttrn.datfs.core.tts.TtsManager
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExamSessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val reviewRepository: ReviewRepository,
    val ttsManager: TtsManager,
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.ExamSession.ARG_DECK_ID])
    private val questionCount: Int =
        checkNotNull(savedStateHandle.get<Int>(Screen.ExamSession.ARG_QUESTION_COUNT))
    private val questionTypeArg: String =
        checkNotNull(savedStateHandle[Screen.ExamSession.ARG_QUESTION_TYPE])
    val timeLimitMinutes: Int? =
        savedStateHandle.get<Int>(Screen.ExamSession.ARG_TIME_LIMIT_MINUTES)?.takeIf { it > 0 }
    private val writeDirectionArg: String =
        savedStateHandle.get<String>(Screen.ExamSession.ARG_WRITE_DIRECTION) ?: WriteDirection.BACK.name

    val questionType: QuestionType = runCatching {
        QuestionType.valueOf(questionTypeArg)
    }.getOrDefault(QuestionType.MULTIPLE_CHOICE)

    val initialWriteDirection: WriteDirection = runCatching {
        WriteDirection.valueOf(writeDirectionArg)
    }.getOrDefault(WriteDirection.BACK)

    companion object {
        internal var pendingResultState: ExamResultUiState? = null
    }

    private val _uiState = MutableStateFlow(ExamSessionUiState())
    val uiState: StateFlow<ExamSessionUiState> = _uiState.asStateFlow()

    private var allBackTexts: List<String> = emptyList()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        generateExamQuestions()
    }

    // ─── Question Generation ────────────────────────────────────────────

    private fun generateExamQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val cards = flashcardRepository.getCardsByDeck(deckId).first()
            allBackTexts = cards.map { it.backText }

            val queue = StudyQueue.buildFor(
                cards = cards,
                mode = StudyMode.EXAMINATION,
                dueOnly = false,
                shuffled = true,
                limit = questionCount,
            )

            val questions = queue.allCards.take(questionCount).map { card ->
                val qType = if (questionType == QuestionType.MIXED) {
                    if (kotlin.random.Random.nextBoolean()) QuestionType.MULTIPLE_CHOICE
                    else QuestionType.WRITE
                } else {
                    questionType
                }

                val options = if (qType == QuestionType.MULTIPLE_CHOICE) {
                    generateDistractors(card)
                } else {
                    emptyList()
                }

                ExamQuestion(
                    card = card,
                    questionType = qType,
                    options = options,
                )
            }

            val total = questions.size

            _uiState.update {
                it.copy(
                    isLoading = false,
                    questions = questions,
                    totalQuestions = total,
                    isLastQuestion = total <= 1,
                    timeLimitMinutes = timeLimitMinutes,
                    timeRemainingSeconds = (timeLimitMinutes ?: 0) * 60,
                    writeDirection = initialWriteDirection,
                )
            }
        }
    }

    private fun generateDistractors(card: Flashcard): List<String> {
        val distractors = allBackTexts
            .filter { it != card.backText }
            .shuffled()
            .take(3)
        val padded = if (distractors.size < 3) {
            distractors + List(3 - distractors.size) { "—" }
        } else {
            distractors
        }
        return (padded + card.backText).shuffled()
    }

    // ─── Answer Handling ─────────────────────────────────────────────────

    fun onSelectAnswer(answer: String) {
        _uiState.update { state ->
            val updated = state.questions.toMutableList()
            updated[state.currentIndex] = updated[state.currentIndex].copy(userAnswer = answer)
            state.copy(questions = updated)
        }
    }

    fun onWriteAnswerChange(text: String) {
        _uiState.update { state ->
            val updated = state.questions.toMutableList()
            updated[state.currentIndex] = updated[state.currentIndex].copy(userAnswer = text)
            state.copy(questions = updated)
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────────

    fun onNextQuestion() {
        val state = _uiState.value
        val newIndex = (state.currentIndex + 1).coerceAtMost(state.questions.size - 1)
        _uiState.update {
            it.copy(
                currentIndex = newIndex,
                isLastQuestion = newIndex == state.questions.size - 1,
                dictationPlayCount = 0,
            )
        }
        val nextQuestion = state.questions.getOrNull(newIndex)
        if (nextQuestion?.questionType == QuestionType.DICTATION) {
            speakCurrentWord()
        }
    }

    fun onPreviousQuestion() {
        _uiState.update { state ->
            val newIndex = (state.currentIndex - 1).coerceAtLeast(0)
            state.copy(currentIndex = newIndex, isLastQuestion = false, dictationPlayCount = 0)
        }
    }

    // ─── Write Direction ─────────────────────────────────────────────────

    fun onToggleWriteDirection() {
        _uiState.update { state ->
            val newDirection = when (state.writeDirection) {
                WriteDirection.BACK -> WriteDirection.FRONT
                WriteDirection.FRONT -> WriteDirection.BACK
            }
            state.copy(writeDirection = newDirection)
        }
    }

    // ─── TTS / Dictation ─────────────────────────────────────────────────

    fun onSpeakWord() {
        val state = _uiState.value
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        val text = when (state.writeDirection) {
            WriteDirection.BACK -> question.card.frontText
            WriteDirection.FRONT -> question.card.backText
        }
        ttsManager.speak(text)
    }

    fun onReplayDictation() {
        _uiState.update { state ->
            val updated = state.questions.toMutableList()
            val current = updated[state.currentIndex]
            val newCount = current.dictationPlayCount + 1
            updated[state.currentIndex] = current.copy(dictationPlayCount = newCount)
            state.copy(questions = updated, dictationPlayCount = newCount)
        }
        onSpeakWord()
    }

    private fun speakCurrentWord() {
        val state = _uiState.value
        val question = state.questions.getOrNull(state.currentIndex) ?: return
        if (question.questionType != QuestionType.DICTATION) return
        val text = when (state.writeDirection) {
            WriteDirection.BACK -> question.card.frontText
            WriteDirection.FRONT -> question.card.backText
        }
        ttsManager.speak(text)
        _uiState.update { state ->
            val updated = state.questions.toMutableList()
            val current = updated[state.currentIndex]
            updated[state.currentIndex] = current.copy(dictationPlayCount = 1)
            state.copy(questions = updated, dictationPlayCount = 1)
        }
    }

    fun onStopTts() {
        ttsManager.stop()
    }

    // ─── Input Focus ─────────────────────────────────────────────────────

    fun onInputFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(isWriteInputFocused = focused) }
    }

    // ─── Submission ──────────────────────────────────────────────────────

    fun onSubmitExam(): String {
        val state = _uiState.value
        val questions = state.questions.map { question ->
            val isCorrect = when (question.questionType) {
                QuestionType.MULTIPLE_CHOICE ->
                    question.userAnswer.equals(question.card.backText, ignoreCase = true)
                QuestionType.WRITE ->
                    isWriteAnswerCorrect(question.userAnswer, getExpectedAnswer(question.card, state.writeDirection))
                QuestionType.DICTATION ->
                    isWriteAnswerCorrect(question.userAnswer, getExpectedAnswer(question.card, state.writeDirection))
                QuestionType.MIXED -> {
                    if (question.options.isNotEmpty()) {
                        question.userAnswer.equals(question.card.backText, ignoreCase = true)
                    } else {
                        isWriteAnswerCorrect(question.userAnswer, getExpectedAnswer(question.card, state.writeDirection))
                    }
                }
            }
            question.copy(isCorrect = isCorrect)
        }

        val correctCount = questions.count { it.isCorrect == true }
        val incorrectCount = questions.count { it.isCorrect == false }
        val passed = questions.isNotEmpty() && correctCount.toFloat() / questions.size >= 0.7f
        val timeTaken = ((timeLimitMinutes ?: 0) * 60 - state.timeRemainingSeconds).coerceAtLeast(0)

        val sessionId = UUID.randomUUID().toString()

        viewModelScope.launch {
            questions.forEach { question ->
                val result = flashcardRepository.getCardById(question.card.id)
                if (result != null) {
                    val rating = SM2Algorithm.Ratings.fromQuizAnswer(question.isCorrect == true)
                    val sm2Result = SM2Algorithm.calculate(
                        rating = rating,
                        easeFactor = question.card.easeFactor,
                        interval = question.card.intervalDays,
                        repetition = question.card.repetitionCount,
                        failureStreak = question.card.failureStreak,
                    )
                    flashcardRepository.updateCard(
                        question.card.copy(
                            easeFactor = sm2Result.newEaseFactor,
                            intervalDays = sm2Result.newIntervalDays,
                            repetitionCount = sm2Result.newRepetitionCount,
                            dueDate = sm2Result.newDueDateMs,
                            failureStreak = sm2Result.newFailureStreak,
                            lastReviewedAt = System.currentTimeMillis(),
                        )
                    )
                }
            }

            val reviewSession = ReviewSession(
                id = sessionId,
                deckId = deckId,
                studyMode = StudyMode.EXAMINATION,
                startedAt = System.currentTimeMillis() - timeTaken * 1000L,
                totalCards = questions.size,
                correctCount = correctCount,
                incorrectCount = incorrectCount,
                durationSeconds = timeTaken,
            )
            reviewRepository.saveSessionWithEncodedMode(reviewSession, "EXAMINATION:${questionType.name}:$passed")

            val today = LocalDate.now().format(dateFormatter)
            reviewRepository.recordStudyActivity(
                date = today,
                cardsStudied = questions.size,
                minutesStudied = (timeTaken + 59) / 60,
                correctAnswers = correctCount,
                totalAnswers = questions.size,
            )
        }

        _uiState.update {
            it.copy(questions = questions, isSubmitted = true)
        }

        pendingResultState = ExamResultUiState(
            deckTitle = _uiState.value.deckTitle,
            sessionId = sessionId,
            score = correctCount,
            totalQuestions = questions.size,
            accuracyPercent = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0,
            passed = passed,
            timeTakenSeconds = timeTaken,
            questions = questions,
            previousConfig = "${state.questions.size}|${questionType.name}|${timeLimitMinutes ?: -1}|${state.writeDirection.name}",
        )

        return sessionId
    }

    private fun getExpectedAnswer(card: Flashcard, direction: WriteDirection): String {
        return when (direction) {
            WriteDirection.BACK -> card.backText
            WriteDirection.FRONT -> card.frontText
        }
    }

    private fun isWriteAnswerCorrect(userAnswer: String?, correctAnswer: String): Boolean {
        if (userAnswer.isNullOrBlank()) return false
        return userAnswer.trim().lowercase() == correctAnswer.trim().lowercase()
    }

    // ─── Timer ───────────────────────────────────────────────────────────

    fun startTimer() {
        if (timeLimitMinutes == null) return
        viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isSubmitted) {
                delay(1000L)
                _uiState.update { state ->
                    val newRemaining = (state.timeRemainingSeconds - 1).coerceAtLeast(0)
                    state.copy(
                        timeRemainingSeconds = newRemaining,
                        isTimeWarning = newRemaining in 1..60,
                    )
                }
            }
        }
    }

    // ─── Exit Dialog ─────────────────────────────────────────────────────

    fun onRequestExit() {
        _uiState.update { it.copy(showExitDialog = true) }
    }

    fun onConfirmExit() {
        ttsManager.stop()
        _uiState.update { it.copy(showExitDialog = false, isSubmitted = true) }
    }

    fun onDismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }
}
