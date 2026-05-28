# Dictation Study Mode — Design Spec

## Overview

Thêm chế độ học "Nghe chính tả" (Dictation) vào FlashMind. Đây là study mode thứ 6, bên cạnh Spaced Repetition, Learn, Write, Quiz, Match.

**Nguyên lý:** TTS đọc text của flashcard → người dùng nghe và gõ lại những gì họ nghe được → so sánh và chấm điểm.

## Key Decisions

| Vấn đề | Quyết định |
|--------|------------|
| Dictation là mode riêng? | Có, study mode thứ 6 |
| Đọc mặt nào của card? | Có toggle chọn frontText hoặc backText |
| Replay giới hạn? | Không giới hạn |
| Speed control? | Có, slider 0.5x - 2.0x |
| Text có bị ẩn? | Ẩn hoàn toàn, không có nút reveal |
| Auto-play? | Có, tự động đọc khi card mới xuất hiện |
| Cách chấm điểm? | Trim + lowercase + bỏ punctuation, so khớp chính xác |
| SM-2 integration? | Giống WRITE: đúng=GOOD(4), sai=AGAIN(0) |

## Architecture

Theo mô hình có sẵn: thêm DICTATION vào `StudyMode` enum, mở rộng `StudySessionScreen` + `StudySessionViewModel`. Không tạo screen/viewmodel riêng để giữ nhất quán kiến trúc.

```
StudyMode enum (thêm DICTATION)
     ↓
StudyModePickerScreen (thêm option card)
     ↓
StudySessionScreen (thêm DictationContent composable)
     ↓
StudySessionViewModel (thêm dictation methods + state)
     ↓
StudyQueue (thêm DICTATION case trong buildFor)
     ↓
SM2Algorithm (dùng lại fromQuizAnswer)
     ↓
TtsManager (thêm setSpeed method)
```

## UI Layout

```
┌─────────────────────────────────┐
│  Deck Title        Mode: Dictation │  ← Top bar (có sẵn)
│  ████████████░░░░  5/20            │  ← Progress bar (có sẵn)
├─────────────────────────────────┤
│                                 │
│  [Front ▼]          (toggle)    │  ← FrontBackToggle (dùng lại)
│                                 │
│     ┌───────────┐               │
│     │  🎧 🔊     │               │  ← Listening indicator
│     │  Đang đọc..│               │    + animated sound waves
│     └───────────┘               │
│                                 │
│  [▶ Replay]  Tốc độ: ──●──     │  ← Replay button + speed slider
│                                 │
│  ┌─────────────────────────┐    │
│  │ Nhập những gì bạn nghe  │    │  ← Input field
│  └─────────────────────────┘    │
│                                 │
│  [Kiểm tra] / [Tiếp theo]       │  ← Action button
│                                 │
│  Phím tắt: Enter=Kiểm tra ...   │  ← Shortcut hints (có sẵn)
└─────────────────────────────────┘
```

### Sau khi nộp câu trả lời

- Input field disabled, viền xanh (đúng) hoặc đỏ (sai)
- Hiển thị câu trả lời đúng + câu trả lời của người dùng
- Nút chuyển từ "Kiểm tra" → "Tiếp theo"

### Trạng thái đặc biệt

- **TTS không khả dụng:** Hiển thị thông báo lỗi, disable nút replay
- **Đang load card mới:** Delay nhẹ trước auto-play

## State Changes (StudySessionUiState)

Thêm các field mới (dùng chung data class với WRITE cho writeAnswer, isAnswerRevealed, isWriteCorrect, isCorrect):

```kotlin
val dictationSpeed: Float = 1.0f,
val dictationPlayCount: Int = 0,
```

Dùng lại từ WRITE: `writeAnswer`, `isAnswerRevealed`, `isWriteCorrect`, `isCorrect`, `showFrontFirst`.

## ViewModel Methods (thêm vào StudySessionViewModel)

| Method | Mô tả |
|--------|-------|
| `onReplayDictation()` | Gọi `ttsManager.speak()` với text hiện tại + speed đã set |
| `onDictationSpeedChange(speed: Float)` | Cập nhật `dictationSpeed`, gọi `ttsManager.setSpeechRate(speed)` |
| `onSubmitDictation()` | So sánh answer, set isAnswerRevealed + isWriteCorrect |
| `onDictationAdvance()` | Gọi `onRateCard()` với rating từ kết quả, load card mới |

## Data Flow

```
1. Card mới → loadNextCard()
2. Auto-play: ttsManager.setSpeed(dictationSpeed) + ttsManager.speak(text)
   - text = showFrontFirst ? card.frontText : card.backText
   - dictationPlayCount = 1, isSpeaking = true
3. User actions:
   - Gõ answer → onWriteAnswerChange()
   - Replay → onReplayDictation() (playCount++)
   - Đổi speed → onDictationSpeedChange()
4. Submit → onDictationSubmit()
   - isDictationMatch(writeAnswer, correctAnswer)
   - isAnswerRevealed = true
5. Advance → onDictationAdvance()
   - fromQuizAnswer(isWriteCorrect) → onRateCard()
   - loadNextCard() → auto-play
```

## Matching Algorithm

```kotlin
fun isDictationMatch(userAnswer: String, correctAnswer: String): Boolean {
    val normalizedUser = userAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    val normalizedCorrect = correctAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    return normalizedUser == normalizedCorrect
}
```

## TtsManager Changes

Thêm method:

```kotlin
fun setSpeed(speed: Float) {
    tts?.setSpeechRate(speed)
}
```

## StudyModePicker

Thêm entry với gradient màu teal/cyan:
- Title: "Nghe chính tả"
- Subtitle: "Luyện nghe và viết lại"
- Description: "TTS đọc từ/cụm từ, bạn gõ lại những gì nghe được"

## StudyQueue Changes

Thêm `StudyMode.DICTATION` vào các nhánh xử lý giống `QUIZ`/`WRITE`/`MATCH` trong `buildFor()`.

## Keyboard Shortcuts

Giống WRITE mode:
- `Enter`: Submit answer (khi chưa reveal) hoặc advance (khi đã reveal)
- `Escape`: Unfocus input field

## Files to Modify

| File | Change |
|------|--------|
| `core/data/local/entity/ReviewSessionEntity.kt` | Thêm `DICTATION` vào `StudyMode` enum |
| `core/domain/study/StudyQueue.kt` | Thêm `DICTATION` case trong `buildFor()` |
| `core/tts/TtsManager.kt` | Thêm `setSpeed()` method |
| `feature/study/StudySessionUiState.kt` | Thêm `dictationSpeed`, `dictationPlayCount` fields |
| `feature/study/StudySessionViewModel.kt` | Thêm dictation methods, auto-play logic |
| `feature/study/StudySessionScreen.kt` | Thêm `DictationContent` composable, wire vào `when(mode)` |
| `feature/study/StudyModePickerScreen.kt` | Thêm dictation option card |

## Testing

- **Unit test:** `isDictationMatch` — các case: exact match, different case, extra whitespace, punctuation diff, wrong answer
- **Unit test:** Dictation flow trong ViewModel — load card, auto-play trigger, submit, advance
- **UI test:** DictationContent hiển thị đúng các trạng thái: listening, answer input, feedback correct, feedback incorrect
- **Integration test:** TTS auto-play được gọi khi card load, replay button hoạt động, speed slider cập nhật TTS rate
