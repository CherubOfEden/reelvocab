package app.reelvocab.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.reelvocab.core.designsystem.R

private val Fraunces = FontFamily(
    Font(R.font.fraunces_variable, FontWeight.Normal),
    Font(R.font.fraunces_variable, FontWeight.Medium),
    Font(R.font.fraunces_variable, FontWeight.SemiBold),
    Font(R.font.fraunces_italic_variable, FontWeight.Normal, FontStyle.Italic),
)

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

@Immutable
data class ReelType(
    val hero: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 56.sp,
        lineHeight = 58.8f.sp,
        letterSpacing = (-0.02f).em,
    ),
    val displayL: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 44f.sp,
        letterSpacing = (-0.02f).em,
    ),
    val displayM: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36f.sp,
        letterSpacing = (-0.01f).em,
    ),
    val wordmark: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 32f.sp,
        letterSpacing = (-0.01f).em,
    ),
    val titleL: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontSize = 26.sp,
        lineHeight = 32f.sp,
        letterSpacing = (-0.01f).em,
    ),
    val titleM: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24f.sp,
    ),
    val bodyL: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26f.sp,
    ),
    val bodyM: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.4f.sp,
    ),
    val quote: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 18f.sp,
        letterSpacing = 0.02f.em,
    ),
    val sectionLabel: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 16f.sp,
        letterSpacing = 0.12f.em,
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18f.sp,
    ),
    val navLabel: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14f.sp,
        letterSpacing = 0.01f.em,
    ),
    val ipa: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 24f.sp,
        letterSpacing = 0.01f.em,
    ),
    val mono: TextStyle = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16f.sp,
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
