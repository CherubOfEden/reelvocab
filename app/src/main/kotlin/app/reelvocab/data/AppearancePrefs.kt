package app.reelvocab.data

import android.content.Context
import app.reelvocab.feature.settings.ThemeMode

object AppearancePrefs {

    private const val PREFS = "appearance"
    private const val KEY_THEME = "theme_mode"

    fun themeMode(context: Context): ThemeMode {
        val stored = prefs(context).getString(KEY_THEME, null) ?: return ThemeMode.System
        return runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.System)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
