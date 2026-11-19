package app.reelvocab.core.data.vault

import android.content.Context
import app.reelvocab.core.domain.model.MediaVaultRepository
import app.reelvocab.core.domain.model.VaultItem
import app.reelvocab.core.domain.model.VaultKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class MediaVaultRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaVaultRepository {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    override fun observeAll(): Flow<List<VaultItem>> = _items.asStateFlow()

    override suspend fun add(item: VaultItem) {
        val next = (_items.value.filterNot { it.id == item.id } + item)
            .sortedByDescending { it.createdAtEpochMs }
        save(next)
    }

    override suspend fun delete(id: String) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        val path = item.uri.removePrefix("file://")
        runCatching { File(path).delete() }
        save(_items.value.filterNot { it.id == id })
    }

    @Synchronized
    private fun save(items: List<VaultItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("uri", item.uri)
                    .put("title", item.title)
                    .put("kind", item.kind.name)
                    .put("created", item.createdAtEpochMs),
            )
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
        _items.value = items
    }

    private fun load(): List<VaultItem> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val kind = runCatching {
                        VaultKind.valueOf(json.getString("kind"))
                    }.getOrNull() ?: continue
                    val uri = json.getString("uri")
                    if (!File(uri.removePrefix("file://")).exists()) continue
                    add(
                        VaultItem(
                            id = json.getString("id"),
                            uri = uri,
                            title = json.getString("title"),
                            kind = kind,
                            createdAtEpochMs = json.getLong("created"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val PREFS = "media_vault"
        const val KEY_ITEMS = "items"
    }
}
