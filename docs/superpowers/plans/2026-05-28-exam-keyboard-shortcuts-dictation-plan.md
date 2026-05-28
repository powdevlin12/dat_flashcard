# Exam Mode: Keyboard Shortcuts, Write Direction & Dictation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add keyboard shortcuts, write-direction config, and dictation question type to examination mode, following patterns from StudySessionScreen.

**Architecture:** Extends existing ExamConfig → ExamSession → ExamResult flow. Follows the FocusRequester + onPreviewKeyEvent keyboard shortcut pattern from StudySessionScreen. Injects the existing `TtsManager` singleton into `ExamSessionViewModel` for dictation audio.

**Tech Stack:** Kotlin, Jetpack Compose, Dagger Hilt, Android TTS, Navigation Compose

---

### Task 1: Add DICTATION to QuestionType + WriteDirection enum

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigUiState.kt`

- [ ] **Step 1: Add DICTATION to QuestionType and create WriteDirection enum**

Replace the entire file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

data class ExamConfigUiState(
    val deckTitle: String = "",
    val totalCards: Int = 0,
    val questionCount: Int = 0,
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val writeDirection: WriteDirection = WriteDirection.BACK,
    val timeLimitMinutes: Int? = null,
    val canStart: Boolean = false,
    val error: String? = null,
)

enum class QuestionType(val displayName: String) {
    MULTIPLE_CHOICE("Trắc nghiệm"),
    WRITE("Gõ đáp án"),
    DICTATION("Nghe chép chính tả"),
    MIXED("Hỗn hợp"),
}

enum class WriteDirection(val displayName: String) {
    BACK("Gõ mặt sau"),
    FRONT("Gõ mặt trước"),
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigUiState.kt
git commit -m "feat: add DICTATION question type and WriteDirection enum"
```

---

### Task 2: Add writeDirection param to ExamSession route

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/navigation/Screen.kt`

- [ ] **Step 1: Update ExamSession route definition**

Update the `ExamSession` object in `Screen.kt` (lines 66-73). Replace with:

```kotlin
    data object ExamSession : Screen("exam_session/{deckId}/{questionCount}/{questionType}/{timeLimitMinutes}/{writeDirection}") {
        fun createRoute(deckId: String, questionCount: Int, questionType: String, timeLimitMinutes: Int, writeDirection: String) =
            "exam_session/$deckId/$questionCount/$questionType/$timeLimitMinutes/$writeDirection"
        const val ARG_DECK_ID = "deckId"
        const val ARG_QUESTION_COUNT = "questionCount"
        const val ARG_QUESTION_TYPE = "questionType"
        const val ARG_TIME_LIMIT_MINUTES = "timeLimitMinutes"
        const val ARG_WRITE_DIRECTION = "writeDirection"
    }
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/navigation/Screen.kt
git commit -m "feat: add writeDirection param to ExamSession route"
```

---

### Task 3: Update ExamConfigViewModel for writeDirection + DICTATION

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigViewModel.kt`
- Modify: `app/src/test/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigViewModelTest.kt`

- [ ] **Step 1: Add writeDirection support to ExamConfigViewModel**

