package com.dttrn.datfs.feature.card

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.usecase.card.AddCardUseCase
import com.dttrn.datfs.core.domain.usecase.card.UpdateCardUseCase
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CardEditorUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val frontText: String = "",
    val backText: String = "",
    val pronunciation: String = "",
    val exampleSentence: String = "",
    val note: String = "",
    val difficultyLevel: Int = 2, // 1=Easy, 2=Normal, 3=Hard
    val frontError: String? = null,
    val backError: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    // Continue adding more cards
    val continueAdding: Boolean = false,
)

@HiltViewModel
class CardEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val flashcardRepository: FlashcardRepository,
    private val addCardUseCase: AddCardUseCase,
    private val updateCardUseCase: UpdateCardUseCase,
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.CardEditor.ARG_DECK_ID])
    private val editCardId: String? = savedStateHandle[Screen.CardEditor.ARG_CARD_ID]

    private val _uiState = MutableStateFlow(CardEditorUiState(isEditing = editCardId != null))
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    init {
        if (editCardId != null) loadCardForEdit(editCardId)
    }

    private fun loadCardForEdit(cardId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            flashcardRepository.getCardById(cardId)?.let { card ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        frontText = card.frontText,
                        backText = card.backText,
                        pronunciation = card.pronunciation ?: "",
                        exampleSentence = card.exampleSentence ?: "",
                        note = card.note ?: "",
                        difficultyLevel = card.difficultyLevel,
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onFrontTextChange(value: String) = _uiState.update { it.copy(frontText = value, frontError = null) }
    fun onBackTextChange(value: String) = _uiState.update { it.copy(backText = value, backError = null) }
    fun onPronunciationChange(value: String) = _uiState.update { it.copy(pronunciation = value) }
    fun onExampleChange(value: String) = _uiState.update { it.copy(exampleSentence = value) }
    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }
    fun onDifficultyChange(level: Int) = _uiState.update { it.copy(difficultyLevel = level) }

    fun onSave(continueAdding: Boolean = false) {
        val state = _uiState.value
        var hasError = false

        if (state.frontText.isBlank()) {
            _uiState.update { it.copy(frontError = "Mặt trước không được để trống") }
            hasError = true
        }
        if (state.backText.isBlank()) {
            _uiState.update { it.copy(backError = "Mặt sau không được để trống") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (state.isEditing && editCardId != null) {
                flashcardRepository.getCardById(editCardId)?.let { existing ->
                    updateCardUseCase(
                        existing.copy(
                            frontText = state.frontText.trim(),
                            backText = state.backText.trim(),
                            pronunciation = state.pronunciation.trim().ifBlank { null },
                            exampleSentence = state.exampleSentence.trim().ifBlank { null },
                            note = state.note.trim().ifBlank { null },
                            difficultyLevel = state.difficultyLevel,
                        )
                    )
                }
            } else {
                addCardUseCase(
                    deckId = deckId,
                    frontText = state.frontText.trim(),
                    backText = state.backText.trim(),
                    pronunciation = state.pronunciation.trim().ifBlank { null },
                    exampleSentence = state.exampleSentence.trim().ifBlank { null },
                    note = state.note.trim().ifBlank { null },
                    difficultyLevel = state.difficultyLevel,
                )
            }

            if (continueAdding && !state.isEditing) {
                // Reset form for next card
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        frontText = "",
                        backText = "",
                        pronunciation = "",
                        exampleSentence = "",
                        note = "",
                        isSaved = false,
                        continueAdding = true,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            }
        }
    }

    fun onContinueAddingReset() {
        _uiState.update { it.copy(continueAdding = false) }
    }

    fun onErrorDismissed() = _uiState.update { it.copy(error = null) }
}
