package app.reelvocab.core.domain.model

enum class WordClass(val storageValue: String) {
    Noun("noun"),
    Verb("verb"),
    Adjective("adjective"),
    Adverb("adverb"),
    ;

    companion object {
        fun fromStorage(value: String?): WordClass? =
            entries.firstOrNull { it.storageValue == value?.lowercase() }
    }
}

data class DictionarySense(
    val definition: String,
    val example: String? = null,
    val examples: List<String> = emptyList(),
) {
    val allExamples: List<String>
        get() = examples.ifEmpty {
            listOfNotNull(example?.trim()?.takeIf { it.isNotEmpty() })
        }
}

data class Thesaurus(
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = synonyms.isEmpty() && antonyms.isEmpty()
}

data class DictionaryEntry(
    val lemma: String,
    val wordClass: WordClass?,
    val ipa: String?,
    val frequencyRank: Int?,
    val difficulty: Int?,
    val senses: List<DictionarySense>,
    val thesaurus: Thesaurus,
    val fallbackWarning: String? = null,
)
