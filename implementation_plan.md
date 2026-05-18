# Implementation Plan: FlashMind Android App

## Tổng Quan

**FlashMind** là ứng dụng học flashcard Android native, offline-first, lấy cảm hứng từ Quizlet. App được xây dựng bằng Kotlin + Jetpack Compose + MVVM + Clean Architecture + Hilt + Room.

### Trạng Thái Hiện Tại

Dự án đang ở bước **skeleton**: chỉ có theme, navigation shell, DI cơ bản và preferences. Package hiện tại là `com.dttrn.datfs`. Tất cả business logic, DB, UI features cần xây dựng từ đầu theo PRD.

> [!IMPORTANT]
> Package name hiện tại là `com.dttrn.datfs` (từ lần reset trước). PRD dùng `com.flashmind` làm ví dụ nhưng ta sẽ giữ nguyên `com.dttrn.datfs` và đổi tên app thành **FlashMind**.

---

## Open Questions

> [!WARNING]
> Cần xác nhận trước khi bắt đầu Phase 5:
> - Apache POI nặng (~15MB), có thể dùng `poi-ooxml-lite` để giảm APK size. Có cần support định dạng `.xls` cũ không, hay chỉ `.xlsx`?

> [!IMPORTANT]
> MVP vs Full scope: PRD có 7 phase, 17-19 tuần. Bạn muốn:
> - **Option A**: Làm đủ MVP trước (Phase 1-3 + Phase 5 import/export cơ bản) → app usable
> - **Option B**: Làm tuần tự theo đúng 7 phase của PRD
> - **Option C**: Ưu tiên phase nào trước?

---

## Proposed Changes

### Phase 1: Foundation & Project Setup (Tuần 1-2)

**Mục tiêu:** Core infrastructure, DB schema, DI, Navigation hoàn chỉnh

---

#### [MODIFY] build.gradle.kts (app)
Thêm dependencies còn thiếu:
- `androidx.datastore:datastore-preferences`
- `io.coil-kt:coil-compose` (image loading)
- `org.apache.poi:poi-ooxml` (Excel — thêm ở Phase 5)
- `kotlinx-coroutines-android`
- `kotlinx-serialization-json` (đã có)
- Thêm `hilt-work` (đã có)

#### [MODIFY] gradle/libs.versions.toml
Thêm version catalog cho DataStore, Coil, Coroutines Test

---

#### [NEW] Database Layer — `core/data/local/`

| File | Mô tả |
|------|-------|
| `AppDatabase.kt` | Room DB, version 1, export schema |
| `entity/DeckEntity.kt` | Table `deck_table` theo PRD §7.1 |
| `entity/FlashcardEntity.kt` | Table `flashcard_table` theo PRD §7.2 + SM-2 metadata |
| `entity/ReviewSessionEntity.kt` | Table `review_session_table` theo PRD §7.3 |
| `entity/StudyStatisticsEntity.kt` | Table `study_statistics_table` theo PRD §7.4 |
| `dao/DeckDao.kt` | CRUD + search + archive theo PRD §7.5 |
| `dao/FlashcardDao.kt` | CRUD + review queue queries |
| `dao/ReviewSessionDao.kt` | Insert/query sessions |
| `dao/StudyStatisticsDao.kt` | Upsert daily stats, streak query |

**Lưu ý:** Tags trong `DeckEntity` dùng `TypeConverter` (JSON string ↔ `List<String>`)

---

#### [NEW] Domain Layer — `core/domain/`

| File | Mô tả |
|------|-------|
| `model/Deck.kt` | Domain model (không phải entity) |
| `model/Flashcard.kt` | Domain model với SM-2 fields |
| `model/ReviewSession.kt` | Domain model |
| `model/StudyStatistics.kt` | Domain model |
| `repository/DeckRepository.kt` | Interface |
| `repository/FlashcardRepository.kt` | Interface |
| `repository/ReviewRepository.kt` | Interface |
| `common/Result.kt` | Sealed class `Result<T>` (Success/Error/Loading) |
| `common/AppException.kt` | AppException hierarchy |

