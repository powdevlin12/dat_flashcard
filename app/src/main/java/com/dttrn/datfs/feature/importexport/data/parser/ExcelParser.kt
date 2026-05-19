package com.dttrn.datfs.feature.importexport.data.parser

import android.content.Context
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

data class ParsedCard(
    val frontText: String,
    val backText: String,
    val pronunciation: String? = null,
    val exampleSentence: String? = null,
    val note: String? = null,
    val difficultyLevel: Int = 2,
)

data class ParseError(
    val row: Int,       // 1-indexed (header = row 0)
    val message: String,
)

data class ExcelParseResult(
    val cards: List<ParsedCard>,
    val errors: List<ParseError>,
    val sheetName: String,
    val totalRows: Int,
)

/**
 * Đọc file .xlsx và parse thành danh sách ParsedCard.
 *
 * Định dạng cột:
 * A = frontText (bắt buộc)
 * B = backText  (bắt buộc)
 * C = pronunciation (tuỳ chọn)
 * D = exampleSentence (tuỳ chọn)
 * E = note (tuỳ chọn)
 * F = difficultyLevel 1|2|3 (tuỳ chọn, mặc định 2)
 */
class ExcelParser @Inject constructor() {

    fun parse(inputStream: InputStream, deckId: String): ExcelParseResult {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)
        val sheetName = sheet.sheetName

        val cards = mutableListOf<ParsedCard>()
        val errors = mutableListOf<ParseError>()
        var dataRowCount = 0

        val lastRow = sheet.lastRowNum
        for (rowIndex in 1..lastRow) {   // skip header row 0
            val row = sheet.getRow(rowIndex) ?: continue
            dataRowCount++

            val front = row.getCell(0)?.getCellString()?.trim() ?: ""
            val back = row.getCell(1)?.getCellString()?.trim() ?: ""

            if (front.isBlank()) {
                errors.add(ParseError(rowIndex + 1, "Cột A (mặt trước) không được để trống"))
                continue
            }
            if (back.isBlank()) {
                errors.add(ParseError(rowIndex + 1, "Cột B (mặt sau) không được để trống"))
                continue
            }

            val pronunciation = row.getCell(2)?.getCellString()?.trim()?.takeIf { it.isNotBlank() }
            val example = row.getCell(3)?.getCellString()?.trim()?.takeIf { it.isNotBlank() }
            val note = row.getCell(4)?.getCellString()?.trim()?.takeIf { it.isNotBlank() }
            val difficulty = row.getCell(5)?.getCellString()?.trim()?.toIntOrNull()
                ?.coerceIn(1, 3) ?: 2

            cards.add(
                ParsedCard(
                    frontText = front,
                    backText = back,
                    pronunciation = pronunciation,
                    exampleSentence = example,
                    note = note,
                    difficultyLevel = difficulty,
                )
            )
        }

        workbook.close()

        return ExcelParseResult(
            cards = cards,
            errors = errors,
            sheetName = sheetName,
            totalRows = dataRowCount,
        )
    }

    /** Chuyển ParsedCard thành FlashcardEntity sẵn sàng insert */
    fun toEntities(
        cards: List<ParsedCard>,
        deckId: String,
        startIndex: Int = 0,
    ): List<FlashcardEntity> {
        val now = System.currentTimeMillis()
        return cards.mapIndexed { idx, card ->
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                deckId = deckId,
                frontText = card.frontText,
                backText = card.backText,
                pronunciation = card.pronunciation,
                exampleSentence = card.exampleSentence,
                note = card.note,
                difficultyLevel = card.difficultyLevel,
                orderIndex = startIndex + idx,
                createdAt = now,
                updatedAt = now,
            )
        }
    }

    private fun org.apache.poi.ss.usermodel.Cell.getCellString(): String {
        return when (cellType) {
            CellType.STRING -> stringCellValue ?: ""
            CellType.NUMERIC -> {
                val num = numericCellValue
                if (num == num.toLong().toDouble()) num.toLong().toString()
                else num.toString()
            }
            CellType.BOOLEAN -> booleanCellValue.toString()
            CellType.FORMULA -> try { stringCellValue } catch (e: Exception) { "" }
            else -> ""
        }
    }
}
