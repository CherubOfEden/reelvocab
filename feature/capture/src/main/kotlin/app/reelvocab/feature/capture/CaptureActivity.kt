package app.reelvocab.feature.capture

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import app.reelvocab.core.designsystem.theme.ReelTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CaptureActivity : ComponentActivity() {

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            CaptureSessionService.startSession(
                context = this,
                resultCode = result.resultCode,
                data = data,
                captureImmediately = true,
            )
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val imagePath = intent.getStringExtra(EXTRA_IMAGE_PATH)
        if (imagePath != null) {
            showCapture(imagePath)
        } else {
            requestConsent()
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
                    onSaved = {
                        Toast.makeText(
                            this@CaptureActivity,
                            R.string.capture_saved,
                            Toast.LENGTH_SHORT,
                        ).show()
                        finishAndRemoveTask()
                    },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "image_path"

        fun consentIntent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)

        fun imageIntent(context: Context, imagePath: String): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)
    }
}
