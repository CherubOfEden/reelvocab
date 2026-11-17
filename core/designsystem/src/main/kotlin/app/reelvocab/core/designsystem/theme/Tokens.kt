package app.reelvocab.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ReelSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val huge: Dp = 40.dp,
    val giant: Dp = 48.dp,
    val colossal: Dp = 64.dp,
    val screenH: Dp = 20.dp,
    val screenBottom: Dp = 24.dp,
    val minTouchTarget: Dp = 48.dp,
    val bottomBarHeight: Dp = 80.dp,
    val controlHeight: Dp = 52.dp,
    val fabSize: Dp = 56.dp,
    val hairline: Dp = 1.dp,
    val thumbZoneStartFraction: Float = 0.4f,
)

@Immutable
data class ReelRadius(
    val chip: Dp = 8.dp,
    val input: Dp = 12.dp,
    val card: Dp = 16.dp,
    val sheet: Dp = 24.dp,
    val sheetTop: Dp = 28.dp,
)

val ReelSpacing = ReelSpacing()
val ReelRadius = ReelRadius()

val ReelShapes = Shapes(
    extraSmall = RoundedCornerShape(ReelRadius.chip),
    small = RoundedCornerShape(ReelRadius.input),
    medium = RoundedCornerShape(ReelRadius.card),
    large = RoundedCornerShape(ReelRadius.sheet),
    extraLarge = RoundedCornerShape(ReelRadius.sheetTop),
)
