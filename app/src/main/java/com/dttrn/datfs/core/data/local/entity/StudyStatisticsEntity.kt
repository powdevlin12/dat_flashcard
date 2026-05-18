package com.dttrn.datfs.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity cho bảng study_statistics_table.
 * Mỗi ngày học có 1 bản ghi, dùng UPSERT để cộng dồn.
 * Dùng để tính streak, heatmap, bar chart trong dashboard.
 */
@Entity(
    tableName = "study_statistics_table",
    indices = [
        Index(value = ["date"], unique = true)
    ]
)
data class StudyStatisticsEntity(
    @PrimaryKey
    val id: String,                         // UUID
    val date: String,                       // Format: YYYY-MM-DD (UNIQUE)
    val cardsStudied: Int = 0,              // Số thẻ đã học trong ngày
    val minutesStudied: Int = 0,            // Tổng phút học trong ngày
    val correctAnswers: Int = 0,            // Số câu trả lời đúng
    val totalAnswers: Int = 0,              // Tổng số câu đã trả lời
    val streakCount: Int = 0,              // Streak liên tiếp tính đến ngày này
)
