package com.dttrn.datfs.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity cho bảng review_session_table.
 * Mỗi phiên học được ghi lại để tính thống kê và streak.
 */
@Entity(
    tableName = "review_session_table",
    foreignKeys = [
        ForeignKey(
            entity = DeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["startedAt"]),
    ]
)
data class ReviewSessionEntity(
    @PrimaryKey
    val id: String,                             // UUID
    val deckId: String,                         // FK → deck_table.id
    val studyMode: String,                      // SWIPE, LEARN, WRITE, QUIZ, MATCH
    val startedAt: Long,                        // Timestamp bắt đầu phiên
    val endedAt: Long? = null,                  // Timestamp kết thúc phiên
    val totalCards: Int = 0,                    // Tổng số thẻ trong phiên
    val correctCount: Int = 0,                  // Số thẻ trả lời đúng
    val incorrectCount: Int = 0,                // Số thẻ trả lời sai
    val durationSeconds: Int? = null,           // Thời gian phiên học (giây)
)

/**
 * Enum cho các chế độ học — giá trị lưu vào DB dưới dạng String
 */
enum class StudyMode(val displayName: String) {
    SPACED_REPETITION("Lặp lại ngắt quãng"),
    LEARN("Học tuần tự"),
    WRITE("Gõ đáp án"),
    QUIZ("Trắc nghiệm"),
    MATCH("Ghép đôi"),
    DICTATION("Nghe chính tả");

    companion object {
        fun fromString(value: String) = entries.firstOrNull { it.name == value } ?: SPACED_REPETITION
    }
}
