package app.reelvocab.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reviews` (
                `card_id` INTEGER NOT NULL,
                `repetitions` INTEGER NOT NULL,
                `ease_factor` REAL NOT NULL,
                `interval_days` INTEGER NOT NULL,
                `due_at` INTEGER NOT NULL,
                `last_reviewed_at` INTEGER,
                `lapses` INTEGER NOT NULL,
                PRIMARY KEY(`card_id`),
                FOREIGN KEY(`card_id`) REFERENCES `cards`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reviews_due_at` ON `reviews` (`due_at`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `review_logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `card_id` INTEGER NOT NULL,
                `grade` TEXT NOT NULL,
                `reviewed_at` INTEGER NOT NULL,
                `repetitions` INTEGER NOT NULL,
                `ease_factor` REAL NOT NULL,
                `interval_days` INTEGER NOT NULL,
                FOREIGN KEY(`card_id`) REFERENCES `cards`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_card_id` ON `review_logs` (`card_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_review_logs_reviewed_at` ON `review_logs` (`reviewed_at`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cards` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
    }
}
