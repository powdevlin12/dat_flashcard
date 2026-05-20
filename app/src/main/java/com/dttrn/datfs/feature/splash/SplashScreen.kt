package com.dttrn.datfs.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Splash Screen — hiển thị logo animated + tagline,
 * đợi 1.5s rồi navigate theo onboardingDone flag.
 */
@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    isOnboardingDone: Boolean,
) {
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "scale",
    )

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "alpha",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Navigation after delay
    LaunchedEffect(Unit) {
        delay(1800)
        if (isOnboardingDone) {
            onNavigateToHome()
        } else {
            onNavigateToOnboarding()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale)
                .alpha(alpha),
        ) {
            // Logo
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = Modifier.size(84.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = "FlashMind Logo",
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "FlashMind",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Học thông minh, nhớ lâu hơn",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
    }
}
