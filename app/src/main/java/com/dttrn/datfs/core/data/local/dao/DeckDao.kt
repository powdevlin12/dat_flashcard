package com.dttrn.datfs.core.data.local.dao

import androidx.room.*
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho deck_table.
 * Tất cả queries trả về Flow để UI tự động cập nhật khi data thay đổi.
 */
@Dao
interface DeckDao {

    // ===== Queries =====

    @Query("SELECT * FROM deck_table WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM deck_table WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM deck_table WHERE isFavorite = 1 AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getFavoriteDecks(): Flow<List<DeckEntity>>

    @Query("SELECT * FROM deck_table WHERE id = :deckId")
    fun getDeckById(deckId: String): Flow<DeckEntity?>

    @Query("SELECT * FROM deck_table WHERE id = :deckId")
    suspend fun getDeckByIdOnce(deckId: String): DeckEntity?

    @Query("SELECT * FROM deck_table WHERE isArchived = 0 ORDER BY updatedAt DESC")
    suspend fun getActiveDecksOnce(): List<DeckEntity>

    @Query("""
        SELECT * FROM deck_table 
        WHERE isArchived = 0 AND (
            title LIKE '%' || :query || '%' OR 
            description LIKE '%' || :query || '%'
        )
        ORDER BY updatedAt DESC
    """)
    fun searchDecks(query: String): Flow<List<DeckEntity>>

    @Query("""
        SELECT * FROM deck_table 
        WHERE isArchived = 0 AND (:category IS NULL OR category = :category)
        ORDER BY 
            CASE WHEN :sortBy = 'TITLE' THEN title END ASC,
            CASE WHEN :sortBy = 'CREATED' THEN createdAt END DESC,
            CASE WHEN :sortBy = 'UPDATED' THEN updatedAt END DESC,
            CASE WHEN :sortBy = 'PROGRESS' THEN studyProgress END DESC,
            updatedAt DESC
    """)
    fun getDecksFiltered(category: String?, sortBy: String): Flow<List<DeckEntity>>

    @Query("SELECT DISTINCT category FROM deck_table WHERE category IS NOT NULL AND isArchived = 0")
    fun getCategories(): Flow<List<String>>

    // ===== Mutations =====

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: DeckEntity)

    @Update
    suspend fun updateDeck(deck: DeckEntity)

    @Query("UPDATE deck_table SET isArchived = :archived, updatedAt = :now WHERE id = :deckId")
    suspend fun setArchived(deckId: String, archived: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE deck_table SET isFavorite = :favorite, updatedAt = :now WHERE id = :deckId")
    suspend fun setFavorite(deckId: String, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE deck_table SET studyProgress = :progress, updatedAt = :now WHERE id = :deckId")
    suspend fun updateProgress(deckId: String, progress: Float, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM deck_table WHERE id = :deckId")
    suspend fun deleteDeck(deckId: String)

    @Query("SELECT COUNT(*) FROM deck_table WHERE isArchived = 0")
    fun getActiveDeckCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM flashcard_table WHERE deckId = :deckId")
    suspend fun getCardCount(deckId: String): Int
}
