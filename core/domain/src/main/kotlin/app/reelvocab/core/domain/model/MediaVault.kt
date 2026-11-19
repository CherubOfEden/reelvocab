package app.reelvocab.core.domain.model

import kotlinx.coroutines.flow.Flow

enum class VaultKind { Image, Video, Audio }

data class VaultItem(
    val id: String,
    val uri: String,
    val title: String,
    val kind: VaultKind,
    val createdAtEpochMs: Long,
)

interface MediaVaultRepository {
    fun observeAll(): Flow<List<VaultItem>>
    suspend fun add(item: VaultItem)
    suspend fun delete(id: String)
}
