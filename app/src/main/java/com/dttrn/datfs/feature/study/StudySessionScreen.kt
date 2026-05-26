package com.dttrn.datfs.feature.study

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.study.SM2Algorithm
import com.dttrn.datfs.core.tts.TtsManager

@Composable
fun StudySessionScreen(
    onBack: () -> Unit,
    onSessionComplete: (deckId: String) -> Unit,
    viewModel: StudySessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }

    // Navigate to result when done
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onSessionComplete(viewModel.deckId)
    }

    // Auto-focus for keyboard capture (delay to ensure layout is ready)
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        focusRequester.requestFocus()
    }
    // Re-focus after dialog closes
    LaunchedEffect(uiState.showRangeDialog) {
        if (!uiState.showRangeDialog) {
            kotlinx.coroutines.delay(100)
            focusRequester.requestFocus()
        }
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    uiState.error?.let { error ->
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(error, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Quay lại") }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            StudySessionTopBar(
                deckTitle = uiState.deckTitle,
                mode = uiState.mode,
                currentIndex = uiState.reviewedCount,
                total = uiState.totalCount,
                progress = uiState.progress,
                isShuffled = uiState.isShuffled,
                isRangeApplied = uiState.isRangeApplied,
                rangeFrom = uiState.rangeFrom,
                rangeTo = uiState.rangeTo,
                originalTotal = uiState.originalTotalCount,
                onBack = onBack,
                onShuffle = viewModel::onShuffleCards,
                onRangeFilter = viewModel::onShowRangeDialog,
            )
        },
        bottomBar = {
            KeyboardShortcutsBar(
                mode = uiState.mode,
                isFlipped = uiState.isFlipped,
                isAnswerRevealed = uiState.isAnswerRevealed,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    if (uiState.showRangeDialog) return@onKeyEvent false
                    handleKeyEvent(event, uiState, viewModel, onBack)
                },
        ) {
            when (viewModel.mode) {
                StudyMode.SPACED_REPETITION -> SwipeLearnContent(
                    uiState = uiState,
                    onFlip = viewModel::onFlipCard,
                    onSwipeLeft = { viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN) },
                    onSwipeRight = { viewModel.onRateCard(SM2Algorithm.Ratings.GOOD) },
                    onRateAgain = { viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN) },
                    onRateHard = { viewModel.onRateCard(SM2Algorithm.Ratings.HARD) },
                    onRateGood = { viewModel.onRateCard(SM2Algorithm.Ratings.GOOD) },
                    onRateEasy = { viewModel.onRateCard(SM2Algorithm.Ratings.EASY) },
                    showRatingButtons = true,
                    onToggleFrontFirst = viewModel::onToggleFrontFirst,
                    onSpeak = viewModel::onSpeakWord,
                    ttsManager = viewModel.ttsManager,
                )
                StudyMode.LEARN -> SwipeLearnContent(
                    uiState = uiState,
                    onFlip = viewModel::onFlipCard,
                    onSwipeLeft = { viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN) },
                    onSwipeRight = { viewModel.onRateCard(SM2Algorithm.Ratings.GOOD) },
                    onRateAgain = { viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN) },
                    onRateHard = { viewModel.onRateCard(SM2Algorithm.Ratings.HARD) },
                    onRateGood = { viewModel.onRateCard(SM2Algorithm.Ratings.GOOD) },
                    onRateEasy = { viewModel.onRateCard(SM2Algorithm.Ratings.EASY) },
                    showRatingButtons = false,
                    onToggleFrontFirst = viewModel::onToggleFrontFirst,
                    onSpeak = viewModel::onSpeakWord,
                    ttsManager = viewModel.ttsManager,
                )
                StudyMode.QUIZ -> QuizContent(
                    uiState = uiState,
                    onSelectAnswer = viewModel::onSelectQuizAnswer,
                )
                StudyMode.WRITE -> WriteContent(
                    uiState = uiState,
                    onAnswerChange = viewModel::onWriteAnswerChange,
                    onSubmit = viewModel::onSubmitWriteAnswer,
                    onAdvance = viewModel::onWriteAdvance,
                    showFrontFirst = uiState.showFrontFirst,
                    onToggleFrontFirst = viewModel::onToggleFrontFirst,
                    onSpeak = viewModel::onSpeakWord,
                    ttsManager = viewModel.ttsManager,
                )
                StudyMode.MATCH -> MatchContent(
                    uiState = uiState,
                    onItemClick = viewModel::onMatchItemClick,
                )
            }
        }
    }

    // Range selection dialog
    if (uiState.showRangeDialog) {
        RangeSelectionDialog(
            originalTotal = uiState.originalTotalCount,
            initialFrom = uiState.rangeFrom ?: 1,
            initialTo = uiState.rangeTo ?: uiState.originalTotalCount,
            isRangeApplied = uiState.isRangeApplied,
            onApply = viewModel::onApplyRange,
            onClear = viewModel::onClearRange,
            onDismiss = viewModel::onDismissRangeDialog,
        )
    }
}

