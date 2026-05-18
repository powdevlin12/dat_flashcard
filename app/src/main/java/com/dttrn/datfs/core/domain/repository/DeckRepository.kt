package com.dttrn.datfs.core.domain.repository

import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Deck
import kotlinx.coroutines.flow.Flow

/**
 * Interface repository cho Deck operations.
 * Implementation ở feature/deck/data/repository/DeckRepositoryImpl.kt
 */
interface DeckRepository {

    fun getActiveDecks(): Flow<List<Deck>>
    fun getArchivedDecks(): Flow<List<Deck>>
    fun getFavoriteDecks(): Flow<List<Deck>>
    fun getDeckById(deckId: String): Flow<Deck?>
    fun searchDecks(query: String): Flow<List<Deck>>
    fun getDecksFiltered(category: String?, sortBy: DeckSortOrder): Flow<List<Deck>>
    fun getCategories(): Flow<List<String>>

    suspend fun createDeck(deck: Deck): Result<Unit>
    suspend fun updateDeck(deck: Deck): Result<Unit>
    suspend fun deleteDeck(deckId: String): Result<Unit>
    suspend fun duplicateDeck(deckId: String, newTitle: String): Result<String>  // Returns new deck ID
    suspend fun setArchived(deckId: String, archived: Boolean): Result<Unit>
    suspend fun setFavorite(deckId: String, favorite: Boolean): Result<Unit>
    suspend fun updateProgress(deckId: String): Result<Unit>  // Recalculates from cards
}

enum class DeckSortOrder(val displayName: String) {
    UPDATED("Ngày cập nhật"),
    CREATED("Ngày tạo"),
    TITLE("Tên A-Z"),
    PROGRESS("Tiến độ"),
    CARD_COUNT("Số thẻ"),
}
