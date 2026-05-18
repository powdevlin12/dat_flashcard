package com.dttrn.datfs.ui.theme

import androidx.compose.ui.graphics.Color

// ===== FlashMind Primary Palette =====
// Primary — Blue (#4A90E2)
val Blue10 = Color(0xFF001C38)
val Blue20 = Color(0xFF003870)
val Blue30 = Color(0xFF0054A8)
val Blue40 = Color(0xFF4A90E2)  // Brand primary
val Blue80 = Color(0xFFAAC7FF)
val Blue90 = Color(0xFFD6E4FF)

// Secondary — Purple (#7B61FF)
val Purple10 = Color(0xFF1B0060)
val Purple20 = Color(0xFF3700B3)
val Purple30 = Color(0xFF5C00E8)
val Purple40 = Color(0xFF7B61FF)  // Brand secondary
val Purple80 = Color(0xFFCCBEFF)
val Purple90 = Color(0xFFE8DEFF)

// Tertiary — Green (#00C853) success
val Green10 = Color(0xFF002200)
val Green20 = Color(0xFF004400)
val Green30 = Color(0xFF006800)
val Green40 = Color(0xFF00A844)
val Green80 = Color(0xFF7AE582)
val Green90 = Color(0xFFB5F2B9)

// Error / Danger
val Red40 = Color(0xFFF44336)
val Red80 = Color(0xFFFFB4AB)
val Red90 = Color(0xFFFFDAD6)

// Neutral surfaces
val NeutralVariant30 = Color(0xFF4A4458)
val NeutralVariant50 = Color(0xFF79747E)
val NeutralVariant80 = Color(0xFFCAC4D0)
val NeutralVariant90 = Color(0xFFECE6F0)

// Deck preset colors (8 options users can pick)
val DeckColorBlue    = Color(0xFF4A90E2)
val DeckColorPurple  = Color(0xFF7B61FF)
val DeckColorGreen   = Color(0xFF00C853)
val DeckColorOrange  = Color(0xFFFF6D00)
val DeckColorRed     = Color(0xFFF44336)
val DeckColorTeal    = Color(0xFF00BCD4)
val DeckColorPink    = Color(0xFFE91E63)
val DeckColorIndigo  = Color(0xFF3F51B5)

val deckPresetColors = listOf(
    DeckColorBlue, DeckColorPurple, DeckColorGreen, DeckColorOrange,
    DeckColorRed, DeckColorTeal, DeckColorPink, DeckColorIndigo
)

val deckPresetColorHexes = listOf(
    "#4A90E2", "#7B61FF", "#00C853", "#FF6D00",
    "#F44336", "#00BCD4", "#E91E63", "#3F51B5"
)

/** Convenience object cho DeckCard và ColorPicker */
object DeckColors {
    val presets = deckPresetColors
    val presetHexes = deckPresetColorHexes
    val default = DeckColorBlue
    val defaultHex = "#4A90E2"
}
