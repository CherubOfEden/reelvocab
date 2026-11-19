package app.reelvocab.capture

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import app.reelvocab.feature.capture.CaptureActivity

abstract class BaseCaptureTile : TileService() {

    protected abstract val mode: String

    override fun onClick() {
        super.onClick()
        val intent = CaptureActivity.recordIntent(this, mode)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
