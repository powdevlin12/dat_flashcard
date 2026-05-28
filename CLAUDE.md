# FlashMind (DatFS) - Project Guide

## Overview
FlashMind (DatFS) là app Android học flashcard sử dụng Kotlin + Jetpack Compose, kiến trúc MVVM + Clean Architecture.

## Tech Stack
- **Ngôn ngữ:** Kotlin 2.2.10
- **Build:** Gradle 9.3.1, AGP 8.8.2, KSP
- **Min SDK:** 26 | **Target SDK:** 36 (Android 16)
- **DI:** Dagger Hilt 2.56
- **UI:** Jetpack Compose (BOM 2024.12.01), Material 3, Navigation Compose 2.8.9
- **Database:** Room 2.7.1 (4 tables), DataStore Preferences 1.1.1
- **Background:** WorkManager 2.9.1, Glance AppWidget 1.1.0
- **Khác:** Coil 2.7.0, Apache POI 5.2.5 (Excel), Kotlinx Serialization JSON 1.7.3, Android TTS
- **Testing:** JUnit 4, MockK 1.13.12, Compose UI Test, Espresso 3.6.1

## Clean Architecture (3 layers)

### 1. `core/data/` - Data Layer
- **DAO:** `DeckDao`, `FlashcardDao`, `ReviewSessionDao`, `StudyStatisticsDao`
- **Entity:** `DeckEntity`, `FlashcardEntity`, `ReviewSessionEntity`, `StudyStatisticsEntity`
- **Repository Impl:** `DeckRepositoryImpl`, `FlashcardRepositoryImpl`, `ReviewRepositoryImpl`
- **Converter:** `StringListConverter` (Room TypeConverter)
- **Database:** `AppDatabase` (v1, fallback: destructiveMigration)
- **Datastore:** `SettingsDataStore` (theme, onboarding, TTS, widget, notification settings)

### 2. `core/domain/` - Domain Layer
- **Models:** `Deck`, `Flashcard`, `StudyModels` (ReviewSession, StudyStatistics, StudyMode enum)
- **Repository Interfaces:** `DeckRepository`, `FlashcardRepository`, `ReviewRepository`
- **Study:** `SM2Algorithm` (spaced repetition), `StudyQueue` (card queue per mode)
- **Use Cases:** `CardUseCases`, `DeckUseCases`, `StudyUseCases` (grouped data classes)
- **Common:** `Result<T>` sealed class (Loading/Success/Error), `AppException`

### 3. `feature/*/presentation/` - UI Layer
Mỗi feature có Screen + ViewModel (Hilt-injected).

## Features & Packages

| Package | Mô tả |
|---|---|
| `feature/home/` | Trang chủ, danh sách deck, tìm kiếm nhanh |
| `feature/deck/` | Chi tiết deck, tạo/sửa deck, danh sách card trong deck |
| `feature/card/` | Tạo/sửa flashcard |
| `feature/study/` | 5 chế độ học + swipeable cards + kết quả |
| `feature/search/` | Tìm kiếm toàn cục |
| `feature/statistics/` | Thống kê: heatmap calendar, bar chart, streak, deck progress |
| `feature/settings/` | Cài đặt (theme, TTS, backup, import/export) |
| `feature/backup/` | Backup & restore database |
| `feature/importexport/` | Import/Export Excel (Apache POI) |
| `feature/onboarding/` | Onboarding flow |
| `feature/splash/` | Splash screen |

## Navigation (`navigation/`)
- **Screen.kt** - Sealed class `Screen` định nghĩa tất cả routes
- **NavGraph.kt** - `@Composable` NavGraph setup, `NavHost`, all composable destinations
- **MainScaffold.kt** - Bottom navigation (4 tabs: Home, Search, Statistics, Settings)

Bottom nav tabs: Home → Search → Statistics → Settings

## Study Modes (`feature/study/`)
- `SPACED_REPETITION` - SM-2 algorithm, due cards first
- `LEARN` - Sequential mode, all cards
- `WRITE` - Type answer mode (show/hide hint toggle)
- `QUIZ` - Multiple choice (4 options, randomized)
- `MATCH` - Match pairs game

## Database Schema (Room entities)
- **deck_table** - id, title, description, tags (List<String>), color (Long), favorite (Boolean), archived (Boolean), timestamps
- **flashcard_table** - id, deckId (FK), frontText, backText, pronunciation, example, plus SM-2 fields: easeFactor, interval, repetitions, dueDate, lapses
- **review_session_table** - id, deckId, mode (enum), cardsStudied, correctCount, incorrectCount, startedAt, endedAt
- **study_statistics_table** - id, date, cardsStudied, studyMinutes, correctCount, incorrectCount (streak tính từ query)

## DI (`di/`)
5 Hilt modules:
- `AppModule` - Application-scoped singletons (Gson, CoroutineDispatchers)
- **DatabaseModule** - Room database + DAOs
- **DataStoreModule** - DataStore preferences
- **RepositoryModule** - Bind repository interfaces → impls
- **TtsModule** - TTS manager (@ActivityRetainedScoped)

## Key Design Patterns
- **Repository pattern:** Interface `core.domain.repository` / Impl `core.data.repository`
- **Result<T> wrapper:** `sealed class Result<T> { Loading, Success(data), Error(exception) }`
- **Use Cases:** Grouped into data classes (`CardUseCases`, `DeckUseCases`, `StudyUseCases`)
- **ViewModel:** Hilt-injected `@HiltViewModel`, exposes `StateFlow<UiState>`
- **SM-2 Algorithm:** Rating 0-5, adjusts ease factor, interval, tracks repetitions & lapses
- **DataStore keys:** `THEME_KEY`, `ONBOARDING_KEY`, `TTS_ENABLED_KEY`, `TTS_SPEED_KEY`, `WIDGET_ENABLED_KEY`, `NOTIFICATION_ENABLED_KEY`, `REVIEW_TIME_HOUR`, `REVIEW_TIME_MINUTE`
