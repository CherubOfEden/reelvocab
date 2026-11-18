package app.reelvocab.reminders

import android.content.Context
import java.util.Locale

object ReminderPrefs {

    private const val PREFS = "reminders"
    private const val KEY_MORNING_ENABLED = "morning_enabled"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"

    const val DEFAULT_HOUR = 8
    const val DEFAULT_MINUTE = 0

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MORNING_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MORNING_ENABLED, enabled).apply()
    }

    fun hour(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_HOUR, DEFAULT_HOUR).coerceIn(0, 23)

    fun minute(context: Context): Int =
        prefs(context).getInt(KEY_MORNING_MINUTE, DEFAULT_MINUTE).coerceIn(0, 59)

    fun setTime(context: Context, hour: Int, minute: Int) {
        prefs(context)
            .edit()
            .putInt(KEY_MORNING_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_MORNING_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun timeLabel(context: Context): String =
        String.format(Locale.US, "%02d:%02d", hour(context), minute(context))

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
