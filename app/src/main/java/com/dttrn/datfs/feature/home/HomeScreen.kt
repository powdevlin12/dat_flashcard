package com.dttrn.datfs.feature.home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateDeck,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo bộ thẻ", modifier = Modifier.size(28.dp))
            }
        },
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            // ===== Top App Bar =====
            item {
                HomeTopBar(
                    onSearchClick = onNavigateToSearch,
                    onSettingsClick = onNavigateToSettings,
                )
            }

            // ===== Greeting + Weekly Stats =====
            item {
                GreetingSection(
                    studyStreak = uiState.studyStreak,
                    todayDue = uiState.todayDueCount,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp),
                )
            }

            // ===== Weekly Performance Card =====
            item {
                WeeklyPerformanceCard(
                    totalCards = uiState.totalCards,
                    deckCount = uiState.decks.size,
                    todayDue = uiState.todayDueCount,
                    studyStreak = uiState.studyStreak,
                    onNavigateToStats = onNavigateToStats,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                )
            }

            // ===== "Your Decks" Header + Sort =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Decks",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.3).sp,
                    )
                    SortRow(
                        selectedSort = uiState.selectedSort,
                        onSortChange = viewModel::onSortChange,
                    )
                }
            }

            // ===== Filter Chips =====
            item {
                FilterRow(
                    selectedFilter = uiState.selectedFilter,
                    onFilterChange = viewModel::onFilterChange,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ===== Deck List =====
            if (uiState.filteredDecks.isEmpty()) {
                item {
                    EmptyState(
                        icon = {
                            Icon(
                                Icons.Default.LibraryBooks,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            )
                        },
                        title = if (uiState.searchQuery.isNotBlank()) "Không tìm thấy kết quả"
                        else "Chưa có bộ thẻ nào",
                        description = if (uiState.searchQuery.isNotBlank()) "Thử tìm với từ khóa khác"
                        else "Nhấn + để tạo bộ thẻ đầu tiên!",
                        action = if (uiState.searchQuery.isBlank()) ({
                            Button(
                                onClick = onCreateDeck,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tạo bộ thẻ ngay", fontWeight = FontWeight.SemiBold)
                            }
                        }) else null,
                        modifier = Modifier.padding(vertical = 64.dp),
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
                                    DeckMenuAction.EDIT -> onDeckClick(deck.id)
                                    DeckMenuAction.DUPLICATE -> viewModel.onDuplicateRequest(deck)
                                    DeckMenuAction.ARCHIVE -> viewModel.onArchiveRequest(deck)
                                    DeckMenuAction.DELETE -> viewModel.onDeleteRequest(deck)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

// ===== Sub-composables =====

@Composable
private fun HomeTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Brand
        Column {
            Text(
                text = "FlashMind",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.5).sp,
            )
            Text(
                text = "Learn smarter, not harder",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Tìm kiếm",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Cài đặt",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun GreetingSection(
    studyStreak: Int,
    todayDue: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = if (studyStreak > 3) "You're on fire! 🔥" else "Good day!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.3).sp,
        )
        if (todayDue > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Bạn có $todayDue thẻ cần ôn hôm nay",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeeklyPerformanceCard(
    totalCards: Int,
    deckCount: Int,
    todayDue: Int,
    studyStreak: Int,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onNavigateToStats,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Weekly Performance",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MiniStatItem(
                        value = deckCount.toString(),
                        label = "Bộ thẻ",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatItem(
                        value = totalCards.toString(),
                        label = "Tổng thẻ",
                        icon = Icons.Default.Style,
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatItem(
                        value = todayDue.toString(),
                        label = "Cần ôn",
                        icon = Icons.Default.Event,
                        modifier = Modifier.weight(1f),
                    )
                    MiniStatItem(
                        value = "${studyStreak}d",
                        label = "Streak",
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatItem(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selectedFilter: DeckFilter,
    onFilterChange: (DeckFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(DeckFilter.entries) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text = filter.displayName,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = Color.Transparent,
                ),
                elevation = FilterChipDefaults.filterChipElevation(elevation = 1.dp),
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

    Box {
        TextButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                Icons.Default.Sort,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = sortLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DeckSortOrder.entries.forEach { sort ->
                DropdownMenuItem(
                    text = {
                        Text(
                            when (sort) {
                                DeckSortOrder.TITLE -> "A–Z"
                                DeckSortOrder.UPDATED -> "Mới cập nhật"
                                DeckSortOrder.CREATED -> "Mới tạo"
                                DeckSortOrder.PROGRESS -> "Tiến độ"
                                DeckSortOrder.CARD_COUNT -> "Số thẻ"
                            },
                            fontWeight = if (sort == selectedSort) FontWeight.Bold else FontWeight.Normal,
                            color = if (sort == selectedSort) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    onClick = { onSortChange(sort); expanded = false },
                    leadingIcon = if (sort == selectedSort) ({
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }) else null,
                )
            }
        }
    }
}
