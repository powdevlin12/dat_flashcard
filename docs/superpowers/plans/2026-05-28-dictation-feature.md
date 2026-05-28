# Dictation Study Mode — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add dictation (Nghe chính tả) as the 6th study mode where TTS reads card text aloud, user types what they hear, and answer is compared for correctness.

**Architecture:** Extend existing study session infrastructure — add DICTATION to StudyMode enum, add dictation-specific state + ViewModel methods, create DictationContent composable, wire into mode picker. Reuses StudyQueue, SM2Algorithm, TtsManager, and session flow.

**Tech Stack:** Kotlin, Jetpack Compose, Android TTS, Dagger Hilt

---

### Task 1: Add DICTATION to StudyMode enum and StudyQueue

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/core/data/local/entity/ReviewSessionEntity.kt:43-53`
- Modify: `app/src/main/java/com/dttrn/datfs/core/domain/study/StudyQueue.kt:29-44, 94-118`

- [ ] **Step 1: Add DICTATION to StudyMode enum**

In `ReviewSessionEntity.kt`, add the new enum value:

```kotlin
enum class StudyMode(val displayName: String) {
    SPACED_REPETITION("Lặp lại ngắt quãng"),
    LEARN("Học tuần tự"),
    WRITE("Gõ đáp án"),
    QUIZ("Trắc nghiệm"),
    MATCH("Ghép đôi"),
    DICTATION("Nghe chính tả");
}
```

- [ ] **Step 2: Add DICTATION case in StudyQueue init block**

In `StudyQueue.kt`, add `StudyMode.DICTATION` alongside WRITE/QUIZ/MATCH in the `init` block (line 42-44):

```kotlin
StudyMode.WRITE,
StudyMode.QUIZ,
StudyMode.MATCH,
StudyMode.DICTATION -> if (shuffled) allCards.shuffled() else allCards
```

- [ ] **Step 3: Add DICTATION case in StudyQueue.buildFor()**

In `StudyQueue.kt`, add `StudyMode.DICTATION` alongside QUIZ/WRITE/MATCH in the `buildFor()` companion method (line 106-113):

```kotlin
StudyMode.QUIZ,
StudyMode.WRITE,
StudyMode.MATCH,
StudyMode.DICTATION -> {
    if (dueOnly) {
        cards.filter { !it.isKnown && (it.isNew || it.isDueBy(now)) }
    } else {
        cards.filter { !it.isKnown }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/core/data/local/entity/ReviewSessionEntity.kt \
        app/src/main/java/com/dttrn/datfs/core/domain/study/StudyQueue.kt
git commit -m "feat: add DICTATION to StudyMode enum and StudyQueue"
```

---

### Task 2: Add setSpeed() to TtsManager

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/core/tts/TtsManager.kt:64-68`

- [ ] **Step 1: Add setSpeed() method**

In `TtsManager.kt`, add after the `speak()` method (after line 68):

```kotlin
/**
 * Set TTS speech rate.
 * @param speed Speech rate multiplier (0.5 = half speed, 1.0 = normal, 2.0 = double speed)
 */
fun setSpeed(speed: Float) {
    tts?.setSpeechRate(speed)
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/core/tts/TtsManager.kt
git commit -m "feat: add setSpeed() method to TtsManager"
```

---

### Task 3: Add dictation state fields to StudySessionUiState

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/study/StudySessionUiState.kt:28-40`

- [ ] **Step 1: Add dictationSpeed and dictationPlayCount fields**

In `StudySessionUiState.kt`, add after the `isWriteCorrect` field (after line 30):

```kotlin
// Dictation specific
val dictationSpeed: Float = 1.0f,
val dictationPlayCount: Int = 0,
```

The updated data class should have these fields in order:

```kotlin
data class StudySessionUiState(
    val isLoading: Boolean = true,
    val deckTitle: String = "",
    val mode: StudyMode = StudyMode.SPACED_REPETITION,
    val currentCard: Flashcard? = null,
    val currentIndex: Int = 0,
    val totalCount: Int = 0,
    val reviewedCount: Int = 0,
    val isFlipped: Boolean = false,
    val showFrontFirst: Boolean = true,
    val isShuffled: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val isComplete: Boolean = false,
    val sessionResults: List<CardResult> = emptyList(),
    val quizOptions: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val isCorrect: Boolean? = null,
    val writeAnswer: String = "",
    val isWriteCorrect: Boolean? = null,
    val dictationSpeed: Float = 1.0f,
    val dictationPlayCount: Int = 0,
    val matchItems: List<MatchItem> = emptyList(),
    val selectedMatchId: String? = null,
    val showRangeDialog: Boolean = false,
    val originalTotalCount: Int = 0,
    val rangeFrom: Int? = null,
    val rangeTo: Int? = null,
    val isRangeApplied: Boolean = false,
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/study/StudySessionUiState.kt
git commit -m "feat: add dictation state fields to StudySessionUiState"
```

---

### Task 4: Add dictation methods to StudySessionViewModel

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/study/StudySessionViewModel.kt:205-233, 345-348`

- [ ] **Step 1: Add dictation answer matching helper**

Add as a private function in the companion object or top-level in the file (before the class closing brace at line 420):

```kotlin
// ===== DICTATION ACTIONS =====

private fun isDictationMatch(userAnswer: String, correctAnswer: String): Boolean {
    val normalizedUser = userAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    val normalizedCorrect = correctAnswer.trim()
        .lowercase()
        .replace(Regex("[\\p{Punct}]"), "")
    return normalizedUser == normalizedCorrect
}
```

- [ ] **Step 2: Add onReplayDictation()**

Add after the matching helper:

```kotlin
fun onReplayDictation() {
    val state = _uiState.value
    val card = state.currentCard ?: return
    val text = if (state.showFrontFirst) card.frontText else card.backText
    ttsManager.setSpeed(state.dictationSpeed)
    ttsManager.speak(text)
    _uiState.update { it.copy(dictationPlayCount = it.dictationPlayCount + 1) }
}
```

- [ ] **Step 3: Add onDictationSpeedChange()**

```kotlin
fun onDictationSpeedChange(speed: Float) {
    ttsManager.setSpeed(speed)
    _uiState.update { it.copy(dictationSpeed = speed) }
}
```

- [ ] **Step 4: Add onSubmitDictation()**

```kotlin
fun onSubmitDictation() {
    if (_uiState.value.isAnswerRevealed) return
    val state = _uiState.value
    val card = state.currentCard ?: return
    val userAnswer = state.writeAnswer
    val correctAnswer = if (state.showFrontFirst) card.backText else card.frontText
    val isCorrect = isDictationMatch(userAnswer, correctAnswer)
    _uiState.update {
        it.copy(
            isAnswerRevealed = true,
            isWriteCorrect = isCorrect,
            isCorrect = isCorrect,
        )
    }
}
```

- [ ] **Step 5: Add onDictationAdvance()**

Note: `loadNextCard()` already handles auto-play for DICTATION mode (see Step 6), so `onDictationAdvance()` only needs to submit the rating. No duplicate TTS call here.

```kotlin
fun onDictationAdvance() {
    val correct = _uiState.value.isWriteCorrect ?: false
    onRateCard(SM2Algorithm.Ratings.fromQuizAnswer(correct))
}
```

- [ ] **Step 6: Add auto-play in loadNextCard() for DICTATION mode**

In `loadNextCard()`, after the `_uiState.update` block that resets card state (after line 113), add:

```kotlin
// Auto-play TTS for dictation mode
if (mode == StudyMode.DICTATION) {
    val text = if (_uiState.value.showFrontFirst) next.frontText else next.backText
    ttsManager.setSpeed(_uiState.value.dictationSpeed)
    ttsManager.speak(text)
    _uiState.update { it.copy(dictationPlayCount = 1) }
}
```

The complete `loadNextCard()` should now look like:

```kotlin
private fun loadNextCard() {
    val q = queue ?: return
    if (mode == StudyMode.MATCH) return
    val next = q.peek()
    if (next == null) {
        pendingResults = _uiState.value.sessionResults
        pendingDeckTitle = _uiState.value.deckTitle
        _uiState.update { it.copy(isComplete = true) }
        return
    }
    _uiState.update { state ->
        state.copy(
            currentCard = next,
            currentIndex = q.masteredCount,
            progress = q.progress,
            isFlipped = false,
            isAnswerRevealed = false,
            selectedAnswer = null,
            isCorrect = null,
            writeAnswer = "",
            isWriteCorrect = null,
            quizOptions = if (mode == StudyMode.QUIZ) emptyList() else state.quizOptions,
        )
    }
    if (mode == StudyMode.QUIZ) generateQuizOptions(next)
    // Auto-play TTS for dictation mode
    if (mode == StudyMode.DICTATION) {
        val text = if (_uiState.value.showFrontFirst) next.frontText else next.backText
        ttsManager.setSpeed(_uiState.value.dictationSpeed)
        ttsManager.speak(text)
        _uiState.update { it.copy(dictationPlayCount = 1) }
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/study/StudySessionViewModel.kt
git commit -m "feat: add dictation actions and auto-play to StudySessionViewModel"
```

---

### Task 5: Add DictationContent composable and wire into StudySessionScreen

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/study/StudySessionScreen.kt:`

This is the largest task. We need to:
1. Add `DictationContent` composable
2. Wire it into the `when(viewModel.mode)` block
3. Add keyboard shortcuts for dictation mode
4. Add shortcut hints for dictation mode
5. Handle Tab/Escape focus toggle for dictation mode

- [ ] **Step 1: Add DictationContent composable**

Add after `WriteContent` (after line 814). This composable reuses `FrontBackToggle`, a listening indicator (replacing the question card), `TtsSpeakButton`, replay button, speed slider, input field, feedback, and submit/advance button:

```kotlin
// ===== DICTATION CONTENT =====

@Composable
private fun DictationContent(
    uiState: StudySessionUiState,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onAdvance: () -> Unit,
    onReplay: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    showFrontFirst: Boolean,
    onToggleFrontFirst: () -> Unit,
    ttsManager: TtsManager,
    parentFocusRequester: FocusRequester,
    inputFocusRequester: FocusRequester,
    isInputFocused: Boolean,
    onInputFocusChanged: (Boolean) -> Unit,
) {
    val card = uiState.currentCard ?: return
    val focusManager = LocalFocusManager.current
    val ttsStatus by ttsManager.status.collectAsStateWithLifecycle()
    val isSpeaking = ttsStatus == TtsManager.TtsStatus.SPEAKING

    val correctAnswerText = if (showFrontFirst) card.backText else card.frontText
    val listeningLabel = if (showFrontFirst) "Nghe từ và gõ lại" else "Nghe nghĩa và gõ lại"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Front/Back toggle
        FrontBackToggle(
            showFrontFirst = showFrontFirst,
            onToggle = onToggleFrontFirst,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        // Listening indicator (replaces question card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSpeaking)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Animated sound waves when speaking
                val infiniteTransition = rememberInfiniteTransition(label = "dictation_wave")
                val waveScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "wave_scale",
                )

                Icon(
                    imageVector = if (isSpeaking) Icons.Default.VolumeUp else Icons.Default.Headphones,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer {
                            if (isSpeaking) {
                                scaleX = waveScale
                                scaleY = waveScale
                            }
                        },
                    tint = if (isSpeaking) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isSpeaking) "Đang đọc..." else "Nghe và gõ lại",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSpeaking) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = listeningLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (uiState.dictationPlayCount > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Đã nghe ${uiState.dictationPlayCount} lần",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Replay button + Speed slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Replay button
            FilledTonalButton(
                onClick = onReplay,
                enabled = ttsStatus != TtsManager.TtsStatus.ERROR,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Default.Replay, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nghe lại")
            }

            Spacer(Modifier.width(16.dp))

            // Speed slider
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tốc độ: ${String.format("%.1f", uiState.dictationSpeed)}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = uiState.dictationSpeed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2.0f,
                    steps = 5, // 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Answer input + focus toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            OutlinedTextField(
                value = uiState.writeAnswer,
                onValueChange = { if (!uiState.isAnswerRevealed) onAnswerChange(it) },
                label = { Text("Nhập những gì bạn nghe được") },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(inputFocusRequester)
                    .onFocusChanged { onInputFocusChanged(it.isFocused) },
                enabled = !uiState.isAnswerRevealed,
                isError = uiState.isWriteCorrect == false,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(100)
                        parentFocusRequester.requestFocus()
                    }
                }),
                minLines = 3,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                ),
            )

            Spacer(Modifier.width(8.dp))

            // Toggle focus: input ↔ shortcuts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (isInputFocused) {
                            focusManager.clearFocus()
                            kotlinx.coroutines.MainScope().launch {
                                kotlinx.coroutines.delay(100)
                                parentFocusRequester.requestFocus()
                            }
                        } else {
                            inputFocusRequester.requestFocus()
                        }
                    },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = if (isInputFocused)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = if (isInputFocused) Icons.Default.Keyboard
                                      else Icons.Default.Edit,
                        contentDescription = if (isInputFocused) "Chế độ phím tắt (Esc)"
                                             else "Chế độ gõ chữ",
                    )
                }
                Text(
                    text = if (isInputFocused) "Phím tắt" else "Gõ chữ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                )
            }
        }

        // Feedback
        AnimatedVisibility(visible = uiState.isAnswerRevealed) {
            Column {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isWriteCorrect == true)
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color(0xFFF44336).copy(alpha = 0.1f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (uiState.isWriteCorrect == true) Icons.Default.CheckCircle
                                else Icons.Default.Cancel,
                                null,
                                tint = if (uiState.isWriteCorrect == true) Color(0xFF4CAF50)
                                else Color(0xFFF44336),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.isWriteCorrect == true) "Chính xác!" else "Chưa đúng",
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (uiState.isWriteCorrect == false) {
                            Spacer(Modifier.height(8.dp))
                            Text("Đáp án đúng: ", style = MaterialTheme.typography.labelMedium)
                            Text(correctAnswerText, fontWeight = FontWeight.Bold)
                        } else {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Bạn đã nghe ${uiState.dictationPlayCount} lần",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (!uiState.isAnswerRevealed) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSubmit()
                    kotlinx.coroutines.MainScope().launch {
                        kotlinx.coroutines.delay(100)
                        parentFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = uiState.writeAnswer.isNotBlank(),
            ) {
                Text("Kiểm tra", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onAdvance,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Tiếp theo →", fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

- [ ] **Step 2: Wire DictationContent into the when(viewModel.mode) block**

In `StudySessionScreen`, after the `StudyMode.MATCH -> MatchContent(...)` block (after line 212), add:

```kotlin
StudyMode.DICTATION -> DictationContent(
    uiState = uiState,
    onAnswerChange = viewModel::onWriteAnswerChange,
    onSubmit = viewModel::onSubmitDictation,
    onAdvance = viewModel::onDictationAdvance,
    onReplay = viewModel::onReplayDictation,
    onSpeedChange = viewModel::onDictationSpeedChange,
    showFrontFirst = uiState.showFrontFirst,
    onToggleFrontFirst = viewModel::onToggleFrontFirst,
    ttsManager = viewModel.ttsManager,
    parentFocusRequester = focusRequester,
    inputFocusRequester = writeInputFocusRequester,
    isInputFocused = isWriteInputFocused,
    onInputFocusChanged = { isWriteInputFocused = it },
)
```

- [ ] **Step 3: Add Tab/Escape handling for DICTATION mode**

In the `onPreviewKeyEvent` handler, update the Tab and Escape conditions to include DICTATION mode. Change line 134 from `viewModel.mode == StudyMode.WRITE` to `(viewModel.mode == StudyMode.WRITE || viewModel.mode == StudyMode.DICTATION)`:

```kotlin
// Tab toggles between input focus and shortcut mode (Write/Dictation mode)
if (event.key == Key.Tab && (viewModel.mode == StudyMode.WRITE || viewModel.mode == StudyMode.DICTATION) && !uiState.isAnswerRevealed) {
```

And line 148 from `viewModel.mode == StudyMode.WRITE` to include dictation:

```kotlin
// Escape in Write/Dictation mode when input focused → unfocus input first
if (event.key == Key.Escape && (viewModel.mode == StudyMode.WRITE || viewModel.mode == StudyMode.DICTATION) && isWriteInputFocused) {
```

- [ ] **Step 4: Add keyboard shortcuts for DICTATION mode**

In `handleKeyEvent()`, update the `P` key shortcut (line 1192-1195) to also work in DICTATION mode:

```kotlin
Key.P -> {
    if (isSwipeLearn || mode == StudyMode.WRITE || mode == StudyMode.DICTATION) { viewModel.onSpeakWord(); true }
    else false
}
```

Update the `T` key shortcut (line 1196-1199) to also work in DICTATION mode:

```kotlin
Key.T -> {
    if (isSwipeLearn || mode == StudyMode.WRITE || mode == StudyMode.DICTATION) { viewModel.onToggleFrontFirst(); true }
    else false
}
```

Add Enter key handling for DICTATION (after the WRITE Enter handler at line 1262-1268):

```kotlin
// === DICTATION: Enter to submit/advance ===
Key.Enter -> {
    if (mode == StudyMode.WRITE) {
        if (uiState.isAnswerRevealed) viewModel.onWriteAdvance()
        else if (uiState.writeAnswer.isNotBlank()) viewModel.onSubmitWriteAnswer()
        true
    } else if (mode == StudyMode.DICTATION) {
        if (uiState.isAnswerRevealed) viewModel.onDictationAdvance()
        else if (uiState.writeAnswer.isNotBlank()) viewModel.onSubmitDictation()
        true
    } else false
}
```

Also add `R` key for replay in dictation mode (add after Enter handler):

```kotlin
Key.R -> {
    if (mode == StudyMode.DICTATION && !uiState.isAnswerRevealed) { viewModel.onReplayDictation(); true }
    else false
}
```

- [ ] **Step 5: Add DICTATION hints to KeyboardShortcutsBar**

In `KeyboardShortcutsBar`, add a DICTATION case to the `when(mode)` block (after the WRITE case at line 1303-1311):

```kotlin
StudyMode.DICTATION -> {
    add("Tab" to if (isWriteInputFocused) "⌨ Phím tắt" else "✏ Gõ chữ")
    if (isWriteInputFocused) {
        add("Esc" to "Thoát gõ")
    } else {
        if (isAnswerRevealed) add("Enter" to "Tiếp")
        else {
            add("Enter" to "Kiểm tra")
            add("R" to "Nghe lại")
        }
    }
}
```

- [ ] **Step 6: Add modeLabel for DICTATION**

In the `modeLabel()` function (line 309-315), add:

```kotlin
StudyMode.DICTATION -> "Chính tả"
```

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/study/StudySessionScreen.kt
git commit -m "feat: add DictationContent composable and wire into study session"
```

---

### Task 6: Add dictation option to StudyModePickerScreen

**Files:**
- Modify: `app/src/main/java/com/dttrn/datfs/feature/study/StudyModePickerScreen.kt:88-130`

- [ ] **Step 1: Add DICTATION to studyModes list**

Add after the MATCH entry (after line 129) in the `studyModes` list:

```kotlin
StudyModeInfo(
    mode = StudyMode.DICTATION,
    title = "Nghe chính tả",
    subtitle = "Luyện nghe & viết",
    description = "TTS đọc từ hoặc cụm từ, bạn gõ lại những gì nghe được. Luyện kỹ năng nghe và chính tả.",
    icon = Icons.Default.Headphones,
    gradientColors = listOf(Color(0xFF009688), Color(0xFF00BCD4)),
),
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/dttrn/datfs/feature/study/StudyModePickerScreen.kt
git commit -m "feat: add dictation option to StudyModePickerScreen"
```

---

### Task 7: Add unit tests for dictation matching and ViewModel

**Files:**
- Create: `app/src/test/java/com/dttrn/datfs/feature/study/DictationTest.kt`

- [ ] **Step 1: Create the test file and write dictation matching tests**

```kotlin
package com.dttrn.datfs.feature.study

import org.junit.Assert.*
import org.junit.Test

class DictationTest {

    // ----- isDictationMatch tests -----

    @Test
    fun `isDictationMatch - exact match`() {
        assertTrue(isDictationMatch("hello", "hello"))
    }

    @Test
    fun `isDictationMatch - different case`() {
        assertTrue(isDictationMatch("Hello", "hello"))
    }

    @Test
    fun `isDictationMatch - extra whitespace`() {
        assertTrue(isDictationMatch("  hello world  ", "hello world"))
    }

    @Test
    fun `isDictationMatch - punctuation difference`() {
        assertTrue(isDictationMatch("hello, world!", "hello world"))
    }

    @Test
    fun `isDictationMatch - both punctuation and case difference`() {
        assertTrue(isDictationMatch("Hello, World!", "hello world"))
    }

    @Test
    fun `isDictationMatch - wrong answer`() {
        assertFalse(isDictationMatch("goodbye", "hello"))
    }

    @Test
    fun `isDictationMatch - partially correct`() {
        assertFalse(isDictationMatch("hello", "hello world"))
    }

    @Test
    fun `isDictationMatch - empty answer`() {
        assertFalse(isDictationMatch("", "hello"))
    }

    @Test
    fun `isDictationMatch - both empty`() {
        assertTrue(isDictationMatch("", ""))
    }

    @Test
    fun `isDictationMatch - numbers and symbols stripped`() {
        assertTrue(isDictationMatch("it's a test.", "its a test"))
    }
}
```

Note: `isDictationMatch` is a `private` function in `StudySessionViewModel`. To test it, make it `internal` instead, or extract it to a testable location. Update the function visibility:

In `StudySessionViewModel.kt`, change:
```kotlin
private fun isDictationMatch(userAnswer: String, correctAnswer: String): Boolean {
```
to:
```kotlin
internal fun isDictationMatch(userAnswer: String, correctAnswer: String): Boolean {
```

- [ ] **Step 2: Run tests**

```bash
cd /Users/tranthudat/Documents/Learn/ANDROID/dat_flashcard && ./gradlew testDebugUnitTest --tests "com.dttrn.datfs.feature.study.DictationTest" 2>&1 | tail -30
```

Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/dttrn/datfs/feature/study/DictationTest.kt \
        app/src/main/java/com/dttrn/datfs/feature/study/StudySessionViewModel.kt
git commit -m "test: add dictation matching unit tests"
```

---

### Task 8: Build and verify

**Files:** None (verification only)

- [ ] **Step 1: Build the project**

```bash
cd /Users/tranthudat/Documents/Learn/ANDROID/dat_flashcard && ./gradlew assembleDebug 2>&1 | tail -30
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify all modified files compile**

If build fails, check the error output and fix any issues. Common issues:
- Missing import for `Icons.Default.Headphones` in StudyModePickerScreen
- Missing import for `Icons.Default.Replay` in StudySessionScreen
- `when` expression not exhaustive — ensure all `StudyMode` branches are handled

- [ ] **Step 3: Final commit (if any fixes needed)**

```bash
git add -A
git commit -m "fix: build issues for dictation feature"
```
