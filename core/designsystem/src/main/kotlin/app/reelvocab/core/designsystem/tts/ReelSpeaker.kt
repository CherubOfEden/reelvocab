package app.reelvocab.core.designsystem.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale

interface ReelSpeaker {
    val speaking: State<String?>

    fun speak(text: String)

    fun stop()
}

val LocalReelSpeaker = staticCompositionLocalOf<ReelSpeaker> { NoopReelSpeaker }

object NoopReelSpeaker : ReelSpeaker {
    private val idle = mutableStateOf<String?>(null)

    override val speaking: State<String?> = idle

    override fun speak(text: String) = Unit

    override fun stop() = Unit
}

@Composable
fun ProvideReelSpeaker(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val speaker = remember(context) { AndroidReelSpeaker(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) speaker.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speaker.shutdown()
        }
    }

    CompositionLocalProvider(LocalReelSpeaker provides speaker, content = content)
}

private class AndroidReelSpeaker(context: Context) : ReelSpeaker, TextToSpeech.OnInitListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val speakingState = mutableStateOf<String?>(null)
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null
    private var activeUtteranceId: String? = null
    private var utteranceCounter = 0

    override val speaking: State<String?> = speakingState

    init {
        engine = TextToSpeech(context, this)
        engine?.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    finishUtterance(utteranceId)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    finishUtterance(utteranceId)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    finishUtterance(utteranceId)
                }
            },
        )
    }

    override fun onInit(status: Int) {
        mainHandler.post {
            val tts = engine
            if (status != TextToSpeech.SUCCESS || tts == null) {
                tts?.shutdown()
                engine = null
                ready = false
                pendingText = null
                activeUtteranceId = null
                speakingState.value = null
                return@post
            }
            val language = tts.setLanguage(Locale.US)
            if (language == TextToSpeech.LANG_MISSING_DATA ||
                language == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.setLanguage(Locale.getDefault())
            }
            ready = true
            pendingText?.let { text ->
                pendingText = null
                speakNow(text)
            }
        }
    }

    override fun speak(text: String) {
        val term = text.trim()
        if (term.isEmpty() || engine == null) return
        if (speakingState.value == term) {
            stop()
            return
        }
        stop()
        speakingState.value = term
        if (ready) {
            speakNow(term)
        } else {
            pendingText = term
        }
    }

    override fun stop() {
        pendingText = null
        activeUtteranceId = null
        engine?.stop()
        speakingState.value = null
    }

    fun shutdown() {
        stop()
        engine?.shutdown()
        engine = null
        ready = false
    }

    private fun speakNow(text: String) {
        val tts = engine
        if (tts == null) {
            activeUtteranceId = null
            speakingState.value = null
            return
        }
        val id = "reel-${++utteranceCounter}"
        activeUtteranceId = id
        val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result == TextToSpeech.ERROR) {
            activeUtteranceId = null
            speakingState.value = null
        }
    }

    private fun finishUtterance(utteranceId: String?) {
        mainHandler.post {
            if (utteranceId == null || utteranceId == activeUtteranceId) {
                activeUtteranceId = null
                speakingState.value = null
            }
        }
    }
}