---

#### [MODIFY] `di/AppModule.kt` → Tách thành:
- `di/DatabaseModule.kt` — cung cấp Room DB + DAOs
- `di/RepositoryModule.kt` — bind interfaces → impls
- `di/DataStoreModule.kt` — cung cấp DataStore

---

#### [NEW] `core/data/datastore/SettingsDataStore.kt`
Lưu: theme, font size, default study mode, notification time, animation enabled

---

#### [MODIFY] Navigation — `navigation/`

| File | Mô tả |
|------|-------|
| `Screen.kt` | Thêm tất cả routes: Home, DeckDetail, CreateEditDeck, CardEditor, StudyMode, Statistics, Settings, ImportExport, Search, Backup |
| `NavGraph.kt` | Wiring tất cả composable destinations |

---

#### [NEW] Theme — `ui/theme/`
- `Color.kt` — Material 3 seed: Primary `#4A90E2`, Secondary `#7B61FF`, Tertiary `#00C853`
- `Type.kt` — Nunito font family (ExtraBold/Bold/SemiBold/Regular)
- `Theme.kt` — Light/Dark theme với dynamic color

---

### Phase 2: Core CRUD Features (Tuần 3-6)

**Mục tiêu:** User có thể tạo, sửa, xóa, tìm kiếm deck và thẻ

---

#### [NEW] Feature: `feature/home/`

| File | Mô tả |
|------|-------|
| `data/repository/HomeRepositoryImpl.kt` | Implement HomeRepository |
| `domain/usecase/GetDecksUseCase.kt` | Flow list decks (active, not archived) |
| `domain/usecase/GetStudySummaryUseCase.kt` | Streak, today's due count |
| `presentation/HomeUiState.kt` | Sealed state: Loading/Success/Error/Empty |
| `presentation/HomeViewModel.kt` | Combine decks + summary |
| `presentation/HomeScreen.kt` | TopBar + Stats banner + Deck sections + FAB |
| `presentation/component/DeckCard.kt` | Reusable deck card với màu sắc, progress |

**Interactions:** Long press → context menu (sửa/xóa/duplicate/archive)

---

#### [NEW] Feature: `feature/deck/`

| File | Mô tả |
|------|-------|
| `data/repository/DeckRepositoryImpl.kt` | Implement CRUD, duplicate, archive |
| `domain/usecase/CreateDeckUseCase.kt` | Validate + insert |
| `domain/usecase/UpdateDeckUseCase.kt` | Update metadata |
| `domain/usecase/DeleteDeckUseCase.kt` | Soft delete (isArchived) hoặc hard delete |
| `domain/usecase/DuplicateDeckUseCase.kt` | Copy deck + tất cả cards trong transaction |
| `domain/usecase/ArchiveDeckUseCase.kt` | Toggle archive |
| `presentation/DeckDetailUiState.kt` | |
| `presentation/DeckDetailViewModel.kt` | Cards list, quick stats |
| `presentation/DeckDetailScreen.kt` | Collapsing header + study mode selector + card list |
| `presentation/CreateEditDeckViewModel.kt` | Form state, validation |
| `presentation/CreateEditDeckScreen.kt` | Form: tên, mô tả, category, tags, color picker |

---

#### [NEW] Feature: `feature/card/`

| File | Mô tả |
|------|-------|
| `data/repository/FlashcardRepositoryImpl.kt` | CRUD, bulk ops |
| `domain/usecase/AddCardUseCase.kt` | Validate + insert |
| `domain/usecase/UpdateCardUseCase.kt` | Update + updatedAt |
| `domain/usecase/DeleteCardUseCase.kt` | Single delete |
| `domain/usecase/BulkEditCardsUseCase.kt` | Multi-select: delete/move/tag |
| `presentation/CardEditorUiState.kt` | |
| `presentation/CardEditorViewModel.kt` | Form state, image handling |
| `presentation/CardEditorScreen.kt` | Front/Back TextField, pronunciation, example, note, difficulty, image picker |

