package com.dttrn.datfs.core.domain.usecase.card

import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Flashcard
import com.dttrn.datfs.core.domain.repository.DeckRepository
import com.dttrn.datfs.core.domain.repository.FlashcardRepository
import java.util.UUID
import javax.inject.Inject

class AddCardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(
        deckId: String,
        frontText: String,
        backText: String,
        pronunciation: String? = null,
        exampleSentence: String? = null,
        note: String? = null,
        imagePath: String? = null,
        difficultyLevel: Int = 2,
    ): Result<Unit> {
        if (frontText.isBlank()) return Result.Error(AppException.ValidationException("Mặt trước không được để trống"))
        if (backText.isBlank()) return Result.Error(AppException.ValidationException("Mặt sau không được để trống"))
        if (frontText.length > 500) return Result.Error(AppException.ValidationException("Mặt trước tối đa 500 ký tự"))
        if (backText.length > 500) return Result.Error(AppException.ValidationException("Mặt sau tối đa 500 ký tự"))

        val card = Flashcard(
            id = UUID.randomUUID().toString(),
            deckId = deckId,
            frontText = frontText.trim(),
            backText = backText.trim(),
            pronunciation = pronunciation?.trim(),
            exampleSentence = exampleSentence?.trim(),
            note = note?.trim(),
            imagePath = imagePath,
            difficultyLevel = difficultyLevel.coerceIn(1, 3),
        )
        return repository.addCard(card).also {
            if (it is Result.Success) deckRepository.updateProgress(deckId)
        }
    }
}

class UpdateCardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
) {
    suspend operator fun invoke(card: Flashcard): Result<Unit> {
        if (card.frontText.isBlank()) return Result.Error(AppException.ValidationException("Mặt trước không được để trống"))
        if (card.backText.isBlank()) return Result.Error(AppException.ValidationException("Mặt sau không được để trống"))
        return repository.updateCard(card.copy(
            frontText = card.frontText.trim(),
            backText = card.backText.trim(),
        ))
    }
}

class DeleteCardUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(cardId: String, deckId: String): Result<Unit> =
        repository.deleteCard(cardId).also {
            if (it is Result.Success) deckRepository.updateProgress(deckId)
        }
}

class BulkDeleteCardsUseCase @Inject constructor(
    private val repository: FlashcardRepository,
    private val deckRepository: DeckRepository,
) {
    suspend operator fun invoke(cardIds: List<String>, deckId: String): Result<Unit> {
        if (cardIds.isEmpty()) return Result.Success(Unit)
        return repository.deleteCards(cardIds).also {
            if (it is Result.Success) deckRepository.updateProgress(deckId)
        }
    }
}
