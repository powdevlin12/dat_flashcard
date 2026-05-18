package com.dttrn.datfs.core.data.local.dao

import androidx.room.*
import com.dttrn.datfs.core.data.local.entity.StudyStatisticsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyStatisticsDao {

    @Query("SELECT * FROM study_statistics_table WHERE date = :date")
    suspend fun getStatsByDate(date: String): StudyStatisticsEntity?

    @Query("SELECT * FROM study_statistics_table ORDER BY date DESC LIMIT :limit")
    fun getRecentStats(limit: Int): Flow<List<StudyStatisticsEntity>>

    @Query("SELECT * FROM study_statistics_table WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getStatsSince(fromDate: String): List<StudyStatisticsEntity>

    @Query("SELECT * FROM study_statistics_table ORDER BY date DESC LIMIT 84")
    suspend fun getLast84Days(): List<StudyStatisticsEntity>

    /** Streak = số ngày liên tiếp gần nhất có cardsStudied > 0 */
    @Query("""
        SELECT COUNT(*) FROM study_statistics_table 
        WHERE date >= :sinceDate AND cardsStudied > 0
        ORDER BY date DESC
    """)
    suspend fun getStudyDayCount(sinceDate: String): Int

    /** UPSERT — insert hoặc update nếu đã có record của ngày đó */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(stats: StudyStatisticsEntity)

    @Query("""
        UPDATE study_statistics_table 
        SET cardsStudied = cardsStudied + :cards,
            minutesStudied = minutesStudied + :minutes,
            correctAnswers = correctAnswers + :correct,
            totalAnswers = totalAnswers + :total
        WHERE date = :date
    """)
    suspend fun incrementStats(date: String, cards: Int, minutes: Int, correct: Int, total: Int)

    @Query("SELECT SUM(cardsStudied) FROM study_statistics_table")
    fun getTotalCardsStudied(): Flow<Int?>

    @Query("SELECT SUM(minutesStudied) FROM study_statistics_table")
    fun getTotalMinutesStudied(): Flow<Int?>
}
