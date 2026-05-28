package com.dttrn.datfs.feature.examination.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.input.key.*
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
                        // Escape in typing mode: unfocus
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
                            QuestionCard(
                                question = currentQuestion,
                                writeDirection = uiState.writeDirection,
                            )

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
                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.HeadsetMic,
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

                OutlinedButton(onClick = onReplay) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Nghe lại (R)")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
