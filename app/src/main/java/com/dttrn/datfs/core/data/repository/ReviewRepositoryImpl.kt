package com.dttrn.datfs.core.data.repository

import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.dao.ReviewSessionDao
import com.dttrn.datfs.core.data.local.dao.StudyStatisticsDao
import com.dttrn.datfs.core.data.local.entity.ReviewSessionEntity
import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.data.local.entity.StudyStatisticsEntity
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.ReviewSession
import com.dttrn.datfs.core.domain.model.StudyStatistics
import com.dttrn.datfs.core.domain.model.StudySummary
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val reviewSessionDao: ReviewSessionDao,
    private val studyStatisticsDao: StudyStatisticsDao,
    private val flashcardDao: FlashcardDao,
) : ReviewRepository {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun saveSession(session: ReviewSession): Result<Unit> = runCatching {
        reviewSessionDao.insertSession(session.toEntity())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun getSessionsSince(fromTimeMs: Long): List<ReviewSession> =
        reviewSessionDao.getSessionsSince(fromTimeMs).map { it.toDomain() }

    override suspend fun recordStudyActivity(
        date: String,
        cardsStudied: Int,
        minutesStudied: Int,
        correctAnswers: Int,
        totalAnswers: Int,
    ): Result<Unit> = runCatching {
        val existing = studyStatisticsDao.getStatsByDate(date)
        if (existing != null) {
            studyStatisticsDao.incrementStats(
                date = date,
                cards = cardsStudied,
                minutes = minutesStudied,
                correct = correctAnswers,
                total = totalAnswers,
            )
        } else {
            val streak = calculateStreak()
            studyStatisticsDao.upsertStats(
                StudyStatisticsEntity(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    cardsStudied = cardsStudied,
                    minutesStudied = minutesStudied,
                    correctAnswers = correctAnswers,
                    totalAnswers = totalAnswers,
                    streakCount = streak + 1,
                )
            )
        }
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override fun getRecentStats(days: Int): Flow<List<StudyStatistics>> =
        studyStatisticsDao.getRecentStats(days).map { list -> list.map { it.toDomain() } }

    override suspend fun getStatsSince(fromDate: String): List<StudyStatistics> =
        studyStatisticsDao.getStatsSince(fromDate).map { it.toDomain() }

    override suspend fun getLast84DaysStats(): List<StudyStatistics> =
        studyStatisticsDao.getLast84Days().map { it.toDomain() }

    override suspend fun calculateStreak(): Int {
        val today = LocalDate.now()
        var streak = 0
        var checkDate = today
        while (true) {
            val dateStr = checkDate.format(dateFormatter)
            val stats = studyStatisticsDao.getStatsByDate(dateStr)
            if (stats != null && stats.cardsStudied > 0) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else {
                // Allow today to not be studied yet — check yesterday if today has no record
                if (checkDate == today && stats == null) {
                    checkDate = checkDate.minusDays(1)
                    continue
                }
                break
            }
        }
        return streak
    }

    override suspend fun getStudySummary(todayEndMs: Long): StudySummary {
        val dueCount = flashcardDao.getDueCardsCount(todayEndMs)
        val streak = calculateStreak()
        val totalCards = studyStatisticsDao.getStatsSince(
            LocalDate.now().minusDays(30).format(dateFormatter)
        )
        val totalCardsLearned = totalCards.sumOf { it.cardsStudied }
        val totalCorrect = totalCards.sumOf { it.correctAnswers }
        val totalAnswers = totalCards.sumOf { it.totalAnswers }
        val accuracy = if (totalAnswers > 0) totalCorrect.toFloat() / totalAnswers else 0f
        return StudySummary(
            todayDueCount = dueCount,
            studyStreak = streak,
            totalCardsLearned = totalCardsLearned,
            accuracyRate30Days = accuracy,
        )
    }

    override fun getTotalCardsStudied(): Flow<Int?> = studyStatisticsDao.getTotalCardsStudied()

    override fun getTotalMinutesStudied(): Flow<Int?> = studyStatisticsDao.getTotalMinutesStudied()
}

// ===== Mappers =====

fun ReviewSessionEntity.toDomain() = ReviewSession(
    id = id,
    deckId = deckId,
    studyMode = StudyMode.fromString(studyMode),
    startedAt = startedAt,
    endedAt = endedAt,
    totalCards = totalCards,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    durationSeconds = durationSeconds,
)

fun ReviewSession.toEntity() = ReviewSessionEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    deckId = deckId,
    studyMode = studyMode.name,
    startedAt = startedAt,
    endedAt = endedAt,
    totalCards = totalCards,
    correctCount = correctCount,
    incorrectCount = incorrectCount,
    durationSeconds = durationSeconds,
)

fun StudyStatisticsEntity.toDomain() = StudyStatistics(
    id = id,
    date = date,
    cardsStudied = cardsStudied,
    minutesStudied = minutesStudied,
    correctAnswers = correctAnswers,
    totalAnswers = totalAnswers,
    streakCount = streakCount,
)
