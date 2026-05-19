package com.dttrn.datfs.feature.study

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.study.SM2Algorithm

@Composable
fun StudySessionScreen(
    onBack: () -> Unit,
    onSessionComplete: (deckId: String) -> Unit,
    viewModel: StudySessionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate to result when done
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onSessionComplete(viewModel.deckId)
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
                onBack = onBack,
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
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
                )
                StudyMode.MATCH -> MatchContent(
                    uiState = uiState,
                    onItemClick = viewModel::onMatchItemClick,
                )
            }
        }
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
    onBack: () -> Unit,
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Text(deckTitle, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${modeLabel(mode)} • $currentIndex/$total thẻ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dừng học")
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
) {
    val card = uiState.currentCard ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Card
        SwipeableCard(
            card = card,
            isFlipped = uiState.isFlipped,
            onFlip = onFlip,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
) {
    val card = uiState.currentCard ?: return
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Question
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Text(
                card.frontText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

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
                            Text(card.backText, fontWeight = FontWeight.Bold)
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
