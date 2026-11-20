package app.reelvocab.core.data.enrichment

import android.content.Context
import app.reelvocab.core.common.ReelVocabServer
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
            .apply()
    }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var serverUrl: String
        get() = prefs.getString(KEY_SERVER_URL, ReelVocabServer.DEFAULT_URL).orEmpty()
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value.trim()).apply()

    val isConfigured: Boolean
        get() = enabled && serverUrl.isNotBlank()

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_SERVER_URL = "server_url"
        const val LEGACY_KEY_ENDPOINT = "endpoint"
        const val LEGACY_KEY_MODEL = "model"
        const val LEGACY_KEY_API_KEY = "api_key"
    }
}
