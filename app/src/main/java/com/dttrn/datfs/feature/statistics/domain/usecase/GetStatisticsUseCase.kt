package com.dttrn.datfs.feature.statistics.domain.usecase

import com.dttrn.datfs.core.domain.model.StudyStatistics
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DeckPerformance(
    val deckId: String,
    val deckTitle: String,
    val colorHex: String,
    val totalCards: Int,
    val knownCards: Int,
    val progress: Float,
)

data class StatisticsData(
    val streak: Int,
    val totalCardsStudied: Int,
    val totalMinutesStudied: Int,
    val last7DaysStats: List<StudyStatistics>,       // bar chart
    val last84DaysStats: List<StudyStatistics>,      // calendar heatmap (12 weeks)
    val deckPerformances: List<DeckPerformance>,
    val overallAccuracy: Float,
    val cardsStudiedToday: Int,
)

class GetStatisticsUseCase @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val deckRepository: DeckRepository,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend operator fun invoke(): StatisticsData {
        val streak = reviewRepository.calculateStreak()

        // 84 days = 12 weeks for heatmap
        val last84 = reviewRepository.getLast84DaysStats()

        // Last 7 days — build complete list filling missing days with empty
        val last7 = buildLast7DaysList(last84)

        val totalCards = last84.sumOf { it.cardsStudied }
        val totalMinutes = last84.sumOf { it.minutesStudied }
        val totalCorrect = last84.sumOf { it.correctAnswers }
        val totalAnswers = last84.sumOf { it.totalAnswers }
        val accuracy = if (totalAnswers > 0) totalCorrect.toFloat() / totalAnswers else 0f

        val todayStr = LocalDate.now().format(formatter)
        val todayStats = last84.find { it.date == todayStr }
        val cardsToday = todayStats?.cardsStudied ?: 0

        // Deck performance from domain models
        // We'll return empty list; StatisticsViewModel will merge with deck data
        return StatisticsData(
            streak = streak,
            totalCardsStudied = totalCards,
            totalMinutesStudied = totalMinutes,
            last7DaysStats = last7,
            last84DaysStats = last84,
            deckPerformances = emptyList(),
            overallAccuracy = accuracy,
            cardsStudiedToday = cardsToday,
        )
    }

    /**
     * Build an ordered list of exactly 7 items (Sun→Sat or Mon→Sun depending on today)
     * filling days with no stats as zero-value entries.
     */
    private fun buildLast7DaysList(allStats: List<StudyStatistics>): List<StudyStatistics> {
        val statsMap = allStats.associateBy { it.date }
        val today = LocalDate.now()
        return (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong())
            val dateStr = date.format(formatter)
            statsMap[dateStr] ?: StudyStatistics(
                id = dateStr,
                date = dateStr,
                cardsStudied = 0,
                minutesStudied = 0,
                correctAnswers = 0,
                totalAnswers = 0,
                streakCount = 0,
            )
        }
    }
}
