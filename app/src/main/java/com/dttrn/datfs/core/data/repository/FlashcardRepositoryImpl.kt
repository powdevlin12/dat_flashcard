package com.dttrn.datfs.core.data.repository

import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlashcardRepositoryImpl @Inject constructor(
    private val flashcardDao: FlashcardDao,
) : FlashcardRepository {

    override fun getCardsByDeck(deckId: String): Flow<List<Flashcard>> =
        flashcardDao.getCardsByDeck(deckId).map { list -> list.map { it.toDomain() } }

    override fun searchCards(query: String): Flow<List<Flashcard>> =
        flashcardDao.searchCards(query).map { list -> list.map { it.toDomain() } }

    override suspend fun searchCardsOnce(query: String): List<Flashcard> =
        flashcardDao.searchCardsOnce(query).map { it.toDomain() }

    override suspend fun getCardById(cardId: String): Flashcard? =
        flashcardDao.getCardById(cardId)?.toDomain()

    override suspend fun getCardsForReview(todayEndMs: Long): List<Flashcard> =
        flashcardDao.getCardsByDeckOnce("").let {
            // Use Flow collect workaround — query all due cards
            emptyList() // Will be handled by dedicated DAO method
        }

    override suspend fun getCardsForReviewByFlow(todayEndMs: Long): List<Flashcard> =
        emptyList() // Placeholder — Phase 3

    override suspend fun getOverdueCards(todayStartMs: Long): List<Flashcard> =
        flashcardDao.getOverdueCards(todayStartMs).map { it.toDomain() }

    override suspend fun getNewCards(deckId: String): List<Flashcard> =
        flashcardDao.getNewCards(deckId).map { it.toDomain() }

    override suspend fun getMostFailedCards(deckId: String, limit: Int): List<Flashcard> =
        flashcardDao.getMostFailedCards(deckId, limit).map { it.toDomain() }

    override suspend fun addCard(card: Flashcard): Result<Unit> = runCatching {
        flashcardDao.insertCard(card.toEntity())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun updateCard(card: Flashcard): Result<Unit> = runCatching {
        flashcardDao.updateCard(card.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun deleteCard(cardId: String): Result<Unit> = runCatching {
        flashcardDao.deleteCard(cardId)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun deleteCards(cardIds: List<String>): Result<Unit> = runCatching {
        flashcardDao.deleteCards(cardIds)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun importCards(cards: List<Flashcard>): Result<Int> = runCatching {
        val entities = cards.map { it.toEntity() }
        flashcardDao.insertCards(entities)
        entities.size
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun updateReviewMetadata(
        cardId: String,
        easeFactor: Float,
        intervalDays: Int,
        repetitionCount: Int,
        dueDate: Long,
        failureStreak: Int,
    ): Result<Unit> = runCatching {
        flashcardDao.updateReviewMetadata(
            cardId = cardId,
            easeFactor = easeFactor,
            intervalDays = intervalDays,
            repetitionCount = repetitionCount,
            dueDate = dueDate,
            failureStreak = failureStreak,
            lastReviewedAt = System.currentTimeMillis(),
        )
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun setKnown(cardId: String, known: Boolean): Result<Unit> = runCatching {
        flashcardDao.setKnown(cardId, known)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun updateOrderIndex(cardId: String, orderIndex: Int): Result<Unit> =
        runCatching { flashcardDao.updateOrderIndex(cardId, orderIndex) }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
        )

    override fun getTotalKnownCards(): Flow<Int> = flashcardDao.getTotalKnownCards()

    override fun getTodaysDueCount(todayEndMs: Long): Flow<Int> =
        flashcardDao.getTodaysDueCount(todayEndMs)
}

// ===== Mappers =====
fun FlashcardEntity.toDomain() = Flashcard(
    id = id, deckId = deckId,
    frontText = frontText, backText = backText,
    imagePath = imagePath, pronunciation = pronunciation,
    exampleSentence = exampleSentence, note = note,
    difficultyLevel = difficultyLevel, orderIndex = orderIndex,
    easeFactor = easeFactor, intervalDays = intervalDays,
    repetitionCount = repetitionCount, dueDate = dueDate,
    failureStreak = failureStreak, lastReviewedAt = lastReviewedAt,
    isKnown = isKnown, createdAt = createdAt, updatedAt = updatedAt,
)

fun Flashcard.toEntity() = FlashcardEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    deckId = deckId,
    frontText = frontText, backText = backText,
    imagePath = imagePath, pronunciation = pronunciation,
    exampleSentence = exampleSentence, note = note,
    difficultyLevel = difficultyLevel, orderIndex = orderIndex,
    easeFactor = easeFactor, intervalDays = intervalDays,
    repetitionCount = repetitionCount, dueDate = dueDate,
    failureStreak = failureStreak, lastReviewedAt = lastReviewedAt,
    isKnown = isKnown,
    createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
)
