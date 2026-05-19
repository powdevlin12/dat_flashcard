package com.dttrn.datfs.feature.statistics.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.feature.statistics.domain.usecase.DeckPerformance
import kotlinx.coroutines.launch

/**
 * List tiến độ học từng deck — mỗi dòng là 1 horizontal progress bar.
 */
@Composable
fun DeckProgressList(
    performances: List<DeckPerformance>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (performances.isEmpty()) {
            Text(
                "Chưa có dữ liệu deck",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        performances.take(8).forEach { perf ->
            DeckProgressRow(performance = perf)
        }
    }
}

@Composable
private fun DeckProgressRow(performance: DeckPerformance) {
    val deckColor = runCatching {
        Color(android.graphics.Color.parseColor(performance.colorHex))
    }.getOrElse { MaterialTheme.colorScheme.primary }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface

    val animProgress = remember(performance.deckId) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(performance.progress) {
        scope.launch {
            animProgress.animateTo(
                targetValue = performance.progress.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(deckColor)
                    }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = performance.deckTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${performance.knownCards}/${performance.totalCards}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${(performance.progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = deckColor,
                fontSize = 12.sp,
            )
        }

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .drawBehind {
                    // Track
                    drawRoundRect(
                        color = trackColor,
                        size = size,
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    // Fill
                    val fillWidth = size.width * animProgress.value
                    if (fillWidth > 0) {
                        drawRoundRect(
                            color = deckColor,
                            topLeft = Offset.Zero,
                            size = Size(fillWidth, size.height),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                        )
                    }
                }
        )
    }
}
