package app.reelvocab.feature.capture

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

data class CaptureWord(
    val text: String,
    val line: String,
)

data class CaptureLine(
    val text: String,
    val words: List<CaptureWord>,
)

data class OcrResult(
    val lines: List<CaptureLine>,
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

interface OcrEngine {
    suspend fun recognize(bitmap: Bitmap): OcrResult
}

@Singleton
class MlKitOcrEngine @Inject constructor() : OcrEngine {

    override suspend fun recognize(bitmap: Bitmap): OcrResult =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { text ->
                    continuation.resume(text.toResult())
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
}

private fun com.google.mlkit.vision.text.Text.toResult(): OcrResult {
    val lines = textBlocks
        .flatMap { block -> block.lines }
        .mapNotNull { line ->
            val lineText = line.text.trim()
            if (lineText.isEmpty()) return@mapNotNull null
            val words = line.elements
                .map { element -> element.text.trim() }
                .filter { word -> word.length >= 3 && word.any(Char::isLetter) }
                .distinctBy { word -> word.lowercase() }
                .map { word -> CaptureWord(text = word, line = lineText) }
            if (words.isEmpty()) null else CaptureLine(text = lineText, words = words)
        }
    return OcrResult(lines = lines)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    @Singleton
    abstract fun bindOcrEngine(impl: MlKitOcrEngine): OcrEngine
}
