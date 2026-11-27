package app.reelvocab.feature.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

data class MediaAccess(
    val images: Boolean = false,
    val video: Boolean = false,
    val audio: Boolean = false,
    val visualPartial: Boolean = false,
) {
    val hasVisual: Boolean get() = images || video
    val hasAny: Boolean get() = hasVisual || audio

    companion object {
        val None = MediaAccess()
    }
}

internal fun currentMediaAccess(context: Context): MediaAccess {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val partial = context.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        val images = context.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
        val video = context.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
        val visual = images || video || partial
        return MediaAccess(
            images = visual,
            video = visual,
            audio = context.isGranted(Manifest.permission.READ_MEDIA_AUDIO),
            visualPartial = partial && !(images && video),
        )
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        return MediaAccess(
            images = context.isGranted(Manifest.permission.READ_MEDIA_IMAGES),
            video = context.isGranted(Manifest.permission.READ_MEDIA_VIDEO),
            audio = context.isGranted(Manifest.permission.READ_MEDIA_AUDIO),
        )
    }
    val legacy = context.isGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
    return MediaAccess(images = legacy, video = legacy, audio = legacy)
}

internal fun requiredMediaPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

internal fun requiredAudioPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

private fun Context.isGranted(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
