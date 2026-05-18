package com.dttrn.datfs.feature.deck

import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.model.Flashcard

data class DeckDetailUiState(
    val isLoading: Boolean = false,
    val deck: Deck? = null,
    val cards: List<Flashcard> = emptyList(),
    val filteredCards: List<Flashcard> = emptyList(),
    val searchQuery: String = "",
    val selectedCardIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null,
    // Dialog states
    val showDeleteDeckDialog: Boolean = false,
    val showDeleteCardDialog: Boolean = false,
    val cardToDelete: Flashcard? = null,
)

data class CreateEditDeckUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val tags: List<String> = emptyList(),
    val colorHex: String = "#4A90E2",
    val tagInput: String = "",
    val error: String? = null,
    val titleError: String? = null,
    val isSaved: Boolean = false,
)
