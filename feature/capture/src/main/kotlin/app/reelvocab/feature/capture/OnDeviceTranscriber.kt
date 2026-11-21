package app.reelvocab.feature.capture

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class OnDeviceTranscriber(
    private val context: Context,
) {
    private val main = Handler(Looper.getMainLooper())

    suspend fun transcribe(segment: SpeechSegment): SpeechTranscript? {
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return null
        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val recognizer = try {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } catch (error: Exception) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                val pipe = try {
                    ParcelFileDescriptor.createPipe()
                } catch (error: Exception) {
                    recognizer.destroy()
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                val readSide = pipe[0]
                val writeSide = pipe[1]
                val finished = AtomicBoolean(false)
                var partial = ""
                var timeoutPosted = false

                fun finish(result: SpeechTranscript?) {
                    if (!finished.compareAndSet(false, true)) return
                    main.removeCallbacksAndMessages(null)
                    runCatching { recognizer.stopListening() }
                    runCatching { recognizer.destroy() }
                    runCatching { readSide.close() }
                    runCatching { writeSide.close() }
                    if (continuation.isActive) continuation.resume(result)
                }

                fun finishOnMain(result: SpeechTranscript?) {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        finish(result)
                    } else {
                        main.post { finish(result) }
                    }
                }

                recognizer.setRecognitionListener(
                    object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) = Unit
                        override fun onBeginningOfSpeech() = Unit
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit

                        override fun onError(error: Int) {
                            finishOnMain(
                                partial.takeIf { it.isNotBlank() }?.let {
                                    SpeechTranscript(it, PARTIAL_CONFIDENCE)
                                },
                            )
                        }

                        override fun onResults(results: Bundle?) {
                            val text = bestResult(results ?: Bundle())
                                ?: partial.takeIf { it.isNotBlank() }
                            finishOnMain(
                                text?.let {
                                    SpeechTranscript(
                                        text,
                                        confidence(results ?: Bundle(), PARTIAL_CONFIDENCE),
                                    )
                                },
                            )
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            bestResult(partialResults ?: Bundle())?.let { partial = it }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    },
                )

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readSide)
                    putExtra(
                        RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT,
                        segment.channelCount,
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING,
                        android.media.AudioFormat.ENCODING_PCM_16BIT,
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE,
                        segment.sampleRate,
                    )
                }

                try {
                    recognizer.startListening(intent)
                } catch (error: Exception) {
                    finish(null)
                    return@suspendCancellableCoroutine
                }

                Thread {
                    runCatching {
                        ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { output ->
                            output.write(segment.pcm)
                            output.flush()
                        }
                    }
                }.start()

                if (!timeoutPosted) {
                    timeoutPosted = true
                    main.postDelayed(
                        {
                            finishOnMain(
                                partial.takeIf { it.isNotBlank() }?.let {
                                    SpeechTranscript(it, PARTIAL_CONFIDENCE)
                                },
                            )
                        },
                        TIMEOUT_MS,
                    )
                }

                continuation.invokeOnCancellation { finishOnMain(null) }
            }
        }
    }

    private fun bestResult(results: Bundle): String? =
        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull { it.isNotBlank() }
            ?.trim()

    private fun confidence(results: Bundle, fallback: Float): Float {
        val scores = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        if (scores == null || scores.isEmpty()) return fallback
        val positive = scores.filter { it > 0f }
        if (positive.isEmpty()) return fallback
        return positive.average().toFloat().coerceIn(0f, 1f)
    }

    private companion object {
        const val TIMEOUT_MS = 12_000L
        const val PARTIAL_CONFIDENCE = 0.65f
    }
}
