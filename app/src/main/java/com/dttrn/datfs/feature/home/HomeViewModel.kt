package com.dttrn.datfs.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.DeckSortOrder
import com.dttrn.datfs.core.domain.usecase.deck.ArchiveDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.DeleteDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.DuplicateDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.ToggleFavoriteDeckUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val deleteDeckUseCase: DeleteDeckUseCase,
    private val duplicateDeckUseCase: DuplicateDeckUseCase,
    private val archiveDeckUseCase: ArchiveDeckUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteDeckUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val todayEndMs: Long get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    init {
        observeDecks()
        observeCategories()
        observeDueCount()
    }

    private fun observeDecks() {
        viewModelScope.launch {
            combine(
                deckRepository.getActiveDecks(),
                deckRepository.getFavoriteDecks(),
                deckRepository.getArchivedDecks(),
            ) { active, favorites, archived ->
                Triple(active, favorites, archived)
            }.collect { (active, favorites, archived) ->
                val current = _uiState.value
                val decks = when (current.selectedFilter) {
                    DeckFilter.ALL -> active
                    DeckFilter.FAVORITES -> favorites
                    DeckFilter.ARCHIVED -> archived
                    DeckFilter.DUE_TODAY -> active.filter { it.dueCount > 0 }
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        decks = active,
                        filteredDecks = applySearch(decks, current.searchQuery),
                        totalCards = active.sumOf { deck -> deck.cardCount },
                    )
                }
            }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            deckRepository.getCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    private fun observeDueCount() {
        viewModelScope.launch {
            // Placeholder — will integrate with FlashcardRepository in Phase 3
            _uiState.update { it.copy(todayDueCount = 0) }
        }
    }

    // ===== Actions =====

    fun onSearchQueryChange(query: String) {
        val currentDecks = getDecksForCurrentFilter()
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredDecks = applySearch(currentDecks, query),
            )
        }
    }

    fun onFilterChange(filter: DeckFilter) {
        val decks = _uiState.value.decks
        val filtered = when (filter) {
            DeckFilter.ALL -> decks
            DeckFilter.FAVORITES -> decks.filter { it.isFavorite }
            DeckFilter.ARCHIVED -> decks // Archived will be loaded separately
            DeckFilter.DUE_TODAY -> decks.filter { it.dueCount > 0 }
        }
        _uiState.update {
            it.copy(
                selectedFilter = filter,
                filteredDecks = applySearch(filtered, it.searchQuery),
            )
        }
        if (filter == DeckFilter.ARCHIVED) loadArchivedDecks()
    }

    fun onSortChange(sort: DeckSortOrder) {
        _uiState.update { it.copy(selectedSort = sort) }
    }

    fun onCategoryChange(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onToggleFavorite(deck: Deck) {
        viewModelScope.launch {
            toggleFavoriteUseCase(deck.id, !deck.isFavorite)
        }
    }

    fun onArchiveRequest(deck: Deck) {
        viewModelScope.launch {
            archiveDeckUseCase(deck.id, !deck.isArchived)
        }
    }

    fun onDeleteRequest(deck: Deck) {
        _uiState.update { it.copy(showDeleteDialog = true, deckToDelete = deck) }
    }

    fun onDeleteConfirmed() {
        val deck = _uiState.value.deckToDelete ?: return
        viewModelScope.launch {
            deleteDeckUseCase(deck.id)
            _uiState.update { it.copy(showDeleteDialog = false, deckToDelete = null) }
        }
    }

    fun onDeleteDismissed() {
        _uiState.update { it.copy(showDeleteDialog = false, deckToDelete = null) }
    }

    fun onDuplicateRequest(deck: Deck) {
        _uiState.update { it.copy(showDuplicateDialog = true, deckToDuplicate = deck) }
    }

    fun onDuplicateConfirmed(newTitle: String) {
        val deck = _uiState.value.deckToDuplicate ?: return
        viewModelScope.launch {
            duplicateDeckUseCase(deck.id, newTitle)
            _uiState.update { it.copy(showDuplicateDialog = false, deckToDuplicate = null) }
        }
    }

    fun onDuplicateDismissed() {
        _uiState.update { it.copy(showDuplicateDialog = false, deckToDuplicate = null) }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }

    // ===== Helpers =====

    private fun getDecksForCurrentFilter(): List<Deck> {
        val state = _uiState.value
        return when (state.selectedFilter) {
            DeckFilter.ALL -> state.decks
            DeckFilter.FAVORITES -> state.decks.filter { it.isFavorite }
            DeckFilter.ARCHIVED -> state.decks.filter { it.isArchived }
            DeckFilter.DUE_TODAY -> state.decks.filter { it.dueCount > 0 }
        }
    }

    private fun loadArchivedDecks() {
        viewModelScope.launch {
            deckRepository.getArchivedDecks().first().let { archived ->
                _uiState.update { it.copy(filteredDecks = applySearch(archived, it.searchQuery)) }
            }
        }
    }

    private fun applySearch(decks: List<Deck>, query: String): List<Deck> {
        if (query.isBlank()) return decks
        return decks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.description?.contains(query, ignoreCase = true) == true ||
                it.category?.contains(query, ignoreCase = true) == true ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }
}
