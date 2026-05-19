package com.dttrn.datfs.core.domain.study

import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.data.local.entity.StudyMode

/**
 * Quản lý hàng đợi thẻ cần học trong một session.
 * Hỗ trợ: shuffle, lấy thẻ mới/đã học/quá hạn, theo dõi tiến độ.
 */
class StudyQueue(
    val allCards: List<Flashcard>,
    private val mode: StudyMode,
    private val shuffled: Boolean = true,
) {
    private val _queue: ArrayDeque<Flashcard>
    private val _failedCards = mutableListOf<Flashcard>()
    private val _reviewedIds = mutableSetOf<String>()

    val totalCount: Int get() = allCards.size
    val reviewedCount: Int get() = _reviewedIds.size
    val remainingCount: Int get() = _queue.size
    val failedCount: Int get() = _failedCards.size
    val isComplete: Boolean get() = _queue.isEmpty()
    val progress: Float get() = if (totalCount == 0) 1f else reviewedCount.toFloat() / totalCount

    init {
        val sorted = when (mode) {
            StudyMode.SPACED_REPETITION -> {
                val now = System.currentTimeMillis()
                // Due cards first, then new cards, then known cards
                allCards.sortedWith(
                    compareBy(
                        { if (it.dueDate != null && it.dueDate <= now) 0 else if (it.isNew) 1 else 2 },
                        { it.dueDate ?: Long.MAX_VALUE }
                    )
                )
            }
            StudyMode.LEARN -> allCards.sortedBy { it.orderIndex }
            StudyMode.WRITE,
            StudyMode.QUIZ,
            StudyMode.MATCH -> if (shuffled) allCards.shuffled() else allCards
        }
        _queue = ArrayDeque(sorted)
    }

    fun peek(): Flashcard? = _queue.firstOrNull()

    fun next(): Flashcard? {
        val card = _queue.removeFirstOrNull()
        card?.let { _reviewedIds.add(it.id) }
        return card
    }

    /**
     * Đánh dấu thẻ thất bại — thêm lại vào cuối queue để ôn lại.
     */
    fun markFailed(card: Flashcard, requeue: Boolean = true) {
        _failedCards.add(card)
        if (requeue) _queue.addLast(card)
    }

    /**
     * Lấy danh sách thẻ đã ôn (theo thứ tự ôn).
     */
    fun getReviewedCards(): List<Flashcard> =
        allCards.filter { it.id in _reviewedIds }

    fun getFailedCards(): List<Flashcard> = _failedCards.toList()

    companion object {
        /**
         * Factory — build queue theo mode và filter cards.
         */
        fun buildFor(
            cards: List<Flashcard>,
            mode: StudyMode,
            dueOnly: Boolean = false,
            shuffled: Boolean = true,
            limit: Int = Int.MAX_VALUE,
        ): StudyQueue {
            val now = System.currentTimeMillis()
            val filtered = when {
                dueOnly -> cards.filter { !it.isKnown && (it.isNew || it.isDueBy(now)) }
                else -> cards.filter { !it.isKnown }
            }.take(limit)
            return StudyQueue(filtered, mode, shuffled)
        }
    }
}
