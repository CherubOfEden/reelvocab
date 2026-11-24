package app.reelvocab.reminders

import android.content.Context
import java.util.Locale

enum class ReminderTimeFormat { TwelveHour, TwentyFourHour }

object ReminderPrefs {

    private const val PREFS = "reminders"
    private const val KEY_MORNING_ENABLED = "morning_enabled"
    private const val KEY_MORNING_HOUR = "morning_hour"
    private const val KEY_MORNING_MINUTE = "morning_minute"
    private const val KEY_FULL_SCREEN = "morning_full_screen"
    private const val KEY_TIME_FORMAT = "reminder_time_format"

    const val DEFAULT_HOUR = 9
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

    fun timeFormat(context: Context): ReminderTimeFormat =
        runCatching {
            ReminderTimeFormat.valueOf(
                prefs(context).getString(KEY_TIME_FORMAT, null)
                    ?: ReminderTimeFormat.TwelveHour.name,
            )
        }.getOrDefault(ReminderTimeFormat.TwelveHour)

    fun is24Hour(context: Context): Boolean =
        timeFormat(context) == ReminderTimeFormat.TwentyFourHour

    fun setTimeFormat(context: Context, format: ReminderTimeFormat) {
        prefs(context).edit().putString(KEY_TIME_FORMAT, format.name).apply()
    }

    fun fullScreen(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FULL_SCREEN, true)

    fun setFullScreen(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FULL_SCREEN, enabled).apply()
    }

    fun timeLabel(context: Context): String =
        formatTime(hour(context), minute(context), is24Hour(context))

    fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String {
        val safeHour = hour.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)
        if (is24Hour) {
            return String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)
        }
        val suffix = if (safeHour < 12) "AM" else "PM"
        val displayHour = when (val twelve = safeHour % 12) {
            0 -> 12
            else -> twelve
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, safeMinute, suffix)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
