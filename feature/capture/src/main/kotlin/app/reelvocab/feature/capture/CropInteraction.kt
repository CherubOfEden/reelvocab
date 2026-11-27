package app.reelvocab.feature.capture

internal enum class CropHandle {
    Left,
    Top,
    Right,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

internal data class CropFrame(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    companion object {
        const val InsetMargin = 0.035f

        val Full = CropFrame()
        val Inset = CropFrame(
            left = InsetMargin,
            top = InsetMargin,
            right = 1f - InsetMargin,
            bottom = 1f - InsetMargin,
        )
    }
}

internal fun cropHandleAt(
    x: Float,
    y: Float,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    edgeTouch: Float,
    cornerTouch: Float,
): CropHandle? {
    val cornerLeft = x in (left - cornerTouch)..(left + cornerTouch)
    val cornerRight = x in (right - cornerTouch)..(right + cornerTouch)
    val cornerTop = y in (top - cornerTouch)..(top + cornerTouch)
    val cornerBottom = y in (bottom - cornerTouch)..(bottom + cornerTouch)

    return when {
        cornerLeft && cornerTop -> CropHandle.TopLeft
        cornerRight && cornerTop -> CropHandle.TopRight
        cornerLeft && cornerBottom -> CropHandle.BottomLeft
        cornerRight && cornerBottom -> CropHandle.BottomRight

        y in (top - edgeTouch)..(top + edgeTouch) &&
            x in (left - edgeTouch)..(right + edgeTouch) -> CropHandle.Top

        y in (bottom - edgeTouch)..(bottom + edgeTouch) &&
            x in (left - edgeTouch)..(right + edgeTouch) -> CropHandle.Bottom

        x in (left - edgeTouch)..(left + edgeTouch) &&
            y in (top - edgeTouch)..(bottom + edgeTouch) -> CropHandle.Left

        x in (right - edgeTouch)..(right + edgeTouch) &&
            y in (top - edgeTouch)..(bottom + edgeTouch) -> CropHandle.Right

        else -> null
    }
}

internal fun dragCropFrame(
    crop: CropFrame,
    handle: CropHandle,
    dragX: Float,
    dragY: Float,
    minWidth: Float,
    minHeight: Float,
    edgeInset: Float = 0f,
): CropFrame {
    val inset = edgeInset.coerceIn(0f, 0.45f)
    val lower = inset
    val upper = 1f - inset
    val available = (upper - lower).coerceAtLeast(0.02f)
    val minW = minWidth.coerceIn(0.01f, available)
    val minH = minHeight.coerceIn(0.01f, available)
    val left = crop.left.coerceIn(lower, upper - minW)
    val top = crop.top.coerceIn(lower, upper - minH)
    val right = crop.right.coerceIn(left + minW, upper)
    val bottom = crop.bottom.coerceIn(top + minH, upper)

    return when (handle) {
        CropHandle.Left -> CropFrame(
            left = (left + dragX).coerceIn(lower, right - minW),
            top = top,
            right = right,
            bottom = bottom,
        )

        CropHandle.Top -> CropFrame(
            left = left,
            top = (top + dragY).coerceIn(lower, bottom - minH),
            right = right,
            bottom = bottom,
        )

        CropHandle.Right -> CropFrame(
            left = left,
            top = top,
            right = (right + dragX).coerceIn(left + minW, upper),
            bottom = bottom,
        )

        CropHandle.Bottom -> CropFrame(
            left = left,
            top = top,
            right = right,
            bottom = (bottom + dragY).coerceIn(top + minH, upper),
        )

        CropHandle.TopLeft -> CropFrame(
            left = (left + dragX).coerceIn(lower, right - minW),
            top = (top + dragY).coerceIn(lower, bottom - minH),
            right = right,
            bottom = bottom,
        )

        CropHandle.TopRight -> CropFrame(
            left = left,
            top = (top + dragY).coerceIn(lower, bottom - minH),
            right = (right + dragX).coerceIn(left + minW, upper),
            bottom = bottom,
        )

        CropHandle.BottomLeft -> CropFrame(
            left = (left + dragX).coerceIn(lower, right - minW),
            top = top,
            right = right,
            bottom = (bottom + dragY).coerceIn(top + minH, upper),
        )

        CropHandle.BottomRight -> CropFrame(
            left = left,
            top = top,
            right = (right + dragX).coerceIn(left + minW, upper),
            bottom = (bottom + dragY).coerceIn(top + minH, upper),
        )
    }
}

internal fun CropFrame.toScreen(
    workLeft: Float,
    workTop: Float,
    workWidth: Float,
    workHeight: Float,
    screenWidth: Float,
    screenHeight: Float,
): CropFrame {
    if (workWidth <= 0f || workHeight <= 0f || screenWidth <= 0f || screenHeight <= 0f) {
        return CropFrame.Full
    }
    return CropFrame(
        left = (workLeft + left * workWidth) / screenWidth,
        top = (workTop + top * workHeight) / screenHeight,
        right = (workLeft + right * workWidth) / screenWidth,
        bottom = (workTop + bottom * workHeight) / screenHeight,
    )
}
