package com.dttrn.datfs.core.domain.usecase.study

import com.dttrn.datfs.core.data.local.entity.StudyMode
import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import com.dttrn.datfs.core.domain.study.SM2Algorithm
import com.dttrn.datfs.core.domain.study.StudyQueue
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Lấy danh sách thẻ cần học cho một session.
 */
class GetStudyQueueUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(
        deckId: String,
        mode: StudyMode,
        dueOnly: Boolean = mode == StudyMode.SPACED_REPETITION,
        limit: Int = 50,
        shuffled: Boolean = mode != StudyMode.LEARN,
    ): Result<StudyQueue> = runCatching {
        val cards = flashcardRepository.getCardsByDeck(deckId).first()
        if (cards.isEmpty()) {
            return Result.Error(AppException.ValidationException("Bộ thẻ này chưa có thẻ nào"))
        }
        val queue = StudyQueue.buildFor(
            cards = cards,
            mode = mode,
            dueOnly = dueOnly,
            shuffled = shuffled,
            limit = limit,
        )
        if (queue.totalCount == 0) {
            return Result.Error(AppException.ValidationException("Không có thẻ nào cần ôn hôm nay 🎉"))
        }
        queue
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )
}

/**
 * Ghi nhận kết quả ôn tập và cập nhật SM-2 metadata cho thẻ.
 */
class SubmitReviewUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(
        card: Flashcard,
        rating: Int,  // SM2Algorithm.Ratings.*
    ): Result<SM2Algorithm.ReviewResult> = runCatching {
        val result = SM2Algorithm.calculate(
            rating = rating,
            easeFactor = card.easeFactor,
            interval = card.intervalDays,
            repetition = card.repetitionCount,
            failureStreak = card.failureStreak,
        )
        flashcardRepository.updateReviewMetadata(
            cardId = card.id,
            easeFactor = result.newEaseFactor,
            intervalDays = result.newIntervalDays,
            repetitionCount = result.newRepetitionCount,
            dueDate = result.newDueDateMs,
            failureStreak = result.newFailureStreak,
        )
        // Auto-mark as known if learned well enough
        if (result.isLearned && !card.isKnown) {
            flashcardRepository.setKnown(card.id, true)
        }
        result
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )
}

/**
 * Reset tiến độ SM-2 của toàn bộ thẻ trong deck về trạng thái ban đầu.
 */
class ResetDeckProgressUseCase @Inject constructor(
    private val flashcardRepository: FlashcardRepository,
) {
    suspend operator fun invoke(deckId: String): Result<Unit> = runCatching {
        val cards = flashcardRepository.getCardsByDeck(deckId).first()
        cards.forEach { card ->
            flashcardRepository.updateReviewMetadata(
                cardId = card.id,
                easeFactor = SM2Algorithm.DEFAULT_EASE_FACTOR,
                intervalDays = 0,
                repetitionCount = 0,
                dueDate = System.currentTimeMillis(), // Due immediately
                failureStreak = 0,
            )
            flashcardRepository.setKnown(card.id, false)
        }
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.DatabaseException(it.message)) }
    )
}
