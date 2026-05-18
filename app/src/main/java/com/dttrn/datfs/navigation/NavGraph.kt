package com.dttrn.datfs.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dttrn.datfs.feature.card.CardEditorScreen
import com.dttrn.datfs.feature.deck.CreateEditDeckScreen
import com.dttrn.datfs.feature.deck.DeckDetailScreen
import com.dttrn.datfs.feature.home.HomeScreen
import com.dttrn.datfs.feature.search.SearchScreen

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Home.route,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {

        // ===== Home =====
        composable(Screen.Home.route) {
            HomeScreen(
                onDeckClick = { deckId ->
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                },
                onCreateDeck = {
                    navController.navigate(Screen.CreateEditDeck.createRoute())
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToStats = {
                    navController.navigate(Screen.Statistics.route)
                },
            )
        }

        // ===== Search =====
        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onDeckClick = { deckId ->
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                },
            )
        }

        // ===== Deck Detail =====
        composable(
            route = Screen.DeckDetail.route,
            arguments = listOf(
                navArgument(Screen.DeckDetail.ARG_DECK_ID) { type = NavType.StringType }
            )
        ) {
            DeckDetailScreen(
                onBack = { navController.popBackStack() },
                onEditDeck = { deckId ->
                    navController.navigate(Screen.CreateEditDeck.createRoute(deckId))
                },
                onAddCard = { deckId ->
                    navController.navigate(Screen.CardEditor.createRoute(deckId))
                },
                onEditCard = { deckId, cardId ->
                    navController.navigate(Screen.CardEditor.createRoute(deckId, cardId))
                },
                onStartStudy = { deckId ->
                    // Phase 3 — placeholder
                    navController.navigate(Screen.DeckDetail.createRoute(deckId))
                },
            )
        }

        // ===== Create/Edit Deck =====
        composable(
            route = Screen.CreateEditDeck.route,
            arguments = listOf(
                navArgument(Screen.CreateEditDeck.ARG_DECK_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            CreateEditDeckScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ===== Card Editor =====
        composable(
            route = Screen.CardEditor.route,
            arguments = listOf(
                navArgument(Screen.CardEditor.ARG_DECK_ID) { type = NavType.StringType },
                navArgument(Screen.CardEditor.ARG_CARD_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            )
        ) {
            CardEditorScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        // ===== Placeholders Phase 3-6 =====
        composable(Screen.Statistics.route) {
            PlaceholderScreen("Thống kê — Phase 4")
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Cài đặt — Phase 6")
        }
        composable(Screen.ImportExport.route) {
            PlaceholderScreen("Import/Export — Phase 5")
        }
        composable(Screen.Backup.route) {
            PlaceholderScreen("Backup — Phase 5")
        }
        composable(Screen.Onboarding.route) {
            PlaceholderScreen("Onboarding — Phase 6")
        }
    }
}

@Composable
private fun PlaceholderScreen(label: String) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