// ===== TOP BAR =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudySessionTopBar(
    deckTitle: String,
    mode: StudyMode,
    currentIndex: Int,
    total: Int,
    progress: Float,
    isShuffled: Boolean,
    isRangeApplied: Boolean,
    rangeFrom: Int?,
    rangeTo: Int?,
    originalTotal: Int,
    onBack: () -> Unit,
    onShuffle: () -> Unit,
    onRangeFilter: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(deckTitle, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    val subtitle = buildString {
                        append(modeLabel(mode))
                        append(" • $currentIndex/$total thẻ")
                        val remaining = total - currentIndex
                        if (remaining > 0 && currentIndex > 0) {
                            append(" (còn $remaining thẻ cần ôn)")
                        }
                        if (isRangeApplied && rangeFrom != null && rangeTo != null) {
                            append(" • vị trí $rangeFrom–$rangeTo/$originalTotal")
                        }
                    }
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isRangeApplied) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dừng học")
                }
            },
            actions = {
                // Range filter button
                IconButton(onClick = onRangeFilter) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Chọn phạm vi thẻ",
                        tint = if (isRangeApplied) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Shuffle button
                IconButton(onClick = onShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Trộn thẻ",
                        tint = if (isShuffled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            strokeCap = StrokeCap.Square,
        )
    }
}

private fun modeLabel(mode: StudyMode) = when (mode) {
    StudyMode.SPACED_REPETITION -> "SM-2"
    StudyMode.LEARN -> "Học"
    StudyMode.QUIZ -> "Trắc nghiệm"
    StudyMode.WRITE -> "Viết"
    StudyMode.MATCH -> "Nối từ"
}

// ===== SWIPE / LEARN CONTENT =====

@Composable
private fun SwipeLearnContent(
    uiState: StudySessionUiState,
    onFlip: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onRateAgain: () -> Unit,
    onRateHard: () -> Unit,
    onRateGood: () -> Unit,
    onRateEasy: () -> Unit,
    showRatingButtons: Boolean,
    onToggleFrontFirst: () -> Unit,
    onSpeak: () -> Unit,
    ttsManager: TtsManager,
) {
    val card = uiState.currentCard ?: return
    val ttsStatus by ttsManager.status.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Front/Back toggle pill
        FrontBackToggle(
            showFrontFirst = uiState.showFrontFirst,
            onToggle = onToggleFrontFirst,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Card
        SwipeableCard(
            card = card,
            isFlipped = uiState.isFlipped,
            showFrontFirst = uiState.showFrontFirst,
            onFlip = onFlip,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        Spacer(Modifier.height(8.dp))

        // TTS Speak Button
        TtsSpeakButton(
            ttsStatus = ttsStatus,
            onSpeak = onSpeak,
        )

        Spacer(Modifier.height(16.dp))

        // Rating buttons
        AnimatedVisibility(visible = uiState.isFlipped) {
            if (showRatingButtons) {
                // SM-2 — 4 buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RatingButton("Quên", Color(0xFFF44336), modifier = Modifier.weight(1f), onClick = onRateAgain)
                    RatingButton("Khó", Color(0xFFFF9800), modifier = Modifier.weight(1f), onClick = onRateHard)
                    RatingButton("Tốt", Color(0xFF4CAF50), modifier = Modifier.weight(1f), onClick = onRateGood)
                    RatingButton("Dễ", Color(0xFF2196F3), modifier = Modifier.weight(1f), onClick = onRateEasy)
                }
            } else {
                // Learn — 2 buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onSwipeLeft,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Chưa biết", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onSwipeRight,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Biết rồi", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Swipe hint when not flipped
        if (!uiState.isFlipped) {
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SwipeDirectionHint(Icons.Default.ArrowBack, "Quên", Color(0xFFF44336), onClick = onSwipeLeft)
                Text("vuốt", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                SwipeDirectionHint(Icons.Default.ArrowForward, "Nhớ", Color(0xFF4CAF50), onClick = onSwipeRight)
            }
        }
    }
}

@Composable
private fun RatingButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    }
}

