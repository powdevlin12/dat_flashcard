package com.dttrn.datfs.feature.statistics.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dttrn.datfs.feature.statistics.presentation.component.CalendarHeatmap
import com.dttrn.datfs.feature.statistics.presentation.component.DeckProgressList
import com.dttrn.datfs.feature.statistics.presentation.component.WeeklyBarChart
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thống kê học tập",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStatistics() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        when (uiState) {
            is StatisticsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Đang tải thống kê…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is StatisticsUiState.Error -> {
                val errorState = uiState as StatisticsUiState.Error
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp),
                        )
                        Text(
                            errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Button(onClick = { viewModel.loadStatistics() }) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            is StatisticsUiState.Success -> {
                val successState = uiState as StatisticsUiState.Success
                val data = successState.data
                val performances = successState.deckPerformances

                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400)) + slideInVertically(
                        tween(400),
                        initialOffsetY = { it / 8 },
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // ===== Streak Badge =====
                        StreakBadge(streak = data.streak)

                        // ===== Summary Cards =====
                        SummaryCardsRow(
                            totalCards = data.totalCardsStudied,
                            totalMinutes = data.totalMinutesStudied,
                            accuracy = data.overallAccuracy,
                            cardsToday = data.cardsStudiedToday,
                        )

                        // ===== Bar Chart =====
                        StatCard(title = "Hoạt động 7 ngày") {
                            if (data.last7DaysStats.isNotEmpty()) {
                                WeeklyBarChart(
                                    stats = data.last7DaysStats,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                EmptyChartPlaceholder("Chưa có dữ liệu học tập")
                            }
                        }

                        // ===== Calendar Heatmap =====
                        StatCard(title = "Lịch học 12 tuần") {
                            if (data.last84DaysStats.isNotEmpty()) {
                                CalendarHeatmap(
                                    stats = data.last84DaysStats,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                EmptyChartPlaceholder("Chưa có dữ liệu học tập")
                            }
                        }

                        // ===== Accuracy Gauge =====
                        if (data.overallAccuracy > 0f) {
                            StatCard(title = "Tỷ lệ đúng tổng thể") {
                                AccuracyGauge(
                                    accuracy = data.overallAccuracy,
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                )
                            }
                        }

                        // ===== Deck Progress =====
                        StatCard(title = "Tiến độ từng bộ thẻ") {
                            DeckProgressList(
                                performances = performances,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ===== Sub-components =====

@Composable
private fun StreakBadge(streak: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(primary, tertiary),
                )
            )
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "🔥",
                fontSize = 48.sp,
            )
            Column {
                Text(
                    "$streak ngày",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
                Text(
                    if (streak == 0) "Bắt đầu học để xây dựng streak!"
                    else "Chuỗi học liên tiếp. Đừng bỏ lỡ hôm nay!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun SummaryCardsRow(
    totalCards: Int,
    totalMinutes: Int,
    accuracy: Float,
    cardsToday: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Style,
            label = "Hôm nay",
            value = "$cardsToday",
            unit = "thẻ",
            iconTint = MaterialTheme.colorScheme.tertiary,
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LibraryBooks,
            label = "Tổng học",
            value = "$totalCards",
            unit = "thẻ",
            iconTint = MaterialTheme.colorScheme.primary,
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Timer,
            label = "Thời gian",
            value = formatMinutes(totalMinutes),
            unit = "",
            iconTint = MaterialTheme.colorScheme.secondary,
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CheckCircle,
            label = "Độ chính xác",
            value = "${(accuracy * 100).toInt()}",
            unit = "%",
            iconTint = Color(0xFF00C853),
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    iconTint: Color,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp, start = 2.dp),
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

/**
 * Circular accuracy gauge drawn with Canvas arc.
 */
@Composable
private fun AccuracyGauge(
    accuracy: Float,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val animAcc = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(accuracy) {
        scope.launch {
            animAcc.animateTo(
                targetValue = accuracy.coerceIn(0f, 1f),
                animationSpec = tween(800, easing = FastOutSlowInEasing),
            )
        }
    }

    Box(
        modifier = modifier
            .size(140.dp)
            .drawBehind {
                val strokeWidth = 14.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth / 2
                val center = Offset(size.width / 2, size.height / 2)

                // Background track
                drawCircle(
                    color = surfaceVariant,
                    radius = radius,
                    style = Stroke(strokeWidth),
                )
                // Filled arc
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * animAcc.value,
                    useCenter = false,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(accuracy * 100).toInt()}%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primary,
            )
            Text(
                "Đúng",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyChartPlaceholder(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatMinutes(minutes: Int): String {
    return when {
        minutes < 60 -> "${minutes}ph"
        else -> "${minutes / 60}g${if (minutes % 60 > 0) "${minutes % 60}ph" else ""}"
    }
}
