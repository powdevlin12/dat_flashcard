package com.dttrn.datfs.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.domain.repository.DeckSortOrder
import com.dttrn.datfs.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDeckClick: (String) -> Unit,
    onCreateDeck: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStats: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ===== Dialogs =====
    if (uiState.showDeleteDialog && uiState.deckToDelete != null) {
        ConfirmDeleteDialog(
            title = "Xóa bộ thẻ",
            message = "Bạn chắc chắn muốn xóa \"${uiState.deckToDelete!!.title}\"? Tất cả thẻ trong bộ này sẽ bị xóa vĩnh viễn.",
            onConfirm = viewModel::onDeleteConfirmed,
            onDismiss = viewModel::onDeleteDismissed,
        )
    }
    if (uiState.showDuplicateDialog && uiState.deckToDuplicate != null) {
        DuplicateDeckDialog(
            originalTitle = uiState.deckToDuplicate!!.title,
            onConfirm = viewModel::onDuplicateConfirmed,
            onDismiss = viewModel::onDuplicateDismissed,
        )
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                onSearchClick = onNavigateToSearch,
                onSettingsClick = onNavigateToSettings,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Tạo bộ thẻ") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onCreateDeck,
                containerColor = MaterialTheme.colorScheme.primary,
            )
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            // ===== Header Stats Card =====
            item {
                HomeStatsCard(
                    totalCards = uiState.totalCards,
                    deckCount = uiState.decks.size,
                    todayDue = uiState.todayDueCount,
                    studyStreak = uiState.studyStreak,
                    modifier = Modifier.padding(16.dp),
                )
            }

            // ===== Filter Chips =====
            item {
                FilterRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterChange = viewModel::onFilterChange,
                )
            }

            // ===== Sort / Category =====
            item {
                SortRow(
                    selectedSort = uiState.selectedSort,
                    onSortChange = viewModel::onSortChange,
                )
            }

            // ===== Deck List =====
            if (uiState.filteredDecks.isEmpty()) {
                item {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Default.LibraryBooks,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        title = if (uiState.searchQuery.isNotBlank()) "Không tìm thấy kết quả"
                        else "Chưa có bộ thẻ nào",
                        description = if (uiState.searchQuery.isNotBlank()) "Thử tìm với từ khóa khác"
                        else "Tạo bộ thẻ đầu tiên để bắt đầu học!",
                        action = if (uiState.searchQuery.isBlank()) ({
                            Button(onClick = onCreateDeck) { Text("Tạo bộ thẻ") }
                        }) else null,
                        modifier = Modifier.padding(vertical = 48.dp),
                    )
                }
            } else {
                items(
                    items = uiState.filteredDecks,
                    key = { it.id },
                ) { deck ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                    ) {
                        DeckCard(
                            deck = deck,
                            onClick = { onDeckClick(deck.id) },
                            onFavoriteToggle = { viewModel.onToggleFavorite(deck) },
                            onMenuClick = { action ->
                                when (action) {
                                    DeckMenuAction.EDIT -> onDeckClick(deck.id) // Navigate to edit
                                    DeckMenuAction.DUPLICATE -> viewModel.onDuplicateRequest(deck)
                                    DeckMenuAction.ARCHIVE -> viewModel.onArchiveRequest(deck)
                                    DeckMenuAction.DELETE -> viewModel.onDeleteRequest(deck)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ===== Sub-composables =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    "FlashMind",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Học thẻ thông minh",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
            }
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Default.Settings, contentDescription = "Cài đặt")
            }
        },
    )
}

@Composable
private fun HomeStatsCard(
    totalCards: Int,
    deckCount: Int,
    todayDue: Int,
    studyStreak: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                StatItem(value = deckCount.toString(), label = "Bộ thẻ")
                StatDivider()
                StatItem(value = totalCards.toString(), label = "Thẻ")
                StatDivider()
                StatItem(value = todayDue.toString(), label = "Cần ôn", highlight = todayDue > 0)
                StatDivider()
                StatItem(value = "${studyStreak}🔥", label = "Chuỗi ngày")
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlight) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
    )
}

@Composable
private fun FilterRow(
    selectedFilter: DeckFilter,
    onFilterChange: (DeckFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(DeckFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.displayName) },
            )
        }
    }
}

@Composable
private fun SortRow(
    selectedSort: DeckSortOrder,
    onSortChange: (DeckSortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val sortLabel = when (selectedSort) {
        DeckSortOrder.TITLE -> "A–Z"
        DeckSortOrder.UPDATED -> "Mới cập nhật"
        DeckSortOrder.CREATED -> "Mới tạo"
        DeckSortOrder.PROGRESS -> "Tiến độ"
        DeckSortOrder.CARD_COUNT -> "Số thẻ"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Box {
            AssistChip(
                onClick = { expanded = true },
                label = { Text("Sắp xếp: $sortLabel") },
                leadingIcon = { Icon(Icons.Default.Sort, null, Modifier.size(16.dp)) },
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DeckSortOrder.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = {
                            Text(when (sort) {
                                DeckSortOrder.TITLE -> "A–Z"
                                DeckSortOrder.UPDATED -> "Mới cập nhật"
                                DeckSortOrder.CREATED -> "Mới tạo"
                                DeckSortOrder.PROGRESS -> "Tiến độ"
                                DeckSortOrder.CARD_COUNT -> "Số thẻ"
                            })
                        },
                        onClick = { onSortChange(sort); expanded = false },
                        leadingIcon = if (sort == selectedSort) ({
                            Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                        }) else null,
                    )
                }
            }
        }
    }
}