**Image handling:** ActivityResultContract cho gallery + camera, scale về max 1024px, lưu vào `filesDir/images/`

---

#### [NEW] Feature: `feature/search/`

| File | Mô tả |
|------|-------|
| `domain/usecase/SearchUseCase.kt` | Debounce 300ms, query decks + cards đồng thời |
| `presentation/SearchViewModel.kt` | Search state + history (10 items, DataStore) |
| `presentation/SearchScreen.kt` | SearchBar + grouped results (Deck/Card), keyword highlight |

---

#### [NEW] Core UI Components — `core/ui/component/`

| File | Mô tả |
|------|-------|
| `FlashcardItem.kt` | Card item trong list (front + back rút gọn) |
| `ConfirmDeleteDialog.kt` | Reusable xác nhận xóa |
| `ColorPicker.kt` | Palette chọn màu cho deck |
| `TagInput.kt` | Input + chip list cho tags |
| `EmptyState.kt` | Empty state với illustration |

---

### Phase 3: Study Engine (Tuần 7-10)

**Mục tiêu:** 5 chế độ học hoạt động + SM-2 algorithm

---

#### [NEW] SM-2 Algorithm — `feature/study/domain/algorithm/`

| File | Mô tả |
|------|-------|
| `SM2Algorithm.kt` | `calculateNextReview(card, quality)` → `ReviewResult` theo công thức PRD §4.4 |
| `StudyQueue.kt` | Logic xây queue: Today / Upcoming / Overdue / New |

**Unit tests bắt buộc:** Test các trường hợp quality 0-5, repetition count, interval escalation

---

#### [NEW] Study UseCases — `feature/study/domain/usecase/`

| File | Mô tả |
|------|-------|
| `GetStudyQueueUseCase.kt` | Lấy queue theo mode (today/overdue/new) |
| `SubmitReviewUseCase.kt` | Áp dụng SM-2, update card, ghi session |

---

#### [NEW] SM-01: Swipe Mode — `feature/study/presentation/swipe/`

| File | Mô tả |
|------|-------|
| `SwipeStudyViewModel.kt` | Queue management, undo stack, session tracking |
| `SwipeStudyScreen.kt` | 3D flip card animation, swipe gesture, progress bar, undo button |
| `FlipCard.kt` | Composable với `animateFloatAsState` 300ms, `FastOutSlowIn` easing |

**Animation:** Card rotate Y 180°, front/back visibility based on rotation angle

---

#### [NEW] SM-02: Learn Mode — `feature/study/presentation/learn/`

| File | Mô tả |
|------|-------|
| `LearnModeViewModel.kt` | Adaptive queue: sai → re-insert, đúng 1 lần → remove |
| `LearnModeScreen.kt` | Hiển thị câu hỏi + hint (1 ký tự đầu) + kết quả |

---

#### [NEW] SM-03: Write Mode — `feature/study/presentation/write/`

| File | Mô tả |
|------|-------|
| `WriteModeViewModel.kt` | String comparison (case-insensitive, trim) |
| `WriteModeScreen.kt` | TextField nhập đáp án, highlight đúng/sai từng ký tự |

---

#### [NEW] SM-04: Quiz Mode — `feature/study/presentation/quiz/`

| File | Mô tả |
|------|-------|
| `QuizViewModel.kt` | Distractor generation (random 3 cards từ cùng deck), timer countdown |
| `QuizScreen.kt` | 4 RadioButton options, highlight đúng/sai sau khi chọn, score |

---

#### [NEW] SM-05: Match Game — `feature/study/presentation/match/`

| File | Mô tả |
|------|-------|
| `MatchGameViewModel.kt` | Grid state, tap-to-match logic, timer, combo multiplier, high score |
| `MatchGameScreen.kt` | Grid 4x2 hoặc 3x2, drag-and-drop hoặc tap-to-select |

