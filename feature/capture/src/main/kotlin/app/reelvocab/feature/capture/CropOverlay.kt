package app.reelvocab.feature.capture

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.reelvocab.core.designsystem.component.ReelButton
import app.reelvocab.core.designsystem.component.ReelButtonStyle
import app.reelvocab.core.designsystem.theme.ReelTheme
import kotlin.math.roundToInt

@Composable
internal fun CropOverlay(
    onDone: (RectF) -> Unit,
    onCancel: () -> Unit,
    bitmap: Bitmap? = null,
    modifier: Modifier = Modifier,
) {
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }
    val cropState = remember(bitmap) {
        mutableStateOf(if (bitmap != null) CropFrame.Full else CropFrame.Inset)
    }
    val activeHandle = remember { mutableStateOf<CropHandle?>(null) }
    val size = remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val inset = WindowInsets.safeDrawing
    val gutter = with(density) { CROP_GUTTER.roundToPx() }.toFloat()
    val edgeTouch = with(density) { EDGE_TOUCH.roundToPx() }.toFloat()
    val cornerTouch = with(density) { CORNER_TOUCH.roundToPx() }.toFloat()
    val minWidth = with(density) { MIN_CROP_WIDTH.roundToPx() }.toFloat()
    val minHeight = with(density) { MIN_CROP_HEIGHT.roundToPx() }.toFloat()
    val safeLeft = inset.getLeft(density, layoutDirection).toFloat()
    val safeTop = inset.getTop(density).toFloat()
    val safeRight = inset.getRight(density, layoutDirection).toFloat()
    val safeBottom = inset.getBottom(density).toFloat()
    val colors = ReelTheme.colors
    val borderColor = colors.accent
    BackHandler(onBack = onCancel)

    val imageRect = bitmap?.let {
        fitImageRect(
            left = safeLeft + gutter,
            top = safeTop + gutter,
            right = (size.value.width - safeRight - gutter).coerceAtLeast(safeLeft + gutter + 1f),
            bottom = (size.value.height - safeBottom - gutter).coerceAtLeast(safeTop + gutter + 1f),
            imageWidth = it.width,
            imageHeight = it.height,
            fraction = 0.88f,
        )
    }
    val workLeft = imageRect?.left ?: (safeLeft + gutter)
    val workTop = imageRect?.top ?: (safeTop + gutter)
    val workRight = imageRect?.right
        ?: (size.value.width - safeRight - gutter).coerceAtLeast(workLeft + 1f)
    val workBottom = imageRect?.bottom
        ?: (size.value.height - safeBottom - gutter).coerceAtLeast(workTop + 1f)
    val workWidth = workRight - workLeft
    val workHeight = workBottom - workTop
    val minWidthNorm = (minWidth / workWidth).coerceIn(0.01f, 1f)
    val minHeightNorm = (minHeight / workHeight).coerceIn(0.01f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .onSizeChanged { size.value = it }
            .pointerInput(
                workLeft,
                workTop,
                workWidth,
                workHeight,
                edgeTouch,
                cornerTouch,
                minWidthNorm,
                minHeightNorm,
            ) {
                if (workWidth <= 0f || workHeight <= 0f) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val frame = cropState.value
                    val left = workLeft + frame.left * workWidth
                    val top = workTop + frame.top * workHeight
                    val right = workLeft + frame.right * workWidth
                    val bottom = workTop + frame.bottom * workHeight
                    val handle = cropHandleAt(
                        x = down.position.x,
                        y = down.position.y,
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        edgeTouch = edgeTouch,
                        cornerTouch = cornerTouch,
                    ) ?: return@awaitEachGesture

                    activeHandle.value = handle
                    down.consume()
                    val pointerId = down.id
                    while (true) {
                        val change = awaitPointerEvent()
                            .changes
                            .firstOrNull { it.id == pointerId }
                            ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            cropState.value = dragCropFrame(
                                crop = cropState.value,
                                handle = handle,
                                dragX = delta.x / workWidth,
                                dragY = delta.y / workHeight,
                                minWidth = minWidthNorm,
                                minHeight = minHeightNorm,
                                edgeInset = if (bitmap != null) 0f else CropFrame.InsetMargin,
                            )
                            change.consume()
                        }
                    }
                    activeHandle.value = null
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (imageBitmap != null && imageRect != null) {
                drawImage(
                    image = imageBitmap,
                    dstOffset = IntOffset(
                        imageRect.left.roundToInt(),
                        imageRect.top.roundToInt(),
                    ),
                    dstSize = IntSize(
                        imageRect.width.roundToInt(),
                        imageRect.height.roundToInt(),
                    ),
                )
            }
            val frame = cropState.value
            val rect = Rect(
                left = workLeft + frame.left * workWidth,
                top = workTop + frame.top * workHeight,
                right = workLeft + frame.right * workWidth,
                bottom = workTop + frame.bottom * workHeight,
            )
            drawCropMask(rect)
            if (rect.width > 0f && rect.height > 0f) {
                drawCropGrid(rect)
                val stroke = 2.dp.toPx()
                drawRect(
                    color = borderColor,
                    topLeft = Offset(rect.left + stroke / 2f, rect.top + stroke / 2f),
                    size = Size(rect.width - stroke, rect.height - stroke),
                    style = Stroke(width = stroke),
                )
                drawCropHandles(rect, borderColor, activeHandle.value)
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
                onClick = {
                    val frame = cropState.value
                    val output = if (bitmap != null) {
                        RectF(frame.left, frame.top, frame.right, frame.bottom)
                    } else {
                        val screen = frame.toScreen(
                            workLeft = workLeft,
                            workTop = workTop,
                            workWidth = workWidth,
                            workHeight = workHeight,
                            screenWidth = size.value.width.toFloat(),
                            screenHeight = size.value.height.toFloat(),
                        )
                        RectF(screen.left, screen.top, screen.right, screen.bottom)
                    }
                    onDone(output)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun fitImageRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    imageWidth: Int,
    imageHeight: Int,
    fraction: Float,
): Rect {
    if (imageWidth <= 0 || imageHeight <= 0) {
        return Rect(left, top, right, bottom)
    }
    val availableWidth = (right - left).coerceAtLeast(1f) * fraction
    val availableHeight = (bottom - top).coerceAtLeast(1f) * fraction
    val scale = minOf(availableWidth / imageWidth, availableHeight / imageHeight)
        .coerceAtLeast(0.0001f)
    val width = imageWidth * scale
    val height = imageHeight * scale
    val centerX = (left + right) / 2f
    val centerY = (top + bottom) / 2f
    return Rect(
        left = centerX - width / 2f,
        top = centerY - height / 2f,
        right = centerX + width / 2f,
        bottom = centerY + height / 2f,
    )
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

private fun DrawScope.drawCropGrid(rect: Rect) {
    val grid = Color.White.copy(alpha = 0.22f)
    val line = 1.dp.toPx()
    drawRect(
        color = grid,
        topLeft = Offset(rect.left + rect.width / 3f, rect.top),
        size = Size(line, rect.height),
    )
    drawRect(
        color = grid,
        topLeft = Offset(rect.left + rect.width * 2f / 3f, rect.top),
        size = Size(line, rect.height),
    )
    drawRect(
        color = grid,
        topLeft = Offset(rect.left, rect.top + rect.height / 3f),
        size = Size(rect.width, line),
    )
    drawRect(
        color = grid,
        topLeft = Offset(rect.left, rect.top + rect.height * 2f / 3f),
        size = Size(rect.width, line),
    )
}

private fun DrawScope.drawCropHandles(
    rect: Rect,
    color: Color,
    active: CropHandle?,
) {
    val cornerRadius = 7.dp.toPx()
    val edgeRadius = 4.5.dp.toPx()
    val activeBoost = 2.dp.toPx()
    val outline = Color.Black.copy(alpha = 0.55f)
    val centerX = rect.center.x
    val centerY = rect.center.y

    fun handle(center: Offset, radius: Float, handle: CropHandle) {
        val drawRadius = if (active == handle) radius + activeBoost else radius
        drawCircle(outline, drawRadius + 1.5.dp.toPx(), center)
        drawCircle(color, drawRadius, center)
    }

    handle(rect.topLeft, cornerRadius, CropHandle.TopLeft)
    handle(rect.topRight, cornerRadius, CropHandle.TopRight)
    handle(rect.bottomLeft, cornerRadius, CropHandle.BottomLeft)
    handle(rect.bottomRight, cornerRadius, CropHandle.BottomRight)
    handle(Offset(centerX, rect.top), edgeRadius, CropHandle.Top)
    handle(Offset(centerX, rect.bottom), edgeRadius, CropHandle.Bottom)
    handle(Offset(rect.left, centerY), edgeRadius, CropHandle.Left)
    handle(Offset(rect.right, centerY), edgeRadius, CropHandle.Right)
}

private val CROP_GUTTER = 18.dp
private val EDGE_TOUCH = 28.dp
private val CORNER_TOUCH = 38.dp
private val MIN_CROP_WIDTH = 64.dp
private val MIN_CROP_HEIGHT = 64.dp
