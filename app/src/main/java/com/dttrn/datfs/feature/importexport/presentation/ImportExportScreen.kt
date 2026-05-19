package com.dttrn.datfs.feature.importexport.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dttrn.datfs.feature.importexport.data.parser.ParseError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    onBack: () -> Unit,
    viewModel: ImportExportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val tab by viewModel.tab.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val decks by viewModel.decks.collectAsState()
    val selectedDeckId by viewModel.selectedDeckId.collectAsState()

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.parseFile(it, context) }
    }

    // Share launcher (export)
    val shareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.resetExport() }

    // Auto-trigger share when ready
    LaunchedEffect(exportState) {
        if (exportState is ExportUiState.ReadyToShare) {
            val intent = (exportState as ExportUiState.ReadyToShare).intent
            shareLauncher.launch(Intent.createChooser(intent, "Chia sẻ file Excel"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import / Export", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ===== Tab Row =====
            TabRow(
                selectedTabIndex = tab.ordinal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[tab.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                Tab(
                    selected = tab == ImportExportTab.IMPORT,
                    onClick = { viewModel.setTab(ImportExportTab.IMPORT) },
                    text = { Text("📥 Import") },
                )
                Tab(
                    selected = tab == ImportExportTab.EXPORT,
                    onClick = { viewModel.setTab(ImportExportTab.EXPORT) },
                    text = { Text("📤 Export") },
                )
            }

            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_content",
            ) { currentTab ->
                when (currentTab) {
                    ImportExportTab.IMPORT -> ImportTab(
                        importState = importState,
                        decks = decks,
                        selectedDeckId = selectedDeckId,
                        onSelectDeck = viewModel::selectDeck,
                        onPickFile = { filePicker.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                        onConfirmImport = viewModel::confirmImport,
                        onReset = viewModel::resetImport,
                    )
                    ImportExportTab.EXPORT -> ExportTab(
                        exportState = exportState,
                        decks = decks,
                        selectedDeckId = selectedDeckId,
                        onSelectDeck = viewModel::selectDeck,
                        onExportAll = viewModel::exportAll,
                        onExportSelected = viewModel::exportSelected,
                        onExportTemplate = viewModel::exportTemplate,
                        onReset = viewModel::resetExport,
                    )
                }
            }
        }
    }
}

// ===== Import Tab =====

@Composable
private fun ImportTab(
    importState: ImportUiState,
    decks: List<com.dttrn.datfs.core.data.local.entity.DeckEntity>,
    selectedDeckId: String?,
    onSelectDeck: (String?) -> Unit,
    onPickFile: () -> Unit,
    onConfirmImport: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Deck selector
        item {
            DeckSelectorCard(
                decks = decks,
                selectedDeckId = selectedDeckId,
                onSelectDeck = onSelectDeck,
                label = "Nhập vào bộ thẻ (tuỳ chọn)",
            )
        }

        // Import state content
        when (importState) {
            is ImportUiState.Idle -> item {
                ImportIdleCard(onPickFile = onPickFile)
            }
            is ImportUiState.Parsing -> item {
                LoadingCard("Đang phân tích file…")
            }
            is ImportUiState.Preview -> {
                val preview = importState.preview
                item {
                    ImportPreviewCard(
                        preview = preview,
                        onConfirm = onConfirmImport,
                        onCancel = onReset,
                    )
                }
                if (preview.parseResult.errors.isNotEmpty()) {
                    item {
                        Text(
                            "Dòng lỗi (${preview.parseResult.errors.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    items(preview.parseResult.errors) { error ->
                        ErrorRowCard(error)
                    }
                }
            }
            is ImportUiState.Importing -> item {
                LoadingCard("Đang import…")
            }
            is ImportUiState.Success -> item {
                SuccessCard(
                    message = "Đã import thành công ${importState.count} thẻ!",
                    icon = "✅",
                    onReset = onReset,
                )
            }
            is ImportUiState.Error -> item {
                ErrorCard(message = importState.message, onReset = onReset)
            }
        }
    }
}

@Composable
private fun ImportIdleCard(onPickFile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("📊", fontSize = 48.sp)
            Text(
                "Import từ file Excel (.xlsx)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "File cần có:\n• Cột A: Mặt trước (bắt buộc)\n• Cột B: Mặt sau (bắt buộc)\n• Cột C: Phiên âm\n• Cột D: Câu ví dụ\n• Cột E: Ghi chú\n• Cột F: Độ khó (1-3)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Chọn file .xlsx")
            }
        }
    }
}

