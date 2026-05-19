package com.dttrn.datfs.feature.importexport.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.StudyStatistics
import com.dttrn.datfs.core.domain.repository.ReviewRepository
import com.dttrn.datfs.feature.importexport.data.exporter.ExcelExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Export toàn bộ (hoặc 1) deck thành file .xlsx và tạo share Intent.
 */
class ExportExcelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
    private val reviewRepository: ReviewRepository,
    private val excelExporter: ExcelExporter,
) {
    /** Export tất cả active decks */
    suspend fun exportAll(): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val decks = deckDao.getActiveDecksOnce()
            val pairs = decks.map { deck ->
                deck to flashcardDao.getCardsByDeckOnce(deck.id)
            }
            val stats = reviewRepository.getLast84DaysStats()
            val file = excelExporter.export(context, pairs, stats)
            buildShareIntent(file.absolutePath)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
        )
    }

    /** Export 1 deck cụ thể */
    suspend fun exportDeck(deckId: String): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val deck = deckDao.getDeckByIdOnce(deckId)
                ?: throw AppException.DatabaseException("Không tìm thấy deck $deckId")
            val cards = flashcardDao.getCardsByDeckOnce(deckId)
            val file = excelExporter.export(context, listOf(deck to cards))
            buildShareIntent(file.absolutePath)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
        )
    }

    /** Tạo file template mẫu và share */
    suspend fun exportTemplate(): Result<Intent> = withContext(Dispatchers.IO) {
        runCatching {
            val file = excelExporter.createTemplate(context)
            buildShareIntent(file.absolutePath)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.DatabaseException(it.message)) },
        )
    }

    private fun buildShareIntent(filePath: String): Intent {
        val file = java.io.File(filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "FlashMind Export - ${file.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
