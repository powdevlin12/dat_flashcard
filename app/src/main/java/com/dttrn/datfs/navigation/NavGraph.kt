package com.dttrn.datfs.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dttrn.datfs.core.data.datastore.SettingsDataStore
import com.dttrn.datfs.feature.card.CardEditorScreen
import com.dttrn.datfs.feature.deck.CreateEditDeckScreen
import com.dttrn.datfs.feature.deck.DeckDetailScreen
import com.dttrn.datfs.feature.home.HomeScreen
import com.dttrn.datfs.feature.search.SearchScreen
import com.dttrn.datfs.feature.backup.presentation.BackupScreen
import com.dttrn.datfs.feature.importexport.presentation.ImportExportScreen
import com.dttrn.datfs.feature.onboarding.OnboardingScreen
import com.dttrn.datfs.feature.settings.presentation.SettingsScreen
import com.dttrn.datfs.feature.splash.SplashScreen
import com.dttrn.datfs.feature.statistics.presentation.StatisticsScreen
import com.dttrn.datfs.feature.study.StudyModePickerScreen
import com.dttrn.datfs.feature.study.StudyResultScreen
import com.dttrn.datfs.feature.study.StudySessionScreen
import com.dttrn.datfs.feature.study.StudySessionViewModel

@Composable
fun NavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    settingsDataStore: SettingsDataStore? = null,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {

        // ===== Splash =====
        composable(Screen.Splash.route) {
            val isOnboardingDone by (settingsDataStore?.onboardingDone
                ?: kotlinx.coroutines.flow.flowOf(true))
                .collectAsState(initial = true)

            SplashScreen(
                isOnboardingDone = isOnboardingDone,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== Onboarding =====
        composable(Screen.Onboarding.route) {
            val onboardingVm: com.dttrn.datfs.feature.onboarding.OnboardingViewModel = hiltViewModel()
            OnboardingScreen(
                onFinish = {
                    onboardingVm.markOnboardingDone()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }

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
                    navController.navigate(Screen.StudyModePicker.createRoute(deckId))
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

        // ===== Study Mode Picker =====
        composable(
            route = Screen.StudyModePicker.route,
            arguments = listOf(
                navArgument(Screen.StudyModePicker.ARG_DECK_ID) { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString(Screen.StudyModePicker.ARG_DECK_ID) ?: return@composable
            StudyModePickerScreen(
                deckTitle = "",  // Will be shown from DeckDetail context
                onBack = { navController.popBackStack() },
                onSelectMode = { mode ->
                    navController.navigate(Screen.StudySession.createRoute(deckId, mode))
                },
            )
        }

        // ===== Study Session =====
        composable(
            route = Screen.StudySession.route,
            arguments = listOf(
                navArgument(Screen.StudySession.ARG_DECK_ID) { type = NavType.StringType },
                navArgument(Screen.StudySession.ARG_MODE) { type = NavType.StringType },
            )
        ) {
            StudySessionScreen(
                onBack = { navController.popBackStack() },
                onSessionComplete = { deckId ->
                    navController.navigate(Screen.StudyResult.createRoute(deckId)) {
                        popUpTo(Screen.StudySession.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== Study Result =====
        composable(
            route = Screen.StudyResult.route,
            arguments = listOf(
                navArgument(Screen.StudyResult.ARG_DECK_ID) { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val deckId = backStackEntry.arguments?.getString(Screen.StudyResult.ARG_DECK_ID) ?: return@composable
            // Read results from companion object (populated before session completed)
            val results = remember { StudySessionViewModel.pendingResults }
            val deckTitle = remember { StudySessionViewModel.pendingDeckTitle }
            StudyResultScreen(
                results = results,
                deckTitle = deckTitle,
                onDone = {
                    navController.popBackStack(Screen.DeckDetail.createRoute(deckId), false)
                },
                onStudyAgain = {
                    navController.navigate(Screen.StudyModePicker.createRoute(deckId)) {
                        popUpTo(Screen.StudyResult.route) { inclusive = true }
                    }
                },
            )
        }

        // ===== Statistics =====
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ===== Settings =====
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToImportExport = {
                    navController.navigate(Screen.ImportExport.route)
                },
                onNavigateToBackup = {
                    navController.navigate(Screen.Backup.route)
                },
            )
        }

        // ===== Import/Export =====
        composable(Screen.ImportExport.route) {
            ImportExportScreen(
                onBack = { navController.popBackStack() },
            )
        }

        // ===== Backup =====
        composable(Screen.Backup.route) {
            BackupScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
