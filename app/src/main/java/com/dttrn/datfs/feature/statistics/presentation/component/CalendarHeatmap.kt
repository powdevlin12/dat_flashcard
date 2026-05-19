package com.dttrn.datfs.feature.statistics.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.core.domain.model.StudyStatistics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar heatmap 12 tuần (84 ngày).
 * Mỗi ô = 1 ngày, màu sắc đậm dần theo số thẻ học.
 * Vẽ thuần Compose Canvas.
 */
@Composable
fun CalendarHeatmap(
    stats: List<StudyStatistics>,
    modifier: Modifier = Modifier,
    cellSize: Dp = 14.dp,
    cellSpacing: Dp = 3.dp,
    emptyColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    filledColor: Color = MaterialTheme.colorScheme.primary,
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val statsMap = stats.associateBy { it.date }
    val maxCards = stats.maxOfOrNull { it.cardsStudied } ?: 1
    val today = LocalDate.now()

    // Build 84-day grid aligned to full weeks (Mon-Sun)
    // Start from 12 weeks ago, aligned to Monday
    val startOfGrid = today.minusDays(83)
    val days = (0..83).map { startOfGrid.plusDays(it.toLong()) }

    // Week labels (M T W T F S S)
    val weekDayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    // Month labels — find first day of each month in range
    val monthLabels = mutableListOf<Pair<Int, String>>() // column index → label
    days.forEachIndexed { idx, date ->
        if (date.dayOfMonth == 1 || idx == 0) {
            val col = idx / 7
            val label = date.month.getDisplayName(TextStyle.SHORT, Locale("vi"))
            if (monthLabels.isEmpty() || monthLabels.last().first != col) {
                monthLabels.add(col to label)
            }
        }
    }

    val primaryColor = filledColor
    val todayStr = today.format(dateFormatter)

    Column(modifier = modifier) {
        // Month row header
        Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp)) {
            val totalCols = 12
            repeat(totalCols) { col ->
                val label = monthLabels.find { it.first == col }?.second ?: ""
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(cellSize + cellSpacing),
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        Row {
            // Day-of-week labels column
            Column(modifier = Modifier.width(18.dp)) {
                weekDayLabels.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.height(cellSize + cellSpacing),
                    )
                }
            }

            // Heatmap grid
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((cellSize + cellSpacing) * 7)
            ) {
                val cell = cellSize.toPx()
                val gap = cellSpacing.toPx()
                val stride = cell + gap

                days.forEachIndexed { idx, date ->
                    val col = idx / 7          // week column (0..11)
                    val row = date.dayOfWeek.value - 1   // Mon=0 .. Sun=6

                    val xPos = col * stride
                    val yPos = row * stride

                    val dateStr = date.format(dateFormatter)
                    val cardsStudied = statsMap[dateStr]?.cardsStudied ?: 0
                    val intensity = if (maxCards > 0) cardsStudied.toFloat() / maxCards else 0f

                    val cellColor = when {
                        cardsStudied == 0 -> emptyColor
                        else -> lerp(
                            primaryColor.copy(alpha = 0.25f),
                            primaryColor,
                            intensity.coerceIn(0f, 1f),
                        )
                    }

                    val isToday = dateStr == todayStr

                    drawRoundRect(
                        color = cellColor,
                        topLeft = Offset(xPos, yPos),
                        size = Size(cell, cell),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )

                    // Today highlight border
                    if (isToday) {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(xPos, yPos),
                            size = Size(cell, cell),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx()
                            ),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Ít",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(4.dp))
            listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { level ->
                val legColor = if (level == 0f) emptyColor
                else lerp(primaryColor.copy(alpha = 0.25f), primaryColor, level)
                Canvas(modifier = Modifier.size(cellSize)) {
                    drawRoundRect(
                        color = legColor,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Nhiều",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
