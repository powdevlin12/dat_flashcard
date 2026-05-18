package com.dttrn.datfs.feature.home

import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.repository.DeckSortOrder

data class HomeUiState(
    val isLoading: Boolean = false,
    val decks: List<Deck> = emptyList(),
    val filteredDecks: List<Deck> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: DeckFilter = DeckFilter.ALL,
    val selectedSort: DeckSortOrder = DeckSortOrder.UPDATED,
    val selectedCategory: String? = null,
    val categories: List<String> = emptyList(),
    val todayDueCount: Int = 0,
    val studyStreak: Int = 0,
    val totalCards: Int = 0,
    val error: String? = null,
    // Dialog states
    val showDeleteDialog: Boolean = false,
    val showDuplicateDialog: Boolean = false,
    val deckToDelete: Deck? = null,
    val deckToDuplicate: Deck? = null,
)

enum class DeckFilter(val displayName: String) {
    ALL("Tất cả"),
    FAVORITES("Yêu thích"),
    ARCHIVED("Lưu trữ"),
    DUE_TODAY("Cần ôn hôm nay"),
}
