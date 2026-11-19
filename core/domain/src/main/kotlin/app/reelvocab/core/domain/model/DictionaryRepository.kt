package app.reelvocab.core.domain.model

interface DictionaryRepository {
    suspend fun lookup(rawLemma: String): DictionaryEntry?
    suspend fun search(prefix: String, limit: Int = 20): List<DictionaryEntry>
    suspend fun thesaurus(rawLemma: String): Thesaurus
    suspend fun wordOfTheDay(): DictionaryEntry?
    suspend fun isReady(): Boolean
}
