package com.dttrn.datfs.core.domain.model

/**
 * Domain model cho Flashcard với đầy đủ SM-2 metadata.
 */
data class Flashcard(
    val id: String,
    val deckId: String,
    val frontText: String,
    val backText: String,
    val imagePath: String? = null,
    val pronunciation: String? = null,
    val exampleSentence: String? = null,
    val note: String? = null,
    val difficultyLevel: Int = 2,       // 1=Dễ, 2=Trung bình, 3=Khó
    val orderIndex: Int = 0,

    // SM-2 fields
    val easeFactor: Float = 2.5f,
    val intervalDays: Int = 0,
    val repetitionCount: Int = 0,
    val dueDate: Long? = null,
    val failureStreak: Int = 0,
    val lastReviewedAt: Long? = null,
    val isKnown: Boolean = false,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** True nếu thẻ chưa học lần nào */
    val isNew: Boolean get() = repetitionCount == 0 && dueDate == null

    /** True nếu thẻ cần ôn hôm nay hoặc đã quá hạn */
    fun isDueBy(todayEndMs: Long): Boolean =
        !isKnown && dueDate != null && dueDate <= todayEndMs
}