---

#### [NEW] Session Result Screen — `feature/study/presentation/`

| File | Mô tả |
|------|-------|
| `StudyResultScreen.kt` | Tóm tắt: đúng/sai/thời gian/accuracy, nút "Học lại" / "Về deck" |

---

### Phase 4: Statistics & Spaced Repetition Queue (Tuần 11-12)

**Mục tiêu:** Dashboard thống kê đầy đủ

---

#### [NEW] Feature: `feature/statistics/`

| File | Mô tả |
|------|-------|
| `domain/usecase/GetStatisticsUseCase.kt` | Tổng hợp: streak, accuracy, cards mastered, study time |
| `presentation/StatisticsViewModel.kt` | Aggregate data từ DB |
| `presentation/StatisticsScreen.kt` | Streak badge, summary cards, bar chart, calendar heatmap, deck performance |

**Charts:** Vẽ thuần Compose Canvas (không dùng thư viện ngoài):
- `BarChart.kt` — thẻ học theo 7 ngày
- `CalendarHeatmap.kt` — 12 tuần mật độ học
- `DeckProgressList.kt` — horizontal progress bar từng deck

---

#### [NEW] Auto Stats Tracking

`SubmitReviewUseCase` tự động upsert `StudyStatisticsEntity` mỗi phiên học (date, cardsStudied, minutesStudied, correctAnswers, streakCount).

---

### Phase 5: Import/Export Excel (Tuần 13-14)

**Mục tiêu:** Import từ .xlsx và export thành công

---

#### [MODIFY] `build.gradle.kts` — Thêm Apache POI

```kotlin
implementation("org.apache.poi:poi-ooxml:5.2.5")
```

Thêm ProGuard rules để giảm APK size.

---

#### [NEW] Feature: `feature/importexport/`

| File | Mô tả |
|------|-------|
| `data/parser/ExcelParser.kt` | Đọc .xlsx (POI), validate cột A/B bắt buộc, parse row → FlashcardEntity |
| `data/exporter/ExcelExporter.kt` | Tạo workbook: mỗi deck = 1 sheet, sheet Statistics |
| `domain/usecase/ImportExcelUseCase.kt` | Parse → preview → confirm import trên IO dispatcher |
| `domain/usecase/ExportExcelUseCase.kt` | Export → temp file → share intent |
| `presentation/ImportExportViewModel.kt` | Tab state, file picker, preview data, error list |
| `presentation/ImportExportScreen.kt` | Tab: Import \| Export, preview table, error list, deck selector |

**Template:** Nút tạo file `.xlsx` mẫu với header + 2-3 dòng example

---

#### [NEW] Feature: `feature/backup/`

| File | Mô tả |
|------|-------|
| `domain/usecase/BackupUseCase.kt` | Serialize toàn bộ DB → JSON, hoặc copy file .db |
| `domain/usecase/RestoreUseCase.kt` | Validate JSON schema → merge hoặc overwrite, rollback nếu fail |
| `presentation/BackupScreen.kt` | 3 loại backup: JSON / DB / Excel, restore với preview |

---

### Phase 6: Polish, Settings & Notifications (Tuần 15-16)

**Mục tiêu:** Production-ready

---

#### [NEW] Feature: `feature/settings/`

| File | Mô tả |
|------|-------|
| `presentation/SettingsViewModel.kt` | Read/write DataStore |
| `presentation/SettingsScreen.kt` | Grouped prefs: theme, font size, animation, notification, default study mode, backup |

---

#### [NEW] Local Notifications — `core/notification/`

| File | Mô tả |
|------|-------|
| `ReviewReminderWorker.kt` | WorkManager worker: query cards due, fire notification |
| `StreakReminderWorker.kt` | Check streak at 23:00 |
| `NotificationScheduler.kt` | Schedule periodic workers theo setting |
| `NotificationChannels.kt` | Create channels (Daily Review, Overdue, Streak) |

