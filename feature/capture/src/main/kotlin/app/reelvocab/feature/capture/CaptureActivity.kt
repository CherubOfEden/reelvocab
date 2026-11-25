package app.reelvocab.feature.capture

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import app.reelvocab.core.designsystem.theme.ReelTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CaptureActivity : ComponentActivity() {

    private var recordingMode: String? = null
    private var selectedAudioSource = AudioSourceMode.Internal
    private var launchSource = CaptureLaunchSource.App
    private var sourceDialog: AlertDialog? = null
    private var pendingCrop: RectF? = null
    private var recordingCrop: RectF? = null
    private var cropOverlayVisible by mutableStateOf(false)
    private var cropDoneAction: ((RectF) -> Unit)? = null
    private var cropCancelAction: (() -> Unit)? = null
    private var recordingLaunched = false
    private var awaitingExternalResult = false

    private val consentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        awaitingExternalResult = false
        val data = result.data
        val mode = recordingMode
        if (mode == null) {
            if (result.resultCode == RESULT_OK && data != null) {
                CaptureSessionService.startSession(
                    context = this,
                    resultCode = result.resultCode,
                    data = data,
                    captureImmediately = true,
                    crop = pendingCrop,
                )
            }
            finish()
            return@registerForActivityResult
        }

        if (result.resultCode == RESULT_OK && data != null) {
            startRecordingService(result.resultCode, data, selectedAudioSource)
        } else if (mode == MODE_AUDIO && selectedAudioSource != AudioSourceMode.Microphone) {
            Toast.makeText(this, R.string.recording_internal_fallback, Toast.LENGTH_LONG).show()
            startRecordingService(0, null, AudioSourceMode.Microphone)
        } else {
            Toast.makeText(this, R.string.recording_screen_permission_required, Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    private val audioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        awaitingExternalResult = false
        val mode = recordingMode ?: return@registerForActivityResult
        if (!granted) {
            if (mode == MODE_AUDIO) {
                Toast.makeText(this, R.string.capture_permission_audio, Toast.LENGTH_LONG).show()
                finish()
                return@registerForActivityResult
            }
            Toast.makeText(this, R.string.recording_video_without_audio, Toast.LENGTH_LONG).show()
        }
        requestProjectionOrStart()
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
            recordingMode = mode
            launchSource = launchSourceFrom(intent)
            intent.getStringExtra(EXTRA_AUDIO_SOURCE)?.let { stored ->
                selectedAudioSource = AudioSourceMode.fromStorage(stored)
            }
            if (mode == MODE_VIDEO && shouldShowCropOverlay()) {
                showCropOverlay(
                    onDone = { crop ->
                        recordingCrop = crop
                        startRecordingSetup()
                    },
                    onCancel = { finish() },
                )
            } else {
                startRecordingSetup()
            }
            return
        }
        if (intent.getBooleanExtra(EXTRA_SESSION_CROP, false)) {
            if (shouldShowCropOverlay()) {
                showCropOverlay(
                    onDone = { crop ->
                        CaptureSessionService.captureWithCrop(this, crop)
                        finish()
                    },
                    onCancel = {
                        finish()
                    },
                )
            } else {
                CaptureSessionService.captureWithCrop(this, null)
                finish()
            }
            return
        }
        if (shouldShowCropOverlay()) {
            showCropOverlay(
                onDone = { crop ->
                    pendingCrop = crop
                    requestConsent()
                },
                onCancel = { finish() },
            )
        } else {
            requestConsent()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isChangingConfigurations || isFinishing || awaitingExternalResult) return
        abandonTransientCapture()
    }

    override fun onDestroy() {
        sourceDialog?.dismiss()
        sourceDialog = null
        if (
            !recordingLaunched &&
            recordingMode != null &&
            CaptureSessionService.isRunning
        ) {
            CaptureSessionService.cropOverlayCancelled(this)
        }
        super.onDestroy()
    }

    private fun abandonTransientCapture() {
        cropOverlayVisible = false
        pendingCrop = null
        recordingCrop = null
        cropDoneAction = null
        cropCancelAction = null
        sourceDialog?.dismiss()
        sourceDialog = null
        if (CaptureSessionService.isRunning) {
            CaptureSessionService.cropOverlayCancelled(this)
            CaptureSessionService.releaseCapture(this)
        }
        if (!isFinishing) finishAndRemoveTask()
    }

    private fun shouldShowCropOverlay(): Boolean =
        CapturePreferences.showCropOverlay(this)

    private fun startRecordingSetup() {
        if (intent.getBooleanExtra(EXTRA_SKIP_SOURCE_DIALOG, false)) {
            continueRecordingSetup()
        } else {
            showAudioSourceDialog()
        }
    }

    private fun showCropOverlay(
        onDone: (RectF) -> Unit,
        onCancel: () -> Unit,
    ) {
        cropDoneAction = onDone
        cropCancelAction = onCancel
        cropOverlayVisible = true
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        if (CaptureSessionService.isRunning) {
            CaptureSessionService.cropOverlayStarted(this)
        }
        setContent {
            ReelTheme {
                if (cropOverlayVisible) {
                    CropOverlay(
                        onDone = { crop ->
                            cropOverlayVisible = false
                            val action = cropDoneAction
                            cropDoneAction = null
                            cropCancelAction = null
                            action?.invoke(normalizedCrop(crop))
                        },
                        onCancel = {
                            cropOverlayVisible = false
                            if (CaptureSessionService.isRunning) {
                                CaptureSessionService.cropOverlayCancelled(this)
                            }
                            val action = cropCancelAction
                            cropDoneAction = null
                            cropCancelAction = null
                            action?.invoke()
                        },
                    )
                }
            }
        }
    }

    private fun showAudioSourceDialog() {
        val current = CapturePreferences.audioSource(this)
        var choice = current
        val labels = arrayOf(
            getString(R.string.recording_audio_source_internal),
            getString(R.string.recording_audio_source_microphone),
            getString(R.string.recording_audio_source_both),
        )
        val selectedIndex = when (current) {
            AudioSourceMode.Internal -> 0
            AudioSourceMode.Microphone -> 1
            AudioSourceMode.Both -> 2
        }
        sourceDialog = AlertDialog.Builder(this)
            .setTitle(R.string.recording_audio_source_title)
            .setSingleChoiceItems(labels, selectedIndex) { _, which ->
                choice = when (which) {
                    1 -> AudioSourceMode.Microphone
                    2 -> AudioSourceMode.Both
                    else -> AudioSourceMode.Internal
                }
            }
            .setPositiveButton(R.string.recording_audio_source_confirm) { dialog, _ ->
                selectedAudioSource = choice
                CapturePreferences.setAudioSource(this, choice)
                sourceDialog = null
                dialog.dismiss()
                continueRecordingSetup()
            }
            .setNegativeButton(R.string.capture_cancel) { dialog, _ ->
                sourceDialog = null
                dialog.dismiss()
                finish()
            }
            .setOnCancelListener {
                sourceDialog = null
                finish()
            }
            .create()
        sourceDialog?.show()
    }

    private fun continueRecordingSetup() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            awaitingExternalResult = true
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            requestProjectionOrStart()
        }
    }

    private fun requestProjectionOrStart() {
        val mode = recordingMode ?: return
        val needsProjection = mode == MODE_VIDEO || selectedAudioSource != AudioSourceMode.Microphone
        if (needsProjection) {
            requestConsent()
        } else {
            startRecordingService(0, null, selectedAudioSource)
        }
    }

    private fun requestConsent() {
        val manager = getSystemService(MediaProjectionManager::class.java)
        awaitingExternalResult = true
        consentLauncher.launch(manager.createScreenCaptureIntent())
    }

    private fun startRecordingService(
        resultCode: Int,
        data: Intent?,
        source: AudioSourceMode,
    ) {
        val mode = recordingMode ?: return
        recordingLaunched = true
        RecordingService.start(
            context = this,
            resultCode = resultCode,
            data = data,
            mode = mode,
            audioSource = source,
            launchSource = launchSource,
            crop = recordingCrop,
        )
        finish()
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
                    onSaved = { finishAndRemoveTask() },
                )
            }
        }
    }

    companion object {
        private const val EXTRA_IMAGE_PATH = "image_path"
        private const val EXTRA_RECORD_MODE = "record_mode"
        const val EXTRA_START_FROM_QUICK_SETTINGS = "start_from_quick_settings"
        const val EXTRA_START_FROM_PIP = "start_from_pip"
        private const val EXTRA_AUDIO_SOURCE = "audio_source"
        private const val EXTRA_SKIP_SOURCE_DIALOG = "skip_source_dialog"
        const val EXTRA_SESSION_CROP = "session_crop"
        const val MODE_VIDEO = "video"
        const val MODE_AUDIO = "audio"

        fun consentIntent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)

        fun cropSessionIntent(context: Context): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_SESSION_CROP, true)

        fun imageIntent(context: Context, imagePath: String): Intent =
            Intent(context, CaptureActivity::class.java)
                .putExtra(EXTRA_IMAGE_PATH, imagePath)

        fun recordIntent(
            context: Context,
            mode: String,
            launchSource: CaptureLaunchSource = CaptureLaunchSource.App,
            audioSource: AudioSourceMode? = null,
            skipSourceDialog: Boolean = false,
        ): Intent = Intent(context, CaptureActivity::class.java).apply {
            putExtra(EXTRA_RECORD_MODE, mode)
            when (launchSource) {
                CaptureLaunchSource.QuickSettings ->
                    putExtra(EXTRA_START_FROM_QUICK_SETTINGS, true)

                CaptureLaunchSource.Pip -> putExtra(EXTRA_START_FROM_PIP, true)
                CaptureLaunchSource.App -> Unit
            }
            if (audioSource != null) {
                putExtra(EXTRA_AUDIO_SOURCE, audioSource.storage)
                putExtra(EXTRA_SKIP_SOURCE_DIALOG, skipSourceDialog)
            }
        }

        private fun launchSourceFrom(intent: Intent): CaptureLaunchSource = when {
            intent.getBooleanExtra(EXTRA_START_FROM_QUICK_SETTINGS, false) ->
                CaptureLaunchSource.QuickSettings

            intent.getBooleanExtra(EXTRA_START_FROM_PIP, false) -> CaptureLaunchSource.Pip
            else -> CaptureLaunchSource.App
        }
    }
}
