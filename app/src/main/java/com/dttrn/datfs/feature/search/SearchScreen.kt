package com.dttrn.datfs.feature.search

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.model.Flashcard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onDeckClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            SearchTopBar(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onBack = onBack,
                onClear = viewModel::onClearQuery,
                focusRequester = focusRequester,
            )
        },
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {

            // ===== Tabs =====
            if (uiState.hasSearched) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bộ thẻ (${uiState.deckResults.size})") },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Thẻ (${uiState.cardResults.size})") },
                    )
                }
            }

            // ===== Content =====
            if (!uiState.hasSearched) {
                // Empty / hint state
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nhập ít nhất 2 ký tự để tìm kiếm",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (uiState.isSearching) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (selectedTab == 0) {
                        if (uiState.deckResults.isEmpty()) {
                            item { EmptySearchResult(type = "bộ thẻ") }
                        } else {
                            items(uiState.deckResults, key = { it.id }) { deck ->
                                DeckSearchResultItem(deck = deck, onClick = { onDeckClick(deck.id) })
                            }
                        }
                    } else {
                        if (uiState.cardResults.isEmpty()) {
                            item { EmptySearchResult(type = "thẻ") }
                        } else {
                            items(uiState.cardResults, key = { it.id }) { card ->
                                CardSearchResultItem(card = card, onClick = {
                                    onDeckClick(card.deckId) // Navigate to deck containing this card
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===== Sub-composables =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Tìm bộ thẻ, từ vựng...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Default.Close, contentDescription = "Xóa")
                        }
                    }
                },
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
    )
}

@Composable
private fun DeckSearchResultItem(deck: Deck, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(deck.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                buildString {
                    deck.category?.let { append("$it • ") }
                    append("${deck.cardCount} thẻ")
                },
                style = MaterialTheme.typography.bodySmall,
            )
        },
        leadingContent = {
            Icon(Icons.Default.LibraryBooks, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    )
}

@Composable
private fun CardSearchResultItem(card: Flashcard, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(card.frontText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(card.backText, style = MaterialTheme.typography.bodySmall,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            Icon(Icons.Default.Style, contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    )
}

@Composable
private fun EmptySearchResult(type: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
        Text(
            "Không tìm thấy $type nào",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
