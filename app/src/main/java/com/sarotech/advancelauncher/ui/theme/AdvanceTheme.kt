package com.sarotech.advancelauncher.ui.theme

import androidx.compose.ui.graphics.Color
import com.sarotech.advancelauncher.data.LauncherConfig

enum class SurfaceStyle {
    GLASS,
    NEOMORPH
}

data class AdvancePalette(
    val style: SurfaceStyle,
    val isLight: Boolean,
    val backdrop: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val glassHighlight: Color,
    val glassBorderStart: Color,
    val glassBorderEnd: Color,
    val neoLightShadow: Color,
    val neoDarkShadow: Color,
    val neoBase: Color,
    val neoRecessed: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val border: Color,
    val glassOpacity: Float,
    val neoDepth: Float
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF4FC3F7)): Color = runCatching {
    val clean = hex.removePrefix("#")
    Color(android.graphics.Color.parseColor("#$clean"))
}.getOrDefault(fallback)

fun resolveAdvancePalette(config: LauncherConfig): AdvancePalette {
    val accent = parseHexColor(config.accentHex)
    val opacity = config.glassOpacity.coerceIn(0.18f, 0.92f)
    val depth = config.neoDepth.coerceIn(0.4f, 1.8f)

    return when (config.themePreset) {
        "GlassLight" -> AdvancePalette(
            style = SurfaceStyle.GLASS,
            isLight = true,
            backdrop = Color(0xFFEAF0FA).copy(alpha = (opacity + 0.30f).coerceAtMost(0.92f)),
            surface = Color(0xFFFFFFFF).copy(alpha = opacity),
            surfaceElevated = Color(0xFFF6F9FF).copy(alpha = (opacity + 0.18f).coerceAtMost(0.94f)),
            glassHighlight = Color(0x88FFFFFF),
            glassBorderStart = Color(0xCCFFFFFF),
            glassBorderEnd = accent.copy(alpha = 0.38f),
            neoLightShadow = Color.Transparent,
            neoDarkShadow = Color.Transparent,
            neoBase = Color(0xFFF4F7FD),
            neoRecessed = Color(0x22000000),
            textPrimary = Color(0xFF0E1420),
            textSecondary = Color(0xFF475569),
            accent = accent,
            border = Color(0x66FFFFFF),
            glassOpacity = opacity,
            neoDepth = depth
        )
        "NeomorphDark" -> AdvancePalette(
            style = SurfaceStyle.NEOMORPH,
            isLight = false,
            backdrop = Color(0xFF1F242D),
            surface = Color(0xFF1F242D),
            surfaceElevated = Color(0xFF252B36),
            glassHighlight = Color.Transparent,
            glassBorderStart = Color(0x28FFFFFF),
            glassBorderEnd = Color(0x38000000),
            neoLightShadow = Color(0xFF2E3645),
            neoDarkShadow = Color(0xFF12151B),
            neoBase = Color(0xFF1F242D),
            neoRecessed = Color(0xFF181C24),
            textPrimary = Color(0xFFF1F5FB),
            textSecondary = Color(0xFF9AA5B8),
            accent = accent,
            border = Color(0x1EFFFFFF),
            glassOpacity = opacity,
            neoDepth = depth
        )
        "NeomorphLight" -> AdvancePalette(
            style = SurfaceStyle.NEOMORPH,
            isLight = true,
            backdrop = Color(0xFFE4EAF2),
            surface = Color(0xFFE4EAF2),
            surfaceElevated = Color(0xFFEAEFF7),
            glassHighlight = Color.Transparent,
            glassBorderStart = Color(0xCCFFFFFF),
            glassBorderEnd = Color(0x44A3B1C6),
            neoLightShadow = Color(0xFFFFFFFF),
            neoDarkShadow = Color(0xFFB5C2D6),
            neoBase = Color(0xFFE4EAF2),
            neoRecessed = Color(0xFFD6DFEC),
            textPrimary = Color(0xFF1B2432),
            textSecondary = Color(0xFF5D6B82),
            accent = accent,
            border = Color(0x88FFFFFF),
            glassOpacity = opacity,
            neoDepth = depth
        )
        else -> AdvancePalette( // GlassDark
            style = SurfaceStyle.GLASS,
            isLight = false,
            backdrop = Color(0xFF0A0F1A).copy(alpha = (opacity + 0.30f).coerceAtMost(0.92f)),
            surface = Color(0xFF101828).copy(alpha = opacity),
            surfaceElevated = Color(0xFF162033).copy(alpha = (opacity + 0.16f).coerceAtMost(0.94f)),
            glassHighlight = Color(0x38FFFFFF),
            glassBorderStart = Color(0x85FFFFFF),
            glassBorderEnd = accent.copy(alpha = 0.42f),
            neoLightShadow = Color.Transparent,
            neoDarkShadow = Color.Transparent,
            neoBase = Color(0xFF121A2B),
            neoRecessed = Color(0x33000000),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFC2CEE0),
            accent = accent,
            border = Color(0x45FFFFFF),
            glassOpacity = opacity,
            neoDepth = depth
        )
    }
}