@Composable
private fun SwipeDirectionHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

// ===== FRONT/BACK TOGGLE =====

@Composable
private fun FrontBackToggle(
    showFrontFirst: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("Mặt trước → Mặt sau", "Mặt sau → Mặt trước")
    val selectedIndex = if (showFrontFirst) 0 else 1

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .clickable { if (!isSelected) onToggle() },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

// ===== QUIZ CONTENT =====

@Composable
private fun QuizContent(
    uiState: StudySessionUiState,
    onSelectAnswer: (String) -> Unit,
) {
    val card = uiState.currentCard ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    card.frontText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Answer options
        uiState.quizOptions.forEachIndexed { index, option ->
            val isSelected = option == uiState.selectedAnswer
            val isCorrect = option == card.backText
            val backgroundColor = when {
                !uiState.isAnswerRevealed -> MaterialTheme.colorScheme.surface
                isCorrect -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                isSelected -> Color(0xFFF44336).copy(alpha = 0.15f)
                else -> MaterialTheme.colorScheme.surface
            }
            val borderColor = when {
                !uiState.isAnswerRevealed && isSelected -> MaterialTheme.colorScheme.primary
                uiState.isAnswerRevealed && isCorrect -> Color(0xFF4CAF50)
                uiState.isAnswerRevealed && isSelected -> Color(0xFFF44336)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                    .clickable(enabled = !uiState.isAnswerRevealed) { onSelectAnswer(option) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${('A' + index)}. ",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (uiState.isAnswerRevealed) {
                        when {
                            isCorrect -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            isSelected -> Icon(Icons.Default.Cancel, null, tint = Color(0xFFF44336))
                        }
                    }
                }
            }
        }
    }
}

// ===== WRITE CONTENT =====

