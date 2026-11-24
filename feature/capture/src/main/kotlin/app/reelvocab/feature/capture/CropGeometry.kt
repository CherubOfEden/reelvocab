package app.reelvocab.feature.capture

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Build
import kotlin.math.roundToInt

internal const val EXTRA_CROP = "crop_rect"

internal fun Intent.readCropRect(): RectF? {
    val value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_CROP, RectF::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra<RectF>(EXTRA_CROP)
    }
    return value?.let { normalizedCrop(it) }
}

internal fun Intent.putCropRect(crop: RectF?): Intent = apply {
    if (crop != null) putExtra(EXTRA_CROP, normalizedCrop(crop))
}

internal fun normalizedCrop(crop: RectF): RectF = RectF(
    crop.left.coerceIn(0f, 1f),
    crop.top.coerceIn(0f, 1f),
    crop.right.coerceIn(0f, 1f),
    crop.bottom.coerceIn(0f, 1f),
).apply {
    if (right < left) {
        val value = left
        left = right
        right = value
    }
    if (bottom < top) {
        val value = top
        top = bottom
        bottom = value
    }
}

internal fun RectF.isFullScreen(): Boolean =
    left <= 0.0001f && top <= 0.0001f && right >= 0.9999f && bottom >= 0.9999f

internal data class PixelCrop(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun normalized(sourceWidth: Int, sourceHeight: Int): RectF = RectF(
        left.toFloat() / sourceWidth,
        top.toFloat() / sourceHeight,
        right.toFloat() / sourceWidth,
        bottom.toFloat() / sourceHeight,
    )
}

internal fun pixelCrop(
    crop: RectF?,
    sourceWidth: Int,
    sourceHeight: Int,
    alignEven: Boolean = false,
): PixelCrop? {
    if (crop == null || crop.isFullScreen() || sourceWidth <= 0 || sourceHeight <= 0) return null
    val safe = normalizedCrop(crop)
    var left = (safe.left * sourceWidth).roundToInt().coerceIn(0, sourceWidth - 1)
    var top = (safe.top * sourceHeight).roundToInt().coerceIn(0, sourceHeight - 1)
    var right = (safe.right * sourceWidth).roundToInt().coerceIn(left + 1, sourceWidth)
    var bottom = (safe.bottom * sourceHeight).roundToInt().coerceIn(top + 1, sourceHeight)

    if (alignEven) {
        right = left + ((right - left) / 2) * 2
        bottom = top + ((bottom - top) / 2) * 2
        if (right > sourceWidth) right = sourceWidth
        if (bottom > sourceHeight) bottom = sourceHeight
        if (right - left < 2) left = (right - 2).coerceAtLeast(0)
        if (bottom - top < 2) top = (bottom - 2).coerceAtLeast(0)
    }

    if (right <= left || bottom <= top) return null
    return PixelCrop(left, top, right, bottom)
}

internal fun cropBitmap(source: Bitmap, crop: RectF?): Bitmap {
    val pixels = pixelCrop(crop, source.width, source.height) ?: return source
    return Bitmap.createBitmap(source, pixels.left, pixels.top, pixels.width, pixels.height)
}
