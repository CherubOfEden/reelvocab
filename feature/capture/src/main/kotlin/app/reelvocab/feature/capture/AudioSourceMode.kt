package app.reelvocab.feature.capture

enum class AudioSourceMode(val storage: String) {
    Internal("internal"),
    Microphone("microphone"),
    Both("both"),
    ;

    companion object {
        fun fromStorage(value: String?): AudioSourceMode =
            entries.firstOrNull { it.storage == value } ?: Internal
    }
}
