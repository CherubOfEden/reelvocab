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
}
