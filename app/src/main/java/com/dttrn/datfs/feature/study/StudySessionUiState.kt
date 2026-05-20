package com.dttrn.datfs.feature.study

import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.study.SM2Algorithm

data class StudySessionUiState(
    val isLoading: Boolean = true,
    val deckTitle: String = "",
    val mode: StudyMode = StudyMode.SPACED_REPETITION,
    val currentCard: Flashcard? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val reviewedCount: Int = 0,
    val isFlipped: Boolean = false,    // For swipe/learn: card flipped?
    val showFrontFirst: Boolean = true, // true = show front first, false = show back first
    val progress: Float = 0f,
    val error: String? = null,
    val isComplete: Boolean = false,
    // Results tracking
    val sessionResults: List<CardResult> = emptyList(),
    // Quiz specific
    val quizOptions: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val isCorrect: Boolean? = null,
    // Write specific
    val writeAnswer: String = "",
    val isWriteCorrect: Boolean? = null,
    // Match specific
    val matchItems: List<MatchItem> = emptyList(),
    val selectedMatchId: String? = null,
)

data class CardResult(
    val card: Flashcard,
    val rating: Int,
    val sm2Result: SM2Algorithm.ReviewResult,
)

data class MatchItem(
    val id: String,
    val text: String,
    val type: MatchItemType,
    val cardId: String,
    val isMatched: Boolean = false,
    val isSelected: Boolean = false,
    val isError: Boolean = false,
)

enum class MatchItemType { FRONT, BACK }
