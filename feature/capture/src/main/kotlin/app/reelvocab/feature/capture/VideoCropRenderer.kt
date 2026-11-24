package app.reelvocab.feature.capture

import android.graphics.RectF
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

internal class VideoCropRenderer(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val outputWidth: Int,
    private val outputHeight: Int,
    private val crop: RectF,
    private val encoderSurface: Surface,
) {
    private val thread = HandlerThread("rv-video-crop")
    private var handler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var textureId = 0
    private var surfaceTexture: android.graphics.SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private var program = 0
    private var positionLocation = 0
    private var texCoordLocation = 0
    private var texMatrixLocation = 0
    private var cropLocation = 0
    private var textureLocation = 0
    private val texMatrix = FloatArray(16)
    private var released = false
    private var reportedError = false
    private var errorCallback: ((Throwable) -> Unit)? = null

    private val vertexBuffer = floatBuffer(
        floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        ),
    )
    private val texBuffer = floatBuffer(
        floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f,
        ),
    )

    fun start(
        onReady: (Surface) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        errorCallback = onError
        thread.start()
        val threadHandler = Handler(thread.looper)
        handler = threadHandler
        threadHandler.post {
            try {
                initialize()
                val surface = inputSurface ?: error("Crop surface unavailable")
                onReady(surface)
            } catch (error: Throwable) {
                reportError(error)
                cleanup()
                thread.quitSafely()
            }
        }
    }

    fun release() {
        val threadHandler = handler
        if (threadHandler == null) {
            if (thread.isAlive) thread.quitSafely()
            return
        }
        threadHandler.post {
            cleanup()
            thread.quitSafely()
        }
        if (Thread.currentThread() !== thread) {
            runCatching { thread.join(1_200L) }
            if (thread.isAlive) thread.interrupt()
        }
    }

    private fun initialize() {
        check(sourceWidth > 0 && sourceHeight > 0)
        check(outputWidth > 0 && outputHeight > 0)
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "EGL display unavailable" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            "EGL initialize failed: ${EGL14.eglGetError()}"
        }

        val configAttributes = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val configCount = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                eglDisplay,
                configAttributes,
                0,
                configs,
                0,
                1,
                configCount,
                0,
            ),
        ) { "EGL config selection failed" }
        val selectedConfig = configs[0] ?: error("No EGL config")
        check(configCount[0] > 0)

        val contextAttributes = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE,
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay,
            selectedConfig,
            EGL14.EGL_NO_CONTEXT,
            contextAttributes,
            0,
        )
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "EGL context creation failed" }

        eglSurface = EGL14.eglCreateWindowSurface(
            eglDisplay,
            selectedConfig,
            encoderSurface,
            intArrayOf(EGL14.EGL_NONE),
            0,
        )
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "EGL window surface creation failed" }
        check(
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext),
        ) { "EGL make current failed" }

        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )

        val texture = android.graphics.SurfaceTexture(textureId).apply {
            setDefaultBufferSize(sourceWidth, sourceHeight)
        }
        surfaceTexture = texture
        inputSurface = Surface(texture)

        program = buildProgram()
        positionLocation = GLES20.glGetAttribLocation(program, "aPosition")
        texCoordLocation = GLES20.glGetAttribLocation(program, "aTexCoord")
        texMatrixLocation = GLES20.glGetUniformLocation(program, "uTexMatrix")
        cropLocation = GLES20.glGetUniformLocation(program, "uCrop")
        textureLocation = GLES20.glGetUniformLocation(program, "uTexture")
        check(positionLocation >= 0)
        check(texCoordLocation >= 0)
        check(texMatrixLocation >= 0)
        check(cropLocation >= 0)
        check(textureLocation >= 0)

        texture.setOnFrameAvailableListener({ onFrameAvailable() }, handler)
    }

    private fun onFrameAvailable() {
        if (released) return
        val texture = surfaceTexture ?: return
        try {
            texture.updateTexImage()
            if (released) return
            texture.getTransformMatrix(texMatrix)
            drawFrame(texture.timestamp)
        } catch (error: Throwable) {
            reportError(error)
        }
    }

    private fun drawFrame(timestampNanos: Long) {
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureLocation, 0)
        GLES20.glUniformMatrix4fv(texMatrixLocation, 1, false, texMatrix, 0)
        GLES20.glUniform4f(
            cropLocation,
            crop.left,
            1f - crop.bottom,
            crop.width(),
            crop.height(),
        )

        GLES20.glEnableVertexAttribArray(positionLocation)
        GLES20.glVertexAttribPointer(
            positionLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer,
        )
        GLES20.glEnableVertexAttribArray(texCoordLocation)
        GLES20.glVertexAttribPointer(
            texCoordLocation,
            2,
            GLES20.GL_FLOAT,
            false,
            0,
            texBuffer,
        )
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionLocation)
        GLES20.glDisableVertexAttribArray(texCoordLocation)

        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timestampNanos)
        check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "EGL buffer swap failed" }
    }

    private fun reportError(error: Throwable) {
        if (reportedError) return
        reportedError = true
        errorCallback?.invoke(error)
    }

    private fun cleanup() {
        if (released) return
        released = true
        runCatching { surfaceTexture?.setOnFrameAvailableListener(null) }
        runCatching { surfaceTexture?.release() }
        surfaceTexture = null
        runCatching { inputSurface?.release() }
        inputSurface = null
        releaseGl()
    }

    private fun releaseGl() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return
        runCatching {
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT,
            )
        }
        if (program != 0) runCatching { GLES20.glDeleteProgram(program) }
        program = 0
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            runCatching { GLES20.glDeleteTextures(1, textures, 0) }
            textureId = 0
        }
        if (eglSurface != EGL14.EGL_NO_SURFACE) {
            runCatching { EGL14.eglDestroySurface(eglDisplay, eglSurface) }
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        if (eglContext != EGL14.EGL_NO_CONTEXT) {
            runCatching { EGL14.eglDestroyContext(eglDisplay, eglContext) }
            eglContext = EGL14.EGL_NO_CONTEXT
        }
        runCatching { EGL14.eglReleaseThread() }
        runCatching { EGL14.eglTerminate(eglDisplay) }
        eglDisplay = EGL14.EGL_NO_DISPLAY
    }

    private fun buildProgram(): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
        val newProgram = GLES20.glCreateProgram()
        check(newProgram != 0) { "GL program creation failed" }
        GLES20.glAttachShader(newProgram, vertexShader)
        GLES20.glAttachShader(newProgram, fragmentShader)
        GLES20.glLinkProgram(newProgram)
        val status = IntArray(1)
        GLES20.glGetProgramiv(newProgram, GLES20.GL_LINK_STATUS, status, 0)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        check(status[0] == GLES20.GL_TRUE) {
            "GL program link failed: ${GLES20.glGetProgramInfoLog(newProgram)}"
        }
        return newProgram
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "GL shader creation failed" }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) {
            "GL shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
        }
        return shader
    }

    private companion object {
        fun floatBuffer(values: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(values.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .apply {
                    put(values)
                    position(0)
                }

        const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            uniform vec4 uCrop;
            varying vec2 vTexCoord;

            void main() {
                gl_Position = aPosition;
                vec2 cropCoord = aTexCoord.xy * uCrop.zw + uCrop.xy;
                vTexCoord = (uTexMatrix * vec4(cropCoord, 0.0, 1.0)).xy;
            }
        """

        const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            varying vec2 vTexCoord;

            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
