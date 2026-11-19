package app.reelvocab.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.reelvocab.MainActivity
import app.reelvocab.R
import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.ReviewRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MorningOverlayService : Service() {

    @Inject lateinit var cards: CardRepository
    @Inject lateinit var reviews: ReviewRepository
    @Inject lateinit var dictionary: DictionaryRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private var overlay: View? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        startForeground()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        scope.launch {
            val review = loadReview()
            showOverlay(review)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        removeOverlay()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun loadReview(): ReviewWord {
        val now = System.currentTimeMillis()
        val all = safe { cards.observeCards().first() }.orEmpty()
        val due = safe { reviews.observeDueCardIds(now).first() }.orEmpty()
        val card = all.firstOrNull { it.id in due } ?: all.firstOrNull()
        if (card != null) {
            val entry = safe { dictionary.lookup(card.headword) }
            return ReviewWord(
                word = card.headword,
                definition = entry?.senses?.firstOrNull()?.definition ?: card.meaning,
                sentence = card.contextLine,
                synonyms = entry?.thesaurus?.synonyms.orEmpty(),
                antonyms = entry?.thesaurus?.antonyms.orEmpty(),
            )
        }
        val entry = safe { dictionary.wordOfTheDay() }
        return ReviewWord(
            word = entry?.lemma ?: getString(R.string.reminder_no_words),
            definition = entry?.senses?.firstOrNull()?.definition
                ?: getString(R.string.reminder_no_words_body),
            sentence = null,
            synonyms = entry?.thesaurus?.synonyms.orEmpty(),
            antonyms = entry?.thesaurus?.antonyms.orEmpty(),
        )
    }

    private suspend fun <T> safe(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        null
    }

    private fun showOverlay(review: ReviewWord) {
        removeOverlay()
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { }
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#17181C"))
                cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat(), dp(28).toFloat())
            }
            elevation = dp(12).toFloat()
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(label(getString(R.string.reminder_overlay_kicker), 12, "#E0B15C", bold = true))
        content.addView(label(review.word, 30, "#F5F1E8", bold = true))
        content.addView(label(review.definition, 15, "#C9C3B8"))
        review.sentence?.let { sentence ->
            content.addView(label("\u201C$sentence\u201D", 14, "#9A958C"))
        }
        if (review.synonyms.isNotEmpty()) {
            content.addView(
                label(
                    getString(R.string.reminder_overlay_synonyms, review.synonyms.take(4).joinToString(", ")),
                    12,
                    "#64C7B8",
                ),
            )
        }
        if (review.antonyms.isNotEmpty()) {
            content.addView(
                label(
                    getString(R.string.reminder_overlay_antonyms, review.antonyms.take(4).joinToString(", ")),
                    12,
                    "#E58C8C",
                ),
            )
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(14), 0, 0)
        }
        actions.addView(actionButton(getString(R.string.reminder_overlay_snooze), filled = false) {
            ReminderScheduler.scheduleSnooze(this@MorningOverlayService)
            stopSelf()
        })
        actions.addView(actionButton(getString(R.string.reminder_overlay_review), filled = true) {
            ReminderScheduler.cancelSnooze(this@MorningOverlayService)
            startActivity(
                Intent(this@MorningOverlayService, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_REVIEW, true)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP,
                    ),
            )
            stopSelf()
        })
        content.addView(actions)
        scroll.addView(content)
        panel.addView(scroll)

        root.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.32f).toInt(),
                Gravity.TOP,
            ),
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        runCatching { windowManager.addView(root, params) }
            .onSuccess { overlay = root }
            .onFailure { stopSelf() }
    }

    private fun label(text: String, sp: Int, color: String, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sp.toFloat()
            setTextColor(Color.parseColor(color))
            setPadding(0, (4 * resources.displayMetrics.density).toInt(), 0, 0)
            maxLines = if (sp >= 20) 2 else 4
            ellipsize = TextUtils.TruncateAt.END
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun actionButton(text: String, filled: Boolean, onClick: () -> Unit): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(28, 18, 28, 18)
            setTextColor(if (filled) Color.parseColor("#17181C") else Color.parseColor("#E0B15C"))
            background = GradientDrawable().apply {
                cornerRadius = 24f * resources.displayMetrics.density
                setColor(
                    if (filled) Color.parseColor("#E0B15C") else Color.parseColor("#24252A"),
                )
            }
            setOnClickListener { onClick() }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            params.marginStart = (8 * resources.displayMetrics.density).toInt()
            layoutParams = params
        }

    private fun removeOverlay() {
        val view = overlay ?: return
        overlay = null
        runCatching { windowManager.removeView(view) }
    }

    private fun startForeground() {
        ensureChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.reminder_overlay_kicker))
            .setContentText(getString(R.string.reminder_overlay_waiting))
            .setOngoing(true)
            .setSilent(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private data class ReviewWord(
        val word: String,
        val definition: String,
        val sentence: String?,
        val synonyms: List<String>,
        val antonyms: List<String>,
    )

    companion object {
        private const val CHANNEL_ID = "morning_overlay"
        private const val NOTIFICATION_ID = 8210

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            ContextCompat.startForegroundService(
                context,
                Intent(context, MorningOverlayService::class.java),
            )
        }
    }
}
