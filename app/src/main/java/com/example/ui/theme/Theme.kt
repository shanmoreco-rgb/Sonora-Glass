package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.SonoraThemeMode

@Immutable
data class SonoraDesignTokens(
    val themeMode: SonoraThemeMode,
    val isDark: Boolean,
    val backdropColors: List<Color>,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val glassFill: Color,
    val glassElevatedFill: Color,
    val glassBorderColors: List<Color>,
    val glassHighlightSheen: Color,
    val navBarFill: Color,
    val accentGlow: Color,
    val secondaryGlow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val offlineBadgeColor: Color,
    val cloudBadgeColor: Color
) {
    val glassBorderBrush: Brush
        get() = Brush.linearGradient(glassBorderColors)
}

private val LiquidGlassTokens = SonoraDesignTokens(
    themeMode = SonoraThemeMode.LIQUID_GLASS,
    isDark = true,
    backdropColors = listOf(LiquidBgTop, LiquidBgMid, LiquidBgBottom),
    orbPrimary = LiquidOrbCyan,
    orbSecondary = LiquidOrbViolet,
    orbTertiary = LiquidOrbTeal,
    glassFill = LiquidGlassFill,
    glassElevatedFill = LiquidGlassElevated,
    glassBorderColors = listOf(LiquidBorderStart, LiquidBorderEnd, Color(0x4D9D8CFF)),
    glassHighlightSheen = Color(0x2EFFFFFF),
    navBarFill = Color(0x9908172E),
    accentGlow = LiquidPrimary,
    secondaryGlow = LiquidSecondary,
    textPrimary = LiquidTextPrimary,
    textSecondary = LiquidTextSecondary,
    offlineBadgeColor = Color(0xFFFFB74D),
    cloudBadgeColor = Color(0xFF5CE1E6)
)

private val ObsidianAmoledTokens = SonoraDesignTokens(
    themeMode = SonoraThemeMode.OBSIDIAN_AMOLED,
    isDark = true,
    backdropColors = listOf(AmoledBg, Color(0xFF05070B), AmoledBg),
    orbPrimary = AmoledOrbMint.copy(alpha = 0.25f),
    orbSecondary = AmoledOrbCyan.copy(alpha = 0.22f),
    orbTertiary = Color(0xFF283593).copy(alpha = 0.20f),
    glassFill = AmoledSurface,
    glassElevatedFill = AmoledElevated,
    glassBorderColors = listOf(Color(0x5500F5A0), Color(0x2200D9F5), Color(0x33FFFFFF)),
    glassHighlightSheen = Color(0x14FFFFFF),
    navBarFill = Color(0xEB07080C),
    accentGlow = AmoledPrimary,
    secondaryGlow = AmoledSecondary,
    textPrimary = AmoledTextPrimary,
    textSecondary = AmoledTextSecondary,
    offlineBadgeColor = Color(0xFFFFB74D),
    cloudBadgeColor = AmoledPrimary
)

private val AuroraNordTokens = SonoraDesignTokens(
    themeMode = SonoraThemeMode.AURORA_NORD,
    isDark = false,
    backdropColors = listOf(NordBgTop, NordBgMid, NordBgBottom),
    orbPrimary = NordOrbBlue,
    orbSecondary = NordOrbLavender,
    orbTertiary = Color(0xFF81E6D9),
    glassFill = NordGlassFill,
    glassElevatedFill = NordGlassElevated,
    glassBorderColors = listOf(NordBorderStart, NordBorderEnd, Color(0x88FFFFFF)),
    glassHighlightSheen = Color(0x66FFFFFF),
    navBarFill = Color(0xD9F0F6FC),
    accentGlow = NordPrimary,
    secondaryGlow = NordSecondary,
    textPrimary = NordTextPrimary,
    textSecondary = NordTextSecondary,
    offlineBadgeColor = Color(0xFFD97706),
    cloudBadgeColor = NordPrimary
)

private val WarmVinylTokens = SonoraDesignTokens(
    themeMode = SonoraThemeMode.WARM_VINYL,
    isDark = true,
    backdropColors = listOf(VinylBgTop, VinylBgMid, VinylBgBottom),
    orbPrimary = VinylOrbAmber,
    orbSecondary = VinylOrbRose,
    orbTertiary = Color(0xFF9C5221),
    glassFill = VinylGlassFill,
    glassElevatedFill = VinylGlassElevated,
    glassBorderColors = listOf(VinylBorderStart, VinylBorderEnd, Color(0x40E06D53)),
    glassHighlightSheen = Color(0x26FFE5C8),
    navBarFill = Color(0xCC160E0A),
    accentGlow = VinylPrimary,
    secondaryGlow = VinylSecondary,
    textPrimary = VinylTextPrimary,
    textSecondary = VinylTextSecondary,
    offlineBadgeColor = VinylPrimary,
    cloudBadgeColor = Color(0xFF98E2C6)
)

val LocalSonoraDesign = staticCompositionLocalOf { LiquidGlassTokens }

private val SonoraShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

fun tokensForTheme(mode: SonoraThemeMode): SonoraDesignTokens = when (mode) {
    SonoraThemeMode.LIQUID_GLASS -> LiquidGlassTokens
    SonoraThemeMode.OBSIDIAN_AMOLED -> ObsidianAmoledTokens
    SonoraThemeMode.AURORA_NORD -> AuroraNordTokens
    SonoraThemeMode.WARM_VINYL -> WarmVinylTokens
}

private fun colorSchemeForTokens(tokens: SonoraDesignTokens): ColorScheme {
    return if (tokens.isDark) {
        darkColorScheme(
            primary = tokens.accentGlow,
            onPrimary = Color(0xFF04101C),
            primaryContainer = tokens.accentGlow.copy(alpha = 0.22f),
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondaryGlow,
            onSecondary = Color(0xFF090B1A),
            secondaryContainer = tokens.secondaryGlow.copy(alpha = 0.22f),
            onSecondaryContainer = tokens.textPrimary,
            background = tokens.backdropColors.first(),
            onBackground = tokens.textPrimary,
            surface = tokens.glassFill,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.glassElevatedFill,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.glassBorderColors.first().copy(alpha = 0.45f),
            error = Color(0xFFFF6B81),
            onError = Color(0xFF1F0307)
        )
    } else {
        lightColorScheme(
            primary = tokens.accentGlow,
            onPrimary = Color.White,
            primaryContainer = tokens.accentGlow.copy(alpha = 0.14f),
            onPrimaryContainer = tokens.textPrimary,
            secondary = tokens.secondaryGlow,
            onSecondary = Color.White,
            secondaryContainer = tokens.secondaryGlow.copy(alpha = 0.14f),
            onSecondaryContainer = tokens.textPrimary,
            background = tokens.backdropColors.first(),
            onBackground = tokens.textPrimary,
            surface = tokens.glassFill,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.glassElevatedFill,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.glassBorderColors[1],
            error = Color(0xFFBA1A1A),
            onError = Color.White
        )
    }
}

@Composable
fun SonoraTheme(
    themeMode: SonoraThemeMode = SonoraThemeMode.LIQUID_GLASS,
    content: @Composable () -> Unit
) {
    val tokens = tokensForTheme(themeMode)
    val colorScheme = colorSchemeForTokens(tokens)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !tokens.isDark
                isAppearanceLightNavigationBars = !tokens.isDark
            }
        }
    }

    CompositionLocalProvider(LocalSonoraDesign provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = SonoraShapes,
            content = content
        )
    }
}
