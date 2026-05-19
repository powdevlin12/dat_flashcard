package com.dttrn.datfs.feature.study

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dttrn.datfs.core.domain.model.Flashcard
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipeable flashcard với flip animation và swipe gesture.
 * Swipe right = GOOD (nhớ), Swipe left = AGAIN (quên).
 */
@Composable
fun SwipeableCard(
    card: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Dùng Animatable để control offset an toàn hơn
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Reset offset khi card thay đổi
    LaunchedEffect(card.id) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
    }

    val rotation = (offsetX.value / 25f).coerceIn(-15f, 15f)
    val swipeProgress = (offsetX.value / 300f).coerceIn(-1f, 1f)
    val absProgress = abs(swipeProgress)

    val tintColor = when {
        swipeProgress > 0.05f -> Color(0xFF4CAF50).copy(alpha = absProgress * 0.25f)
        swipeProgress < -0.05f -> Color(0xFFF44336).copy(alpha = absProgress * 0.25f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate(rotation)
            .shadow(16.dp + (absProgress * 8).dp, RoundedCornerShape(32.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(card.id) {
                detectDragGestures(
                    onDragEnd = {
                        val currentX = offsetX.value
                        when {
                            currentX > 150f -> onSwipeRight()
                            currentX < -150f -> onSwipeLeft()
                        }
                        // Reset sau khi quyết định
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                        scope.launch {
                            offsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                            )
                        }
                    },
                    onDragCancel = {
                        scope.launch { offsetX.animateTo(0f) }
                        scope.launch { offsetY.animateTo(0f) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x * 0.85f)
                        }
                        scope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.25f)
                        }
                    }
                )
            }
    ) {
        // Màu overlay khi vuốt
        if (absProgress > 0.05f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(tintColor)
            )
        }

        // Swipe hint badges
        if (swipeProgress > 0.12f) {
            SwipeHintBadge(
                text = "NHỚ ✓",
                color = Color(0xFF4CAF50),
                alpha = (swipeProgress * 2.5f).coerceIn(0f, 1f),
                modifier = Modifier.align(Alignment.TopStart).padding(20.dp),
            )
        } else if (swipeProgress < -0.12f) {
            SwipeHintBadge(
                text = "QUÊN ✗",
                color = Color(0xFFF44336),
                alpha = (absProgress * 2.5f).coerceIn(0f, 1f),
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp),
            )
        }

        // Card flip content
        FlipCard(
            card = card,
            isFlipped = isFlipped,
            onFlip = onFlip,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun FlipCard(
    card: Flashcard,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(targetState = isFlipped, label = "flip_transition")
    val rotationY by transition.animateFloat(
        transitionSpec = { tween(400, easing = FastOutSlowInEasing) },
        label = "rotation_y"
    ) { flipped -> if (flipped) 180f else 0f }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (rotationY <= 90f) {
            // Front face
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = rotationY },
                color = Color.Transparent,
                onClick = onFlip,
            ) {
                CardFaceContent(
                    label = "MẶT TRƯỚC",
                    labelColor = MaterialTheme.colorScheme.primary,
                    text = card.frontText,
                    subText = card.pronunciation,
                    hint = "Nhấn để xem mặt sau",
                )
            }
        } else {
            // Back face — corrected mirroring
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { this.rotationY = rotationY - 180f },
                color = Color.Transparent,
                onClick = onFlip,
            ) {
                CardFaceContent(
                    label = "MẶT SAU",
                    labelColor = MaterialTheme.colorScheme.secondary,
                    text = card.backText,
                    subText = card.exampleSentence,
                    hint = null,
                )
            }
        }
    }
}

@Composable
private fun CardFaceContent(
    label: String,
    labelColor: Color,
    text: String,
    subText: String?,
    hint: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Label badge
        Box(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(labelColor.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )

        subText?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        hint?.let {
            Spacer(Modifier.height(24.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SwipeHintBadge(
    text: String,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .alpha(alpha)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
        )
    }
}
