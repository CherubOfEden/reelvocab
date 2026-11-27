@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package app.reelvocab.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.reelvocab.core.designsystem.R

private val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
    Font(R.font.amiri_italic, FontWeight.Normal, FontStyle.Italic),
)

private val Jost = FontFamily(
    Font(
        resId = R.font.jost_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        resId = R.font.jost_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        resId = R.font.jost_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

@Immutable
data class ReelType(
    val hero: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 42.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.01f).em,
    ),
    val displayL: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.01f).em,
    ),
    val displayM: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.em,
    ),
    val wordmark: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.em,
    ),
    val titleL: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontSize = 21.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.em,
    ),
    val titleM: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01f.em,
    ),
    val bodyL: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.01f.em,
    ),
    val bodyM: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.01f.em,
    ),
    val quote: TextStyle = TextStyle(
        fontFamily = Amiri,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 15.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.01f.em,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.04f.em,
    ),
    val sectionLabel: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.14f.em,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.01f.em,
    ),
    val navLabel: TextStyle = TextStyle(
        fontFamily = Jost,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.02f.em,
    ),
    val ipa: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.01f.em,
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.02f.em,
    ),
) {
    fun toMaterialTypography(): Typography = Typography(
        displayLarge = hero,
        displayMedium = displayL,
        displaySmall = displayM,
        headlineLarge = titleL,
        headlineMedium = titleL,
        headlineSmall = titleM,
        titleLarge = titleM,
        titleMedium = bodyL.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = bodyM.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyL,
        bodyMedium = bodyM,
        bodySmall = caption,
        labelLarge = bodyM.copy(fontWeight = FontWeight.Medium),
        labelMedium = label,
        labelSmall = sectionLabel,
    )
}

val ReelTypography = ReelType()
