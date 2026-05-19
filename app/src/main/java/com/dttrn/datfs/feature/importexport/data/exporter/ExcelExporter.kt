package com.dttrn.datfs.feature.importexport.data.exporter

import android.content.Context
import com.dttrn.datfs.core.data.local.entity.DeckEntity
import com.dttrn.datfs.core.data.local.entity.FlashcardEntity
import com.dttrn.datfs.core.domain.model.StudyStatistics
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Tạo file .xlsx từ deck + flashcard data.
 * Cấu trúc: mỗi deck = 1 sheet, sheet cuối = Statistics (nếu có).
 */
class ExcelExporter @Inject constructor() {

    companion object {
        private val HEADER_COLUMNS = listOf(
            "Mặt trước *", "Mặt sau *",
            "Phiên âm", "Câu ví dụ", "Ghi chú", "Độ khó (1-3)"
        )
    }

    /**
     * Export danh sách decks + cards thành file .xlsx và trả về File.
     * File được lưu vào cacheDir, caller sẽ share qua Intent.
     */
    fun export(
        context: Context,
        decks: List<Pair<DeckEntity, List<FlashcardEntity>>>,
        statistics: List<StudyStatistics> = emptyList(),
    ): File {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)
        val dataStyle = createDataStyle(workbook)
        val errorStyle = createAlternateStyle(workbook)

        decks.forEachIndexed { idx, (deck, cards) ->
            // Sanitize sheet name — max 31 chars, no invalid chars
            val sheetName = sanitizeSheetName(deck.title, idx)
            val sheet = workbook.createSheet(sheetName)

            // Set column widths
            listOf(8000, 8000, 4000, 10000, 6000, 3000).forEachIndexed { col, width ->
                sheet.setColumnWidth(col, width)
            }

            // Header row
            val headerRow = sheet.createRow(0)
            HEADER_COLUMNS.forEachIndexed { col, label ->
                val cell = headerRow.createCell(col)
                cell.setCellValue(label)
                cell.cellStyle = headerStyle
            }

            // Data rows
            cards.forEachIndexed { cardIdx, card ->
                val row = sheet.createRow(cardIdx + 1)
                val style = if (cardIdx % 2 == 0) dataStyle else errorStyle
                row.createCell(0).also { it.setCellValue(card.frontText); it.cellStyle = style }
                row.createCell(1).also { it.setCellValue(card.backText); it.cellStyle = style }
                row.createCell(2).also { it.setCellValue(card.pronunciation ?: ""); it.cellStyle = style }
                row.createCell(3).also { it.setCellValue(card.exampleSentence ?: ""); it.cellStyle = style }
                row.createCell(4).also { it.setCellValue(card.note ?: ""); it.cellStyle = style }
                row.createCell(5).also {
                    it.setCellValue(card.difficultyLevel.toDouble())
                    it.cellStyle = style
                }
            }
        }

        // Statistics sheet (optional)
        if (statistics.isNotEmpty()) {
            val statsSheet = workbook.createSheet("Thống kê")
            statsSheet.setColumnWidth(0, 4000)
            statsSheet.setColumnWidth(1, 3000)
            statsSheet.setColumnWidth(2, 3000)
            statsSheet.setColumnWidth(3, 4000)
            statsSheet.setColumnWidth(4, 4000)

            val statsHeader = statsSheet.createRow(0)
            listOf("Ngày", "Số thẻ học", "Phút học", "Số đúng", "Tổng câu")
                .forEachIndexed { col, label ->
                    statsHeader.createCell(col).also {
                        it.setCellValue(label)
                        it.cellStyle = headerStyle
                    }
                }
            statistics.sortedByDescending { it.date }.forEachIndexed { idx, stat ->
                val row = statsSheet.createRow(idx + 1)
                row.createCell(0).setCellValue(stat.date)
                row.createCell(1).setCellValue(stat.cardsStudied.toDouble())
                row.createCell(2).setCellValue(stat.minutesStudied.toDouble())
                row.createCell(3).setCellValue(stat.correctAnswers.toDouble())
                row.createCell(4).setCellValue(stat.totalAnswers.toDouble())
            }
        }

        // Write to cache
        val timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val fileName = "FlashMind_export_$timestamp.xlsx"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    /** Tạo file .xlsx mẫu với header + 3 dòng ví dụ */
    fun createTemplate(context: Context): File {
        val workbook = XSSFWorkbook()
        val headerStyle = createHeaderStyle(workbook)
        val dataStyle = createDataStyle(workbook)

        val sheet = workbook.createSheet("Sheet1")
        listOf(8000, 8000, 4000, 10000, 6000, 3000).forEachIndexed { col, width ->
            sheet.setColumnWidth(col, width)
        }

        val headerRow = sheet.createRow(0)
        HEADER_COLUMNS.forEachIndexed { col, label ->
            headerRow.createCell(col).also {
                it.setCellValue(label)
                it.cellStyle = headerStyle
            }
        }

        // Sample rows
        val samples = listOf(
            listOf("Hello", "Xin chào", "/həˈloʊ/", "Hello, how are you?", "Lời chào thông dụng", "1"),
            listOf("Thank you", "Cảm ơn", "/θæŋk juː/", "Thank you very much!", "", "1"),
            listOf("Vocabulary", "Từ vựng", "/vəˈkæbjʊleri/", "I'm building my vocabulary.", "học thuật", "2"),
        )
        samples.forEachIndexed { rowIdx, row ->
            val dataRow = sheet.createRow(rowIdx + 1)
            row.forEachIndexed { colIdx, value ->
                dataRow.createCell(colIdx).also {
                    it.setCellValue(value)
                    it.cellStyle = dataStyle
                }
            }
        }

        val file = File(context.cacheDir, "FlashMind_template.xlsx")
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        val font = workbook.createFont().apply {
            bold = true
            color = IndexedColors.WHITE.index
        }
        setFont(font)
        setFillForegroundColor(IndexedColors.DARK_BLUE.index)
        fillPattern = FillPatternType.SOLID_FOREGROUND
        alignment = HorizontalAlignment.CENTER
        setBorderBottom(BorderStyle.THIN)
    }

    private fun createDataStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        setBorderBottom(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
        bottomBorderColor = IndexedColors.GREY_25_PERCENT.index
    }

    private fun createAlternateStyle(workbook: XSSFWorkbook) = workbook.createCellStyle().apply {
        setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.index)
        fillPattern = FillPatternType.SOLID_FOREGROUND
        setBorderBottom(BorderStyle.THIN)
        setBorderRight(BorderStyle.THIN)
        bottomBorderColor = IndexedColors.GREY_25_PERCENT.index
    }

    private fun sanitizeSheetName(name: String, fallbackIndex: Int): String {
        val invalid = Regex("[\\\\/:*?\\[\\]]")
        val sanitized = name.replace(invalid, "_").take(31)
        return sanitized.ifBlank { "Sheet${fallbackIndex + 1}" }
    }
}
