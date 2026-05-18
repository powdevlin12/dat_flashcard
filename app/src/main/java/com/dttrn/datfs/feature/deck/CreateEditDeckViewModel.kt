package com.dttrn.datfs.feature.deck

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.usecase.deck.CreateDeckUseCase
import com.dttrn.datfs.core.domain.usecase.deck.UpdateDeckUseCase
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateEditDeckViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
    private val createDeckUseCase: CreateDeckUseCase,
    private val updateDeckUseCase: UpdateDeckUseCase,
) : ViewModel() {

    private val editDeckId: String? = savedStateHandle[Screen.CreateEditDeck.ARG_DECK_ID]

    private val _uiState = MutableStateFlow(
        CreateEditDeckUiState(isEditing = editDeckId != null)
    )
    val uiState: StateFlow<CreateEditDeckUiState> = _uiState.asStateFlow()

    init {
        if (editDeckId != null) loadDeckForEdit(editDeckId)
    }

    private fun loadDeckForEdit(deckId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deckRepository.getDeckById(deckId).first()?.let { deck ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        title = deck.title,
                        description = deck.description ?: "",
                        category = deck.category ?: "",
                        tags = deck.tags,
                        colorHex = deck.colorHex,
                    )
                }
            } ?: _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ===== Field updates =====

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onCategoryChange(value: String) {
        _uiState.update { it.copy(category = value) }
    }

    fun onColorChange(hex: String) {
        _uiState.update { it.copy(colorHex = hex) }
    }

    fun onTagInputChange(value: String) {
        _uiState.update { it.copy(tagInput = value) }
    }

    fun onAddTag() {
        val tag = _uiState.value.tagInput.trim()
        if (tag.isBlank()) return
        if (_uiState.value.tags.size >= 10) {
            _uiState.update { it.copy(error = "Tối đa 10 tag") }
            return
        }
        if (tag !in _uiState.value.tags) {
            _uiState.update { it.copy(tags = it.tags + tag, tagInput = "") }
        }
    }

    fun onRemoveTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    // ===== Save =====

    fun onSave() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Tên bộ thẻ không được để trống") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = if (state.isEditing && editDeckId != null) {
                // Load existing deck to preserve other fields
                deckRepository.getDeckById(editDeckId).first()?.let { existing ->
                    updateDeckUseCase(
                        existing.copy(
                            title = state.title.trim(),
                            description = state.description.trim().ifBlank { null },
                            category = state.category.trim().ifBlank { null },
                            tags = state.tags,
                            colorHex = state.colorHex,
                        )
                    )
                }
            } else {
                createDeckUseCase(
                    title = state.title.trim(),
                    description = state.description.trim().ifBlank { null },
                    category = state.category.trim().ifBlank { null },
                    tags = state.tags,
                    colorHex = state.colorHex,
                ).let { null } // CreateDeckUseCase returns Result<String>
            }
            _uiState.update {
                it.copy(isLoading = false, isSaved = true, error = null)
            }
        }
    }

    fun onErrorDismissed() {
        _uiState.update { it.copy(error = null) }
    }
}
