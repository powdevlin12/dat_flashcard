package com.dttrn.datfs.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val deckResults: List<Deck> = emptyList(),
    val cardResults: List<Flashcard> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val recentSearches: List<String> = emptyList(),
)

enum class SearchTab { DECKS, CARDS }

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val deckRepository: DeckRepository,
    private val flashcardRepository: FlashcardRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        // Debounced search
        viewModelScope.launch {
            queryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .filter { it.length >= 2 }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, hasSearched = query.length >= 2) }
        queryFlow.value = query
        if (query.length < 2) {
            _uiState.update { it.copy(deckResults = emptyList(), cardResults = emptyList()) }
        }
    }

    fun onClearQuery() {
        _uiState.update { SearchUiState() }
        queryFlow.value = ""
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            try {
                // Chạy 2 truy vấn song song bằng coroutineScope + async trên IO dispatcher
                val (decks, cards) = withContext(Dispatchers.Default) {
                    coroutineScope {
                        val decksDeferred = async { deckRepository.searchDecksOnce(query) }
                        val cardsDeferred = async { flashcardRepository.searchCardsOnce(query) }
                        Pair(decksDeferred.await(), cardsDeferred.await())
                    }
                }
                _uiState.update {
                    it.copy(
                        isSearching = false,
                        deckResults = decks,
                        cardResults = cards,
                    )
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Coroutine bị cancel bởi search mới — không làm gì
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }
}
