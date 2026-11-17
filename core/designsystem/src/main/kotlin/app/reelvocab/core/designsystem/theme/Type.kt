package app.reelvocab.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.reelvocab.core.designsystem.R

private val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
    Font(R.font.fraunces_bold, FontWeight.Bold),
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

private val SourceCodePro = FontFamily(
    Font(R.font.source_code_pro_regular, FontWeight.Normal),
)

@Immutable
data class ReelType(
    val hero: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 61.6f.sp,
        letterSpacing = (-0.02f).em,
    ),
    val displayL: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44f.sp,
        letterSpacing = (-0.02f).em,
    ),
    val displayM: TextStyle = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 35.2f.sp,
        letterSpacing = (-0.01f).em,
    ),
    val titleL: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 31.2f.sp,
        letterSpacing = (-0.01f).em,
    ),
    val titleM: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26f.sp,
    ),
    val bodyL: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.4f.sp,
    ),
    val bodyM: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.6f.sp,
    ),
    val label: TextStyle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16f.sp,
        letterSpacing = 0.02f.em,
    ),
    val ipa: TextStyle = TextStyle(
        fontFamily = SourceCodePro,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
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
        bodySmall = label.copy(fontWeight = FontWeight.Normal),
        labelLarge = bodyM.copy(fontWeight = FontWeight.Medium),
        labelMedium = label,
        labelSmall = label.copy(fontSize = 11.sp),
    )
}

val ReelTypography = ReelType()
