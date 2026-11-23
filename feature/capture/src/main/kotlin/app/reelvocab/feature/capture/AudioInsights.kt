package app.reelvocab.feature.capture

import app.reelvocab.core.domain.model.CardRepository
import app.reelvocab.core.domain.model.DictionaryRepository
import app.reelvocab.core.domain.model.EnrichmentService
import app.reelvocab.core.domain.model.NewCard
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SpeechSegment(
    val pcm: ByteArray,
    val startMs: Long,
    val endMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
)

data class SpeechTranscript(
    val text: String,
    val confidence: Float,
)

object VocabularyExtractor {

    private val stopWords = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can",
        "could", "did", "do", "does", "for", "from", "get", "got", "had", "has",
        "have", "he", "her", "here", "him", "his", "how", "i", "if", "in", "is",
        "it", "its", "just", "know", "like", "me", "my", "no", "not", "of", "on",
        "or", "our", "out", "she", "so", "that", "the", "their", "them", "then",
        "there", "they", "this", "to", "up", "us", "was", "we", "were", "what",
        "when", "where", "which", "who", "why", "will", "with", "would", "you",
        "your", "gonna", "wanna", "gotta", "yeah", "okay", "well", "really",
    )

    private val commonLongWords = setOf(
        "something", "anything", "everything", "nothing", "because", "before",
        "after", "people", "little", "right", "think", "going", "never", "always",
        "another", "around", "through", "should", "still", "these", "those",
    )

    private val wordPattern = Regex("[A-Za-z][A-Za-z'-]*")

    fun candidates(text: String, confidence: Float): List<String> {
        if (confidence in 0.01f..0.54f) return emptyList()
        val words = wordPattern.findAll(text)
            .map { it.value.lowercase() }
            .toList()
        if (words.isEmpty()) return emptyList()

        val chosen = linkedSetOf<String>()
        bestPhrase(words)?.let { chosen += it }

        words.asSequence()
            .filter { word ->
                word.length >= 6 &&
                    word !in stopWords &&
                    word !in commonLongWords &&
                    word.any { it in 'a'..'z' }
            }
            .distinct()
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            .take(2)
            .forEach { chosen += it }

        return chosen.take(3).toList()
    }

    private fun bestPhrase(words: List<String>): String? {
        var best: Pair<String, Int>? = null
        for (start in words.indices) {
            for (size in 2..4) {
                val end = start + size
                if (end > words.size) break
                val window = words.subList(start, end)
                val content = window.count { it !in stopWords }
                val uncommon = window.count { it !in commonLongWords }
                val phrase = window.joinToString(" ")
                if (content < 2 || uncommon < 1 || phrase.length !in 8..42) continue
                val score = content * 3 + size * 2 + phrase.length / 6
                val current = best
                if (current == null || score > current.second) {
                    best = phrase to score
                }
            }
        }
        return best?.first
    }

}

@Singleton
class AutoCaptureManager @Inject constructor(
    private val cards: CardRepository,
    private val dictionary: DictionaryRepository,
    private val enrichment: EnrichmentService,
) {
    private val mutex = Mutex()
    private val seen = ConcurrentHashMap.newKeySet<String>()

    fun reset() {
        seen.clear()
    }

    suspend fun capture(
        transcript: SpeechTranscript,
        segmentStartMs: Long,
        sourceTitle: String,
        recordingStartEpochMs: Long,
    ) {
        val candidates = VocabularyExtractor.candidates(transcript.text, transcript.confidence)
        if (candidates.isEmpty()) return
        mutex.withLock {
            candidates.forEach { candidate ->
                val key = candidate.lowercase()
                if (!seen.add(key)) return@forEach
                try {
                    if (cards.hasHeadword(candidate)) return@forEach
                    val local = dictionary.lookupLocal(candidate)
                    if (!isUseful(candidate, local)) return@forEach
                    val enriched = try {
                        enrichment.enrich(candidate, transcript.text)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        null
                    }
                    val meaning = enriched
                        ?.definitions
                        ?.firstOrNull()
                        ?.definition
                        ?.takeIf { it.isNotBlank() }
                        ?: local?.senses?.firstOrNull()?.definition?.takeIf { it.isNotBlank() }
                        ?: "Heard in $sourceTitle: ${transcript.text}"
                    cards.addCard(
                        NewCard(
                            headword = candidate,
                            meaning = meaning,
                            example = transcript.text.takeIf { it.isNotBlank() },
                            ipa = enriched?.ipa ?: local?.ipa,
                            wordClass = enriched?.wordClass ?: local?.wordClass,
                            contextLine = transcript.text,
                            sourceTitle = sourceTitle,
                            sourceTimestampMs = recordingStartEpochMs + segmentStartMs,
                            hasAudio = true,
                            tags = listOf("auto-capture", "recording"),
                        ),
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Auto-capture is best effort and must never interrupt recording.
                }
            }
        }
    }

    private fun isUseful(
        candidate: String,
        local: app.reelvocab.core.domain.model.DictionaryEntry?,
    ): Boolean {
        if (candidate.contains(' ')) return local == null
        if (local == null) return candidate.length >= 8
        val difficulty = local.difficulty
        val rank = local.frequencyRank
        return (difficulty != null && difficulty >= 3) ||
            rank == null ||
            rank > 12_000
    }
}
