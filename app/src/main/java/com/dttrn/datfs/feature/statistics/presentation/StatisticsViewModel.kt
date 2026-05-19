package com.dttrn.datfs.feature.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.feature.statistics.domain.usecase.DeckPerformance
import com.dttrn.datfs.feature.statistics.domain.usecase.GetStatisticsUseCase
import com.dttrn.datfs.feature.statistics.domain.usecase.StatisticsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StatisticsUiState {
    data object Loading : StatisticsUiState()
    data class Success(
        val data: StatisticsData,
        val deckPerformances: List<DeckPerformance>,
    ) : StatisticsUiState()
    data class Error(val message: String) : StatisticsUiState()
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val deckRepository: DeckRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatisticsUiState>(StatisticsUiState.Loading)
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = StatisticsUiState.Loading
            try {
                val statsData = getStatisticsUseCase()
                val decks = deckRepository.getActiveDecks().first()

                val deckPerfs = decks.map { deck ->
                    val total = deck.cardCount
                    val known = (deck.studyProgress * total).toInt()
                    DeckPerformance(
                        deckId = deck.id,
                        deckTitle = deck.title,
                        colorHex = deck.colorHex ?: "#4A90E2",
                        totalCards = total,
                        knownCards = known,
                        progress = deck.studyProgress,
                    )
                }.sortedByDescending { it.progress }

                _uiState.value = StatisticsUiState.Success(
                    data = statsData,
                    deckPerformances = deckPerfs,
                )
            } catch (e: Exception) {
                _uiState.value = StatisticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
