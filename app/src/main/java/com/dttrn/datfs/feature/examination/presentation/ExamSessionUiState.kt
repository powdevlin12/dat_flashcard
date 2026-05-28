package com.dttrn.datfs.feature.examination.presentation

import com.dttrn.datfs.core.domain.model.Flashcard

data class ExamSessionUiState(
    val isLoading: Boolean = true,
    val deckTitle: String = "",
    val questions: List<ExamQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val totalQuestions: Int = 0,
    val isLastQuestion: Boolean = false,
    val timeLimitMinutes: Int? = null,
    val timeRemainingSeconds: Int = 0,
    val isTimeWarning: Boolean = false,
    val isSubmitted: Boolean = false,
    val showExitDialog: Boolean = false,
    val error: String? = null,
    val writeDirection: WriteDirection = WriteDirection.BACK,
    val isWriteInputFocused: Boolean = false,
    val dictationPlayCount: Int = 0,
)

data class ExamQuestion(
    val card: Flashcard,
    val questionType: QuestionType,
    val options: List<String> = emptyList(),
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val dictationPlayCount: Int = 0,
)
