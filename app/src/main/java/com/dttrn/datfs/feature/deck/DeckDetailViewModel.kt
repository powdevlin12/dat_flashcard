package com.dttrn.datfs.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.usecase.card.BulkDeleteCardsUseCase
import com.dttrn.datfs.core.domain.usecase.card.DeleteCardUseCase
import com.dttrn.datfs.core.domain.usecase.deck.ArchiveDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.DeleteDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.ToggleFavoriteDeckUseCase
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
    private val deleteCardUseCase: DeleteCardUseCase,
    private val bulkDeleteCardsUseCase: BulkDeleteCardsUseCase,
    private val deleteDeckUseCase: DeleteDeckUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteDeckUseCase,
    private val archiveDeckUseCase: ArchiveDeckUseCase,
) : ViewModel() {

    private val deckId: String = checkNotNull(savedStateHandle[Screen.DeckDetail.ARG_DECK_ID])

    private val _uiState = MutableStateFlow(DeckDetailUiState(isLoading = true))
    val uiState: StateFlow<DeckDetailUiState> = _uiState.asStateFlow()

    init {
        observeDeck()
        observeCards()
    }

    private fun observeDeck() {
        viewModelScope.launch {
            deckRepository.getDeckById(deckId).collect { deck ->
                _uiState.update { it.copy(deck = deck, isLoading = false) }
            }
        }
    }

    private fun observeCards() {
        viewModelScope.launch {
            flashcardRepository.getCardsByDeck(deckId).collect { cards ->
                val query = _uiState.value.searchQuery
                _uiState.update {
                    it.copy(
                        cards = cards,
                        filteredCards = applySearch(cards, query),
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredCards = applySearch(it.cards, query),
            )
        }
    }

    fun onToggleFavorite() {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            toggleFavoriteUseCase(deck.id, !deck.isFavorite)
        }
    }

    fun onArchive() {
        val deck = _uiState.value.deck ?: return
        viewModelScope.launch {
            archiveDeckUseCase(deck.id, !deck.isArchived)
        }
    }

    fun onDeleteDeckRequest() {
        _uiState.update { it.copy(showDeleteDeckDialog = true) }
    }

    fun onDeleteDeckConfirmed(onDeleted: () -> Unit) {
        viewModelScope.launch {
            deleteDeckUseCase(deckId)
            onDeleted()
        }
    }

    fun onDeleteDeckDismissed() {
        _uiState.update { it.copy(showDeleteDeckDialog = false) }
    }

    fun onDeleteCardRequest(card: Flashcard) {
        _uiState.update { it.copy(showDeleteCardDialog = true, cardToDelete = card) }
    }

    fun onDeleteCardConfirmed() {
        val card = _uiState.value.cardToDelete ?: return
        viewModelScope.launch {
            deleteCardUseCase(card.id, deckId)
            _uiState.update { it.copy(showDeleteCardDialog = false, cardToDelete = null) }
        }
    }

    fun onDeleteCardDismissed() {
        _uiState.update { it.copy(showDeleteCardDialog = false, cardToDelete = null) }
    }

    // ===== Selection Mode =====

    fun onToggleSelectionMode() {
        _uiState.update {
            it.copy(
                isSelectionMode = !it.isSelectionMode,
                selectedCardIds = emptySet(),
            )
        }
    }

    fun onToggleCardSelection(cardId: String) {
        _uiState.update {
            val newSet = it.selectedCardIds.toMutableSet()
            if (cardId in newSet) newSet.remove(cardId) else newSet.add(cardId)
            it.copy(selectedCardIds = newSet)
        }
    }

    fun onSelectAll() {
        _uiState.update { it.copy(selectedCardIds = it.cards.map { c -> c.id }.toSet()) }
    }

    fun onBulkDelete() {
        val ids = _uiState.value.selectedCardIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            bulkDeleteCardsUseCase(ids, deckId)
            _uiState.update { it.copy(isSelectionMode = false, selectedCardIds = emptySet()) }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applySearch(cards: List<Flashcard>, query: String): List<Flashcard> {
        if (query.isBlank()) return cards
        return cards.filter {
            it.frontText.contains(query, ignoreCase = true) ||
                it.backText.contains(query, ignoreCase = true) ||
                it.note?.contains(query, ignoreCase = true) == true
        }
    }
}
