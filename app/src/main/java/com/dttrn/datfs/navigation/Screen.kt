package com.dttrn.datfs.navigation

import com.dttrn.datfs.core.data.local.entity.StudyMode

/**
 * Tất cả routes của ứng dụng FlashMind.
 */
sealed class Screen(val route: String) {

    // ===== Top-level =====
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
    data object ImportExport : Screen("import_export")
    data object Backup : Screen("backup")
    data object Onboarding : Screen("onboarding")

    // ===== Deck =====
    data object DeckDetail : Screen("deck_detail/{deckId}") {
        fun createRoute(deckId: String) = "deck_detail/$deckId"
        const val ARG_DECK_ID = "deckId"
    }

    data object CreateEditDeck : Screen("create_edit_deck?deckId={deckId}") {
        fun createRoute(deckId: String? = null) =
            if (deckId != null) "create_edit_deck?deckId=$deckId" else "create_edit_deck"
        const val ARG_DECK_ID = "deckId"
    }

    // ===== Card =====
    data object CardEditor : Screen("card_editor/{deckId}?cardId={cardId}") {
        fun createRoute(deckId: String, cardId: String? = null) =
            if (cardId != null) "card_editor/$deckId?cardId=$cardId" else "card_editor/$deckId"
        const val ARG_DECK_ID = "deckId"
        const val ARG_CARD_ID = "cardId"
    }

    // ===== Study =====
    data object StudySession : Screen("study/{deckId}/{mode}") {
        fun createRoute(deckId: String, mode: StudyMode) = "study/$deckId/${mode.name}"
        const val ARG_DECK_ID = "deckId"
        const val ARG_MODE = "mode"
    }
}
