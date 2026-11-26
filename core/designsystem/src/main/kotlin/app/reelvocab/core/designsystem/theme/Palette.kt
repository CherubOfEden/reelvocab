package app.reelvocab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal object ReelArgb {
    const val DarkBase: Long = 0xFF0E0D0B
    const val DarkSurface: Long = 0xFF151310
    const val DarkElevated: Long = 0xFF1D1A16
    const val DarkStroke: Long = 0xFF302A23
    const val DarkTextPrimary: Long = 0xFFF3EDE3
    const val DarkTextSecondary: Long = 0xFFB8AD9E
    const val DarkTextTertiary: Long = 0xFF8C8173
    const val DarkGold: Long = 0xFFB08A5A
    const val DarkGoldDim: Long = 0xFF7E6344
    const val DarkTeal: Long = 0xFF7C8870
    const val DarkRose: Long = 0xFFC87173
    const val DarkGreen: Long = 0xFF7F9B78
    const val DarkOnAccent: Long = 0xFF16120C

    const val LightBase: Long = 0xFFF5F0E7
    const val LightSurface: Long = 0xFFFBF7F0
    const val LightElevated: Long = 0xFFFFFCF6
    const val LightStroke: Long = 0xFFDED3C3
    const val LightTextPrimary: Long = 0xFF211E1A
    const val LightTextSecondary: Long = 0xFF625B51
    const val LightTextTertiary: Long = 0xFF6E655A
    const val LightGold: Long = 0xFF7E5F3A
    const val LightGoldDim: Long = 0xFF9A7B52
    const val LightTeal: Long = 0xFF5F6D57
    const val LightRose: Long = 0xFFA34B50
    const val LightGreen: Long = 0xFF4E7455
    const val LightOnAccent: Long = 0xFFFFFDF8

    const val DarkScrim: Long = 0x99000000
    const val LightScrim: Long = 0x99000000
}

@Immutable
data class ReelColors(
    val base: Color,
    val surface: Color,
    val elevated: Color,
    val strokeSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gold: Color,
    val goldDim: Color,
    val teal: Color,
    val rose: Color,
    val green: Color,
    val scrim: Color,
    val onAccent: Color,
    val isDark: Boolean,
) {
    val accent: Color get() = gold
    val accentMuted: Color get() = goldDim
    val secondary: Color get() = teal
    val divider: Color get() = strokeSubtle
    val error: Color get() = rose
    val success: Color get() = green
}

val ReelDarkColors = ReelColors(
    base = Color(ReelArgb.DarkBase),
    surface = Color(ReelArgb.DarkSurface),
    elevated = Color(ReelArgb.DarkElevated),
    strokeSubtle = Color(ReelArgb.DarkStroke),
    textPrimary = Color(ReelArgb.DarkTextPrimary),
    textSecondary = Color(ReelArgb.DarkTextSecondary),
    textTertiary = Color(ReelArgb.DarkTextTertiary),
    gold = Color(ReelArgb.DarkGold),
    goldDim = Color(ReelArgb.DarkGoldDim),
    teal = Color(ReelArgb.DarkTeal),
    rose = Color(ReelArgb.DarkRose),
    green = Color(ReelArgb.DarkGreen),
    scrim = Color(ReelArgb.DarkScrim),
    onAccent = Color(ReelArgb.DarkOnAccent),
    isDark = true,
)

val ReelLightColors = ReelColors(
    base = Color(ReelArgb.LightBase),
    surface = Color(ReelArgb.LightSurface),
    elevated = Color(ReelArgb.LightElevated),
    strokeSubtle = Color(ReelArgb.LightStroke),
    textPrimary = Color(ReelArgb.LightTextPrimary),
    textSecondary = Color(ReelArgb.LightTextSecondary),
    textTertiary = Color(ReelArgb.LightTextTertiary),
    gold = Color(ReelArgb.LightGold),
    goldDim = Color(ReelArgb.LightGoldDim),
    teal = Color(ReelArgb.LightTeal),
    rose = Color(ReelArgb.LightRose),
    green = Color(ReelArgb.LightGreen),
    scrim = Color(ReelArgb.LightScrim),
    onAccent = Color(ReelArgb.LightOnAccent),
    isDark = false,
)