Replace the entire file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.ExamConfig.ARG_DECK_ID])

    private val _uiState = MutableStateFlow(ExamConfigUiState())
    val uiState: StateFlow<ExamConfigUiState> = _uiState.asStateFlow()

    private val validTimeLimits = setOf(null, 5, 10, 15, 30)

    init {
        val previousConfig = savedStateHandle.get<String>(Screen.ExamConfig.ARG_PREVIOUS_CONFIG)
        viewModelScope.launch {
            loadDeckInfo()
            if (previousConfig != null) {
                restoreConfig(previousConfig)
            }
        }
    }

    private suspend fun loadDeckInfo() {
        deckRepository.getDeckById(deckId).first()?.let { deck ->
            val totalCards = deck.cardCount
            _uiState.update {
                it.copy(
                    deckTitle = deck.title,
                    totalCards = totalCards,
                    questionCount = totalCards.coerceAtLeast(0),
                    canStart = totalCards >= 5,
                    error = when {
                        totalCards == 0 -> "Deck trống, không thể tạo bài kiểm tra"
                        totalCards < 5 -> "Cần ít nhất 5 thẻ để tạo bài kiểm tra"
                        else -> null
                    },
                )
            }
        }
    }

    private fun restoreConfig(config: String) {
        try {
            val parts = config.split("|")
            if (parts.size >= 3) {
                val count = parts[0].toIntOrNull()
                val type = runCatching { QuestionType.valueOf(parts[1]) }.getOrNull()
                val timeLimit = parts[2].toIntOrNull().let { if (it == -1) null else it }
                val direction = if (parts.size >= 4) {
                    runCatching { WriteDirection.valueOf(parts[3]) }.getOrNull()
                } else null
                _uiState.update {
                    it.copy(
                        questionCount = count?.coerceIn(5, it.totalCards) ?: it.questionCount,
                        questionType = type ?: it.questionType,
                        timeLimitMinutes = if (timeLimit in validTimeLimits) timeLimit else it.timeLimitMinutes,
                        writeDirection = direction ?: it.writeDirection,
                    )
                }
            }
        } catch (_: Exception) {
            // Invalid config format, keep defaults
        }
    }

    fun onQuestionCountChange(count: Int) {
        _uiState.update {
            it.copy(questionCount = count.coerceIn(5, it.totalCards.coerceAtLeast(5)))
        }
    }

    fun onQuestionTypeChange(type: QuestionType) {
        _uiState.update { it.copy(questionType = type) }
    }

    fun onWriteDirectionChange(direction: WriteDirection) {
        _uiState.update { it.copy(writeDirection = direction) }
    }

    fun onTimeLimitChange(minutes: Int?) {
        if (minutes in validTimeLimits) {
            _uiState.update { it.copy(timeLimitMinutes = minutes) }
        }
    }

    fun buildStartConfig(): String {
        val state = _uiState.value
        return "${state.questionCount}|${state.questionType.name}|${state.timeLimitMinutes ?: -1}|${state.writeDirection.name}"
    }
}
```

- [ ] **Step 2: Update ExamConfigViewModelTest for writeDirection**

Replace the entire test file:

```kotlin
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
```

- [ ] **Step 3: Run tests to verify**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dttrn.datfs.feature.examination.presentation.ExamConfigViewModelTest" 2>&1 | tail -20
```

All tests should pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigViewModel.kt app/src/test/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigViewModelTest.kt
git commit -m "feat: add writeDirection to ExamConfigViewModel with backward-compatible config restore"
```

---

### Task 4: Update ExamConfigScreen with write-direction sub-option + DICTATION

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigScreen.kt`

- [ ] **Step 1: Add write-direction sub-option and DICTATION radio**

