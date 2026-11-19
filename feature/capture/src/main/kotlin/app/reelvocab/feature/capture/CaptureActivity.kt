package app.reelvocab.feature.capture

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import app.reelvocab.core.designsystem.theme.ReelTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CaptureActivity : ComponentActivity() {

    private var recordingMode: String? = null

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val mode = recordingMode
            if (mode != null) {
                CaptureSessionService.startRecording(
                    context = this,
                    resultCode = result.resultCode,
                    data = data,
                    mode = mode,
                )
            } else {
                CaptureSessionService.startSession(
                    context = this,
                    resultCode = result.resultCode,
                    data = data,
                    captureImmediately = true,
                )
            }
        }
        finish()
    }

    private val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val mode = recordingMode ?: return@registerForActivityResult
        if (!granted && mode == MODE_AUDIO) {
            Toast.makeText(this, R.string.capture_permission_audio, Toast.LENGTH_LONG).show()
            finish()
            return@registerForActivityResult
        }
        if (mode == MODE_VIDEO) {
            requestConsent()
        } else {
            CaptureSessionService.startRecording(
                context = this,
                resultCode = 0,
                data = null,
                mode = mode,
            )
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            showCapture(imagePath)
            return
        }
        val mode = intent.getStringExtra(EXTRA_RECORD_MODE)
        if (mode != null) {
            beginRecording(mode)
            return
        }
        requestConsent()
    }

    private fun beginRecording(mode: String) {
        recordingMode = mode
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else if (mode == MODE_VIDEO) {
            requestConsent()
        } else {
            CaptureSessionService.startRecording(
                context = this,
                resultCode = 0,
                data = null,
                mode = mode,
            )
            finish()
        }
    }

    private fun requestConsent() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        consentLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun showCapture(imagePath: String) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        File(imagePath).delete()
        if (bitmap == null) {
            finish()
            return
        }
        setContent {
            ReelTheme {
                InAppCapture(
                    bitmap = bitmap,
                    sourceTitle = getString(R.string.capture_screen_source),
                    timestampMs = 0L,
                    onDismiss = { finishAndRemoveTask() },
                    onSaved = { },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "image_path"
        private const val EXTRA_RECORD_MODE = "record_mode"
        const val MODE_VIDEO = "video"
        const val MODE_AUDIO = "audio"

        fun consentIntent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)

        fun imageIntent(context: Context, imagePath: String): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)

        fun recordIntent(context: Context, mode: String): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_RECORD_MODE, mode)
    }
}
