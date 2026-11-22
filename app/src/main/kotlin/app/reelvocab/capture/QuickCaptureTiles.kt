package app.reelvocab.capture

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import app.reelvocab.feature.capture.CaptureActivity
import app.reelvocab.feature.capture.CaptureLaunchSource

abstract class BaseCaptureTile : TileService() {

    protected abstract val mode: String

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = CaptureActivity.recordIntent(
            context = this,
            mode = mode,
            launchSource = CaptureLaunchSource.QuickSettings,
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}

class VideoCaptureTileService : BaseCaptureTile() {
    override val mode: String = CaptureActivity.MODE_VIDEO
}

class AudioCaptureTileService : BaseCaptureTile() {
    override val mode: String = CaptureActivity.MODE_AUDIO
}
