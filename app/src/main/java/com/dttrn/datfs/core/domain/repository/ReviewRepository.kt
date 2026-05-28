package com.dttrn.datfs.core.domain.repository

import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.ReviewSession
import com.dttrn.datfs.core.domain.model.StudyStatistics
import com.dttrn.datfs.core.domain.model.StudySummary
import kotlinx.coroutines.flow.Flow

/**
 * Interface repository cho study sessions và statistics.
 */
interface ReviewRepository {

    suspend fun saveSession(session: ReviewSession): Result<Unit>
    suspend fun saveSessionWithEncodedMode(session: ReviewSession, encodedMode: String): Result<Unit>
    suspend fun getSessionById(sessionId: String): ReviewSession?
    suspend fun getSessionsSince(fromTimeMs: Long): List<ReviewSession>

    /** Upsert stats cho ngày hôm nay sau khi kết thúc phiên học */
    suspend fun recordStudyActivity(
        date: String,
        cardsStudied: Int,
        minutesStudied: Int,
        correctAnswers: Int,
        totalAnswers: Int,
    ): Result<Unit>

    fun getRecentStats(days: Int): Flow<List<StudyStatistics>>
    suspend fun getStatsSince(fromDate: String): List<StudyStatistics>
    suspend fun getLast84DaysStats(): List<StudyStatistics>
    suspend fun calculateStreak(): Int
    suspend fun getStudySummary(todayEndMs: Long): StudySummary

    fun getTotalCardsStudied(): Flow<Int?>
    fun getTotalMinutesStudied(): Flow<Int?>
}
