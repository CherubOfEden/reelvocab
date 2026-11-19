package app.reelvocab.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.reelvocab.MainActivity
import app.reelvocab.R
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var cards: CardRepository
    @Inject lateinit var dictionary: DictionaryRepository

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SNOOZE) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            ReminderScheduler.scheduleSnooze(context)
            return
        }
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        if (!snooze && !ReminderPrefs.isEnabled(context)) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (Settings.canDrawOverlays(context)) {
            MorningOverlayService.start(context)
        }

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val review = loadReview(context)
                showNotification(context, review)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun loadReview(context: Context): ReviewWord {
        val cardList = runCatching { cards.observeCards().first() }.getOrDefault(emptyList())
        val card = cardList.firstOrNull()
        if (card != null) {
            val entry = runCatching { dictionary.lookup(card.headword) }.getOrNull()
            return ReviewWord(
                word = card.headword,
                definition = entry?.senses?.firstOrNull()?.definition ?: card.meaning,
            )
        }
        val entry = runCatching { dictionary.wordOfTheDay() }.getOrNull()
        return ReviewWord(
            word = entry?.lemma ?: context.getString(R.string.reminder_title),
            definition = entry?.senses?.firstOrNull()?.definition
                ?: context.getString(R.string.reminder_body),
        )
    }

    @SuppressLint("MissingPermission")
    private fun showNotification(context: Context, review: ReviewWord) {
        createChannel(context)

        val reviewIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        val reviewPendingIntent = PendingIntent.getActivity(
            context,
            REVIEW_REQUEST_CODE,
            reviewIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            SNOOZE_REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_word_title, review.word))
            .setContentText(review.definition)
            .setStyle(NotificationCompat.BigTextStyle().bigText(review.definition))
            .setContentIntent(reviewPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.reminder_overlay_snooze), snoozeIntent)
            .addAction(0, context.getString(R.string.reminder_overlay_review), reviewPendingIntent)

        if (
            ReminderPrefs.fullScreen(context) &&
            FullScreenAlarms.isAllowed(context) &&
            !Settings.canDrawOverlays(context)
        ) {
            builder.setFullScreenIntent(reviewPendingIntent, true)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        channel.description = context.getString(R.string.reminder_channel_description)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private data class ReviewWord(
        val word: String,
        val definition: String,
    )

    companion object {
        const val EXTRA_SNOOZE = "app.reelvocab.extra.SNOOZE"
        private const val ACTION_SNOOZE = "app.reelvocab.reminder.SNOOZE"
        private const val CHANNEL_ID = "morning_alarm"
        private const val LEGACY_CHANNEL_ID = "morning_review"
        private const val NOTIFICATION_ID = 8201
        private const val REVIEW_REQUEST_CODE = 8202
        private const val SNOOZE_REQUEST_CODE = 8203
    }
}
