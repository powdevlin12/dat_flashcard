package com.dttrn.datfs.feature.study

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.core.domain.study.SM2Algorithm
import kotlin.math.roundToInt

/**
 * Màn hình kết quả sau khi hoàn thành study session.
 */
@Composable
fun StudyResultScreen(
    results: List<CardResult>,
    deckTitle: String,
    onDone: () -> Unit,
    onStudyAgain: () -> Unit,
) {
    val totalCount = results.size
    val correctCount = results.count { it.rating >= SM2Algorithm.Ratings.HARD }
    val accuracy = if (totalCount > 0) correctCount.toFloat() / totalCount else 0f

    val progressAnim by animateFloatAsState(
        targetValue = accuracy,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "accuracy",
    )

    Scaffold(
        topBar = {
            // No TopBar — full result immersion
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ===== Result Header =====
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                if (accuracy >= 0.7f)
                                    listOf(Color(0xFF4CAF50), Color(0xFF00BCD4))
                                else
                                    listOf(Color(0xFFFF9800), Color(0xFFFF6B6B))
                            )
                        )
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (accuracy >= 0.9f) "🎉 Tuyệt vời!"
                            else if (accuracy >= 0.7f) "👏 Tốt lắm!"
                            else if (accuracy >= 0.5f) "🙂 Cần cải thiện"
                            else "📚 Hãy ôn thêm nhé",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            deckTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                }
            }

            // ===== Accuracy Circle =====
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 32.dp),
                ) {
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 12.dp,
                            strokeCap = StrokeCap.Round,
                            color = if (accuracy >= 0.7f) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${(accuracy * 100).roundToInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Chính xác",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ===== Stats Row =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ResultStatCard(
                        label = "Đúng",
                        value = correctCount.toString(),
                        color = Color(0xFF4CAF50),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f),
                    )
                    ResultStatCard(
                        label = "Sai",
                        value = (totalCount - correctCount).toString(),
                        color = Color(0xFFF44336),
                        icon = Icons.Default.Cancel,
                        modifier = Modifier.weight(1f),
                    )
                    ResultStatCard(
                        label = "Tổng",
                        value = totalCount.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.Style,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ===== Rating Breakdown =====
            item {
                RatingBreakdown(results = results, total = totalCount)
                Spacer(Modifier.height(16.dp))
            }

            // ===== Cards that need review =====
            val failedCards = results.filter { it.rating < SM2Algorithm.Ratings.HARD }
            if (failedCards.isNotEmpty()) {
                item {
                    Text(
                        "Thẻ cần ôn thêm (${failedCards.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                items(failedCards) { result ->
                    FailedCardItem(
                        result = result,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp),
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            // ===== Action Buttons =====
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = onStudyAgain,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Học lại")
                    }
                    OutlinedButton(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Xong")
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ResultStatCard(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RatingBreakdown(results: List<CardResult>, total: Int) {
    val ratings = listOf(
        "Dễ" to SM2Algorithm.Ratings.EASY,
        "Tốt" to SM2Algorithm.Ratings.GOOD,
        "Khó" to SM2Algorithm.Ratings.HARD,
        "Quên" to SM2Algorithm.Ratings.AGAIN,
    )
    val colors = listOf(Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFF44336))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Phân tích kết quả", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            ratings.forEachIndexed { index, (label, rating) ->
                val count = results.count { it.rating == rating }
                val fraction = if (total > 0) count.toFloat() / total else 0f

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(40.dp))
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = colors[index],
                        trackColor = colors[index].copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun FailedCardItem(result: CardResult, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF44336)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    result.card.frontText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    result.card.backText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Ôn lại: ${result.sm2Result.newIntervalDays}d",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
