package com.dttrn.datfs.feature.importexport.domain.usecase

import android.content.Context
import com.dttrn.datfs.core.data.local.dao.DeckDao
import com.dttrn.datfs.core.data.local.dao.FlashcardDao
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.feature.importexport.data.parser.ExcelParser
import com.dttrn.datfs.feature.importexport.data.parser.ExcelParseResult
import com.dttrn.datfs.feature.importexport.data.parser.ParsedCard
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

data class ImportPreview(
    val parseResult: ExcelParseResult,
    val deckId: String,
    val deckTitle: String,
    val existingCardCount: Int,
)

/**
 * Use case xử lý import Excel theo 2 bước:
 * 1. preview() — parse file, trả về preview để user xác nhận
 * 2. confirm() — insert các cards đã parse vào DB
 */
class ImportExcelUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val excelParser: ExcelParser,
    private val deckDao: DeckDao,
    private val flashcardDao: FlashcardDao,
) {
    /**
     * Parse file .xlsx, tạo deck mới (hoặc dùng deckId đã chọn).
     * Chạy trên IO dispatcher.
     */
    suspend fun preview(
        inputStream: InputStream,
        targetDeckId: String? = null,
        newDeckTitle: String = "Deck nhập khẩu",
    ): Result<ImportPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val parseResult = excelParser.parse(inputStream, targetDeckId ?: "")

            val deckId = targetDeckId ?: UUID.randomUUID().toString()
            val deckTitle = if (targetDeckId != null) {
                deckDao.getDeckByIdOnce(targetDeckId)?.title ?: newDeckTitle
            } else {
                newDeckTitle.ifBlank { parseResult.sheetName.ifBlank { "Deck nhập khẩu" } }
            }
            val existingCount = if (targetDeckId != null) {
                flashcardDao.getCardsByDeckOnce(targetDeckId).size
            } else 0

            ImportPreview(
                parseResult = parseResult,
                deckId = deckId,
                deckTitle = deckTitle,
                existingCardCount = existingCount,
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(com.dttrn.datfs.core.domain.common.AppException.DatabaseException(it.message)) },
        )
    }

    /**
     * Confirm import: tạo deck mới nếu chưa có, insert tất cả cards.
     */
    suspend fun confirm(preview: ImportPreview): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            // Tạo deck mới nếu chưa tồn tại
            val deckExists = deckDao.getDeckByIdOnce(preview.deckId) != null
            if (!deckExists) {
                val now = System.currentTimeMillis()
                deckDao.insertDeck(
                    DeckEntity(
                        id = preview.deckId,
                        title = preview.deckTitle,
                        colorHex = "#4A90E2",
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }

            val entities = excelParser.toEntities(
                cards = preview.parseResult.cards,
                deckId = preview.deckId,
                startIndex = preview.existingCardCount,
            )
            flashcardDao.insertCards(entities)
            entities.size
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(com.dttrn.datfs.core.domain.common.AppException.DatabaseException(it.message)) },
        )
    }
}
