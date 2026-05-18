package com.dttrn.datfs.feature.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyResultUiState(
    val isLoading: Boolean = false,
    val deckTitle: String = "",
    val deckId: String = "",
    // Results are passed via SharedViewModel — see StudyResultScreen
)

@HiltViewModel
class StudyResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.StudySession.ARG_DECK_ID])

    private val _uiState = MutableStateFlow(StudyResultUiState(deckId = deckId))
    val uiState: StateFlow<StudyResultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            deckRepository.getDeckById(deckId).first()?.let { deck ->
                _uiState.update { it.copy(deckTitle = deck.title) }
            }
        }
    }
}
