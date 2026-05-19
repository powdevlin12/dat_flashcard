package com.dttrn.datfs.feature.backup.domain.usecase

import android.content.Context
import android.net.Uri
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class RestorePreview(
    val deckCount: Int,
    val cardCount: Int,
    val exportedAt: String,
    val backupVersion: Int,
)

class RestoreUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Validate JSON backup và trả về preview */
    suspend fun previewJson(uri: Uri): Result<RestorePreview> = withContext(Dispatchers.IO) {
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: throw Exception("Không đọc được file backup")
            val backup = json.decodeFromString<BackupData>(content)
            RestorePreview(
                deckCount = backup.decks.size,
                cardCount = backup.decks.sumOf { it.cards.size },
                exportedAt = backup.exportedAt,
                backupVersion = backup.version,
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException("File không hợp lệ: ${it.message}")) },
        )
    }

    /**
     * Restore từ JSON backup — MERGE mode: không xóa deck hiện có,
     * chỉ thêm deck/card mới. Nếu deck id trùng thì skip.
     */
    suspend fun restoreJson(uri: Uri, overwrite: Boolean = false): Result<RestorePreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: throw Exception("Không đọc được file backup")
                val backup = json.decodeFromString<BackupData>(content)
                var deckInserted = 0
                var cardInserted = 0

                backup.decks.forEach { deckBackup ->
                    val existing = deckDao.getDeckByIdOnce(deckBackup.id)
                    if (existing != null && !overwrite) return@forEach

                    deckDao.insertDeck(
                        DeckEntity(
                            id = if (overwrite) deckBackup.id else UUID.randomUUID().toString(),
                            title = deckBackup.title,
                            description = deckBackup.description,
                            category = deckBackup.category,
                            tags = deckBackup.tags,
                            colorHex = deckBackup.colorHex,
                            isFavorite = deckBackup.isFavorite,
                            isArchived = deckBackup.isArchived,
                            studyProgress = deckBackup.studyProgress,
                            createdAt = deckBackup.createdAt,
                            updatedAt = System.currentTimeMillis(),
                        )
                    )
                    deckInserted++

                    val newDeckId = if (overwrite) deckBackup.id else deckDao
                        .getActiveDecksOnce().lastOrNull()?.id ?: deckBackup.id

                    val cards = deckBackup.cards.map { cardBackup ->
                        FlashcardEntity(
                            id = UUID.randomUUID().toString(),
                            deckId = newDeckId,
                            frontText = cardBackup.frontText,
                            backText = cardBackup.backText,
                            pronunciation = cardBackup.pronunciation,
                            exampleSentence = cardBackup.exampleSentence,
                            note = cardBackup.note,
                            difficultyLevel = cardBackup.difficultyLevel,
                            orderIndex = cardBackup.orderIndex,
                            easeFactor = cardBackup.easeFactor,
                            intervalDays = cardBackup.intervalDays,
                            repetitionCount = cardBackup.repetitionCount,
                            dueDate = cardBackup.dueDate,
                            failureStreak = cardBackup.failureStreak,
                            isKnown = cardBackup.isKnown,
                            createdAt = cardBackup.createdAt,
                        )
                    }
                    flashcardDao.insertCards(cards)
                    cardInserted += cards.size
                }

                RestorePreview(
                    deckCount = deckInserted,
                    cardCount = cardInserted,
                    exportedAt = backup.exportedAt,
                    backupVersion = backup.version,
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
            )
        }
}
