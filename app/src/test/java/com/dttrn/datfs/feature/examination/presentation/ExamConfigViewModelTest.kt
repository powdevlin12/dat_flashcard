package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.repository.DeckRepository
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
class ExamConfigViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var deckRepository: DeckRepository
    private lateinit var savedStateHandle: SavedStateHandle

    private fun createViewModel(
        previousConfig: String? = null,
        totalCards: Int = 20,
        deckTitle: String = "Test Deck",
    ): ExamConfigViewModel {
        val deck = Deck(
            id = "deck1",
            title = deckTitle,
            cardCount = totalCards,
        )
        every { deckRepository.getDeckById("deck1") } returns flowOf(deck)
        every { savedStateHandle.get<String>(Screen.ExamConfig.ARG_DECK_ID) } returns "deck1"
        every { savedStateHandle.get<String>(Screen.ExamConfig.ARG_PREVIOUS_CONFIG) } returns previousConfig

        return ExamConfigViewModel(savedStateHandle, deckRepository)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        deckRepository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `canStart is false when totalCards less than 5`() = runTest {
        val vm = createViewModel(totalCards = 3)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertFalse(state.canStart)
        assertEquals("Cần ít nhất 5 thẻ để tạo bài kiểm tra", state.error)
    }

    @Test
    fun `canStart is true when totalCards at least 5`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state.canStart)
        assertNull(state.error)
    }

    @Test
    fun `questionCount is clamped to range 5 to totalCards`() = runTest {
        val vm = createViewModel(totalCards = 15)
        advanceUntilIdle()

        vm.onQuestionCountChange(2)
        assertEquals(5, vm.uiState.value.questionCount)

        vm.onQuestionCountChange(20)
        assertEquals(15, vm.uiState.value.questionCount)

        vm.onQuestionCountChange(10)
        assertEquals(10, vm.uiState.value.questionCount)
    }

    @Test
    fun `timeLimitMinutes only accepts valid presets`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()

        vm.onTimeLimitChange(5)
        assertEquals(5, vm.uiState.value.timeLimitMinutes)

        vm.onTimeLimitChange(null)
        assertNull(vm.uiState.value.timeLimitMinutes)

        vm.onTimeLimitChange(7)
        assertNull(vm.uiState.value.timeLimitMinutes)
    }

    @Test
    fun `onQuestionTypeChange updates questionType`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()

        vm.onQuestionTypeChange(QuestionType.WRITE)
        assertEquals(QuestionType.WRITE, vm.uiState.value.questionType)

        vm.onQuestionTypeChange(QuestionType.MIXED)
        assertEquals(QuestionType.MIXED, vm.uiState.value.questionType)

        vm.onQuestionTypeChange(QuestionType.DICTATION)
        assertEquals(QuestionType.DICTATION, vm.uiState.value.questionType)
    }

    @Test
    fun `onWriteDirectionChange updates writeDirection`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()

        assertEquals(WriteDirection.BACK, vm.uiState.value.writeDirection)

        vm.onWriteDirectionChange(WriteDirection.FRONT)
        assertEquals(WriteDirection.FRONT, vm.uiState.value.writeDirection)
    }

    @Test
    fun `previousConfig restores with writeDirection`() = runTest {
        val vm = createViewModel(
            totalCards = 20,
            previousConfig = "15|WRITE|10|FRONT",
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(15, state.questionCount)
        assertEquals(QuestionType.WRITE, state.questionType)
        assertEquals(10, state.timeLimitMinutes)
        assertEquals(WriteDirection.FRONT, state.writeDirection)
    }

    @Test
    fun `previousConfig without writeDirection defaults to BACK`() = runTest {
        val vm = createViewModel(
            totalCards = 20,
            previousConfig = "15|WRITE|10",
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(WriteDirection.BACK, state.writeDirection)
    }

    @Test
    fun `buildStartConfig includes writeDirection`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()

        vm.onQuestionCountChange(8)
        vm.onQuestionTypeChange(QuestionType.WRITE)
        vm.onTimeLimitChange(30)
        vm.onWriteDirectionChange(WriteDirection.FRONT)

        val config = vm.buildStartConfig()
        assertEquals("8|WRITE|30|FRONT", config)
    }

    @Test
    fun `buildStartConfig with no time limit returns -1`() = runTest {
        val vm = createViewModel(totalCards = 10)
        advanceUntilIdle()

        val config = vm.buildStartConfig()
        assertTrue(config.endsWith("|-1|BACK"))
    }
}
