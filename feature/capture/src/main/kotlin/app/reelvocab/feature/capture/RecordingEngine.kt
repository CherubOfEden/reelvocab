package app.reelvocab.feature.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.view.Surface
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque
import kotlin.math.sqrt

enum class RecordingKind { Video, Audio }

interface RecordingListener {
    fun onLevel(level: Float)
    fun onSpeechSegment(segment: SpeechSegment)
    fun onError(error: Throwable)
    fun onAudioSourceChanged(requested: AudioSourceMode, active: AudioSourceMode)
}

class MediaRecordingEngine(
    private val context: Context,
    private val kind: RecordingKind,
    private val projection: MediaProjection?,
    private val requestedAudioSource: AudioSourceMode,
    private val outputFile: File,
    private val listener: RecordingListener,
) {
    private val lock = Any()
    private val sink = MuxerSink(outputFile)

    private var startUs = System.nanoTime() / 1000L
    private var started = false
    private var finished = false

    @Volatile private var paused = false
    @Volatile private var stopping = false
    @Volatile private var pausedAccumUs = 0L
    private var pauseStartUs = 0L

    private var videoCodec: MediaCodec? = null
    private var videoSurface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var videoThread: Thread? = null
    private var firstVideoPtsUs = -1L

    @Volatile private var audioInputs: List<AudioInput> = emptyList()
    private var audioCodec: MediaCodec? = null
    private var audioThread: Thread? = null
    private var audioPtsUs = 0L

    @Volatile var activeAudioSource: AudioSourceMode = requestedAudioSource
        private set

    fun start() {
        if (started) return
        started = true
        startUs = System.nanoTime() / 1000L
        try {
            if (kind == RecordingKind.Video) {
                prepareVideo()
            }

            val audioReady = prepareAudio()
            if (kind == RecordingKind.Audio && !audioReady) {
                throw IOException("Audio capture could not start")
            }

            sink.configure(
                expectVideo = kind == RecordingKind.Video,
                expectAudio = audioReady,
            )

            if (kind == RecordingKind.Video) {
                startVideoCapture()
            }
            if (audioReady) {
                startAudioCapture()
                if (activeAudioSource != requestedAudioSource) {
                    listener.onAudioSourceChanged(requestedAudioSource, activeAudioSource)
                }
            }
        } catch (error: Throwable) {
            cleanup()
            throw error
        }
    }

    fun pause() {
        synchronized(lock) {
            if (paused || stopping || finished) return
            paused = true
            pauseStartUs = System.nanoTime() / 1000L
            runCatching { virtualDisplay?.setSurface(null) }
        }
    }

    fun resume() {
        synchronized(lock) {
            if (!paused || stopping || finished) return
            val now = System.nanoTime() / 1000L
            pausedAccumUs += (now - pauseStartUs).coerceAtLeast(0L)
            paused = false
            runCatching { virtualDisplay?.setSurface(videoSurface) }
        }
    }

    fun elapsedMs(): Long = synchronized(lock) {
        if (!started) return 0L
        val endUs = if (paused) pauseStartUs else System.nanoTime() / 1000L
        ((endUs - startUs - pausedAccumUs) / 1000L).coerceAtLeast(0L)
    }

    fun isPaused(): Boolean = paused

    @Synchronized
    fun stop(): Boolean {
        if (finished) return false
        finished = true
        stopping = true
        paused = false
        runCatching { virtualDisplay?.setSurface(null) }
        runCatching { videoCodec?.signalEndOfInputStream() }
        audioInputs.forEach { input -> runCatching { input.record.stop() } }

        audioThread?.let { thread -> runCatching { thread.join(2_500) } }
        if (audioThread?.isAlive == true) {
            releaseAudio()
            runCatching { audioThread?.join(500) }
        }

        videoThread?.let { thread -> runCatching { thread.join(2_500) } }
        if (videoThread?.isAlive == true) {
            runCatching { videoCodec?.stop() }
            runCatching { videoThread?.join(500) }
        }

        releaseVideo()
        releaseAudio()
        return sink.finish()
    }

    private fun cleanup() {
        releaseAudio()
        releaseVideo()
        sink.finish()
    }

    @SuppressLint("MissingPermission")
    private fun prepareVideo() {
        val metrics = context.resources.displayMetrics
        val width = ((metrics.widthPixels / 16) * 16).coerceAtLeast(16)
        val height = ((metrics.heightPixels / 16) * 16).coerceAtLeast(16)
        val format = MediaFormat.createVideoFormat(VIDEO_MIME, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
            )
            setInteger(MediaFormat.KEY_BIT_RATE, 6_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, 30)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec = MediaCodec.createEncoderByType(VIDEO_MIME).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        }
        val surface = codec.createInputSurface()
        codec.start()
        videoCodec = codec
        videoSurface = surface
    }

    private fun startVideoCapture() {
        val codec = videoCodec ?: throw IOException("Video encoder unavailable")
        val surface = videoSurface ?: throw IOException("Video surface unavailable")
        val mediaProjection = projection ?: throw IOException("Screen permission missing")
        val metrics = context.resources.displayMetrics
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "reelvocab-recording",
            ((metrics.widthPixels / 16) * 16).coerceAtLeast(16),
            ((metrics.heightPixels / 16) * 16).coerceAtLeast(16),
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface,
            null,
            null,
        )
        videoThread = Thread({ drainVideo(codec) }, "rv-video-encoder").apply { start() }
    }

    private fun drainVideo(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (true) {
                val index = codec.dequeueOutputBuffer(info, 10_000L)
                when {
                    index == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit

                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        sink.setVideoFormat(codec.outputFormat)
                    }

                    index >= 0 -> {
                        val buffer = codec.getOutputBuffer(index)
                        if (
                            buffer != null &&
                            info.size > 0 &&
                            (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                            !paused
                        ) {
                            if (firstVideoPtsUs < 0L) firstVideoPtsUs = info.presentationTimeUs
                            info.presentationTimeUs =
                                (info.presentationTimeUs - firstVideoPtsUs - pausedAccumUs)
                                    .coerceAtLeast(0L)
                            sink.writeVideo(buffer, info)
                        }
                        codec.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            }
        } catch (cancelled: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Exception) {
            if (!stopping) listener.onError(error)
        }
    }

    @SuppressLint("MissingPermission")
    private fun prepareAudio(): Boolean {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val internal = if (requestedAudioSource != AudioSourceMode.Microphone) {
            if (projection != null) tryOpenAudioRecord(playback = true) else null
        } else {
            null
        }
        val mic = if (requestedAudioSource != AudioSourceMode.Internal) {
            tryOpenAudioRecord(playback = false)
        } else {
            null
        }

        val inputs = mutableListOf<AudioInput>()
        internal?.let { inputs += AudioInput(it, playback = true) }
        mic?.let { inputs += AudioInput(it, playback = false) }

        when (requestedAudioSource) {
            AudioSourceMode.Internal -> {
                if (internal != null) {
                    activeAudioSource = AudioSourceMode.Internal
                } else {
                    val fallback = tryOpenAudioRecord(playback = false)
                    if (fallback == null) return false
                    inputs += AudioInput(fallback, playback = false)
                    activeAudioSource = AudioSourceMode.Microphone
                }
            }

            AudioSourceMode.Microphone -> {
                if (mic == null) return false
                activeAudioSource = AudioSourceMode.Microphone
            }

            AudioSourceMode.Both -> {
                if (inputs.isEmpty()) return false
                activeAudioSource = when {
                    internal != null && mic != null -> AudioSourceMode.Both
                    internal != null -> AudioSourceMode.Internal
                    else -> AudioSourceMode.Microphone
                }
            }
        }

        if (inputs.isEmpty()) return false

        val codec = try {
            MediaCodec.createEncoderByType(AUDIO_MIME)
        } catch (error: Exception) {
            inputs.forEach { runCatching { it.record.release() } }
            return false
        }
        val format = MediaFormat.createAudioFormat(AUDIO_MIME, SAMPLE_RATE, CHANNEL_COUNT).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_READ_BYTES)
        }
        try {
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            inputs.forEach { input ->
                input.record.startRecording()
                if (input.record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    throw IllegalStateException("AudioRecord did not start")
                }
            }
        } catch (error: Exception) {
            runCatching { codec.stop() }
            runCatching { codec.release() }
            inputs.forEach { input ->
                runCatching { input.record.stop() }
                runCatching { input.record.release() }
            }
            return false
        }
        audioInputs = inputs
        audioCodec = codec
        return true
    }

    @SuppressLint("MissingPermission")
    private fun tryOpenAudioRecord(playback: Boolean): AudioRecord? {
        val projection = projection
        if (playback && projection == null) return null
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferSize = maxOf(minBuffer, AUDIO_READ_BYTES) * 4
        val builder = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSize)
        if (playback) {
            val capture = AudioPlaybackCaptureConfiguration.Builder(projection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build()
            builder.setAudioPlaybackCaptureConfig(capture)
        } else {
            builder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }
        return try {
            val record = builder.build()
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { record.release() }
                null
            } else {
                record
            }
        } catch (error: Exception) {
            null
        }
    }

    private fun startAudioCapture() {
        val codec = audioCodec ?: return
        if (audioInputs.isEmpty()) return
        audioThread = Thread({ drainAudio(codec) }, "rv-audio-encoder").apply { start() }
    }

    @SuppressLint("MissingPermission")
    private fun drainAudio(codec: MediaCodec) {
        val inputs = audioInputs.toMutableList()
        val buffers = inputs.map { ByteArray(AUDIO_READ_BYTES) }.toMutableList()
        val lengths = MutableList(inputs.size) { 0 }
        val mixed = ByteArray(AUDIO_READ_BYTES)
        val segmenter = VadSegmenter(
            sampleRate = SAMPLE_RATE,
            channelCount = CHANNEL_COUNT,
            onSegment = listener::onSpeechSegment,
        )
        var silenceMs = 0L
        var fallbackAttempted = requestedAudioSource != AudioSourceMode.Internal
        try {
            while (!stopping) {
                var maxRead = 0
                for (index in inputs.indices) {
                    val read = runCatching {
                        inputs[index].record.read(
                            buffers[index],
                            0,
                            AUDIO_READ_BYTES,
                            AudioRecord.READ_BLOCKING,
                        )
                    }.getOrDefault(0)
                    lengths[index] = read.coerceAtLeast(0)
                    if (lengths[index] > maxRead) maxRead = lengths[index]
                }
                if (maxRead <= 0) {
                    if (stopping) break
                    Thread.sleep(10L)
                    continue
                }
                if (paused) {
                    silenceMs = 0L
                    continue
                }

                val pcm = if (inputs.size == 1) {
                    buffers[0]
                } else {
                    mixAudio(inputs, buffers, lengths, mixed)
                    mixed
                }
                val level = rmsLevel(pcm, maxRead)
                listener.onLevel(level)
                val startMs = audioPtsUs / 1000L
                feedAudio(codec, pcm, maxRead)
                segmenter.feed(pcm, maxRead, startMs, level)
                drainAudioOutput(codec)

                if (!fallbackAttempted && activeAudioSource == AudioSourceMode.Internal) {
                    val chunkMs = (maxRead / BYTES_PER_MS).coerceAtLeast(1).toLong()
                    if (level < INTERNAL_SILENCE_LEVEL) silenceMs += chunkMs else silenceMs = 0L
                    if (silenceMs >= INTERNAL_SILENCE_FALLBACK_MS) {
                        fallbackAttempted = true
                        silenceMs = 0L
                        val mic = tryOpenAudioRecord(playback = false)
                        var startedMic = false
                        if (mic != null) {
                            startedMic = runCatching {
                                mic.startRecording()
                                mic.recordingState == AudioRecord.RECORDSTATE_RECORDING
                            }.getOrDefault(false)
                        }
                        if (startedMic && mic != null) {
                            inputs.forEach { input ->
                                runCatching { input.record.stop() }
                                runCatching { input.record.release() }
                            }
                            inputs.clear()
                            buffers.clear()
                            lengths.clear()
                            inputs += AudioInput(mic, playback = false)
                            buffers += ByteArray(AUDIO_READ_BYTES)
                            lengths += 0
                            activeAudioSource = AudioSourceMode.Microphone
                            audioInputs = inputs.toList()
                            listener.onAudioSourceChanged(
                                AudioSourceMode.Internal,
                                AudioSourceMode.Microphone,
                            )
                        } else {
                            runCatching { mic?.release() }
                        }
                    }
                }
            }
            segmenter.flush()
            queueAudioEnd(codec)
            drainAudioOutput(codec, untilEnd = true)
        } catch (cancelled: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (error: Exception) {
            sink.disableAudio()
            if (!stopping && kind == RecordingKind.Audio) listener.onError(error)
        }
    }

    private fun mixAudio(
        inputs: List<AudioInput>,
        buffers: List<ByteArray>,
        lengths: List<Int>,
        output: ByteArray,
    ): Int {
        val length = lengths.maxOrNull() ?: 0
        for (index in 0 until length step 2) {
            var sum = 0
            var count = 0
            for (inputIndex in inputs.indices) {
                if (index + 1 < lengths[inputIndex]) {
                    val low = buffers[inputIndex][index].toInt() and 0xff
                    val high = buffers[inputIndex][index + 1].toInt() shl 8
                    sum += (high or low).toShort().toInt()
                    count++
                }
            }
            val sample = if (count == 0) 0 else (sum / count).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[index] = (sample and 0xff).toByte()
            output[index + 1] = ((sample shr 8) and 0xff).toByte()
        }
        return length
    }

    private fun feedAudio(codec: MediaCodec, pcm: ByteArray, read: Int) {
        val inputIndex = codec.dequeueInputBuffer(10_000L)
        if (inputIndex < 0) return
        val input = codec.getInputBuffer(inputIndex) ?: return
        input.clear()
        input.put(pcm, 0, read)
        val samples = read / (2 * CHANNEL_COUNT)
        val durationUs = samples.toLong() * 1_000_000L / SAMPLE_RATE
        codec.queueInputBuffer(inputIndex, 0, read, audioPtsUs, 0)
        audioPtsUs += durationUs
    }

    private fun queueAudioEnd(codec: MediaCodec) {
        val inputIndex = codec.dequeueInputBuffer(50_000L)
        if (inputIndex >= 0) {
            codec.queueInputBuffer(
                inputIndex,
                0,
                0,
                audioPtsUs,
                MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        }
    }

    private fun drainAudioOutput(codec: MediaCodec, untilEnd: Boolean = false) {
        val info = MediaCodec.BufferInfo()
        while (true) {
            val index = codec.dequeueOutputBuffer(info, if (untilEnd) 10_000L else 0L)
            when {
                index == MediaCodec.INFO_TRY_AGAIN_LATER -> if (!untilEnd) return
                index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    sink.setAudioFormat(codec.outputFormat)
                }

                index >= 0 -> {
                    val buffer = codec.getOutputBuffer(index)
                    if (
                        buffer != null &&
                        info.size > 0 &&
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                        !paused
                    ) {
                        sink.writeAudio(buffer, info)
                    }
                    codec.releaseOutputBuffer(index, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    private fun releaseVideo() {
        virtualDisplay?.release()
        virtualDisplay = null
        runCatching { videoCodec?.stop() }
        runCatching { videoCodec?.release() }
        videoCodec = null
        runCatching { videoSurface?.release() }
        videoSurface = null
    }

    private fun releaseAudio() {
        audioInputs.forEach { input ->
            runCatching { input.record.stop() }
            runCatching { input.record.release() }
        }
        audioInputs = emptyList()
        runCatching { audioCodec?.stop() }
        runCatching { audioCodec?.release() }
        audioCodec = null
    }

    private fun rmsLevel(pcm: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sum = 0.0
        var count = 0
        var index = 0
        while (index + 1 < length) {
            val sample = ((pcm[index + 1].toInt() shl 8) or (pcm[index].toInt() and 0xff)).toShort()
            val value = sample / 32768.0
            sum += value * value
            count++
            index += 2
        }
        if (count == 0) return 0f
        return (sqrt(sum / count) * 4.0).toFloat().coerceIn(0f, 1f)
    }

    private data class AudioInput(
        val record: AudioRecord,
        val playback: Boolean,
    )

    private class VadSegmenter(
        private val sampleRate: Int,
        private val channelCount: Int,
        private val onSegment: (SpeechSegment) -> Unit,
    ) {
        private val bytesPerMs = (sampleRate * channelCount * 2 / 1000).coerceAtLeast(1)
        private val buffer = ByteArrayOutputStream()
        private val preRoll = ArrayDeque<ByteArray>()
        private var preRollBytes = 0
        private var active = false
        private var startMs = 0L
        private var silenceMs = 0L
        private var noiseFloor = 0.004f

        fun feed(pcm: ByteArray, length: Int, elapsedMs: Long, level: Float) {
            val chunkMs = (length / bytesPerMs).coerceAtLeast(1).toLong()
            if (!active) {
                if (level < START_LEVEL) {
                    noiseFloor = (noiseFloor * 0.96f + level * 0.04f)
                        .coerceIn(0.002f, 0.025f)
                }
                rememberPreRoll(pcm, length)
                if (level >= startThreshold()) {
                    active = true
                    startMs = (elapsedMs - preRollBytes / bytesPerMs).coerceAtLeast(0L)
                    silenceMs = 0L
                    buffer.reset()
                    preRoll.forEach { buffer.write(it, 0, it.size) }
                    preRoll.clear()
                    preRollBytes = 0
                }
                return
            }
            buffer.write(pcm, 0, length)
            if (level < silenceThreshold()) silenceMs += chunkMs else silenceMs = 0L
            val durationMs = elapsedMs - startMs
            if (silenceMs >= END_SILENCE_MS || durationMs >= MAX_SEGMENT_MS) {
                emit(elapsedMs)
                active = false
                silenceMs = 0L
            }
        }

        fun flush() {
            if (active) emit(startMs + buffer.size() / bytesPerMs)
            active = false
            buffer.reset()
            preRoll.clear()
            preRollBytes = 0
        }

        private fun rememberPreRoll(pcm: ByteArray, length: Int) {
            if (length <= 0) return
            val limit = PREROLL_MS * bytesPerMs
            val copy = pcm.copyOf(length)
            preRoll.addLast(copy)
            preRollBytes += copy.size
            while (preRollBytes > limit && preRoll.isNotEmpty()) {
                preRollBytes -= preRoll.removeFirst().size
            }
        }

        private fun emit(endMs: Long) {
            if (buffer.size() < MIN_SEGMENT_MS * bytesPerMs) {
                buffer.reset()
                return
            }
            val pcm = buffer.toByteArray()
            onSegment(
                SpeechSegment(
                    pcm = pcm,
                    startMs = startMs,
                    endMs = endMs.coerceAtLeast(startMs),
                    sampleRate = sampleRate,
                    channelCount = channelCount,
                ),
            )
            buffer.reset()
            val overlapBytes = (OVERLAP_MS * bytesPerMs).coerceAtMost(pcm.size.toLong()).toInt()
            if (overlapBytes > 0) {
                preRoll.clear()
                preRollBytes = 0
                rememberOverlap(pcm, overlapBytes)
            }
        }

        private fun rememberOverlap(pcm: ByteArray, length: Int) {
            if (length <= 0) return
            val offset = pcm.size - length
            preRoll.addLast(pcm.copyOfRange(offset, pcm.size))
            preRollBytes += length
        }

        private fun startThreshold(): Float =
            (START_LEVEL.coerceAtLeast(noiseFloor * 2.4f)).coerceAtMost(0.06f)

        private fun silenceThreshold(): Float =
            (SILENCE_LEVEL.coerceAtLeast(noiseFloor * 1.25f)).coerceAtMost(0.03f)

        private companion object {
            const val START_LEVEL = 0.016f
            const val SILENCE_LEVEL = 0.007f
            const val END_SILENCE_MS = 420L
            const val MAX_SEGMENT_MS = 9_000L
            const val MIN_SEGMENT_MS = 350L
            const val PREROLL_MS = 450L
            const val OVERLAP_MS = 180L
        }
    }

    private class MuxerSink(private val file: File) {
        private val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        private val pending = ArrayDeque<PendingFrame>()
        private var expectVideo = false
        private var expectAudio = false
        private var videoTrack = -1
        private var audioTrack = -1
        private var started = false
        private var released = false

        @Synchronized
        fun configure(expectVideo: Boolean, expectAudio: Boolean) {
            this.expectVideo = expectVideo
            this.expectAudio = expectAudio
            maybeStart()
        }

        @Synchronized
        fun setVideoFormat(format: MediaFormat) {
            if (!expectVideo || videoTrack >= 0 || released) return
            videoTrack = runCatching { muxer.addTrack(format) }.getOrDefault(-1)
            maybeStart()
        }

        @Synchronized
        fun setAudioFormat(format: MediaFormat) {
            if (!expectAudio || audioTrack >= 0 || released) return
            audioTrack = runCatching { muxer.addTrack(format) }.getOrDefault(-1)
            maybeStart()
        }

        @Synchronized
        fun disableAudio() {
            if (started) return
            expectAudio = false
            maybeStart()
        }

        @Synchronized
        fun writeVideo(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            write(videoTrack, buffer, info)
        }

        @Synchronized
        fun writeAudio(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            write(audioTrack, buffer, info)
        }

        private fun write(track: Int, buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
            if (released || track < 0) return
            if (!started) {
                pending.add(PendingFrame(track, copy(buffer, info), info.presentationTimeUs, info.flags))
                return
            }
            val data = buffer.duplicate().apply {
                position(info.offset)
                limit(info.offset + info.size)
            }
            val safeInfo = MediaCodec.BufferInfo().apply {
                set(0, info.size, info.presentationTimeUs, info.flags)
            }
            runCatching { muxer.writeSampleData(track, data, safeInfo) }
        }

        private fun copy(buffer: ByteBuffer, info: MediaCodec.BufferInfo): ByteArray {
            val bytes = ByteArray(info.size)
            val duplicate = buffer.duplicate()
            duplicate.position(info.offset)
            duplicate.get(bytes)
            return bytes
        }

        private fun maybeStart() {
            if (started || released) return
            if (expectVideo && videoTrack < 0) return
            if (expectAudio && audioTrack < 0) return
            if (!expectVideo && !expectAudio) return
            runCatching {
                muxer.start()
                started = true
                while (pending.isNotEmpty()) {
                    val frame = pending.removeFirst()
                    muxer.writeSampleData(
                        frame.track,
                        ByteBuffer.wrap(frame.bytes),
                        MediaCodec.BufferInfo().apply {
                            set(0, frame.bytes.size, frame.presentationTimeUs, frame.flags)
                        },
                    )
                }
            }.onFailure {
                released = true
                pending.clear()
                runCatching { muxer.release() }
            }
        }

        @Synchronized
        fun finish(): Boolean {
            if (released) return false
            released = true
            if (started) runCatching { muxer.stop() }
            runCatching { muxer.release() }
            pending.clear()
            return started && file.length() > 0L
        }

        private data class PendingFrame(
            val track: Int,
            val bytes: ByteArray,
            val presentationTimeUs: Long,
            val flags: Int,
        )
    }

    private companion object {
        const val VIDEO_MIME = "video/avc"
        const val AUDIO_MIME = "audio/mp4a-latm"
        const val SAMPLE_RATE = 44_100
        const val CHANNEL_COUNT = 1
        const val AUDIO_READ_BYTES = 4096
        const val BYTES_PER_MS = SAMPLE_RATE * CHANNEL_COUNT * 2 / 1000
        const val INTERNAL_SILENCE_LEVEL = 0.004f
        const val INTERNAL_SILENCE_FALLBACK_MS = 3_000L
    }
}
