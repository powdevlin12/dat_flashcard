# Exam Mode: Keyboard Shortcuts, Write Direction & Dictation

## Summary
Add keyboard shortcuts, write-direction config, and dictation question type to the examination feature, following patterns from StudySessionScreen.

## 1. Keyboard Shortcuts in ExamSessionScreen

Follow the FocusRequester + onPreviewKeyEvent pattern from StudySessionScreen.

### Mechanism
- Add `FocusRequester` to the root Box of ExamSessionScreen
- `onPreviewKeyEvent` intercepts keys before TextField
- `Tab` toggles between "typing mode" (TextField focused) and "shortcut mode"
- `Esc` in typing mode → unfocus TextField, return to shortcut mode
- `Esc` in shortcut mode → show exit confirmation dialog

### Key bindings

| Key | Context | Action |
|-----|---------|--------|
| `Tab` | WRITE / DICTATION | Toggle typing ↔ shortcut mode |
| `Esc` | Typing mode | Exit typing, return to shortcut mode |
| `Esc` | Shortcut mode | Show exit dialog |
| `Enter` | WRITE / DICTATION (answer checked) | Next question |
| `Enter` | WRITE / DICTATION (unchecked) | Check answer |
| `A / B / C / D` | MC (options present) | Select corresponding option |
| `←` | All | Previous question |
| `→` | All | Next question |
| `P` | WRITE / DICTATION | Speak current word (TTS) |
| `T` | WRITE / DICTATION | Toggle write/listen direction |
| `R` | DICTATION (unchecked) | Replay dictation audio |

### KeyboardShortcutsBar
Reusable composable at bottom bar showing active shortcuts as hint chips, styled same as StudySessionScreen.

## 2. Write Direction

### Config (ExamConfigScreen)
When `WRITE` or `MIXED` is selected, show a sub-option radio group:
- "Gõ mặt sau (đáp án)" — show frontText, user types backText (default)
- "Gõ mặt trước (câu hỏi)" — show backText, user types frontText

### New types
```kotlin
enum class WriteDirection(val displayName: String) {
    BACK("Gõ mặt sau"),
    FRONT("Gõ mặt trước"),
}
```

### Runtime toggle
- `T` key toggles write direction during the exam
- Applies to both WRITE and DICTATION questions in the current session

### Answer checking
- Compare user answer against the correct side based on current `writeDirection`
- `BACK`: correct = `card.backText`, `FRONT`: correct = `card.frontText`

## 3. Dictation Question Type

### New QuestionType entry
```kotlin
DICTATION("Nghe chép chính tả")
```

### ExamConfigScreen
- Add `DICTATION` as a radio option with description: "Nghe âm thanh, gõ lại chính tả"
- Note: `MIXED` only randomizes between MC and WRITE — DICTATION is excluded from mixed mode because it's a fundamentally different interaction (audio-based).

### ExamSessionScreen — Dictation UI
The dictation card replaces the question card:
- Icon (HeadsetMic / VolumeUp) with wave animation when speaking
- "Nghe và gõ lại" status text
- Auto-speak the word on entering the question
- Show play count ("Đã nghe N lần")
- `R` key or tap icon to replay
- `T` key toggles direction: "Nghe mặt trước, gõ mặt sau" ↔ "Nghe mặt sau, gõ mặt trước"

Uses `TtsManager` (existing singleton, injected into ViewModel).

### Answer validation
Same as WRITE: case-insensitive, trimmed comparison against the expected side.

## 4. Route Changes

`ExamSession` route adds `writeDirection` parameter:
```
exam_session/{deckId}/{questionCount}/{questionType}/{timeLimitMinutes}/{writeDirection}
```

`ExamConfig` passes `writeDirection` through the start config string and route.

## 5. Files Changed

| File | Changes |
|------|---------|
| `ExamConfigUiState.kt` | Add `DICTATION` to `QuestionType`, add `WriteDirection` enum |
| `ExamConfigScreen.kt` | Add write-direction sub-option, DICTATION radio, pass writeDirection |
| `ExamConfigViewModel.kt` | Add `writeDirection` to state, update `buildStartConfig()` |
| `ExamSessionUiState.kt` | Add `writeDirection`, `isWriteInputFocused`, `dictationPlayCount` |
| `ExamSessionViewModel.kt` | Inject `TtsManager`, add keyboard/speak/replay/write-direction actions, dictation generation |
| `ExamSessionScreen.kt` | Add FocusRequester, onPreviewKeyEvent, handleKeyEvent, KeyboardShortcutsBar, Dictation UI, write-direction logic |
| `Screen.kt` | Add `writeDirection` param to `ExamSession` route |
| `NavGraph.kt` | Pass `writeDirection` through nav arguments |
