package com.dttrn.datfs.feature.card

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CardEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditing) "Chỉnh sửa thẻ" else "Thêm thẻ mới",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (!uiState.isEditing) {
                        TextButton(onClick = { viewModel.onSave(continueAdding = true) }) {
                            Text("Thêm & tiếp tục")
                        }
                    }
                    TextButton(
                        onClick = { viewModel.onSave() },
                        enabled = !uiState.isLoading,
                    ) {
                        Text("Lưu", fontWeight = FontWeight.SemiBold)
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== Front =====
            SectionLabel("Mặt trước *")
            OutlinedTextField(
                value = uiState.frontText,
                onValueChange = viewModel::onFrontTextChange,
                placeholder = { Text("Từ, câu hỏi, khái niệm...") },
                isError = uiState.frontError != null,
                supportingText = uiState.frontError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )

            // ===== Back =====
            SectionLabel("Mặt sau *")
            OutlinedTextField(
                value = uiState.backText,
                onValueChange = viewModel::onBackTextChange,
                placeholder = { Text("Định nghĩa, đáp án, nghĩa...") },
                isError = uiState.backError != null,
                supportingText = uiState.backError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
            )

            HorizontalDivider()

            // ===== Optional Fields =====
            SectionLabel("Thông tin bổ sung")

            OutlinedTextField(
                value = uiState.pronunciation,
                onValueChange = viewModel::onPronunciationChange,
                label = { Text("Phiên âm") },
                placeholder = { Text("/prəˌnʌnsiˈeɪʃən/") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = uiState.exampleSentence,
                onValueChange = viewModel::onExampleChange,
                label = { Text("Ví dụ") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Ghi chú") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
            )

            // ===== Difficulty =====
            SectionLabel("Độ khó")
            DifficultySelector(
                selected = uiState.difficultyLevel,
                onSelect = viewModel::onDifficultyChange,
            )

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun DifficultySelector(selected: Int, onSelect: (Int) -> Unit) {
    val levels = listOf(
        1 to "Dễ",
        2 to "Bình thường",
        3 to "Khó",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        levels.forEach { (level, label) ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(label) },
                leadingIcon = when (level) {
                    1 -> ({ Icon(Icons.Default.SentimentSatisfied, null, Modifier.size(16.dp)) })
                    2 -> ({ Icon(Icons.Default.SentimentNeutral, null, Modifier.size(16.dp)) })
                    else -> ({ Icon(Icons.Default.SentimentDissatisfied, null, Modifier.size(16.dp)) })
                },
            )
        }
    }
}
