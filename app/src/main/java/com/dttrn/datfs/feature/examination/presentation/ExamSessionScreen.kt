package com.dttrn.datfs.feature.examination.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamSessionScreen(
    onSubmitExam: (sessionId: String) -> Unit,
    onExit: () -> Unit,
    viewModel: ExamSessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Start timer when loaded
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            viewModel.startTimer()
        }
    }

    // Auto-submit when timer reaches 0
    LaunchedEffect(uiState.timeRemainingSeconds, uiState.isSubmitted) {
        if (!uiState.isSubmitted && uiState.timeLimitMinutes != null && uiState.timeRemainingSeconds <= 0 && !uiState.isLoading) {
            val sessionId = viewModel.onSubmitExam()
            onSubmitExam(sessionId)
        }
    }

    // Exit confirmation dialog
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
            if (!uiState.isLoading && uiState.questions.isNotEmpty()) {
                Surface(
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        // Previous button
                        if (uiState.currentIndex > 0) {
                            OutlinedButton(onClick = { viewModel.onPreviousQuestion() }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Câu trước")
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        // Next / Submit button
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
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

                // Question card (front text)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    ),
                ) {
                    Text(
                        currentQuestion.card.frontText,
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }

                // Question type label
                Text(
                    when (currentQuestion.questionType) {
                        QuestionType.MULTIPLE_CHOICE -> "Chọn đáp án đúng:"
                        QuestionType.WRITE -> "Nhập câu trả lời:"
                        QuestionType.MIXED -> if (currentQuestion.options.isNotEmpty()) "Chọn đáp án đúng:" else "Nhập câu trả lời:"
                        QuestionType.DICTATION -> "Nghe và nhập câu trả lời:"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                // Answer area
                if (currentQuestion.options.isNotEmpty()) {
                    // Multiple choice
                    val labels = listOf("A", "B", "C", "D")
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isSelected = currentQuestion.userAnswer == option
                        OutlinedCard(
                            onClick = {
                                viewModel.onSelectAnswer(option)
                                // Auto-advance after a short delay for MC
                                if (uiState.isLastQuestion) return@OutlinedCard
                                // Don't auto-advance; user controls navigation
                            },
                            modifier = Modifier.fillMaxWidth(),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
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
                                Text(
                                    option,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                } else {
                    // Write answer
                    OutlinedTextField(
                        value = currentQuestion.userAnswer ?: "",
                        onValueChange = { viewModel.onWriteAnswerChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Nhập câu trả lời...") },
                        singleLine = true,
                    )
                }
            }
        }
    }
}
