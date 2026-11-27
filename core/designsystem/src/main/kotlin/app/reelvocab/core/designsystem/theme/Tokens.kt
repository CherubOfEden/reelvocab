package app.reelvocab.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ReelSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 6.dp,
    val s: Dp = 10.dp,
    val m: Dp = 14.dp,
    val l: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    val huge: Dp = 56.dp,
    val giant: Dp = 64.dp,
    val colossal: Dp = 80.dp,
    val screenH: Dp = 32.dp,
    val screenBottom: Dp = 48.dp,
    val minTouchTarget: Dp = 48.dp,
    val bottomBarHeight: Dp = 84.dp,
    val controlHeight: Dp = 52.dp,
    val fabSize: Dp = 56.dp,
    val hairline: Dp = 1.dp,
    val thumbZoneStartFraction: Float = 0.4f,
)

@Immutable
data class ReelRadius(
    val chip: Dp = 4.dp,
    val input: Dp = 8.dp,
    val card: Dp = 2.dp,
    val sheet: Dp = 20.dp,
    val sheetTop: Dp = 24.dp,
)

val DefaultReelSpacing = ReelSpacing()
val DefaultReelRadius = ReelRadius()

val ReelShapes = Shapes(
    extraSmall = RoundedCornerShape(DefaultReelRadius.chip),
    small = RoundedCornerShape(DefaultReelRadius.input),
    medium = RoundedCornerShape(DefaultReelRadius.card),
    large = RoundedCornerShape(DefaultReelRadius.sheet),
    extraLarge = RoundedCornerShape(DefaultReelRadius.sheetTop),
)
