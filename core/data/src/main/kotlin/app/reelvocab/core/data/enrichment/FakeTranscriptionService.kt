package app.reelvocab.core.data.enrichment

import app.reelvocab.core.domain.model.Transcription
import app.reelvocab.core.domain.model.TranscriptionRequest
import app.reelvocab.core.domain.model.TranscriptionService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * M2 stand-in. Returns a fixed line after a short delay so capture timing can
 * be exercised end to end; M5 swaps in the on-device recognizer.
 */
@Singleton
class FakeTranscriptionService @Inject constructor() : TranscriptionService {

    override suspend fun transcribe(request: TranscriptionRequest): Transcription {
        delay(300)
        return Transcription(text = CANNED_LINE)
    }

    private companion object {
        const val CANNED_LINE =
            "You gotta have grit if you wanna make it in this town."
    }
}
