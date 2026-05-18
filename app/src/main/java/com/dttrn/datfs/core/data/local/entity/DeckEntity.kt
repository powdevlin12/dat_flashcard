package com.dttrn.datfs.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dttrn.datfs.core.data.local.converter.StringListConverter

/**
 * Entity cho bảng deck_table.
 * Mỗi deck là một bộ thẻ flashcard với metadata và cấu hình học.
 */
@Entity(
    tableName = "deck_table",
    indices = [
        Index(value = ["isArchived"]),
        Index(value = ["isFavorite"]),
        Index(value = ["title"]),
    ]
)
@TypeConverters(StringListConverter::class)
data class DeckEntity(
    @PrimaryKey
    val id: String,                             // UUID
    val title: String,                          // Tên bộ thẻ (max 100 ký tự)
    val description: String? = null,            // Mô tả (max 500 ký tự)
    val category: String? = null,               // Danh mục tùy chỉnh
    val tags: List<String> = emptyList(),       // Tối đa 10 tag (lưu dạng JSON)
    val colorHex: String = "#4A90E2",           // Mã màu HEX
    val isFavorite: Boolean = false,            // Đánh dấu yêu thích
    val isArchived: Boolean = false,            // Ẩn/lưu trữ
    val studyProgress: Float = 0f,              // % thẻ đã thuộc (0.0 - 1.0)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
