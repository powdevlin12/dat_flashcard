package com.dttrn.datfs.core.domain.repository

import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Flashcard
import kotlinx.coroutines.flow.Flow

/**
 * Interface repository cho Flashcard operations.
 */
interface FlashcardRepository {

    fun getCardsByDeck(deckId: String): Flow<List<Flashcard>>
    fun searchCards(query: String): Flow<List<Flashcard>>
    suspend fun getCardById(cardId: String): Flashcard?
    suspend fun getCardsForReview(todayEndMs: Long): List<Flashcard>
    suspend fun getCardsForReviewByFlow(todayEndMs: Long): List<Flashcard>  // Phase 3
    suspend fun getOverdueCards(todayStartMs: Long): List<Flashcard>
    suspend fun getNewCards(deckId: String): List<Flashcard>
    suspend fun getMostFailedCards(deckId: String, limit: Int = 20): List<Flashcard>

    suspend fun addCard(card: Flashcard): Result<Unit>
    suspend fun updateCard(card: Flashcard): Result<Unit>
    suspend fun deleteCard(cardId: String): Result<Unit>
    suspend fun deleteCards(cardIds: List<String>): Result<Unit>
    suspend fun importCards(cards: List<Flashcard>): Result<Int>  // Returns imported count

    suspend fun updateReviewMetadata(
        cardId: String,
        easeFactor: Float,
        intervalDays: Int,
        repetitionCount: Int,
        dueDate: Long,
        failureStreak: Int,
    ): Result<Unit>

    suspend fun setKnown(cardId: String, known: Boolean): Result<Unit>
    suspend fun updateOrderIndex(cardId: String, orderIndex: Int): Result<Unit>

    fun getTotalKnownCards(): Flow<Int>
    fun getTodaysDueCount(todayEndMs: Long): Flow<Int>
}
