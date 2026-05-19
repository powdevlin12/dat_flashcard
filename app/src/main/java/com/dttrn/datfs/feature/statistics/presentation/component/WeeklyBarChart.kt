package com.dttrn.datfs.feature.statistics.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.core.domain.model.StudyStatistics
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Bar chart hiển thị số thẻ học theo 7 ngày gần nhất.
 * Vẽ thuần Compose Canvas — không dùng thư viện ngoài.
 */
@Composable
fun WeeklyBarChart(
    stats: List<StudyStatistics>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barColorSecondary: Color = MaterialTheme.colorScheme.tertiary,
    barWidth: Dp = 28.dp,
) {
    val maxValue = stats.maxOfOrNull { it.cardsStudied } ?: 0
    val maxDisplay = maxOf(maxValue, 1)

    // Animate bars on first compose
    val animProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(stats) {
        scope.launch {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            )
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val todayStr = LocalDate.now().format(dateFormatter)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val todayColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (stats.isEmpty()) return@Canvas
            val canvasWidth = size.width
            val canvasHeight = size.height
            val chartHeight = canvasHeight - 32.dp.toPx()  // reserve bottom for labels
            val totalBars = stats.size
            val spacing = (canvasWidth - barWidth.toPx() * totalBars) / (totalBars + 1)

            stats.forEachIndexed { idx, stat ->
                val barHeightRatio = stat.cardsStudied.toFloat() / maxDisplay
                val animatedHeight = chartHeight * barHeightRatio * animProgress.value
                val xStart = spacing + idx * (barWidth.toPx() + spacing)
                val yStart = chartHeight - animatedHeight

                val isToday = stat.date == todayStr
                val brush = if (isToday) {
                    Brush.verticalGradient(
                        colors = listOf(barColorSecondary, barColor),
                        startY = yStart,
                        endY = chartHeight,
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(barColor.copy(alpha = 0.6f), barColor.copy(alpha = 0.3f)),
                        startY = yStart,
                        endY = chartHeight,
                    )
                }

                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(xStart, yStart),
                    size = Size(barWidth.toPx(), animatedHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )
            }

            // Baseline
            drawLine(
                color = labelColor.copy(alpha = 0.15f),
                start = Offset(0f, chartHeight),
                end = Offset(canvasWidth, chartHeight),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Card count row (above day labels)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            stats.forEach { stat ->
                val isToday = stat.date == todayStr
                Text(
                    text = if (stat.cardsStudied > 0) "${stat.cardsStudied}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    modifier = Modifier.width(36.dp),
                )
            }
        }

        // Day labels below
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            stats.forEach { stat ->
                val date = LocalDate.parse(stat.date, dateFormatter)
                val isToday = stat.date == todayStr
                Text(
                    text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("vi")).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp),
                )
            }
        }
    }
}

