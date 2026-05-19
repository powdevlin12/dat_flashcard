package com.dttrn.datfs.feature.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
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
                            Text("Thêm & tiếp tục", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Button(
                        onClick = { viewModel.onSave() },
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Lưu", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ===== Front =====
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Mặt trước *")
                PremiumTextField(
                    value = uiState.frontText,
                    onValueChange = viewModel::onFrontTextChange,
                    placeholder = "Từ, câu hỏi, khái niệm...",
                    isError = uiState.frontError != null,
                    errorText = uiState.frontError,
                    minLines = 3,
                    maxLines = 6,
                )
            }

            // ===== Back =====
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Mặt sau *")
                PremiumTextField(
                    value = uiState.backText,
                    onValueChange = viewModel::onBackTextChange,
                    placeholder = "Định nghĩa, đáp án, nghĩa...",
                    isError = uiState.backError != null,
                    errorText = uiState.backError,
                    minLines = 3,
                    maxLines = 6,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            )

            // ===== Optional Fields =====
            SectionLabel("Thông tin bổ sung")

            PremiumTextField(
                value = uiState.pronunciation,
                onValueChange = viewModel::onPronunciationChange,
                label = "Phiên âm",
                placeholder = "/prəˌnʌnsiˈeɪʃən/",
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            PremiumTextField(
                value = uiState.exampleSentence,
                onValueChange = viewModel::onExampleChange,
                label = "Ví dụ",
                minLines = 2,
                maxLines = 4,
            )

            PremiumTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                label = "Ghi chú",
                minLines = 2,
                maxLines = 4,
            )

            // ===== Difficulty =====
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionLabel("Độ khó")
                DifficultySelector(
                    selected = uiState.difficultyLevel,
                    onSelect = viewModel::onDifficultyChange,
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    label: String? = null,
    isError: Boolean = false,
    errorText: String? = null,
    minLines: Int = 1,
    maxLines: Int = 1,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) } },
        label = label?.let { { Text(it) } },
        isError = isError,
        supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DifficultySelector(selected: Int, onSelect: (Int) -> Unit) {
    val levels = listOf(
        1 to "Dễ",
        2 to "Bình thường",
        3 to "Khó",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        levels.forEach { (level, label) ->
            FilterChip(
                selected = selected == level,
                onClick = { onSelect(level) },
                label = { Text(label, fontWeight = if (selected == level) FontWeight.Bold else FontWeight.Medium) },
                leadingIcon = when (level) {
                    1 -> ({ Icon(Icons.Default.SentimentSatisfied, null, Modifier.size(18.dp)) })
                    2 -> ({ Icon(Icons.Default.SentimentNeutral, null, Modifier.size(18.dp)) })
                    else -> ({ Icon(Icons.Default.SentimentDissatisfied, null, Modifier.size(18.dp)) })
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == level,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
        }
    }
}
