package app.reelvocab.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagsTest {

    @Test
    fun `parse trims collapses and deduplicates`() {
        assertEquals(
            listOf("film noir", "idiom", "Casablanca"),
            Tags.parse(" film   noir , idiom;Casablanca , FILM NOIR "),
        )
    }

    @Test
    fun `encode round trips through decode`() {
        val tags = listOf("idiom", "film noir", "idiom")
        assertEquals(listOf("idiom", "film noir"), Tags.decode(Tags.encode(tags)))
    }

    @Test
    fun `adding a duplicate ignores case`() {
        val tags = Tags.withAdded(listOf("Idiom"), "idiom")
        assertEquals(listOf("Idiom"), tags)
    }

    @Test
    fun `adding stops at the tag limit`() {
        val full = (1..Tags.MAX_TAGS).map { "tag$it" }
        assertEquals(full, Tags.withAdded(full, "one more"))
        assertTrue(Tags.MAX_TAGS >= 8)
    }

    @Test
    fun `empty values are ignored`() {
        assertEquals(emptyList<String>(), Tags.parse(" , ; \n "))
    }
}
