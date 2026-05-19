package com.dttrn.datfs.ui.theme

import androidx.compose.ui.graphics.Color

// ===== FlashMind Premium Palette =====
// Primary — Vibrant Blue
val PrimaryLight = Color(0xFF004AC6)
val OnPrimaryLight = Color.White
val PrimaryContainerLight = Color(0xFF2563EB)
val OnPrimaryContainerLight = Color(0xFFEEEFFF)

val PrimaryDark = Color(0xFFB4C5FF)
val OnPrimaryDark = Color(0xFF00174B)
val PrimaryContainerDark = Color(0xFF003EA8)
val OnPrimaryContainerDark = Color(0xFFDBE1FF)

// Secondary — Soft Violet / Mesh Gradient
val SecondaryLight = Color(0xFF712AE2)
val OnSecondaryLight = Color.White
val SecondaryContainerLight = Color(0xFF8A4CFC)
val OnSecondaryContainerLight = Color(0xFFFFFBFF)

val SecondaryDark = Color(0xFFD2BBFF)
val OnSecondaryDark = Color(0xFF25005A)
val SecondaryContainerDark = Color(0xFF5A00C6)
val OnSecondaryContainerDark = Color(0xFFEADDFF)

// Tertiary — Teal
val TertiaryLight = Color(0xFF006058)
val OnTertiaryLight = Color.White
val TertiaryContainerLight = Color(0xFF007B71)
val OnTertiaryContainerLight = Color(0xFFB3FFF3)

val TertiaryDark = Color(0xFF6BD8CB)
val OnTertiaryDark = Color(0xFF00201D)
val TertiaryContainerDark = Color(0xFF005049)
val OnTertiaryContainerDark = Color(0xFF89F5E7)

// Error / Danger
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color.White
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF93000A)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// Neutral surfaces
val BackgroundLight = Color(0xFFF7F9FB)
val OnBackgroundLight = Color(0xFF191C1E)
val SurfaceLight = Color(0xFFF7F9FB)
val OnSurfaceLight = Color(0xFF191C1E)
val SurfaceVariantLight = Color(0xFFE0E3E5)
val OnSurfaceVariantLight = Color(0xFF434655)
val OutlineLight = Color(0xFF737686)

val BackgroundDark = Color(0xFF191C1E)
val OnBackgroundDark = Color(0xFFE0E3E5)
val SurfaceDark = Color(0xFF191C1E)
val OnSurfaceDark = Color(0xFFE0E3E5)
val SurfaceVariantDark = Color(0xFF434655)
val OnSurfaceVariantDark = Color(0xFFC3C6D7)
val OutlineDark = Color(0xFF8D90A1)

// Deck preset colors (8 options users can pick)
val DeckColorBlue    = Color(0xFF2563EB)
val DeckColorPurple  = Color(0xFF7C3AED)
val DeckColorTeal    = Color(0xFF0D9488)
val DeckColorOrange  = Color(0xFFF97316)
val DeckColorRed     = Color(0xFFEF4444)
val DeckColorPink    = Color(0xFFEC4899)
val DeckColorIndigo  = Color(0xFF4F46E5)
val DeckColorGreen   = Color(0xFF10B981)

val deckPresetColors = listOf(
    DeckColorBlue, DeckColorPurple, DeckColorTeal, DeckColorOrange,
    DeckColorRed, DeckColorPink, DeckColorIndigo, DeckColorGreen
)

val deckPresetColorHexes = listOf(
    "#2563EB", "#7C3AED", "#0D9488", "#F97316",
    "#EF4444", "#EC4899", "#4F46E5", "#10B981"
)

/** Convenience object cho DeckCard và ColorPicker */
object DeckColors {
    val presets = deckPresetColors
    val presetHexes = deckPresetColorHexes
    val default = DeckColorBlue
    val defaultHex = "#2563EB"
}
