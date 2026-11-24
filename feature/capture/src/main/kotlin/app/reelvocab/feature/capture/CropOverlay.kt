package app.reelvocab.feature.capture

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.theme.ReelTheme
import android.graphics.RectF
import kotlin.math.max

private enum class CropHandle {
    Left,
    Top,
    Right,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

@Composable
internal fun CropOverlay(
    onDone: (RectF) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var crop by remember { mutableStateOf(RectF(0f, 0f, 1f, 1f)) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var activeHandle by remember { mutableStateOf<CropHandle?>(null) }
    val density = LocalDensity.current
    val edgeTouch = with(density) { 26.dp.toPx() }
    val cornerTouch = with(density) { 34.dp.toPx() }
    val minWidth = with(density) { 56.dp.toPx() }
    val minHeight = with(density) { 56.dp.toPx() }
    val colors = ReelTheme.colors
    val borderColor = colors.gold
    BackHandler(onBack = onCancel)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeHandle = hitTest(
                            offset = offset,
                            crop = crop,
                            size = size,
                            edgeTouch = edgeTouch,
                            cornerTouch = cornerTouch,
                        )
                    },
                    onDragEnd = { activeHandle = null },
                    onDragCancel = { activeHandle = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val handle = activeHandle ?: return@detectDragGestures
                        crop = dragCrop(
                            crop = crop,
                            handle = handle,
                            dragX = dragAmount.x,
                            dragY = dragAmount.y,
                            size = size,
                            minWidth = minWidth,
                            minHeight = minHeight,
                        )
                    },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val rect = crop.toRect(size)
            drawCropMask(rect)
            if (rect.width > 0f && rect.height > 0f) {
                drawRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(rect.left + rect.width / 3f, rect.top),
                    size = Size(1f, rect.height),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(rect.left + rect.width * 2f / 3f, rect.top),
                    size = Size(1f, rect.height),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(rect.left, rect.top + rect.height / 3f),
                    size = Size(rect.width, 1f),
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(rect.left, rect.top + rect.height * 2f / 3f),
                    size = Size(rect.width, 1f),
                )
                drawRect(
                    color = borderColor,
                    topLeft = Offset(rect.left, rect.top),
                    size = Size(rect.width, rect.height),
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCornerArrows(rect, borderColor)
            }
        }

