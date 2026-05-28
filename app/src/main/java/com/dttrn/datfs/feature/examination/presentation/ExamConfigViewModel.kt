package com.dttrn.datfs.feature.examination.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExamConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckRepository: DeckRepository,
) : ViewModel() {

    val deckId: String = checkNotNull(savedStateHandle[Screen.ExamConfig.ARG_DECK_ID])

    private val _uiState = MutableStateFlow(ExamConfigUiState())
    val uiState: StateFlow<ExamConfigUiState> = _uiState.asStateFlow()

    private val validTimeLimits = setOf(null, 5, 10, 15, 30)

    init {
        val previousConfig = savedStateHandle.get<String>(Screen.ExamConfig.ARG_PREVIOUS_CONFIG)
        viewModelScope.launch {
            loadDeckInfo()
            if (previousConfig != null) {
                restoreConfig(previousConfig)
            }
        }
    }

    private suspend fun loadDeckInfo() {
        deckRepository.getDeckById(deckId).first()?.let { deck ->
            val totalCards = deck.cardCount
            _uiState.update {
                it.copy(
                    deckTitle = deck.title,
                    totalCards = totalCards,
                    questionCount = totalCards.coerceAtLeast(0),
                    canStart = totalCards >= 5,
                    error = when {
                        totalCards == 0 -> "Deck trống, không thể tạo bài kiểm tra"
                        totalCards < 5 -> "Cần ít nhất 5 thẻ để tạo bài kiểm tra"
                        else -> null
                    },
                )
            }
        }
    }

    private fun restoreConfig(config: String) {
        try {
            val parts = config.split("|")
            if (parts.size >= 3) {
                val count = parts[0].toIntOrNull()
                val type = runCatching { QuestionType.valueOf(parts[1]) }.getOrNull()
                val timeLimit = parts[2].toIntOrNull().let { if (it == -1) null else it }
                val direction = if (parts.size >= 4) {
                    runCatching { WriteDirection.valueOf(parts[3]) }.getOrNull()
                } else null
                _uiState.update {
                    it.copy(
                        questionCount = count?.coerceIn(5, it.totalCards) ?: it.questionCount,
                        questionType = type ?: it.questionType,
                        timeLimitMinutes = if (timeLimit in validTimeLimits) timeLimit else it.timeLimitMinutes,
                        writeDirection = direction ?: it.writeDirection,
                    )
                }
            }
        } catch (_: Exception) {
            // Invalid config format, keep defaults
        }
    }

    fun onQuestionCountChange(count: Int) {
        _uiState.update {
            it.copy(questionCount = count.coerceIn(5, it.totalCards.coerceAtLeast(5)))
        }
    }

    fun onQuestionTypeChange(type: QuestionType) {
        _uiState.update { it.copy(questionType = type) }
    }

    fun onWriteDirectionChange(direction: WriteDirection) {
        _uiState.update { it.copy(writeDirection = direction) }
    }

    fun onTimeLimitChange(minutes: Int?) {
        if (minutes in validTimeLimits) {
            _uiState.update { it.copy(timeLimitMinutes = minutes) }
        }
    }

    fun buildStartConfig(): String {
        val state = _uiState.value
        return "${state.questionCount}|${state.questionType.name}|${state.timeLimitMinutes ?: -1}|${state.writeDirection.name}"
    }
}