@Composable
private fun ImportPreviewCard(
    preview: com.dttrn.datfs.feature.importexport.domain.usecase.ImportPreview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val result = preview.parseResult
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Xem trước Import",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Divider()

            // Stats grid
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Thẻ hợp lệ",
                    value = "${result.cards.size}",
                    color = MaterialTheme.colorScheme.primary,
                )
                StatChip(
                    modifier = Modifier.weight(1f),
                    label = "Dòng lỗi",
                    value = "${result.errors.size}",
                    color = if (result.errors.isEmpty()) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error,
                )
            }

            Text(
                "Nhập vào: ${preview.deckTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Preview table (first 3 rows)
            if (result.cards.isNotEmpty()) {
                PreviewTable(cards = result.cards.take(3))
                if (result.cards.size > 3) {
                    Text(
                        "…và ${result.cards.size - 3} thẻ khác",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) { Text("Huỷ") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    enabled = result.cards.isNotEmpty(),
                ) { Text("Xác nhận Import") }
            }
        }
    }
}

@Composable
private fun PreviewTable(cards: List<com.dttrn.datfs.feature.importexport.data.parser.ParsedCard>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text("Mặt trước", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text("Mặt sau", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        cards.forEach { card ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    card.frontText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    card.backText,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ErrorRowCard(error: ParseError) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Dòng ${error.row}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
        Text(
            error.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

// ===== Export Tab =====

@Composable
private fun ExportTab(
    exportState: ExportUiState,
    decks: List<com.dttrn.datfs.core.data.local.entity.DeckEntity>,
    selectedDeckId: String?,
    onSelectDeck: (String?) -> Unit,
    onExportAll: () -> Unit,
    onExportSelected: () -> Unit,
    onExportTemplate: () -> Unit,
    onReset: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (exportState is ExportUiState.Exporting) {
            item { LoadingCard("Đang tạo file Excel…") }
            return@LazyColumn
        }
        if (exportState is ExportUiState.Error) {
            item { ErrorCard(message = exportState.message, onReset = onReset) }
            return@LazyColumn
        }

        item {
            DeckSelectorCard(
                decks = decks,
                selectedDeckId = selectedDeckId,
                onSelectDeck = onSelectDeck,
                label = "Chọn deck để export (bỏ chọn = export tất cả)",
                allowDeselect = true,
            )
        }

        item {
            ExportActionsCard(
                hasSelectedDeck = selectedDeckId != null,
                selectedDeckTitle = decks.find { it.id == selectedDeckId }?.title,
                onExportAll = onExportAll,
                onExportSelected = onExportSelected,
                onExportTemplate = onExportTemplate,
            )
        }
    }
}

@Composable
private fun ExportActionsCard(
    hasSelectedDeck: Boolean,
    selectedDeckTitle: String?,
    onExportAll: () -> Unit,
    onExportSelected: () -> Unit,
    onExportTemplate: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Tuỳ chọn Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            // Export all
            OutlinedButton(
                onClick = onExportAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.SelectAll, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export tất cả decks")
            }

            // Export selected
            Button(
                onClick = onExportSelected,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = hasSelectedDeck,
            ) {
                Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (hasSelectedDeck) "Export: $selectedDeckTitle" else "Chọn deck để export")
            }

            HorizontalDivider()

            // Template
            OutlinedButton(
                onClick = onExportTemplate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Article, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tải file mẫu (.xlsx)")
            }
        }
    }
}

// ===== Shared Components =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckSelectorCard(
    decks: List<com.dttrn.datfs.core.data.local.entity.DeckEntity>,
    selectedDeckId: String?,
    onSelectDeck: (String?) -> Unit,
    label: String,
    allowDeselect: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedDeck = decks.find { it.id == selectedDeckId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedDeck?.title ?: if (allowDeselect) "Tất cả decks" else "Tạo deck mới",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    if (allowDeselect) {
                        DropdownMenuItem(
                            text = { Text("Tất cả decks") },
                            onClick = { onSelectDeck(null); expanded = false },
                            leadingIcon = { Icon(Icons.Default.SelectAll, null) },
                        )
                        HorizontalDivider()
                    }
                    decks.forEach { deck ->
                        DropdownMenuItem(
                            text = { Text(deck.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            onClick = { onSelectDeck(deck.id); expanded = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Style,
                                    null,
                                    tint = runCatching {
                                        Color(android.graphics.Color.parseColor(deck.colorHex))
                                    }.getOrElse { MaterialTheme.colorScheme.primary }
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun LoadingCard(message: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SuccessCard(message: String, icon: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(icon, fontSize = 36.sp)
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            TextButton(onClick = onReset) { Text("Import thêm") }
        }
    }
}

@Composable
private fun ErrorCard(message: String, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                Text("Lỗi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onReset) { Text("Thử lại") }
        }
    }
}
