package com.dttrn.datfs.feature.backup.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.dao.ReviewSessionDao
import com.dttrn.datfs.core.data.local.dao.StudyStatisticsDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String,
    val decks: List<DeckBackup>,
)

@Serializable
data class DeckBackup(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val colorHex: String = "#4A90E2",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val studyProgress: Float = 0f,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val cards: List<CardBackup> = emptyList(),
)

@Serializable
data class CardBackup(
    val id: String,
    val frontText: String,
    val backText: String,
    val pronunciation: String? = null,
    val exampleSentence: String? = null,
    val note: String? = null,
    val difficultyLevel: Int = 2,
    val orderIndex: Int = 0,
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitionCount: Int = 0,
    val dueDate: Long? = null,
    val failureStreak: Int = 0,
    val isKnown: Boolean = false,
    val createdAt: Long = 0L,
)

class BackupUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** Tạo file JSON backup và trả về share Intent */
    suspend fun backupToJson(): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val decks = deckDao.getActiveDecksOnce()
            val deckBackups = decks.map { deck ->
                val cards = flashcardDao.getCardsByDeckOnce(deck.id)
                DeckBackup(
                    id = deck.id,
                    title = deck.title,
                    description = deck.description,
                    category = deck.category,
                    tags = deck.tags,
                    colorHex = deck.colorHex,
                    isFavorite = deck.isFavorite,
                    isArchived = deck.isArchived,
                    studyProgress = deck.studyProgress,
                    createdAt = deck.createdAt,
                    updatedAt = deck.updatedAt,
                    cards = cards.map { it.toBackup() },
                )
            }
            val backup = BackupData(
                exportedAt = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                decks = deckBackups,
            )
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val file = File(context.cacheDir, "FlashMind_backup_$timestamp.json")
            file.writeText(json.encodeToString(backup))
            buildShareIntent(file, "application/json")
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
        )
    }

    /** Copy file .db trực tiếp và share */
    suspend fun backupDb(): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = context.getDatabasePath("flashmind.db")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
            val dest = File(context.cacheDir, "FlashMind_db_$timestamp.db")
            dbFile.copyTo(dest, overwrite = true)
            buildShareIntent(dest, "application/octet-stream")
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
        )
    }

    private fun buildShareIntent(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FlashMind Backup - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun FlashcardEntity.toBackup() = CardBackup(
        id = id, frontText = frontText, backText = backText,
        pronunciation = pronunciation, exampleSentence = exampleSentence, note = note,
        difficultyLevel = difficultyLevel, orderIndex = orderIndex,
        easeFactor = easeFactor, intervalDays = intervalDays, repetitionCount = repetitionCount,
        dueDate = dueDate, failureStreak = failureStreak, isKnown = isKnown, createdAt = createdAt,
    )
}
