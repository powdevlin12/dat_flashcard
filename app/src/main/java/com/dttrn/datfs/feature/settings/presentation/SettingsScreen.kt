package com.dttrn.datfs.feature.settings.presentation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dttrn.datfs.core.notification.NotificationScheduler
import java.util.Locale

/**
 * Màn hình Cài đặt — hub điều hướng + full preferences.
 * Kết nối SettingsViewModel để đọc/ghi DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToImportExport: () -> Unit,
    onNavigateToBackup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    // ===== Collect state from ViewModel =====
    val theme by viewModel.theme.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val defaultStudyMode by viewModel.defaultStudyMode.collectAsState()
    val animationEnabled by viewModel.animationEnabled.collectAsState()
    val notificationEnabled by viewModel.notificationEnabled.collectAsState()
    val notificationHour by viewModel.notificationHour.collectAsState()
    val notificationMinute by viewModel.notificationMinute.collectAsState()
    val exportFormat by viewModel.exportFormat.collectAsState()

    // ===== Dialogs state =====
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showStudyModeDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showExportFormatDialog by remember { mutableStateOf(false) }

    // ===== Notification permission =====
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationEnabled(true)
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Cài đặt",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            "FlashMind",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                )
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ===== Dữ liệu =====
            item {
                SettingsSectionCard(icon = "📊", title = "Dữ liệu & Học tập") {
                    SettingsMenuItem(
                        icon = Icons.Default.ImportExport,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Import / Export Excel",
                        subtitle = "Nhập thẻ từ .xlsx hoặc xuất ra file Excel",
                        onClick = onNavigateToImportExport,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    SettingsMenuItem(
                        icon = Icons.Default.CloudUpload,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Sao lưu & Phục hồi",
                        subtitle = "Backup toàn bộ dữ liệu ra JSON hoặc .db",
                        onClick = onNavigateToBackup,
                    )
                }
            }

            // ===== Giao diện =====
            item {
                SettingsSectionCard(icon = "🎨", title = "Giao diện") {
                    SettingsMenuItemChoice(
                        icon = Icons.Default.DarkMode,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Chủ đề",
                        subtitle = themeDisplayName(theme),
                        onClick = { showThemeDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    SettingsMenuItemChoice(
                        icon = Icons.Default.FormatSize,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Cỡ chữ",
                        subtitle = fontSizeDisplayName(fontSize),
                        onClick = { showFontSizeDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    SettingsMenuItemToggle(
                        icon = Icons.Default.Animation,
                        iconTint = MaterialTheme.colorScheme.tertiary,
                        title = "Animation thẻ",
                        subtitle = "Hiệu ứng lật thẻ và chuyển động",
                        checked = animationEnabled,
                        onCheckedChange = { viewModel.setAnimationEnabled(it) },
                    )
                }
            }

            // ===== Học tập =====
            item {
                SettingsSectionCard(icon = "📖", title = "Học tập") {
                    SettingsMenuItemChoice(
                        icon = Icons.Default.School,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = "Chế độ học mặc định",
                        subtitle = studyModeDisplayName(defaultStudyMode),
                        onClick = { showStudyModeDialog = true },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    SettingsMenuItemChoice(
                        icon = Icons.Default.FileDownload,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        title = "Định dạng export mặc định",
                        subtitle = if (exportFormat == "JSON") "JSON" else "Excel (.xlsx)",
                        onClick = { showExportFormatDialog = true },
                    )
                }
            }

            // ===== Thông báo =====
            item {
                SettingsSectionCard(icon = "🔔", title = "Thông báo") {
                    SettingsMenuItemToggle(
                        icon = Icons.Default.Notifications,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = "Nhắc nhở ôn bài",
                        subtitle = if (notificationEnabled) {
                            "Hàng ngày lúc ${String.format(Locale.US, "%02d:%02d", notificationHour, notificationMinute)}"
                        } else {
                            "Tắt"
                        },
                        checked = notificationEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.setNotificationEnabled(enabled)
                            }
                        },
                    )
                    if (notificationEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )
                        SettingsMenuItemChoice(
                            icon = Icons.Default.Schedule,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Giờ nhắc nhở",
                            subtitle = String.format(Locale.US, "%02d:%02d", notificationHour, notificationMinute),
                            onClick = { showTimePicker = true },
                        )
                    }
                }
            }

            // ===== Thông tin =====
            item {
                SettingsSectionCard(icon = "ℹ️", title = "Thông tin") {
                    SettingsInfoRow(label = "Phiên bản", value = "1.0.0")
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                    SettingsInfoRow(label = "Build", value = "Phase 6 (RC)")
                }
            }

            // Extra bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // ===== Dialogs =====

    if (showThemeDialog) {
        SingleChoiceDialog(
            title = "Chủ đề",
            options = listOf("SYSTEM" to "Theo hệ thống", "LIGHT" to "Sáng", "DARK" to "Tối"),
            selectedValue = theme,
            onSelect = { viewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showFontSizeDialog) {
        SingleChoiceDialog(
            title = "Cỡ chữ",
            options = listOf("SMALL" to "Nhỏ", "MEDIUM" to "Vừa", "LARGE" to "Lớn"),
            selectedValue = fontSize,
            onSelect = { viewModel.setFontSize(it) },
            onDismiss = { showFontSizeDialog = false },
        )
    }

    if (showStudyModeDialog) {
        SingleChoiceDialog(
            title = "Chế độ học mặc định",
            options = listOf(
                "SPACED_REPETITION" to "Lặp lại ngắt quãng",
                "LEARN" to "Học tuần tự",
                "WRITE" to "Gõ đáp án",
                "QUIZ" to "Trắc nghiệm",
                "MATCH" to "Ghép đôi",
            ),
            selectedValue = defaultStudyMode,
            onSelect = { viewModel.setDefaultStudyMode(it) },
            onDismiss = { showStudyModeDialog = false },
        )
    }

    if (showExportFormatDialog) {
        SingleChoiceDialog(
            title = "Định dạng export",
            options = listOf("EXCEL" to "Excel (.xlsx)", "JSON" to "JSON"),
            selectedValue = exportFormat,
            onSelect = { viewModel.setExportFormat(it) },
            onDismiss = { showExportFormatDialog = false },
        )
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            onConfirm = { hour, minute ->
                viewModel.setNotificationTime(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

// ===== Display name helpers =====

private fun themeDisplayName(theme: String) = when (theme) {
    "LIGHT" -> "Sáng"
    "DARK" -> "Tối"
    else -> "Theo hệ thống"
}

private fun fontSizeDisplayName(size: String) = when (size) {
    "SMALL" -> "Nhỏ"
    "LARGE" -> "Lớn"
    else -> "Vừa"
}

private fun studyModeDisplayName(mode: String) = when (mode) {
    "LEARN" -> "Học tuần tự"
    "WRITE" -> "Gõ đáp án"
    "QUIZ" -> "Trắc nghiệm"
    "MATCH" -> "Ghép đôi"
    else -> "Lặp lại ngắt quãng"
}

// ===== Sub-composables =====

@Composable
private fun SettingsSectionCard(
    icon: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(icon, fontSize = 18.sp)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            content()
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBox(icon, iconTint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.Default.ChevronRight, null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsMenuItemChoice(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsIconBox(icon, iconTint)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Icon(
                Icons.Default.ChevronRight, null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsMenuItemToggle(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconBox(icon, iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsIconBox(icon: ImageVector, tint: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ===== Dialogs =====

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedValue == value,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn giờ nhắc nhở", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Xác nhận")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        },
    )
}
