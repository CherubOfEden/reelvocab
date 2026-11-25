package app.reelvocab.feature.capture

import android.content.Context

enum class PipCaptureMode(val storage: String) {
    WordsOnly("words"),
    Video("video"),
    Audio("audio"),
    All("all"),
    ;

    companion object {
        fun fromStorage(value: String?): PipCaptureMode =
            entries.firstOrNull { it.storage == value } ?: WordsOnly
    }
}

object CapturePreferences {

    private const val PREFS = "capture_prefs"
    private const val KEY_PIP_MODE = "pip_mode"
    private const val KEY_AUDIO_SOURCE = "audio_source"
    private const val KEY_CROP_OVERLAY = "crop_overlay"

    fun pipMode(context: Context): PipCaptureMode =
        PipCaptureMode.fromStorage(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PIP_MODE, null),
        )

    fun setPipMode(context: Context, mode: PipCaptureMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PIP_MODE, mode.storage)
            .apply()
    }

    fun audioSource(context: Context): AudioSourceMode =
        AudioSourceMode.fromStorage(
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_AUDIO_SOURCE, null),
        )

    fun setAudioSource(context: Context, source: AudioSourceMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUDIO_SOURCE, source.storage)
            .apply()
    }

    fun showCropOverlay(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_CROP_OVERLAY, false)

    fun setShowCropOverlay(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CROP_OVERLAY, enabled)
            .apply()
    }
}
