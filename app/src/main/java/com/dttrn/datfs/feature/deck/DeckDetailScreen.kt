package com.dttrn.datfs.feature.deck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.ui.components.ConfirmDeleteDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    onBack: () -> Unit,
    onEditDeck: (String) -> Unit,
    onAddCard: (String) -> Unit,
    onEditCard: (String, String) -> Unit,
    onStartStudy: (String) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deck = uiState.deck

    // ===== Dialogs =====
    if (uiState.showDeleteDeckDialog) {
        ConfirmDeleteDialog(
            title = "Xóa bộ thẻ",
            message = "Bộ thẻ và tất cả ${uiState.cards.size} thẻ sẽ bị xóa vĩnh viễn.",
            onConfirm = { viewModel.onDeleteDeckConfirmed(onBack) },
            onDismiss = viewModel::onDeleteDeckDismissed,
        )
    }
    if (uiState.showDeleteCardDialog && uiState.cardToDelete != null) {
        ConfirmDeleteDialog(
            title = "Xóa thẻ",
            message = "Bạn chắc chắn muốn xóa thẻ này?",
            onConfirm = viewModel::onDeleteCardConfirmed,
            onDismiss = viewModel::onDeleteCardDismissed,
        )
    }

    Scaffold(
        topBar = {
            DeckDetailTopBar(
                deck = deck,
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedCardIds.size,
                totalCount = uiState.cards.size,
                onBack = onBack,
                onEdit = { deck?.let { onEditDeck(it.id) } },
                onToggleFavorite = viewModel::onToggleFavorite,
                onArchive = viewModel::onArchive,
                onDelete = viewModel::onDeleteDeckRequest,
                onToggleSelectionMode = viewModel::onToggleSelectionMode,
                onSelectAll = viewModel::onSelectAll,
                onBulkDelete = viewModel::onBulkDelete,
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // Study button
                    if (uiState.cards.isNotEmpty()) {
                        SmallFloatingActionButton(
                            onClick = { deck?.let { onStartStudy(it.id) } },
                            containerColor = MaterialTheme.colorScheme.tertiary,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Học ngay")
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // Add card button
                    FloatingActionButton(
                        onClick = { deck?.let { onAddCard(it.id) } },
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm thẻ")
                    }
                }
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // Deck stats header
            deck?.let { d ->
                item {
                    DeckStatsHeader(
                        deck = d,
                        cardCount = uiState.cards.size,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Tìm thẻ...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    )
                )
            }

            // Card list
            if (uiState.filteredCards.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (uiState.searchQuery.isNotBlank()) "Không tìm thấy thẻ nào"
                                else "Chưa có thẻ nào",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            if (uiState.searchQuery.isBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Nhấn nút + để thêm thẻ đầu tiên",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.filteredCards, key = { it.id }) { card ->
                    FlashcardItem(
                        card = card,
                        isSelected = card.id in uiState.selectedCardIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) viewModel.onToggleCardSelection(card.id)
                            else deck?.let { onEditCard(it.id, card.id) }
                        },
                        onLongClick = {
                            if (!uiState.isSelectionMode) viewModel.onToggleSelectionMode()
                            viewModel.onToggleCardSelection(card.id)
                        },
                        onDelete = { viewModel.onDeleteCardRequest(card) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ===== Sub-composables =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckDetailTopBar(
    deck: Deck?,
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onBulkDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            if (isSelectionMode) {
                Text("$selectedCount/$totalCount đã chọn", fontWeight = FontWeight.Bold)
            } else {
                Text(
                    deck?.title ?: "Bộ thẻ",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = if (isSelectionMode) onToggleSelectionMode else onBack) {
                Icon(
                    if (isSelectionMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
        },
        actions = {
            if (isSelectionMode) {
                if (selectedCount < totalCount) {
                    IconButton(onClick = onSelectAll) {
                        Icon(Icons.Default.SelectAll, contentDescription = "Chọn tất cả")
                    }
                }
                IconButton(onClick = onBulkDelete, enabled = selectedCount > 0) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa đã chọn",
                        tint = if (selectedCount > 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                deck?.let { d ->
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (d.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (d.isFavorite) Color(0xFFFF6B6B)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa") },
                            onClick = { menuExpanded = false; onEdit() },
                        )
                        DropdownMenuItem(
                            text = { Text(if (deck?.isArchived == true) "Bỏ lưu trữ" else "Lưu trữ") },
                            onClick = { menuExpanded = false; onArchive() },
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Xóa bộ thẻ", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() },
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
        )
    )
}

@Composable
private fun DeckStatsHeader(deck: Deck, cardCount: Int, modifier: Modifier = Modifier) {
    val deckColor = remember(deck.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(deck.colorHex)) }
            .getOrDefault(Color(0xFF4A90E2))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = deckColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(24.dp)) {
            if (!deck.description.isNullOrBlank()) {
                Text(deck.description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                StatChip("$cardCount thẻ", Icons.Default.Style, deckColor)
                StatChip("${(deck.studyProgress * 100).toInt()}%", Icons.Default.TrendingUp, deckColor)
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { deck.studyProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = deckColor,
                trackColor = deckColor.copy(alpha = 0.15f),
            )
        }
    }
}

@Composable
private fun StatChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FlashcardItem(
    card: Flashcard,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Selection checkbox
            AnimatedVisibility(visible = isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.frontText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.backText.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        card.backText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Tags/badges
                if (card.isKnown || card.dueDate != null) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (card.isKnown) {
                            Box(
                                Modifier.clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Đã thuộc", style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (!isSelectionMode) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