Replace the entire file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamConfigScreen(
    deckId: String,
    previousConfig: String?,
    onStartExam: (questionCount: Int, questionType: String, timeLimitMinutes: Int, writeDirection: String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ExamConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val timeLimitOptions = listOf(null to "Không giới hạn", 5 to "5 phút", 10 to "10 phút", 15 to "15 phút", 30 to "30 phút")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cấu hình kiểm tra", fontWeight = FontWeight.Bold)
                        Text(
                            uiState.deckTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Question count
            Text("Số câu hỏi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (uiState.totalCards >= 5) {
                Text(
                    "${uiState.questionCount} / ${uiState.totalCards} câu",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiState.questionCount.toFloat(),
                    onValueChange = { viewModel.onQuestionCountChange(it.toInt()) },
                    valueRange = 5f..uiState.totalCards.coerceAtLeast(5).toFloat(),
                    steps = 0,
                )
            }

            // Question type
            Text("Dạng câu hỏi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            QuestionType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = uiState.questionType == type,
                        onClick = { viewModel.onQuestionTypeChange(type) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(type.displayName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            when (type) {
                                QuestionType.MULTIPLE_CHOICE -> "4 lựa chọn, chọn đáp án đúng"
                                QuestionType.WRITE -> "Tự gõ câu trả lời"
                                QuestionType.DICTATION -> "Nghe âm thanh, gõ lại chính tả"
                                QuestionType.MIXED -> "Ngẫu nhiên trắc nghiệm hoặc gõ đáp án"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Write direction sub-option (shown when WRITE or MIXED is selected)
            if (uiState.questionType == QuestionType.WRITE || uiState.questionType == QuestionType.MIXED) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Hướng gõ đáp án",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                WriteDirection.entries.forEach { direction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.writeDirection == direction,
                            onClick = { viewModel.onWriteDirectionChange(direction) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(direction.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when (direction) {
                                    WriteDirection.BACK -> "Hiện mặt trước, gõ mặt sau (đáp án)"
                                    WriteDirection.FRONT -> "Hiện mặt sau, gõ mặt trước (câu hỏi)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Time limit
            Text("Giới hạn thời gian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            timeLimitOptions.forEach { (minutes, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = uiState.timeLimitMinutes == minutes,
                        onClick = { viewModel.onTimeLimitChange(minutes) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Error message
            uiState.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Start button
            Button(
                onClick = {
                    onStartExam(
                        uiState.questionCount,
                        uiState.questionType.name,
                        uiState.timeLimitMinutes ?: -1,
                        uiState.writeDirection.name,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canStart,
            ) {
                Text("Bắt đầu kiểm tra")
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamConfigScreen.kt
git commit -m "feat: add write-direction sub-option and DICTATION type to ExamConfigScreen"
```

---

### Task 5: Update ExamSessionUiState with new fields

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionUiState.kt`

- [ ] **Step 1: Add writeDirection, isWriteInputFocused, dictationPlayCount**

Replace the entire file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

import com.dttrn.datfs.core.domain.model.Flashcard

data class ExamSessionUiState(
    val isLoading: Boolean = true,
    val deckTitle: String = "",
    val questions: List<ExamQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val isLastQuestion: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val timeRemainingSeconds: Int = 0,
    val isTimeWarning: Boolean = false,
    val isSubmitted: Boolean = false,
    val showExitDialog: Boolean = false,
    val error: String? = null,
    val writeDirection: WriteDirection = WriteDirection.BACK,
    val isWriteInputFocused: Boolean = false,
    val dictationPlayCount: Int = 0,
)

data class ExamQuestion(
    val card: Flashcard,
    val questionType: QuestionType,
    val options: List<String> = emptyList(),
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val dictationPlayCount: Int = 0,
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionUiState.kt
git commit -m "feat: add writeDirection, isWriteInputFocused, dictationPlayCount to ExamSessionUiState"
```

---

### Task 6: Update ExamSessionViewModel with TTS, dictation, write-direction logic

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionViewModel.kt`
- Modify: `app/src/test/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionViewModelTest.kt`

- [ ] **Step 1: Rewrite ExamSessionViewModel with all new logic**

Replace the entire file:

```kotlin
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

    fun onCheckWriteAnswer() {
        // No state change needed — correctness is calculated on submit
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
        // Auto-speak for dictation questions when entering
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
            val newCount = 1
            updated[state.currentIndex] = current.copy(dictationPlayCount = newCount)
            state.copy(questions = updated, dictationPlayCount = newCount)
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
        val timeTaken = ((timeLimitMinutes ?: 0) * 60 - state.timeRemainingSeconds)

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
```

- [ ] **Step 2: Update ExamSessionViewModelTest**

Replace the entire test file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import com.dttrn.datfs.core.tts.TtsManager
import com.dttrn.datfs.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
```

- [ ] **Step 3: Run tests to verify**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dttrn.datfs.feature.examination.presentation.ExamSessionViewModelTest" 2>&1 | tail -20
```

All tests should pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionViewModel.kt app/src/test/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionViewModelTest.kt
git commit -m "feat: add TTS, dictation, write-direction logic to ExamSessionViewModel"
```

---

### Task 7: Rewrite ExamSessionScreen with keyboard shortcuts, Dictation UI, write-direction

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionScreen.kt`

This is the largest task. The screen needs FocusRequester, onPreviewKeyEvent, handleKeyEvent, KeyboardShortcutsBar, Dictation UI, and write-direction support.

- [ ] **Step 1: Replace ExamSessionScreen with full implementation**

Replace the entire file:

```kotlin
package com.dttrn.datfs.feature.examination.presentation

import androidx.compose.animation.animateFloat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.tts.TtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSessionScreen(
    onSubmitExam: (sessionId: String) -> Unit,
    onExit: () -> Unit,
    viewModel: ExamSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val writeInputFocusRequester = remember { FocusRequester() }
    var isWriteInputFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            viewModel.startTimer()
        }
    }

    // Auto-focus for keyboard capture
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }

    LaunchedEffect(uiState.timeRemainingSeconds, uiState.isSubmitted) {
        if (!uiState.isSubmitted && uiState.timeLimitMinutes != null && uiState.timeRemainingSeconds <= 0 && !uiState.isLoading) {
            val sessionId = viewModel.onSubmitExam()
            onSubmitExam(sessionId)
        }
    }

    if (uiState.showExitDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissExitDialog() },
            title = { Text("Thoát bài kiểm tra?") },
            text = { Text("Bạn có chắc muốn thoát? Tiến trình làm bài sẽ bị mất.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onConfirmExit()
                    onExit()
                }) {
                    Text("Thoát")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissExitDialog() }) {
                    Text("Ở lại")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kiểm tra", fontWeight = FontWeight.Bold)
                        if (uiState.totalQuestions > 0) {
                            Text(
                                "Câu ${uiState.currentIndex + 1} / ${uiState.totalQuestions}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onRequestExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Thoát")
                    }
                },
            )
        },
        bottomBar = {
            Column {
                if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                    KeyboardShortcutsBar(
                        questionType = uiState.questions.getOrNull(uiState.currentIndex)?.questionType,
                        isWriteInputFocused = isWriteInputFocused,
                        isMC = uiState.questions.getOrNull(uiState.currentIndex)?.options?.isNotEmpty() == true,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    Surface(shadowElevation = 8.dp) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (uiState.currentIndex > 0) {
                                OutlinedButton(onClick = { viewModel.onPreviousQuestion() }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Câu trước")
                                }
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }

                            if (uiState.isLastQuestion) {
                                Button(onClick = {
                                    val sessionId = viewModel.onSubmitExam()
                                    onSubmitExam(sessionId)
                                }) {
                                    Text("Nộp bài")
                                }
                            } else {
                                Button(onClick = { viewModel.onNextQuestion() }) {
                                    Text("Câu tiếp theo")
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.questions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("Không có câu hỏi nào", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            val currentQuestion = uiState.questions[uiState.currentIndex]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                        val isWriteOrDictation = currentQuestion.questionType == QuestionType.WRITE ||
                            currentQuestion.questionType == QuestionType.DICTATION

                        // Tab toggles between input focus and shortcut mode
                        if (event.key == Key.Tab && isWriteOrDictation) {
                            coroutineScope.launch {
                                if (isWriteInputFocused) {
                                    isWriteInputFocused = false
                                    viewModel.onInputFocusChanged(false)
                                    focusRequester.requestFocus()
                                } else {
                                    isWriteInputFocused = true
                                    viewModel.onInputFocusChanged(true)
                                    writeInputFocusRequester.requestFocus()
                                }
                            }
                            return@onPreviewKeyEvent true
                        }
                        // Escape in typing mode → unfocus
                        if (event.key == Key.Escape && isWriteOrDictation && isWriteInputFocused) {
                            coroutineScope.launch {
                                isWriteInputFocused = false
                                viewModel.onInputFocusChanged(false)
                                focusRequester.requestFocus()
                            }
                            return@onPreviewKeyEvent true
                        }

                        // If input is focused, don't intercept other keys
                        if (isWriteInputFocused) return@onPreviewKeyEvent false

                        handleKeyEvent(event, uiState, viewModel, onExit)
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Progress bar + timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LinearProgressIndicator(
                            progress = {
                                if (uiState.totalQuestions > 0)
                                    (uiState.currentIndex + 1).toFloat() / uiState.totalQuestions
                                else 0f
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        if (uiState.timeLimitMinutes != null) {
                            val minutes = uiState.timeRemainingSeconds / 60
                            val seconds = uiState.timeRemainingSeconds % 60
                            val timerColor = when {
                                uiState.timeRemainingSeconds <= 30 -> Color(0xFFE53935)
                                uiState.isTimeWarning -> Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(
                                "%02d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = timerColor,
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    when (currentQuestion.questionType) {
                        QuestionType.DICTATION -> {
                            DictationQuestionContent(
                                question = currentQuestion,
                                writeDirection = uiState.writeDirection,
                                ttsManager = viewModel.ttsManager,
                                onAnswerChange = { viewModel.onWriteAnswerChange(it) },
                                onReplay = { viewModel.onReplayDictation() },
                                inputFocusRequester = writeInputFocusRequester,
                                isInputFocused = isWriteInputFocused,
                                onInputFocusChanged = { focused ->
                                    isWriteInputFocused = focused
                                    viewModel.onInputFocusChanged(focused)
                                },
                                parentFocusRequester = focusRequester,
                            )
                        }
                        else -> {
                            // Question card
                            QuestionCard(
                                question = currentQuestion,
                                writeDirection = uiState.writeDirection,
                            )

                            // Question type label
                            Text(
                                when (currentQuestion.questionType) {
                                    QuestionType.MULTIPLE_CHOICE -> "Chọn đáp án đúng:"
                                    QuestionType.WRITE -> "Nhập câu trả lời:"
                                    QuestionType.MIXED -> if (currentQuestion.options.isNotEmpty()) "Chọn đáp án đúng:" else "Nhập câu trả lời:"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            // Answer area
                            if (currentQuestion.options.isNotEmpty()) {
                                MultipleChoiceAnswers(
                                    question = currentQuestion,
                                    onSelect = { viewModel.onSelectAnswer(it) },
                                )
                            } else {
                                WriteAnswerField(
                                    value = currentQuestion.userAnswer ?: "",
                                    onValueChange = { viewModel.onWriteAnswerChange(it) },
                                    focusRequester = writeInputFocusRequester,
                                    isFocused = isWriteInputFocused,
                                    onFocusChanged = { focused ->
                                        isWriteInputFocused = focused
                                        viewModel.onInputFocusChanged(focused)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Question Card ─────────────────────────────────────────────────────

@Composable
private fun QuestionCard(
    question: ExamQuestion,
    writeDirection: WriteDirection,
) {
    val displayText = if (question.questionType == QuestionType.WRITE) {
        when (writeDirection) {
            WriteDirection.BACK -> question.card.frontText
            WriteDirection.FRONT -> question.card.backText
        }
    } else {
        question.card.frontText
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        ),
    ) {
        Text(
            displayText,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Multiple Choice Answers ───────────────────────────────────────────

@Composable
private fun MultipleChoiceAnswers(
    question: ExamQuestion,
    onSelect: (String) -> Unit,
) {
    val labels = listOf("A", "B", "C", "D")
    question.options.forEachIndexed { index, option ->
        val isSelected = question.userAnswer == option
        OutlinedCard(
            onClick = { onSelect(option) },
            modifier = Modifier.fillMaxWidth(),
            border = if (isSelected) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                CardDefaults.outlinedCardBorder()
            },
            colors = if (isSelected) {
                CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                )
            } else {
                CardDefaults.outlinedCardColors()
            },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    labels.getOrElse(index) { "" },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(12.dp))
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ─── Write Answer Field ────────────────────────────────────────────────

@Composable
private fun WriteAnswerField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    isFocused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text("Nhập câu trả lời...") },
        singleLine = true,
    )
}

// ─── Dictation Question Content ────────────────────────────────────────

@Composable
private fun DictationQuestionContent(
    question: ExamQuestion,
    writeDirection: WriteDirection,
    ttsManager: TtsManager,
    onAnswerChange: (String) -> Unit,
    onReplay: () -> Unit,
    inputFocusRequester: FocusRequester,
    isInputFocused: Boolean,
    onInputFocusChanged: (Boolean) -> Unit,
    parentFocusRequester: FocusRequester,
) {
    val ttsStatus by ttsManager.status.collectAsStateWithLifecycle()
    val isSpeaking = ttsStatus == TtsManager.TtsStatus.SPEAKING

    val listeningLabel = when (writeDirection) {
        WriteDirection.BACK -> "Nghe từ và gõ lại"
        WriteDirection.FRONT -> "Nghe nghĩa và gõ lại"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Listening indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSpeaking)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "dictation_wave")
                val waveScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "wave_scale",
                )

                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.HeadsetMic,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            if (isSpeaking) {
                                scaleX = waveScale
                                scaleY = waveScale
                            }
                        },
                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isSpeaking) "Đang đọc..." else "Nghe và gõ lại",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSpeaking) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listeningLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (question.dictationPlayCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Đã nghe ${question.dictationPlayCount} lần",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Replay button
                OutlinedButton(onClick = onReplay) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Nghe lại (R)")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Write answer field
        Text(
            "Nhập câu trả lời:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = question.userAnswer ?: "",
            onValueChange = onAnswerChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(inputFocusRequester),
            placeholder = { Text("Gõ lại những gì bạn nghe được...") },
            singleLine = true,
        )
    }
}

// ─── Keyboard Event Handler ────────────────────────────────────────────

private fun handleKeyEvent(
    event: KeyEvent,
    uiState: ExamSessionUiState,
    viewModel: ExamSessionViewModel,
    onExit: () -> Unit,
): Boolean {
    val question = uiState.questions.getOrNull(uiState.currentIndex) ?: return false
    val isMC = question.options.isNotEmpty()
    val isWrite = question.questionType == QuestionType.WRITE
    val isDictation = question.questionType == QuestionType.DICTATION

    return when (event.key) {
        Key.Escape -> { viewModel.onRequestExit(); true }

        Key.DirectionLeft -> {
            if (uiState.currentIndex > 0) { viewModel.onPreviousQuestion(); true }
            else false
        }
        Key.DirectionRight -> {
            if (!uiState.isLastQuestion) { viewModel.onNextQuestion(); true }
            else false
        }

        Key.A -> {
            if (isMC && question.options.isNotEmpty()) { viewModel.onSelectAnswer(question.options[0]); true }
            else false
        }
        Key.B -> {
            if (isMC && question.options.size > 1) { viewModel.onSelectAnswer(question.options[1]); true }
            else false
        }
        Key.C -> {
            if (isMC && question.options.size > 2) { viewModel.onSelectAnswer(question.options[2]); true }
            else false
        }
        Key.D -> {
            if (isMC && question.options.size > 3) { viewModel.onSelectAnswer(question.options[3]); true }
            else false
        }

        Key.P -> {
            if (isWrite || isDictation) { viewModel.onSpeakWord(); true }
            else false
        }
        Key.T -> {
            if (isWrite || isDictation) { viewModel.onToggleWriteDirection(); true }
            else false
        }
        Key.R -> {
            if (isDictation) { viewModel.onReplayDictation(); true }
            else false
        }

        Key.Enter -> {
            if (!uiState.isLastQuestion) { viewModel.onNextQuestion(); true }
            else false
        }

        else -> false
    }
}

// ─── Keyboard Shortcuts Hint Bar ───────────────────────────────────────

@Composable
private fun KeyboardShortcutsBar(
    questionType: QuestionType?,
    isWriteInputFocused: Boolean,
    isMC: Boolean,
    modifier: Modifier = Modifier,
) {
    val hints = remember(questionType, isWriteInputFocused, isMC) {
        buildList {
            when (questionType) {
                QuestionType.MULTIPLE_CHOICE -> add("A/B/C/D" to "Chọn đáp án")
                QuestionType.WRITE -> {
                    add("Tab" to if (isWriteInputFocused) "⌨ Phím tắt" else "✏ Gõ chữ")
                    if (isWriteInputFocused) {
                        add("Esc" to "Thoát gõ")
                    }
                    add("T" to "Đổi hướng")
                    add("P" to "Phát âm")
                }
                QuestionType.DICTATION -> {
                    add("Tab" to if (isWriteInputFocused) "⌨ Phím tắt" else "✏ Gõ chữ")
                    if (isWriteInputFocused) {
                        add("Esc" to "Thoát gõ")
                    } else {
                        add("R" to "Nghe lại")
                    }
                    add("T" to "Đổi hướng")
                    add("P" to "Phát âm")
                }
                QuestionType.MIXED -> {
                    if (isMC) add("A/B/C/D" to "Chọn đáp án")
                    else {
                        add("Tab" to if (isWriteInputFocused) "⌨ Phím tắt" else "✏ Gõ chữ")
                        if (isWriteInputFocused) add("Esc" to "Thoát gõ")
                        add("T" to "Đổi hướng")
                        add("P" to "Phát âm")
                    }
                }
                else -> {}
            }
            if (!isWriteInputFocused) {
                add("←→" to "Di chuyển")
                add("Enter" to "Tiếp")
                add("Esc" to "Thoát")
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            hints.forEach { (key, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        key,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify the build compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/examination/presentation/ExamSessionScreen.kt
git commit -m "feat: add keyboard shortcuts, dictation UI, and write-direction to ExamSessionScreen"
```

---

### Task 8: Update NavGraph to pass writeDirection

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/navigation/NavGraph.kt`

- [ ] **Step 1: Update ExamConfig and ExamSession navigation**

Update the ExamConfig composable call (around line 255) — change `onStartExam` signature:

```kotlin
// Find this block (around lines 252-260) and replace:
            ExamConfigScreen(
                deckId = deckId,
                previousConfig = previousConfig,
                onStartExam = { questionCount, questionType, timeLimitMinutes ->
                    navController.navigate(
                        Screen.ExamSession.createRoute(deckId, questionCount, questionType, timeLimitMinutes)
                    )
                },
                onNavigateBack = { navController.popBackStack() },
            )

// Replace with:
            ExamConfigScreen(
                deckId = deckId,
                previousConfig = previousConfig,
                onStartExam = { questionCount, questionType, timeLimitMinutes, writeDirection ->
                    navController.navigate(
                        Screen.ExamSession.createRoute(deckId, questionCount, questionType, timeLimitMinutes, writeDirection)
                    )
                },
                onNavigateBack = { navController.popBackStack() },
            )
```

Update the ExamSession route arguments (around lines 266-272):

```kotlin
// Find this block and add the writeDirection argument:
            arguments = listOf(
                navArgument(Screen.ExamSession.ARG_DECK_ID) { type = NavType.StringType },
                navArgument(Screen.ExamSession.ARG_QUESTION_COUNT) { type = NavType.IntType },
                navArgument(Screen.ExamSession.ARG_QUESTION_TYPE) { type = NavType.StringType },
                navArgument(Screen.ExamSession.ARG_TIME_LIMIT_MINUTES) { type = NavType.IntType },
                navArgument(Screen.ExamSession.ARG_WRITE_DIRECTION) { type = NavType.StringType },
            )
```

- [ ] **Step 2: Verify the build compiles**

```bash
./gradlew :app:compileDebugKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/navigation/NavGraph.kt
git commit -m "feat: pass writeDirection through exam navigation"
```

---

### Task 9: End-to-end verification

- [ ] **Step 1: Run all examination tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.dttrn.datfs.feature.examination.*" 2>&1 | tail -20
```

All tests should pass.

- [ ] **Step 2: Run full unit test suite to check for regressions**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

All tests should pass.

- [ ] **Step 3: Commit any remaining changes**

```bash
git status
```
