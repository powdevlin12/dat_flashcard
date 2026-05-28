package com.dttrn.datfs.core.data.local.dao

import androidx.room.*
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho flashcard_table.
 * Bao gồm queries CRUD và review queue theo SM-2 algorithm.
 */
@Dao
interface FlashcardDao {

    // ===== Queries =====

    @Query("SELECT * FROM flashcard_table WHERE deckId = :deckId ORDER BY orderIndex ASC")
    fun getCardsByDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcard_table WHERE deckId = :deckId ORDER BY orderIndex ASC")
    suspend fun getCardsByDeckOnce(deckId: String): List<FlashcardEntity>

    @Query("SELECT * FROM flashcard_table WHERE id = :cardId")
    suspend fun getCardById(cardId: String): FlashcardEntity?

    @Query("SELECT COUNT(*) FROM flashcard_table WHERE deckId = :deckId")
    fun getCardCount(deckId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcard_table WHERE deckId = :deckId AND isKnown = 1")
    fun getKnownCardCount(deckId: String): Flow<Int>

    // ===== SM-2 Review Queues =====

    /** Thẻ cần ôn hôm nay: dueDate <= endOfToday AND chưa thuộc */
    @Query("""
        SELECT * FROM flashcard_table 
        WHERE dueDate <= :todayEnd AND isKnown = 0
        ORDER BY dueDate ASC
    """)
    fun getCardsForReview(todayEnd: Long): Flow<List<FlashcardEntity>>

    /** Thẻ quá hạn: dueDate < startOfToday AND chưa thuộc */
    @Query("""
        SELECT * FROM flashcard_table 
        WHERE dueDate < :todayStart AND isKnown = 0
        ORDER BY dueDate ASC
    """)
    suspend fun getOverdueCards(todayStart: Long): List<FlashcardEntity>

    /** Thẻ mới chưa học lần nào */
    @Query("""
        SELECT * FROM flashcard_table 
        WHERE repetitionCount = 0 AND deckId = :deckId
        ORDER BY orderIndex ASC
    """)
    suspend fun getNewCards(deckId: String): List<FlashcardEntity>

    /** Thẻ sai nhiều nhất (failureStreak cao) */
    @Query("""
        SELECT * FROM flashcard_table 
        WHERE deckId = :deckId AND failureStreak > 0
        ORDER BY failureStreak DESC
        LIMIT :limit
    """)
    suspend fun getMostFailedCards(deckId: String, limit: Int = 20): List<FlashcardEntity>

    // ===== Search =====

    @Query("""
        SELECT * FROM flashcard_table
        WHERE (
            frontText LIKE '%' || :query || '%' OR
            backText LIKE '%' || :query || '%' OR
            note LIKE '%' || :query || '%' OR
            exampleSentence LIKE '%' || :query || '%' OR
            pronunciation LIKE '%' || :query || '%'
        )
        ORDER BY updatedAt DESC
    """)
    fun searchCards(query: String): Flow<List<FlashcardEntity>>

    @Query("""
        SELECT * FROM flashcard_table
        WHERE (
            frontText LIKE '%' || :query || '%' OR
            backText LIKE '%' || :query || '%' OR
            note LIKE '%' || :query || '%' OR
            exampleSentence LIKE '%' || :query || '%' OR
            pronunciation LIKE '%' || :query || '%'
        )
        ORDER BY updatedAt DESC
    """)
    suspend fun searchCardsOnce(query: String): List<FlashcardEntity>

    // ===== Mutations =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<FlashcardEntity>)

    @Update
    suspend fun updateCard(card: FlashcardEntity)

    @Query("""
        UPDATE flashcard_table 
        SET easeFactor = :easeFactor, 
            intervalDays = :intervalDays, 
            repetitionCount = :repetitionCount, 
            dueDate = :dueDate, 
            failureStreak = :failureStreak,
            lastReviewedAt = :lastReviewedAt,
            updatedAt = :now
        WHERE id = :cardId
    """)
    suspend fun updateReviewMetadata(
        cardId: String,
        easeFactor: Float,
        intervalDays: Int,
        repetitionCount: Int,
        dueDate: Long,
        failureStreak: Int,
        lastReviewedAt: Long,
        now: Long = System.currentTimeMillis()
    )

    @Query("UPDATE flashcard_table SET isKnown = :known, updatedAt = :now WHERE id = :cardId")
    suspend fun setKnown(cardId: String, known: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM flashcard_table WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM flashcard_table WHERE id IN (:cardIds)")
    suspend fun deleteCards(cardIds: List<String>)

    @Query("DELETE FROM flashcard_table WHERE deckId = :deckId")
    suspend fun deleteCardsByDeck(deckId: String)

    @Query("""
        UPDATE flashcard_table 
        SET orderIndex = :orderIndex, updatedAt = :now 
        WHERE id = :cardId
    """)
    suspend fun updateOrderIndex(cardId: String, orderIndex: Int, now: Long = System.currentTimeMillis())

    // ===== Statistics helpers =====

    @Query("SELECT COUNT(*) FROM flashcard_table WHERE isKnown = 1")
    fun getTotalKnownCards(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM flashcard_table 
        WHERE dueDate <= :todayEnd AND isKnown = 0
    """)
    fun getTodaysDueCount(todayEnd: Long): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM flashcard_table
        WHERE dueDate <= :todayEnd AND isKnown = 0
    """)
    suspend fun getDueCardsCount(todayEnd: Long): Int

    @Query("""
        SELECT COUNT(*) FROM flashcard_table
        WHERE deckId = :deckId AND dueDate IS NOT NULL AND dueDate <= :now AND isKnown = 0
    """)
    suspend fun getDueCardsCountForDeck(deckId: String, now: Long): Int
}
