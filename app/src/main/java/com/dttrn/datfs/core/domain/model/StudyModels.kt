package com.dttrn.datfs.core.domain.model

import com.dttrn.datfs.core.data.local.entity.StudyMode

data class ReviewSession(
    val id: String,
    val deckId: String,
    val studyMode: StudyMode,
    val startedAt: Long,
    val endedAt: Long? = null,
    val totalCards: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0,
    val durationSeconds: Int? = null,
) {
    val accuracyRate: Float
        get() = if (totalCards > 0) correctCount.toFloat() / totalCards else 0f
}

data class StudyStatistics(
    val id: String,
    val date: String,                   // YYYY-MM-DD
    val cardsStudied: Int = 0,
    val minutesStudied: Int = 0,
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val streakCount: Int = 0,
) {
    val accuracyRate: Float
        get() = if (totalAnswers > 0) correctAnswers.toFloat() / totalAnswers else 0f
}

/** Summary tổng hợp cho Home screen banner */
data class StudySummary(
    val todayDueCount: Int = 0,
    val studyStreak: Int = 0,
    val totalCardsLearned: Int = 0,
    val accuracyRate30Days: Float = 0f,
)
