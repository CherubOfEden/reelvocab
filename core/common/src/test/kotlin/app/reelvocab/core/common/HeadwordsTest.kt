package app.reelvocab.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class HeadwordsTest {

    @Test
    fun trimsSurroundingPunctuation() {
        assertEquals("grit", Headwords.normalize("Grit."))
        assertEquals("grit", Headwords.normalize("\u201cGrit,\u201d"))
        assertEquals("grit", Headwords.normalize("  grit!  "))
    }

    @Test
    fun keepsInnerApostrophesAndHyphens() {
        assertEquals("don't", Headwords.normalize("Don't"))
        assertEquals("well-known", Headwords.normalize("Well-Known"))
    }

    @Test
    fun returnsEmptyForPunctuationOnly() {
        assertEquals("", Headwords.normalize("..."))
        assertEquals("", Headwords.normalize(""))
    }
}
