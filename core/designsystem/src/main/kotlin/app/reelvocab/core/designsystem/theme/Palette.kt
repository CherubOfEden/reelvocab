package app.reelvocab.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal object ReelArgb {
    const val DarkBase: Long = 0xFF0B0C10
    const val DarkSurface: Long = 0xFF14161C
    const val DarkElevated: Long = 0xFF1C1F27
    const val DarkStroke: Long = 0xFF262A33
    const val DarkTextPrimary: Long = 0xFFF2F3F5
    const val DarkTextSecondary: Long = 0xFFA8AEBB
    const val DarkTextTertiary: Long = 0xFF7B8291
    const val DarkGold: Long = 0xFFE8B44A
    const val DarkGoldDim: Long = 0xFF9C7A2E
    const val DarkTeal: Long = 0xFF3FC1C9
    const val DarkRose: Long = 0xFFE4576B
    const val DarkGreen: Long = 0xFF4CC38A
    const val DarkOnAccent: Long = 0xFF14110A

    const val LightBase: Long = 0xFFF5F6F8
    const val LightSurface: Long = 0xFFFFFFFF
    const val LightElevated: Long = 0xFFFBFBFC
    const val LightStroke: Long = 0xFFE2E4EA
    const val LightTextPrimary: Long = 0xFF111217
    const val LightTextSecondary: Long = 0xFF5A6170
    const val LightTextTertiary: Long = 0xFF676E7C
    const val LightGold: Long = 0xFFE0B152
    const val LightGoldDim: Long = 0xFFB08A33
    const val LightTeal: Long = 0xFF1F8A91
    const val LightRose: Long = 0xFFC93F53
    const val LightGreen: Long = 0xFF2E9A69
    const val LightOnAccent: Long = 0xFF1A1408

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
)

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
