package app.reelvocab.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object ReminderScheduler {

    private const val REQUEST_CODE = 8001

    fun scheduleIfEnabled(context: Context) {
        if (ReminderPrefs.isEnabled(context)) schedule(context)
    }

    fun schedule(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        val hour = ReminderPrefs.hour(context)
        val minute = ReminderPrefs.minute(context)
        runCatching {
            manager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextReminderEpochMs(hour, minute),
                AlarmManager.INTERVAL_DAY,
                pending,
            )
        }
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        manager?.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context, flags: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextReminderEpochMs(
        hour: Int,
        minute: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = ZonedDateTime.now(zone)
        val target = LocalTime.of(hour, minute)
        val next = if (now.toLocalTime() < target) {
            now.toLocalDate().atTime(target)
        } else {
            now.toLocalDate().plusDays(1).atTime(target)
        }
        return Instant.from(next.atZone(zone)).toEpochMilli()
    }
}
