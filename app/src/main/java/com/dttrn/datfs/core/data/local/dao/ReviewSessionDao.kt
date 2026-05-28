package com.dttrn.datfs.core.data.local.dao

import androidx.room.*
import com.dttrn.datfs.core.data.local.entity.ReviewSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReviewSessionEntity)

    @Query("SELECT * FROM review_session_table WHERE deckId = :deckId ORDER BY startedAt DESC")
    fun getSessionsByDeck(deckId: String): Flow<List<ReviewSessionEntity>>

    @Query("SELECT * FROM review_session_table WHERE startedAt >= :fromTime ORDER BY startedAt DESC")
    suspend fun getSessionsSince(fromTime: Long): List<ReviewSessionEntity>

    @Query("""
        SELECT SUM(durationSeconds) FROM review_session_table 
        WHERE startedAt >= :fromTime
    """)
    suspend fun getTotalStudySecondsSince(fromTime: Long): Int?

    @Query("""
        SELECT SUM(correctCount) * 1.0 / NULLIF(SUM(totalCards), 0)
        FROM review_session_table 
        WHERE startedAt >= :fromTime
    """)
    suspend fun getAccuracyRateSince(fromTime: Long): Float?

    @Query("SELECT * FROM review_session_table WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: String): ReviewSessionEntity?

    @Query("DELETE FROM review_session_table WHERE deckId = :deckId")
    suspend fun deleteSessionsByDeck(deckId: String)
}
