package com.dttrn.datfs.feature.examination.presentation

data class ExamConfigUiState(
    val deckTitle: String = "",
    val totalCards: Int = 0,
    val questionCount: Int = 0,
    val questionType: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val writeDirection: WriteDirection = WriteDirection.BACK,
    val timeLimitMinutes: Int? = null,
    val canStart: Boolean = false,
    val error: String? = null,
)

enum class QuestionType(val displayName: String) {
    MULTIPLE_CHOICE("Trắc nghiệm"),
    WRITE("Gõ đáp án"),
    DICTATION("Nghe chép chính tả"),
    MIXED("Hỗn hợp"),
}

enum class WriteDirection(val displayName: String) {
    BACK("Gõ mặt sau"),
    FRONT("Gõ mặt trước"),
}
