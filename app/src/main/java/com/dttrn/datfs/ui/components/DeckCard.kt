package com.dttrn.datfs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.ui.theme.DeckColors

/**
 * Card hiển thị một Deck trong danh sách Home.
 * Hỗ trợ swipe actions và long-press menu.
 */
@Composable
fun DeckCard(
    deck: Deck,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onMenuClick: (DeckMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deckColor = remember(deck.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(deck.colorHex)) }
            .getOrDefault(Color(0xFF4A90E2))
    }
    val onDeckColor = if (deckColor.luminance() > 0.5f) Color.Black else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ===== Header Row =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Color indicator circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(deckColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = deck.title.first().uppercaseChar().toString(),
                        color = onDeckColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!deck.category.isNullOrBlank()) {
                        Text(
                            text = deck.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Favorite button
                val favColor by animateColorAsState(
                    targetValue = if (deck.isFavorite) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(300),
                    label = "fav_color"
                )
                IconButton(onClick = onFavoriteToggle) {
                    Icon(
                        imageVector = if (deck.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (deck.isFavorite) "Bỏ yêu thích" else "Yêu thích",
                        tint = favColor,
                    )
                }

                // More menu
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DeckDropdownMenu(
                        expanded = menuExpanded,
                        isArchived = deck.isArchived,
                        onDismiss = { menuExpanded = false },
                        onAction = { action ->
                            menuExpanded = false
                            onMenuClick(action)
                        }
                    )
                }
            }

            // ===== Description =====
            if (!deck.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = deck.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            // ===== Progress Bar =====
            val progressAnim by animateFloatAsState(
                targetValue = deck.studyProgress,
                animationSpec = tween(600),
                label = "progress"
            )
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${deck.cardCount} thẻ",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(deck.studyProgress * 100).toInt()}% thành thạo",
                        style = MaterialTheme.typography.labelMedium,
                        color = deckColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressAnim },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = deckColor,
                    trackColor = deckColor.copy(alpha = 0.1f),
                )
            }

            // ===== Due count badge =====
            if (deck.dueCount > 0) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${deck.dueCount} cần ôn",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

// ===== Dropdown Menu =====
enum class DeckMenuAction { EDIT, DUPLICATE, ARCHIVE, DELETE }

@Composable
private fun DeckDropdownMenu(
    expanded: Boolean,
    isArchived: Boolean,
    onDismiss: () -> Unit,
    onAction: (DeckMenuAction) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Chỉnh sửa") },
            onClick = { onAction(DeckMenuAction.EDIT) }
        )
        DropdownMenuItem(
            text = { Text("Nhân bản") },
            onClick = { onAction(DeckMenuAction.DUPLICATE) }
        )
        DropdownMenuItem(
            text = { Text(if (isArchived) "Bỏ lưu trữ" else "Lưu trữ") },
            onClick = { onAction(DeckMenuAction.ARCHIVE) }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
            onClick = { onAction(DeckMenuAction.DELETE) }
        )
    }
}
