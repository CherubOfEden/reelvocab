package app.reelvocab.navigation

import app.reelvocab.feature.deck.DeckFilter
import app.reelvocab.feature.deck.DeckRoute
import app.reelvocab.feature.library.LibraryRoute
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
