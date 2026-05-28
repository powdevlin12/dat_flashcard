package com.dttrn.datfs.feature.examination.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                                QuestionType.MIXED -> "Ngẫu nhiên trắc nghiệm hoặc gõ đáp án"
                                QuestionType.DICTATION -> "Nghe và gõ đáp án"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
