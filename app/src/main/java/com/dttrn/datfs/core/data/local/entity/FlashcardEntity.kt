package com.dttrn.datfs.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity cho bảng flashcard_table.
 * Mỗi flashcard thuộc về 1 deck, có đầy đủ metadata và trường SM-2 cho spaced repetition.
 */
@Entity(
    tableName = "flashcard_table",
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
        Index(value = ["dueDate"]),
        Index(value = ["isKnown"]),
        Index(value = ["frontText"]),
    ]
)
data class FlashcardEntity(
    @PrimaryKey
    val id: String,                             // UUID
    val deckId: String,                         // FK → deck_table.id
    val frontText: String,                      // Mặt trước (bắt buộc, max 500 ký tự)
    val backText: String,                       // Mặt sau (bắt buộc, max 500 ký tự)
    val imagePath: String? = null,              // Đường dẫn ảnh local (app-specific storage)
    val pronunciation: String? = null,          // Phiên âm (max 200 ký tự)
    val exampleSentence: String? = null,        // Câu ví dụ (max 1000 ký tự)
    val note: String? = null,                   // Ghi chú cá nhân (max 500 ký tự)
    val difficultyLevel: Int = 2,               // 1=Dễ, 2=Trung bình, 3=Khó
    val orderIndex: Int = 0,                    // Thứ tự hiển thị trong deck

    // ===== SM-2 Spaced Repetition Metadata =====
    val easeFactor: Float = 2.5f,              // EF (min 1.3, mặc định 2.5)
    val intervalDays: Int = 0,                  // Số ngày đến lần ôn tiếp (0 = chưa học)
    val repetitionCount: Int = 0,               // Số lần đã ôn thành công liên tiếp
    val dueDate: Long? = null,                  // Timestamp ngày cần ôn tiếp
    val failureStreak: Int = 0,                 // Số lần sai liên tiếp
    val lastReviewedAt: Long? = null,           // Timestamp lần ôn cuối
    val isKnown: Boolean = false,               // Người dùng đánh dấu đã thuộc

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
