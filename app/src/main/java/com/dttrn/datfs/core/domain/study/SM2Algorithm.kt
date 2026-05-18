package com.dttrn.datfs.core.domain.study

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Implementation thuật toán SM-2 (SuperMemo 2) cho Spaced Repetition.
 *
 * Rating scale (0-5):
 *   0 = Hoàn toàn quên (blackout)
 *   1 = Sai, nhưng nhớ câu trả lời khi thấy
 *   2 = Sai, nhưng câu trả lời dễ nhận ra
 *   3 = Đúng, nhưng khó nhớ (hard)
 *   4 = Đúng với một chút do dự (good)
 *   5 = Đúng ngay lập tức, hoàn toàn (easy)
 */
object SM2Algorithm {

    const val MIN_EASE_FACTOR = 1.3f
    const val DEFAULT_EASE_FACTOR = 2.5f
    const val MAX_EASE_FACTOR = 4.0f

    data class ReviewResult(
        val newEaseFactor: Float,
        val newIntervalDays: Int,
        val newRepetitionCount: Int,
        val newDueDateMs: Long,
        val newFailureStreak: Int,
        val isLearned: Boolean,   // repetitionCount >= 3 && rating >= 3
    )

    /**
     * Tính toán lịch ôn tập tiếp theo dựa trên SM-2.
     *
     * @param rating     Đánh giá 0–5
     * @param easeFactor Ease factor hiện tại (default 2.5)
     * @param interval   Interval hiện tại (số ngày)
     * @param repetition Số lần đã ôn thành công liên tiếp
     * @param failureStreak Chuỗi lần quên liên tiếp
     */
    fun calculate(
        rating: Int,
        easeFactor: Float = DEFAULT_EASE_FACTOR,
        interval: Int = 0,
        repetition: Int = 0,
        failureStreak: Int = 0,
    ): ReviewResult {
        val clampedRating = rating.coerceIn(0, 5)

        val newFailureStreak = if (clampedRating < 3) failureStreak + 1 else 0

        // Cập nhật ease factor (EF mới = EF cũ + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)))
        val efDelta = 0.1 - (5 - clampedRating) * (0.08 + (5 - clampedRating) * 0.02)
        val newEaseFactor = (easeFactor + efDelta)
            .toFloat()
            .coerceIn(MIN_EASE_FACTOR, MAX_EASE_FACTOR)

        // Tính interval và repetition
        val (newInterval, newRepetition) = when {
            clampedRating < 3 -> {
                // Quên — reset về 1 ngày
                Pair(1, 0)
            }
            repetition == 0 -> Pair(1, 1)
            repetition == 1 -> Pair(6, 2)
            else -> {
                val nextInterval = (interval * newEaseFactor).roundToInt()
                    .coerceAtLeast(interval + 1)
                Pair(nextInterval, repetition + 1)
            }
        }

        val nowMs = System.currentTimeMillis()
        val dueDateMs = nowMs + newInterval.toLong() * 24 * 60 * 60 * 1000L

        return ReviewResult(
            newEaseFactor = newEaseFactor,
            newIntervalDays = newInterval,
            newRepetitionCount = newRepetition,
            newDueDateMs = dueDateMs,
            newFailureStreak = newFailureStreak,
            isLearned = newRepetition >= 3 && clampedRating >= 3,
        )
    }

    /**
     * Mapping từ các chế độ học sang SM-2 rating.
     */
    object Ratings {
        const val AGAIN = 0   // Quên hoàn toàn
        const val HARD  = 2   // Nhớ nhưng khó
        const val GOOD  = 4   // Nhớ tốt
        const val EASY  = 5   // Nhớ hoàn toàn dễ dàng

        /** Cho Swipe mode: swipe left = AGAIN, swipe right = GOOD */
        fun fromSwipe(swipedRight: Boolean) = if (swipedRight) GOOD else AGAIN

        /** Cho Quiz mode: đúng/sai */
        fun fromQuizAnswer(correct: Boolean, tookHint: Boolean = false): Int = when {
            !correct -> AGAIN
            tookHint -> HARD
            else -> GOOD
        }
    }
}