**Permission:** `POST_NOTIFICATIONS` trên API 33+

---

#### [NEW] Onboarding — `feature/onboarding/`

3-step onboarding (chỉ hiện lần đầu, lưu flag trong DataStore):
1. Welcome + giải thích SM-2
2. Hướng dẫn tạo deck
3. Hướng dẫn import Excel

---

#### [MODIFY] Splash Screen
- Animated logo FlashMind
- Khởi tạo DB, load settings → navigate Home
- Handle DB migration failure

---

#### UI Polish Checklist
- [ ] Micro-animations: Card appear (slide up + fade), FAB rotation
- [ ] Error handling: Snackbar cho mọi lỗi user-facing
- [ ] Pull-to-refresh trên Home
- [ ] Swipe-to-delete trên Card list (với undo Snackbar)
- [ ] Loading skeletons
- [ ] Accessibility: contentDescription, touch target 48dp, contrast 4.5:1
- [ ] Dynamic font size (sp units)

---

### Phase 7: Testing & Release (Tuần 17-18)

---

#### Unit Tests

| Test | Scope | Framework |
|------|-------|-----------|
| `SM2AlgorithmTest` | SM-2 tất cả quality 0-5 | JUnit4 |
| `DeckUseCaseTest` | Create/Update/Delete/Duplicate | JUnit4 + MockK |
| `ExcelParserTest` | Import validation, error cases | JUnit4 |
| `StudyQueueTest` | Queue ordering, SM-2 integration | JUnit4 |
| `HomeViewModelTest` | UiState flow | JUnit4 + Coroutines Test |

#### Integration Tests

| Test | Scope | Framework |
|------|-------|-----------|
| `DeckDaoTest` | CRUD + search trên in-memory Room | Room Testing |
| `FlashcardDaoTest` | Review queue queries | Room Testing |
| `BackupRestoreTest` | JSON round-trip integrity | JUnit4 |

#### UI Tests (Compose Testing)

| Test | Flow |
|------|------|
| `CreateDeckFlow` | Home → FAB → Form → Save → Deck appears |
| `StudySessionFlow` | DeckDetail → Swipe Mode → Complete → Result |
| `ImportFlowTest` | ImportExport → Pick file → Preview → Confirm |

---

## Verification Plan

### Automated Tests
```bash
# Unit + integration tests
./gradlew test

# Instrumentation tests
./gradlew connectedAndroidTest

# Build release APK
./gradlew assembleRelease
```

### Manual Verification
Sau mỗi phase:
- [ ] Build thành công, không crash
- [ ] Screen navigation hoạt động đúng
- [ ] Dark/Light theme toggle
- [ ] Dữ liệu persist sau kill app

---

## Dependency Map (Build Order)

```
Phase 1: DB Schema → DAOs → Domain Models → Repositories → DI → Navigation
    ↓
Phase 2: Home → Deck CRUD → Card CRUD → Search
    ↓
Phase 3: SM-2 Algorithm → Study Queue → Swipe → Learn → Write → Quiz → Match
    ↓
Phase 4: Stats Tracking (auto) → Statistics Dashboard
    ↓
Phase 5: Excel Parser → Import/Export → Backup/Restore
    ↓
Phase 6: Settings → Notifications → Polish → Onboarding
    ↓
Phase 7: Tests → Release
```

---

## Ước Tính File Cần Tạo

| Phase | Files mới | Files sửa |
|-------|-----------|-----------|
| 1 — Foundation | ~20 | 3 |
| 2 — Core CRUD | ~30 | 2 |
| 3 — Study Engine | ~20 | 1 |
| 4 — Statistics | ~8 | 1 |
| 5 — Import/Export | ~10 | 1 |
| 6 — Polish | ~12 | 5 |
| 7 — Testing | ~10 | 0 |
| **Tổng** | **~110** | **~13** |

