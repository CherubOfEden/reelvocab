package app.reelvocab.feature.capture

enum class CaptureLaunchSource(val storage: String) {
    App("app"),
    QuickSettings("quick_settings"),
    Pip("pip"),
    ;

    companion object {
        fun fromStorage(value: String?): CaptureLaunchSource =
            entries.firstOrNull { it.storage == value } ?: App
    }
}
