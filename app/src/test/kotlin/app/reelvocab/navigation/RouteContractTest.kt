package app.reelvocab.navigation

import app.reelvocab.feature.deck.DeckFilter
import app.reelvocab.feature.deck.DeckRoute
import app.reelvocab.feature.dictionary.DictionaryRoute
import app.reelvocab.feature.library.LibraryRoute
import app.reelvocab.feature.media.MediaRoute
import app.reelvocab.feature.media.PlayerRoute
import app.reelvocab.feature.media.PlayerTrack
import app.reelvocab.feature.settings.SettingsRoute
import app.reelvocab.feature.today.TodayRoute
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteContractTest {

    @Test
    fun deckRouteRoundTripsWithFilter() {
        val encoded = Json.encodeToString(DeckRoute.serializer(), DeckRoute(DeckFilter.Due))
        val decoded = Json.decodeFromString(DeckRoute.serializer(), encoded)
        assertEquals(DeckRoute(DeckFilter.Due), decoded)
    }

    @Test
    fun playerRouteRoundTripsWithQueue() {
        val route = PlayerRoute(
            uri = "content://media/1",
            title = "Film",
            kind = "video",
            mimeType = "video/mp4",
            queue = listOf(
                PlayerTrack("content://media/1", "Film", "video", "video/mp4"),
                PlayerTrack("content://media/2", "Next", "video", "video/mp4"),
            ),
            index = 0,
        )
        val encoded = Json.encodeToString(PlayerRoute.serializer(), route)
        val decoded = Json.decodeFromString(PlayerRoute.serializer(), encoded)
        assertEquals(route, decoded)
    }

    @Test
    fun routeNamesAreStableForDeepLinks() {
        assertEquals(
            "app.reelvocab.feature.today.TodayRoute",
            TodayRoute.serializer().descriptor.serialName,
        )
        assertEquals(
            "app.reelvocab.feature.deck.DeckRoute",
            DeckRoute.serializer().descriptor.serialName,
        )
        assertEquals(
            "app.reelvocab.feature.dictionary.DictionaryRoute",
            DictionaryRoute.serializer().descriptor.serialName,
        )
        assertEquals(
            "app.reelvocab.feature.media.MediaRoute",
            MediaRoute.serializer().descriptor.serialName,
        )
        assertEquals(
            "app.reelvocab.feature.library.LibraryRoute",
            LibraryRoute.serializer().descriptor.serialName,
        )
        assertEquals(
            "app.reelvocab.feature.settings.SettingsRoute",
            SettingsRoute.serializer().descriptor.serialName,
        )
    }

    @Test
    fun deckFilterIsStableForDeepLinks() {
        assertEquals(
            "app.reelvocab.feature.deck.DeckFilter",
            DeckFilter.serializer().descriptor.serialName,
        )
        assertEquals(
            listOf("All", "Due", "Words", "Sentences"),
            DeckFilter.entries.map { it.name },
        )
    }
}
