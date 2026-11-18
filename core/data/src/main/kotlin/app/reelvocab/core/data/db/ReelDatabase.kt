package app.reelvocab.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        WordEntity::class,
        WordSenseEntity::class,
        FilmEntity::class,
        CardEntity::class,
        ReviewEntity::class,
        ReviewLogEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class ReelDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao
    abstract fun wordDao(): WordDao
    abstract fun filmDao(): FilmDao
    abstract fun reviewDao(): ReviewDao
}
