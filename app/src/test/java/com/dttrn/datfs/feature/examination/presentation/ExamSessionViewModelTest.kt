package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import com.dttrn.datfs.core.tts.TtsManager
import com.dttrn.datfs.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExamSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var flashcardRepository: FlashcardRepository
    private lateinit var reviewRepository: ReviewRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var ttsManager: TtsManager

    private fun createCards(count: Int): List<Flashcard> {
        return (1..count).map { i ->
            Flashcard(
                id = "card$i",
                deckId = "deck1",
                frontText = "Question $i",
                backText = "Answer $i",
                imagePath = null,
                pronunciation = null,
                exampleSentence = null,
                note = null,
                difficultyLevel = 2,
                orderIndex = i,
                easeFactor = 2.5f,
                intervalDays = 0,
                repetitionCount = 0,
                dueDate = null,
                failureStreak = 0,
                lastReviewedAt = null,
                isKnown = false,
            )
        }
    }

    private fun createViewModel(
        questionCount: Int = 10,
        questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
        timeLimitMinutes: Int = -1,
        writeDirection: WriteDirection = WriteDirection.BACK,
        totalCards: Int = 20,
    ): ExamSessionViewModel {
        val cards = createCards(totalCards)
        every { flashcardRepository.getCardsByDeck("deck1") } returns flowOf(cards)
        every { savedStateHandle.get<String>(Screen.ExamSession.ARG_DECK_ID) } returns "deck1"
        every { savedStateHandle.get<Int>(Screen.ExamSession.ARG_QUESTION_COUNT) } returns questionCount
        every { savedStateHandle.get<String>(Screen.ExamSession.ARG_QUESTION_TYPE) } returns questionType.name
        every { savedStateHandle.get<Int>(Screen.ExamSession.ARG_TIME_LIMIT_MINUTES) } returns timeLimitMinutes
        every { savedStateHandle.get<String>(Screen.ExamSession.ARG_WRITE_DIRECTION) } returns writeDirection.name

        return ExamSessionViewModel(savedStateHandle, flashcardRepository, reviewRepository, ttsManager)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        flashcardRepository = mockk(relaxed = true)
        reviewRepository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)
        ttsManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `generates correct number of questions`() = runTest {
        val vm = createViewModel(questionCount = 10, totalCards = 20)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(10, state.totalQuestions)
        assertEquals(10, state.questions.size)
    }

    @Test
    fun `multiple choice questions have 4 options`() = runTest {
        val vm = createViewModel(questionCount = 5, questionType = QuestionType.MULTIPLE_CHOICE, totalCards = 10)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.questions.forEach { question ->
            assertEquals(QuestionType.MULTIPLE_CHOICE, question.questionType)
            assertEquals(4, question.options.size)
        }
    }

    @Test
    fun `write questions have empty options`() = runTest {
        val vm = createViewModel(questionCount = 5, questionType = QuestionType.WRITE, totalCards = 10)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.questions.forEach { question ->
            assertEquals(QuestionType.WRITE, question.questionType)
            assertTrue(question.options.isEmpty())
        }
    }

    @Test
    fun `dictation questions have empty options`() = runTest {
        val vm = createViewModel(questionCount = 5, questionType = QuestionType.DICTATION, totalCards = 10)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.questions.forEach { question ->
            assertEquals(QuestionType.DICTATION, question.questionType)
            assertTrue(question.options.isEmpty())
        }
    }

    @Test
    fun `mixed mode only assigns MC or WRITE, never DICTATION`() = runTest {
        val vm = createViewModel(questionCount = 10, questionType = QuestionType.MIXED, totalCards = 20)
        advanceUntilIdle()

        val state = vm.uiState.value
        state.questions.forEach { question ->
            assertTrue(question.questionType == QuestionType.MULTIPLE_CHOICE || question.questionType == QuestionType.WRITE)
        }
    }

    @Test
    fun `initial writeDirection from savedStateHandle`() = runTest {
        val vm = createViewModel(questionCount = 5, writeDirection = WriteDirection.FRONT, totalCards = 10)
        advanceUntilIdle()

        assertEquals(WriteDirection.FRONT, vm.uiState.value.writeDirection)
    }

    @Test
    fun `onToggleWriteDirection switches direction`() = runTest {
        val vm = createViewModel(questionCount = 5, totalCards = 10)
        advanceUntilIdle()

        assertEquals(WriteDirection.BACK, vm.uiState.value.writeDirection)
        vm.onToggleWriteDirection()
        assertEquals(WriteDirection.FRONT, vm.uiState.value.writeDirection)
        vm.onToggleWriteDirection()
        assertEquals(WriteDirection.BACK, vm.uiState.value.writeDirection)
    }

    @Test
    fun `onSelectAnswer stores user answer`() = runTest {
        val vm = createViewModel(questionCount = 5, totalCards = 10)
        advanceUntilIdle()

        val firstOption = vm.uiState.value.questions[0].options.first()
        vm.onSelectAnswer(firstOption)

        val updatedQuestion = vm.uiState.value.questions[0]
        assertEquals(firstOption, updatedQuestion.userAnswer)
    }

    @Test
    fun `onWriteAnswerChange stores user answer`() = runTest {
        val vm = createViewModel(questionCount = 5, questionType = QuestionType.WRITE, totalCards = 10)
        advanceUntilIdle()

        vm.onWriteAnswerChange("test answer")

        val updatedQuestion = vm.uiState.value.questions[0]
        assertEquals("test answer", updatedQuestion.userAnswer)
    }

    @Test
    fun `onNextQuestion advances index`() = runTest {
        val vm = createViewModel(questionCount = 5, totalCards = 10)
        advanceUntilIdle()

        assertEquals(0, vm.uiState.value.currentIndex)
        vm.onNextQuestion()
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `onPreviousQuestion goes back`() = runTest {
        val vm = createViewModel(questionCount = 5, totalCards = 10)
        advanceUntilIdle()

        vm.onNextQuestion()
        vm.onNextQuestion()
        assertEquals(2, vm.uiState.value.currentIndex)

        vm.onPreviousQuestion()
        assertEquals(1, vm.uiState.value.currentIndex)
    }

    @Test
    fun `isLastQuestion is true on last question`() = runTest {
        val vm = createViewModel(questionCount = 2, totalCards = 5)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLastQuestion)
        vm.onNextQuestion()
        assertTrue(vm.uiState.value.isLastQuestion)
    }
}
