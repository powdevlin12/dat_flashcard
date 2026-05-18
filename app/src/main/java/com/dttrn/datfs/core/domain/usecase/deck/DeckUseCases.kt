package com.dttrn.datfs.core.domain.usecase.deck

import com.dttrn.datfs.core.domain.common.AppException
import com.dttrn.datfs.core.domain.common.Result
import com.dttrn.datfs.core.domain.model.Deck
import com.dttrn.datfs.core.domain.repository.DeckRepository
import java.util.UUID
import javax.inject.Inject

class CreateDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        category: String? = null,
        tags: List<String> = emptyList(),
        colorHex: String = "#4A90E2",
    ): Result<String> {
        if (title.isBlank()) return Result.Error(AppException.ValidationException("Tên bộ thẻ không được để trống"))
        if (title.length > 100) return Result.Error(AppException.ValidationException("Tên bộ thẻ tối đa 100 ký tự"))
        if (tags.size > 10) return Result.Error(AppException.ValidationException("Tối đa 10 tag"))

        val id = UUID.randomUUID().toString()
        val deck = Deck(
            id = id,
            title = title.trim(),
            description = description?.trim(),
            category = category?.trim(),
            tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
            colorHex = colorHex,
        )
        return when (val result = repository.createDeck(deck)) {
            is Result.Success -> Result.Success(id)
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
}

class UpdateDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(deck: Deck): Result<Unit> {
        if (deck.title.isBlank()) return Result.Error(AppException.ValidationException("Tên bộ thẻ không được để trống"))
        if (deck.title.length > 100) return Result.Error(AppException.ValidationException("Tên bộ thẻ tối đa 100 ký tự"))
        if (deck.tags.size > 10) return Result.Error(AppException.ValidationException("Tối đa 10 tag"))
        return repository.updateDeck(deck.copy(title = deck.title.trim()))
    }
}

class DeleteDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(deckId: String): Result<Unit> =
        repository.deleteDeck(deckId)
}

class DuplicateDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(deckId: String, newTitle: String): Result<String> {
        if (newTitle.isBlank()) return Result.Error(AppException.ValidationException("Tên bộ thẻ không được để trống"))
        return repository.duplicateDeck(deckId, newTitle.trim())
    }
}

class ArchiveDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(deckId: String, archived: Boolean): Result<Unit> =
        repository.setArchived(deckId, archived)
}

class ToggleFavoriteDeckUseCase @Inject constructor(
    private val repository: DeckRepository
) {
    suspend operator fun invoke(deckId: String, favorite: Boolean): Result<Unit> =
        repository.setFavorite(deckId, favorite)
}
