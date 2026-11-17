package app.reelvocab.core.common

import java.util.Locale

object Headwords {

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOfFirst { it.isLetterOrDigit() || it == '\'' || it == '-' }
        if (start < 0) return ""
        val end = trimmed.indexOfLast { it.isLetterOrDigit() || it == '\'' || it == '-' }
        if (end < start) return ""
        return trimmed.substring(start, end + 1)
            .lowercase(Locale.ROOT)
            .replace(Regex("\\s+"), " ")
    }
}