        Text(
            text = stringResource(R.string.capture_crop_hint),
            style = ReelTheme.type.label,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.52f),
                    shape = RoundedCornerShape(50),
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ReelButton(
                text = stringResource(R.string.capture_crop_cancel),
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                style = ReelButtonStyle.Secondary,
            )
            ReelButton(
                text = stringResource(R.string.capture_crop_done),
                onClick = { onDone(RectF(crop)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun DrawScope.drawCropMask(rect: Rect) {
    val mask = Color.Black.copy(alpha = 0.55f)
    drawRect(mask, topLeft = Offset.Zero, size = Size(size.width, rect.top))
    drawRect(
        mask,
        topLeft = Offset(0f, rect.bottom),
        size = Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
    )
    drawRect(
        mask,
        topLeft = Offset(0f, rect.top),
        size = Size(rect.left, rect.height),
    )
    drawRect(
        mask,
        topLeft = Offset(rect.right, rect.top),
        size = Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
    )
}

private fun DrawScope.drawCornerArrows(rect: Rect, color: Color) {
    val inset = 6.dp.toPx()
    val length = 16.dp.toPx()
    val head = 4.dp.toPx()
    val stroke = 2.dp.toPx()

    fun arrow(from: Offset, to: Offset) {
        drawLine(color, from, to, strokeWidth = stroke, cap = StrokeCap.Round)
        val direction = to - from
        val magnitude = max(direction.getDistance(), 0.001f)
        val unit = direction / magnitude
        val perpendicular = Offset(-unit.y, unit.x)
        drawLine(
            color,
            to,
            to - unit * head + perpendicular * head,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            to,
            to - unit * head - perpendicular * head,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }

    arrow(
        Offset(rect.left + inset + length, rect.top + inset + length),
        Offset(rect.left + inset, rect.top + inset),
    )
    arrow(
        Offset(rect.right - inset - length, rect.top + inset + length),
        Offset(rect.right - inset, rect.top + inset),
    )
    arrow(
        Offset(rect.left + inset + length, rect.bottom - inset - length),
        Offset(rect.left + inset, rect.bottom - inset),
    )
    arrow(
        Offset(rect.right - inset - length, rect.bottom - inset - length),
        Offset(rect.right - inset, rect.bottom - inset),
    )
}

private fun hitTest(
    offset: Offset,
    crop: RectF,
    size: IntSize,
    edgeTouch: Float,
    cornerTouch: Float,
): CropHandle? {
    if (size.width <= 0 || size.height <= 0) return null
    val rect = crop.toRect(size)
    val x = offset.x
    val y = offset.y
    val nearLeft = x in (rect.left - edgeTouch)..(rect.left + edgeTouch)
    val nearRight = x in (rect.right - edgeTouch)..(rect.right + edgeTouch)
    val nearTop = y in (rect.top - edgeTouch)..(rect.top + edgeTouch)
    val nearBottom = y in (rect.bottom - edgeTouch)..(rect.bottom + edgeTouch)
    val cornerLeft = x in (rect.left - cornerTouch)..(rect.left + cornerTouch)
    val cornerRight = x in (rect.right - cornerTouch)..(rect.right + cornerTouch)
    val cornerTop = y in (rect.top - cornerTouch)..(rect.top + cornerTouch)
    val cornerBottom = y in (rect.bottom - cornerTouch)..(rect.bottom + cornerTouch)

    return when {
        cornerLeft && cornerTop -> CropHandle.TopLeft
        cornerRight && cornerTop -> CropHandle.TopRight
        cornerLeft && cornerBottom -> CropHandle.BottomLeft
        cornerRight && cornerBottom -> CropHandle.BottomRight
        nearLeft -> CropHandle.Left
        nearRight -> CropHandle.Right
        nearTop -> CropHandle.Top
        nearBottom -> CropHandle.Bottom
        else -> null
    }
}

private fun dragCrop(
    crop: RectF,
    handle: CropHandle,
    dragX: Float,
    dragY: Float,
    size: IntSize,
    minWidth: Float,
    minHeight: Float,
): RectF {
    if (size.width <= 0 || size.height <= 0) return crop
    val minW = (minWidth / size.width).coerceIn(0.01f, 1f)
    val minH = (minHeight / size.height).coerceIn(0.01f, 1f)
    val dx = dragX / size.width
    val dy = dragY / size.height
    val left = crop.left
    val top = crop.top
    val right = crop.right
    val bottom = crop.bottom

    return when (handle) {
        CropHandle.Left -> RectF(
            (left + dx).coerceIn(0f, right - minW),
            top,
            right,
            bottom,
        )

        CropHandle.Top -> RectF(
            left,
            (top + dy).coerceIn(0f, bottom - minH),
            right,
            bottom,
        )

        CropHandle.Right -> RectF(
            left,
            top,
            (right + dx).coerceIn(left + minW, 1f),
            bottom,
        )

        CropHandle.Bottom -> RectF(
            left,
            top,
            right,
            (bottom + dy).coerceIn(top + minH, 1f),
        )

        CropHandle.TopLeft -> RectF(
            (left + dx).coerceIn(0f, right - minW),
            (top + dy).coerceIn(0f, bottom - minH),
            right,
            bottom,
        )

        CropHandle.TopRight -> RectF(
            left,
            (top + dy).coerceIn(0f, bottom - minH),
            (right + dx).coerceIn(left + minW, 1f),
            bottom,
        )

        CropHandle.BottomLeft -> RectF(
            (left + dx).coerceIn(0f, right - minW),
            top,
            right,
            (bottom + dy).coerceIn(top + minH, 1f),
        )

        CropHandle.BottomRight -> RectF(
            left,
            top,
            (right + dx).coerceIn(left + minW, 1f),
            (bottom + dy).coerceIn(top + minH, 1f),
        )
    }
}

private fun RectF.toRect(size: IntSize): Rect = Rect(
    left = left * size.width,
    top = top * size.height,
    right = right * size.width,
    bottom = bottom * size.height,
)
