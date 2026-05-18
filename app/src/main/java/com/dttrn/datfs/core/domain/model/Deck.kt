package com.dttrn.datfs.core.domain.model

/**
 * Domain model cho Deck — không phụ thuộc vào Room entity.
 * Được map từ DeckEntity ở repository layer.
 */
data class Deck(
    val id: String,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val colorHex: String = "#4A90E2",
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val studyProgress: Float = 0f,      // 0.0 - 1.0
    val cardCount: Int = 0,             // Computed field (không lưu trực tiếp trong entity)
    val dueCount: Int = 0,              // Số thẻ cần ôn hôm nay
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)
