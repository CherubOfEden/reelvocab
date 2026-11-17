package app.reelvocab.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        WordSenseEntity::class,
        FilmEntity::class,
        CardEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ReelDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun wordDao(): WordDao
    abstract fun filmDao(): FilmDao
}
