package app.reelvocab.core.data.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.reelvocab.core.common.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DictionaryAsset @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: AppDispatchers,
) {
    private val mutex = Mutex()
    private var database: SQLiteDatabase? = null

    suspend fun <T> read(block: (SQLiteDatabase) -> T): T = withContext(dispatchers.io) {
        mutex.withLock {
            val db = database ?: open().also { database = it }
            block(db)
        }
    }

    private fun open(): SQLiteDatabase {
        val target = context.getDatabasePath(DATABASE_NAME)
        val versionFile = File(target.parentFile, "$DATABASE_NAME.version")
        val installed = target.exists() &&
            versionFile.exists() &&
            versionFile.readText() == VERSION

        if (!installed) {
            target.parentFile?.mkdirs()
            context.assets.open(DATABASE_NAME).use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            versionFile.writeText(VERSION)
        }

        return SQLiteDatabase.openDatabase(
            target.path,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
    }

    private companion object {
        const val DATABASE_NAME = "dict.db"
        const val VERSION = "wn31-1"
    }
}
