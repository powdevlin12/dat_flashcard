package com.dttrn.datfs.feature.backup.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.resetBackup() }

    val restoreFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.previewRestore(it) } }

    LaunchedEffect(uiState) {
        if (uiState is BackupUiState.ReadyToShare) {
            val intent = (uiState as BackupUiState.ReadyToShare).intent
            shareLauncher.launch(Intent.createChooser(intent, "Lưu backup"))
        }
    }

    // Restore confirm dialog
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreOverwrite by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is BackupUiState.RestorePreview) {
            pendingRestoreUri = (uiState as BackupUiState.RestorePreview).uri
            showRestoreDialog = true
        }
    }

    if (showRestoreDialog && uiState is BackupUiState.RestorePreview) {
        val preview = (uiState as BackupUiState.RestorePreview).preview
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false; viewModel.resetBackup() },
            icon = { Icon(Icons.Default.Restore, null) },
            title = { Text("Xác nhận Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backup ngày: ${preview.exportedAt}")
                    Text("• ${preview.deckCount} bộ thẻ")
                    Text("• ${preview.cardCount} flashcard")
                    HorizontalDivider()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = restoreOverwrite,
                            onCheckedChange = { restoreOverwrite = it },
                        )
                        Text(
                            "Ghi đè dữ liệu trùng lặp",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (restoreOverwrite) {
                        Text(
                            "⚠️ Chế độ ghi đè sẽ thay thế dữ liệu hiện có!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showRestoreDialog = false
                    pendingRestoreUri?.let { viewModel.confirmRestore(it, restoreOverwrite) }
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreDialog = false
                    viewModel.resetBackup()
                }) { Text("Huỷ") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sao lưu & Phục hồi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ===== Status =====
            if (uiState is BackupUiState.Loading || uiState is BackupUiState.Restoring) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            Text(
                                if (uiState is BackupUiState.Restoring) "Đang phục hồi dữ liệu…"
                                else "Đang tạo backup…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            if (uiState is BackupUiState.Error) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                (uiState as BackupUiState.Error).message,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            if (uiState is BackupUiState.RestoreSuccess) {
                item {
                    val s = uiState as BackupUiState.RestoreSuccess
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("✅", fontSize = 32.sp)
                            Text("Đã phục hồi ${s.deckCount} bộ thẻ, ${s.cardCount} flashcard!", textAlign = TextAlign.Center)
                            TextButton(onClick = viewModel::resetBackup) { Text("Đóng") }
                        }
                    }
                }
            }

            // ===== Backup Section =====
            item {
                SectionCard(
                    title = "💾 Sao lưu dữ liệu",
                    subtitle = "Xuất toàn bộ dữ liệu ra file để lưu trữ",
                ) {
                    BackupOptionButton(
                        icon = Icons.Default.DataObject,
                        title = "Backup JSON",
                        subtitle = "Toàn bộ thẻ + tiến độ học (có thể restore)",
                        onClick = viewModel::backupJson,
                    )
                    Spacer(Modifier.height(8.dp))
                    BackupOptionButton(
                        icon = Icons.Default.Storage,
                        title = "Backup Database (.db)",
                        subtitle = "Bản sao hoàn chỉnh — khôi phục nhanh nhất",
                        onClick = viewModel::backupDb,
                    )
                }
            }

            // ===== Restore Section =====
            item {
                SectionCard(
                    title = "🔄 Phục hồi dữ liệu",
                    subtitle = "Nhập backup JSON để phục hồi dữ liệu",
                ) {
                    BackupOptionButton(
                        icon = Icons.Default.Restore,
                        title = "Restore từ JSON",
                        subtitle = "Chọn file .json backup để phục hồi",
                        onClick = { restoreFilePicker.launch("application/json") },
                    )
                }
            }

            // ===== Info =====
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    ),
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ℹ️ Lưu ý", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "• Backup JSON có thể dùng để restore (có thể chọn merge hoặc ghi đè)\n" +
                                    "• File .db cần cài lại app để sử dụng (dành cho developer)\n" +
                                    "• Nên backup trước khi xóa nhiều dữ liệu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
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
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun BackupOptionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
    }
}
