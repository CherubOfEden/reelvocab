package app.reelvocab.reminders

import android.content.Context

object ReminderPrefs {

    private const val PREFS = "reminders"
    private const val KEY_MORNING_ENABLED = "morning_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_MORNING_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_MORNING_ENABLED, enabled)
            .apply()
    }
}
