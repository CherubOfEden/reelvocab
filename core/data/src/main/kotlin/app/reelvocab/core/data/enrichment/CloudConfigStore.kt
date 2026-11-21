package app.reelvocab.core.data.enrichment

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudConfigStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("cloud_enrichment", Context.MODE_PRIVATE)

    init {
        prefs.edit()
            .remove(LEGACY_KEY_ENDPOINT)
            .remove(LEGACY_KEY_MODEL)
            .remove(LEGACY_KEY_API_KEY)
            .remove(LEGACY_KEY_SERVER_URL)
            .apply()
    }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val LEGACY_KEY_ENDPOINT = "endpoint"
        const val LEGACY_KEY_MODEL = "model"
        const val LEGACY_KEY_API_KEY = "api_key"
        const val LEGACY_KEY_SERVER_URL = "server_url"
    }
}
