package com.dttrn.datfs.core.data.repository

import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.DeckSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeckRepositoryImpl @Inject constructor(
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
) : DeckRepository {

    override fun getActiveDecks(): Flow<List<Deck>> =
        deckDao.getActiveDecks().map { list -> list.map { it.toDomain() } }

    override fun getArchivedDecks(): Flow<List<Deck>> =
        deckDao.getArchivedDecks().map { list -> list.map { it.toDomain() } }

    override fun getFavoriteDecks(): Flow<List<Deck>> =
        deckDao.getFavoriteDecks().map { list -> list.map { it.toDomain() } }

    override fun getDeckById(deckId: String): Flow<Deck?> =
        deckDao.getDeckById(deckId).map { it?.toDomain() }

    override fun searchDecks(query: String): Flow<List<Deck>> =
        deckDao.searchDecks(query).map { list -> list.map { it.toDomain() } }

    override fun getDecksFiltered(category: String?, sortBy: DeckSortOrder): Flow<List<Deck>> =
        deckDao.getDecksFiltered(category, sortBy.name).map { list -> list.map { it.toDomain() } }

    override fun getCategories(): Flow<List<String>> = deckDao.getCategories()

    override suspend fun createDeck(deck: Deck): Result<Unit> = runCatching {
        deckDao.insertDeck(deck.toEntity())
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun updateDeck(deck: Deck): Result<Unit> = runCatching {
        deckDao.updateDeck(deck.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun deleteDeck(deckId: String): Result<Unit> = runCatching {
        deckDao.deleteDeck(deckId)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )

    override suspend fun duplicateDeck(deckId: String, newTitle: String): Result<String> =
        runCatching {
            val original = deckDao.getDeckByIdOnce(deckId)
                ?: throw AppException.DatabaseException("Deck not found: $deckId")
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            deckDao.insertDeck(
                original.copy(
                    id = newId,
                    title = newTitle,
                    isFavorite = false,
                    isArchived = false,
                    studyProgress = 0f,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            // Duplicate cards
            val cards = flashcardDao.getCardsByDeckOnce(deckId)
            flashcardDao.insertCards(cards.map { card ->
                card.copy(
                    id = UUID.randomUUID().toString(),
                    deckId = newId,
                    repetitionCount = 0,
                    intervalDays = 0,
                    dueDate = null,
                    failureStreak = 0,
                    lastReviewedAt = null,
                    isKnown = false,
                    createdAt = now,
                    updatedAt = now,
                )
            })
            newId
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
        )

    override suspend fun setArchived(deckId: String, archived: Boolean): Result<Unit> =
        runCatching { deckDao.setArchived(deckId, archived) }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
        )

    override suspend fun setFavorite(deckId: String, favorite: Boolean): Result<Unit> =
        runCatching { deckDao.setFavorite(deckId, favorite) }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
        )

    override suspend fun updateProgress(deckId: String): Result<Unit> = runCatching {
        val cards = flashcardDao.getCardsByDeckOnce(deckId)
        val progress = if (cards.isEmpty()) 0f
        else cards.count { it.isKnown }.toFloat() / cards.size
        deckDao.updateProgress(deckId, progress)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )
}

// ===== Mappers =====
fun DeckEntity.toDomain() = Deck(
    id = id,
    title = title,
    description = description,
    category = category,
    tags = tags,
    colorHex = colorHex,
    isFavorite = isFavorite,
    isArchived = isArchived,
    studyProgress = studyProgress,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Deck.toEntity() = DeckEntity(
    id = id.ifEmpty { UUID.randomUUID().toString() },
    title = title,
    description = description,
    category = category,
    tags = tags,
    colorHex = colorHex,
    isFavorite = isFavorite,
    isArchived = isArchived,
    studyProgress = studyProgress,
    createdAt = createdAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis(),
)