@Composable
private fun WriteContent(
    uiState: StudySessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAdvance: () -> Unit,
    showFrontFirst: Boolean,
    onToggleFrontFirst: () -> Unit,
    onSpeak: () -> Unit,
    ttsManager: TtsManager,
) {
    val card = uiState.currentCard ?: return
    val focusManager = LocalFocusManager.current
    val ttsStatus by ttsManager.status.collectAsStateWithLifecycle()

    val questionText = if (showFrontFirst) card.frontText else card.backText
    val correctAnswerText = if (showFrontFirst) card.backText else card.frontText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Front/Back toggle
        FrontBackToggle(
            showFrontFirst = showFrontFirst,
            onToggle = onToggleFrontFirst,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Question card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    questionText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // TTS Speak Button
        TtsSpeakButton(
            ttsStatus = ttsStatus,
            onSpeak = onSpeak,
        )

        Spacer(Modifier.height(12.dp))

        // Answer input
        OutlinedTextField(
            value = uiState.writeAnswer,
            onValueChange = { if (!uiState.isAnswerRevealed) onAnswerChange(it) },
            label = { Text("Câu trả lời của bạn") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isAnswerRevealed,
            isError = uiState.isWriteCorrect == false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); onSubmit() }),
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            )
        )

        // Feedback
        AnimatedVisibility(visible = uiState.isAnswerRevealed) {
            Column {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isWriteCorrect == true)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color(0xFFF44336).copy(alpha = 0.1f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.isWriteCorrect == true) Icons.Default.CheckCircle
                                else Icons.Default.Cancel,
                                null,
                                tint = if (uiState.isWriteCorrect == true) Color(0xFF4CAF50)
                                else Color(0xFFF44336),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.isWriteCorrect == true) "Chính xác!" else "Chưa đúng",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (uiState.isWriteCorrect == false) {
                            Spacer(Modifier.height(8.dp))
                            Text("Đáp án đúng: ", style = MaterialTheme.typography.labelMedium)
                            Text(correctAnswerText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (!uiState.isAnswerRevealed) {
            Button(
                onClick = { focusManager.clearFocus(); onSubmit() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.writeAnswer.isNotBlank(),
            ) {
                Text("Kiểm tra", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Tiếp theo →", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ===== MATCH CONTENT =====

@Composable
private fun MatchContent(
    uiState: StudySessionUiState,
    onItemClick: (String) -> Unit,
) {
    val matchedCount = uiState.matchItems.count { it.isMatched } / 2
    val totalPairs = uiState.matchItems.size / 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header with progress
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Nối từ với nghĩa đúng",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (totalPairs > 0) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "$matchedCount/$totalPairs cặp",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }

        if (uiState.matchItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(uiState.matchItems, key = { it.id }) { item ->
                MatchItemCard(
                    item = item,
                    onClick = { onItemClick(item.id) },
                )
            }
        }
    }
}

@Composable
private fun MatchItemCard(
    item: MatchItem,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        item.isMatched -> Color(0xFF4CAF50).copy(alpha = 0.15f)
        item.isError -> Color(0xFFF44336).copy(alpha = 0.15f)
        item.isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        item.isMatched -> Color(0xFF4CAF50)
        item.isError -> Color(0xFFF44336)
        item.isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !item.isMatched, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(8.dp),
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

// ===== TTS SPEAK BUTTON =====

@Composable
private fun TtsSpeakButton(
    ttsStatus: TtsManager.TtsStatus,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSpeaking = ttsStatus == TtsManager.TtsStatus.SPEAKING
    val isError = ttsStatus == TtsManager.TtsStatus.ERROR
    val isReady = ttsStatus == TtsManager.TtsStatus.READY

    // Pulse animation khi đang phát
    val infiniteTransition = rememberInfiniteTransition(label = "tts_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tts_scale",
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isSpeaking) scale else 1f,
        label = "button_scale",
    )

    val containerColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isSpeaking -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isSpeaking -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = { if (isReady || isSpeaking) onSpeak() },
            enabled = !isError,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { scaleX = buttonScale; scaleY = buttonScale },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Đang phát âm..." else "Phát âm từ này",
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(
            targetState = when {
                isError -> "TTS không khả dụng"
                isSpeaking -> "Đang phát âm..."
                else -> "Nhấn để nghe phát âm"
            },
            label = "tts_label",
        ) { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isError) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ===== RANGE SELECTION DIALOG =====

@Composable
private fun RangeSelectionDialog(
    originalTotal: Int,
    initialFrom: Int,
    initialTo: Int,
    isRangeApplied: Boolean,
    onApply: (from: Int, to: Int) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var fromText by remember { mutableStateOf(initialFrom.toString()) }
    var toText by remember { mutableStateOf(initialTo.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        },
        title = {
            Text(
                "Chọn phạm vi học",
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Tổng số thẻ: $originalTotal. Chọn vị trí bắt đầu và kết thúc để học một phần.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = fromText,
                        onValueChange = { fromText = it; errorMessage = null },
                        label = { Text("Từ vị trí") },
                        placeholder = { Text("1") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    Text("→", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = toText,
                        onValueChange = { toText = it; errorMessage = null },
                        label = { Text("Đến vị trí") },
                        placeholder = { Text("$originalTotal") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                // Preview count
                val from = fromText.toIntOrNull()
                val to = toText.toIntOrNull()
                if (from != null && to != null && from in 1..originalTotal && to >= from && to <= originalTotal) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            "Sẽ học ${to - from + 1} thẻ (vị trí $from → $to)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }

                // Error message
                errorMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // Clear range button (if range is applied)
                if (isRangeApplied) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        ),
                    ) {
                        Icon(Icons.Default.ClearAll, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Xóa bộ lọc — học tất cả", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val from = fromText.toIntOrNull()
                    val to = toText.toIntOrNull()
                    when {
                        from == null || to == null -> errorMessage = "Vui lòng nhập số hợp lệ"
                        from < 1 -> errorMessage = "Vị trí bắt đầu phải ≥ 1"
                        to > originalTotal -> errorMessage = "Vị trí kết thúc phải ≤ $originalTotal"
                        from > to -> errorMessage = "Vị trí bắt đầu phải ≤ vị trí kết thúc"
                        else -> onApply(from, to)
                    }
                },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Áp dụng", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}

// ===== KEYBOARD SHORTCUT HANDLER =====

/**
 * Xử lý phím tắt bàn phím ngoài cho máy tính bảng.
 *
 * | Phím       | Chức năng                          | Chế độ              |
 * |------------|------------------------------------|---------------------|
 * | Space      | Lật thẻ                            | Swipe/Learn         |
 * | 1          | Quên (Again)                       | SM-2 (đã lật)       |
 * | 2          | Khó (Hard)                         | SM-2 (đã lật)       |
 * | 3          | Tốt (Good)                         | SM-2 (đã lật)       |
 * | 4          | Dễ (Easy)                          | SM-2 (đã lật)       |
 * | ←          | Chưa biết / Quên                   | Learn/Swipe         |
 * | →          | Biết rồi / Nhớ                     | Learn/Swipe         |
 * | A/B/C/D    | Chọn đáp án trắc nghiệm            | Quiz                |
 * | Enter      | Kiểm tra / Tiếp theo               | Write               |
 * | T          | Đổi mặt trước ↔ mặt sau            | Swipe/Learn/Write   |
 * | S          | Trộn thẻ                           | Tất cả              |
 * | P          | Phát âm (TTS)                      | Swipe/Learn/Write   |
 * | F          | Mở bộ lọc phạm vi                  | Tất cả              |
 * | Escape     | Quay lại                           | Tất cả              |
 */
private fun handleKeyEvent(
    event: KeyEvent,
    uiState: StudySessionUiState,
    viewModel: StudySessionViewModel,
    onBack: () -> Unit,
): Boolean {
    val mode = viewModel.mode
    val isSwipeLearn = mode == StudyMode.SPACED_REPETITION || mode == StudyMode.LEARN

    return when (event.key) {
        // === GLOBAL ===
        Key.Escape -> { onBack(); true }
        Key.S -> { viewModel.onShuffleCards(); true }
        Key.F -> { viewModel.onShowRangeDialog(); true }
        Key.P -> {
            if (isSwipeLearn || mode == StudyMode.WRITE) { viewModel.onSpeakWord(); true }
            else false
        }
        Key.T -> {
            if (isSwipeLearn || mode == StudyMode.WRITE) { viewModel.onToggleFrontFirst(); true }
            else false
        }

        // === SWIPE / LEARN: Space to flip ===
        Key.Spacebar -> {
            if (isSwipeLearn) { viewModel.onFlipCard(); true }
            else false
        }

        // === SM-2 ratings: 1-4 (only when flipped) ===
        Key.One -> {
            if (mode == StudyMode.SPACED_REPETITION && uiState.isFlipped) {
                viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN); true
            } else false
        }
        Key.Two -> {
            if (mode == StudyMode.SPACED_REPETITION && uiState.isFlipped) {
                viewModel.onRateCard(SM2Algorithm.Ratings.HARD); true
            } else false
        }
        Key.Three -> {
            if (mode == StudyMode.SPACED_REPETITION && uiState.isFlipped) {
                viewModel.onRateCard(SM2Algorithm.Ratings.GOOD); true
            } else false
        }
        Key.Four -> {
            if (mode == StudyMode.SPACED_REPETITION && uiState.isFlipped) {
                viewModel.onRateCard(SM2Algorithm.Ratings.EASY); true
            } else false
        }

        // === LEARN: arrow keys ===
        Key.DirectionLeft -> {
            if (isSwipeLearn) { viewModel.onRateCard(SM2Algorithm.Ratings.AGAIN); true }
            else false
        }
        Key.DirectionRight -> {
            if (isSwipeLearn) { viewModel.onRateCard(SM2Algorithm.Ratings.GOOD); true }
            else false
        }

        // === QUIZ: A/B/C/D ===
        Key.A -> {
            if (mode == StudyMode.QUIZ && uiState.quizOptions.isNotEmpty() && !uiState.isAnswerRevealed) {
                viewModel.onSelectQuizAnswer(uiState.quizOptions[0]); true
            } else false
        }
        Key.B -> {
            if (mode == StudyMode.QUIZ && uiState.quizOptions.size > 1 && !uiState.isAnswerRevealed) {
                viewModel.onSelectQuizAnswer(uiState.quizOptions[1]); true
            } else false
        }
        Key.C -> {
            if (mode == StudyMode.QUIZ && uiState.quizOptions.size > 2 && !uiState.isAnswerRevealed) {
                viewModel.onSelectQuizAnswer(uiState.quizOptions[2]); true
            } else false
        }
        Key.D -> {
            if (mode == StudyMode.QUIZ && uiState.quizOptions.size > 3 && !uiState.isAnswerRevealed) {
                viewModel.onSelectQuizAnswer(uiState.quizOptions[3]); true
            } else false
        }

        // === WRITE: Enter to submit/advance ===
        Key.Enter -> {
            if (mode == StudyMode.WRITE) {
                if (uiState.isAnswerRevealed) viewModel.onWriteAdvance()
                else if (uiState.writeAnswer.isNotBlank()) viewModel.onSubmitWriteAnswer()
                true
            } else false
        }

        else -> false
    }
}

// ===== KEYBOARD SHORTCUTS HINT BAR =====

@Composable
private fun KeyboardShortcutsBar(
    mode: StudyMode,
    isFlipped: Boolean,
    isAnswerRevealed: Boolean,
    modifier: Modifier = Modifier,
) {
    val hints = remember(mode, isFlipped, isAnswerRevealed) {
        buildList {
            when (mode) {
                StudyMode.SPACED_REPETITION -> {
                    add("Space" to "Lật")
                    if (isFlipped) {
                        add("1" to "Quên")
                        add("2" to "Khó")
                        add("3" to "Tốt")
                        add("4" to "Dễ")
                    }
                    add("←→" to "Quên/Nhớ")
                }
                StudyMode.LEARN -> {
                    add("Space" to "Lật")
                    add("←" to "Chưa biết")
                    add("→" to "Biết rồi")
                }
                StudyMode.QUIZ -> add("A/B/C/D" to "Chọn đáp án")
                StudyMode.WRITE -> {
                    if (isAnswerRevealed) add("Enter" to "Tiếp")
                    else add("Enter" to "Kiểm tra")
                }
                StudyMode.MATCH -> {} // no card-specific shortcuts
            }
            add("P" to "Phát âm")
            add("T" to "Đổi mặt")
            add("S" to "Trộn")
            add("F" to "Lọc")
            add("Esc" to "Thoát")
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            hints.forEach { (key, label) ->
                KeyHintChip(key = key, label = label)
            }
        }
    }
}

@Composable
private fun KeyHintChip(key: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                fontSize = 10.sp,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}
