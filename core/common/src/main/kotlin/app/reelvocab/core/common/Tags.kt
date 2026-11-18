package app.reelvocab.core.common

/**
 * Tags are free-form labels kept on a saved card. They are stored on one line
 * with commas between values so a card row can be read without a join, while
 * all trimming, de-duplication and matching stays in one place.
 */
object Tags {

    const val MAX_TAGS: Int = 8
    const val MAX_LENGTH: Int = 24

    private const val SEPARATOR = ","
    private val SPLIT = Regex("[,;\\n]")
    private val WHITESPACE = Regex("\\s+")

    fun parse(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SPLIT)
            .mapNotNull(::normalize)
            .distinctBy { it.lowercase() }
            .take(MAX_TAGS)
    }

    fun decode(stored: String?): List<String> = parse(stored)

    fun encode(tags: List<String>): String =
        tags.mapNotNull(::normalize)
            .distinctBy { it.lowercase() }
            .take(MAX_TAGS)
            .joinToString(SEPARATOR)

    fun withAdded(current: List<String>, raw: String): List<String> {
        val result = current.toMutableList()
        parse(raw).forEach { tag ->
            if (result.size >= MAX_TAGS) return@forEach
            if (result.none { it.equals(tag, ignoreCase = true) }) {
                result += tag
            }
        }
        return result
    }

    fun without(current: List<String>, tag: String): List<String> =
        current.filterNot { it.equals(tag, ignoreCase = true) }

    fun normalize(raw: String): String? {
        val collapsed = raw.trim().replace(WHITESPACE, " ")
        if (collapsed.isEmpty()) return null
        return collapsed.take(MAX_LENGTH)
    }
}
