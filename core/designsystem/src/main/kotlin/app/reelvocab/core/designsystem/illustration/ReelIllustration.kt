package app.reelvocab.core.designsystem.illustration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.theme.ReelTheme

enum class ReelIllustration { Poster, EmptyDeck, Ticket, Projector }

@Composable
fun ReelIllustrationArt(
    illustration: ReelIllustration,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = ReelTheme.colors
    val accent = tint ?: colors.goldDim

    Canvas(modifier = modifier.size(width = 132.dp, height = 104.dp)) {
        val w = size.width
        val h = size.height
        val hairline = 2.dp.toPx()
        when (illustration) {
            ReelIllustration.Poster -> {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.12f, h * 0.08f),
                    size = Size(w * 0.76f, h * 0.84f),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(hairline),
                )
                val play = Path().apply {
                    moveTo(w * 0.43f, h * 0.36f)
                    lineTo(w * 0.65f, h * 0.5f)
                    lineTo(w * 0.43f, h * 0.64f)
                    close()
                }
                drawPath(play, color = colors.gold)
                drawRoundRect(
                    color = colors.strokeSubtle,
                    topLeft = Offset(w * 0.24f, h * 0.76f),
                    size = Size(w * 0.52f, hairline),
                    cornerRadius = CornerRadius(hairline),
                )
            }

            ReelIllustration.EmptyDeck -> {
                drawRoundRect(
                    color = colors.strokeSubtle,
                    topLeft = Offset(w * 0.24f, h * 0.28f),
                    size = Size(w * 0.54f, h * 0.54f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(hairline),
                )
                drawRoundRect(
                    color = colors.strokeSubtle,
                    topLeft = Offset(w * 0.17f, h * 0.19f),
                    size = Size(w * 0.54f, h * 0.54f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(hairline),
                )
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.10f, h * 0.10f),
                    size = Size(w * 0.54f, h * 0.54f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(hairline),
                )
                drawLine(
                    color = colors.gold,
                    start = Offset(w * 0.13f, h * 0.16f),
                    end = Offset(w * 0.13f, h * 0.58f),
                    strokeWidth = 3.dp.toPx(),
                )
            }

            ReelIllustration.Ticket -> {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.08f, h * 0.30f),
                    size = Size(w * 0.84f, h * 0.40f),
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(hairline),
                )
                drawCircle(color = colors.base, radius = h * 0.09f, center = Offset(w * 0.08f, h * 0.5f))
                drawCircle(color = colors.base, radius = h * 0.09f, center = Offset(w * 0.92f, h * 0.5f))
                drawLine(
                    color = colors.gold,
                    start = Offset(w * 0.28f, h * 0.5f),
                    end = Offset(w * 0.72f, h * 0.5f),
                    strokeWidth = hairline,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }

            ReelIllustration.Projector -> {
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(w * 0.14f, h * 0.34f),
                    size = Size(w * 0.32f, h * 0.34f),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(hairline),
                )
                drawCircle(color = colors.gold, radius = h * 0.07f, center = Offset(w * 0.30f, h * 0.51f))
                val beam = Path().apply {
                    moveTo(w * 0.48f, h * 0.42f)
                    lineTo(w * 0.86f, h * 0.26f)
                    lineTo(w * 0.86f, h * 0.76f)
                    lineTo(w * 0.48f, h * 0.60f)
                    close()
                }
                drawPath(beam, color = colors.teal.copy(alpha = 0.22f))
                drawLine(
                    color = colors.strokeSubtle,
                    start = Offset(w * 0.22f, h * 0.34f),
                    end = Offset(w * 0.22f, h * 0.68f),
                    strokeWidth = hairline,
                )
            }
        }
    }
}
