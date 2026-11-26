package com.pesa.studygrid.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Aqua = Color(0xFF2BD9FE)
val Lime = Color(0xFF7AE582)
val Sky = Color(0xFF4FA3FF)
val Violet = Color(0xFFB48CFF)
val Coral = Color(0xFFFF8A65)

/** Goal color choices, stored as ARGB ints. */
val GoalPalette = listOf(Aqua, Lime, Sky, Violet, Coral)

/** Aero-specific tokens Material's ColorScheme has no slot for. */
@Immutable
data class AeroColors(
    val dark: Boolean,
    /** Laid over the background photo: darkens it to "night" in dark mode, softens it in light mode. */
    val scrim: Color,
    val glassTint: Color,
    val graphPanel: Color,
    val emptyCell: Color,
    val emptyCellBorder: Color,
    val dialogBg: Color,
    val muted: Color,
    /** Accent (light, deep) for gel buttons and glass-button glow. */
    val accent: Color,
    val accentDeep: Color,
    /** Icon color on glass buttons. */
    val ink: Color,
)

private val DarkAero = AeroColors(
    dark = true,
    scrim = Color(0xFF041322).copy(alpha = 0.62f),
    glassTint = Color.White.copy(alpha = 0.08f),
    graphPanel = Color(0xFF0A1A2B).copy(alpha = 0.62f),
    emptyCell = Color.White.copy(alpha = 0.07f),
    emptyCellBorder = Color.White.copy(alpha = 0.12f),
    dialogBg = Color(0xFF0E2236).copy(alpha = 0.97f),
    muted = Color(0xFF9CC0DD),
    accent = Color(0xFF6FB6FF),
    accentDeep = Color(0xFF1F4FA8),
    ink = Color.White,
)

// Day palette sampled from the user's "Green and Blue" moodboard (turquoise water, jade, teal, marble blue).
private val Turquoise = Color(0xFF03AAB1)
private val Jade = Color(0xFF4FAA92)
private val DeepTeal = Color(0xFF026B72)
private val AbyssTeal = Color(0xFF0E504F)

private val LightAero = AeroColors(
    dark = false,
    scrim = Turquoise.copy(alpha = 0.16f),
    glassTint = Color(0xFFE6FAF7).copy(alpha = 0.32f),
    graphPanel = Color(0xFFE9FBF7).copy(alpha = 0.62f),
    emptyCell = AbyssTeal.copy(alpha = 0.09f),
    emptyCellBorder = AbyssTeal.copy(alpha = 0.17f),
    dialogBg = Color(0xFFF1FCFA).copy(alpha = 0.97f),
    muted = Color(0xFF1A5C5A),
    accent = Jade,
    accentDeep = DeepTeal,
    ink = DeepTeal,
)

val LocalAero = staticCompositionLocalOf { DarkAero }

@Composable
fun StudyGridTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = Aqua, onPrimary = Color(0xFF06121F),
            background = Color(0xFF06121F), surface = Color(0xFF0E2236),
            surfaceContainer = Color(0xFF10263C), onSurface = Color(0xFFE6F1FF),
            onSurfaceVariant = Color(0xFF8FB3D1), outline = Color.White.copy(alpha = 0.3f),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF038C94), onPrimary = Color.White,
            background = Color(0xFFE9FBF7), surface = Color.White,
            surfaceContainer = Color(0xFFF1FCFA), onSurface = Color(0xFF0B3B3A),
            onSurfaceVariant = Color(0xFF1A5C5A), outline = AbyssTeal.copy(alpha = 0.3f),
        )
    }
    MaterialTheme(colorScheme = scheme) {
        // Transparent surfaces (Scaffold, dialogs) don't set a content color; default it here.
        CompositionLocalProvider(
            LocalAero provides if (dark) DarkAero else LightAero,
            LocalContentColor provides scheme.onSurface,
            content = content,
        )
    }
}
