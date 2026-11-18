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
    private const val MORNING_HOUR = 8

    fun scheduleIfEnabled(context: Context) {
        if (ReminderPrefs.isEnabled(context)) schedule(context)
    }

    fun schedule(context: Context) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        runCatching {
            manager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                nextMorningEpochMs(),
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

    private fun nextMorningEpochMs(
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = ZonedDateTime.now(zone)
        val next = if (now.toLocalTime() < LocalTime.of(MORNING_HOUR, 0)) {
            now.toLocalDate().atTime(MORNING_HOUR, 0)
        } else {
            now.toLocalDate().plusDays(1).atTime(MORNING_HOUR, 0)
        }
        return Instant.from(next.atZone(zone)).toEpochMilli()
    }
}
