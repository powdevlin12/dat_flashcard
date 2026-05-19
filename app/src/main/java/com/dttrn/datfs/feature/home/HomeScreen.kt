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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            HomeTopBar(
                onSearchClick = onNavigateToSearch,
                onSettingsClick = onNavigateToSettings,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Tạo bộ thẻ", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = onCreateDeck,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ===== Header Stats Card =====
            item {
                HomeStatsCard(
                    totalCards = uiState.totalCards,
                    deckCount = uiState.decks.size,
                    todayDue = uiState.todayDueCount,
                    studyStreak = uiState.studyStreak,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            
            // ===== List Title & Sort =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Danh sách bộ thẻ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            )
                        },
                        title = if (uiState.searchQuery.isNotBlank()) "Không tìm thấy kết quả"
                        else "Chưa có bộ thẻ nào",
                        description = if (uiState.searchQuery.isNotBlank()) "Thử tìm với từ khóa khác"
                        else "Tạo bộ thẻ đầu tiên để bắt đầu học!",
                        action = if (uiState.searchQuery.isBlank()) ({
                            Button(
                                onClick = onCreateDeck,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) { 
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tạo bộ thẻ ngay") 
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
    scrollBehavior: TopAppBarScrollBehavior
) {
    LargeTopAppBar(
        title = {
            Column {
                Text(
                    "FlashMind",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Learn smarter, not harder",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .background(Color.Transparent, shape = CircleShape)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(Color.Transparent, shape = CircleShape)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        )
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
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2563EB), // Vibrant Blue
                            Color(0xFF7C3AED), // Soft Violet
                        )
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Tổng quan học tập",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    value = deckCount.toString(),
                    label = "Bộ thẻ",
                    icon = Icons.Default.Folder,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = totalCards.toString(),
                    label = "Tổng thẻ",
                    icon = Icons.Default.Style,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatItem(
                    value = todayDue.toString(),
                    label = "Cần ôn",
                    icon = Icons.Default.Event,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    value = "$studyStreak ngày",
                    label = "Chuỗi học",
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), shape = CircleShape)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
                label = { 
                    Text(
                        text = filter.displayName,
                        fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.Medium
                    ) 
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedFilter == filter,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                Icons.Default.Sort, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = sortLabel, 
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        DropdownMenu(
            expanded = expanded, 
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(
                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
                RoundedCornerShape(12.dp)
            )
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
                            color = if (sort == selectedSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = { onSortChange(sort); expanded = false },
                    leadingIcon = if (sort == selectedSort) ({
                        Icon(
                            Icons.Default.Check, 
                            contentDescription = null, 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }) else null,
                )
            }
        }
    }
}
