package com.lifeos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeos.app.core.security.DatabasePassphraseProvider
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.dao.ExpenseDao
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.dao.ReminderDao
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.TaskEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * The single Room database backing all of LifeOS (Section 57: Data/Room layer).
 * All data lives here, on-device, encrypted at rest with SQLCipher. The
 * passphrase is random per install and wrapped by an Android Keystore key
 * (see core/security/DatabasePassphraseProvider.kt).
 */
@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        ExpenseEntity::class,
        DiaryEntity::class,
        ReminderEntity::class,
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun diaryDao(): DiaryDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    // Load the SQLCipher native library before opening the DB.
                    System.loadLibrary("sqlcipher")
                    val passphrase = DatabasePassphraseProvider.getOrCreate(context.applicationContext)
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        DatabasePassphraseProvider.DATABASE_NAME
                    )
                        // SQLCipher encrypts the database at rest.
                        .openHelperFactory(SupportOpenHelperFactory(passphrase))
                        // No destructive fallback in production; migrations must be added
                        // explicitly as the schema evolves post-v1.
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                        .build()
                }.also { INSTANCE = it }
            }
        }

        /**
         * v1 → v2: add the single-column indexes the hot day/range queries scan.
         * Creates every index Room's default naming would have generated
         * (`index_<table>_<column>`) so the running schema exactly matches the
         * exported v2 schema.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_createdAt ON notes(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_updatedAt ON notes(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_isDeleted ON notes(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_dueDateEpochDay ON tasks(dueDateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_createdAt ON tasks(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_updatedAt ON tasks(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_completedAtEpochMillis ON tasks(completedAtEpochMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isDeleted ON tasks(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isCompleted ON tasks(isCompleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habits_isArchived ON habits(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_completions_dateEpochDay ON habit_completions(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_diary_entries_dateEpochDay ON diary_entries(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_dateEpochDay ON expenses(dateEpochDay)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_captures_dateEpochDay ON captures(dateEpochDay)")
            }
        }

        /**
         * v2 → v3: the Notes and Capture features were removed wholesale. Drop
         * their two tables and nothing else — every retained table (tasks,
         * habits, habit_completions, expenses, diary_entries) keeps its columns and
         * rows untouched. SQLite drops each table's indexes automatically.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS notes")
                db.execSQL("DROP TABLE IF EXISTS captures")
            }
        }

        /**
         * v3 → v4: add the reminders table backing the reliable AlarmManager
         * notifications. Pure additive — no retained table is touched, so rows in
         * tasks/habits/etc. survive intact. The table is empty until the reminder
         * repository reconciles it from the `reminderEpochMillis` mirrors on the
         * tasks/habits tables.
         *
         * Column list mirrors Room's generated SQL exactly (enum → TEXT, booleans
         * → INTEGER, non-null Kotlin fields → NOT NULL) and the index names match
         * Room's default `index_<table>_<column>` naming so the running schema
         * equals the exported v4 schema.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reminders` (" +
                        "`id` TEXT NOT NULL, " +
                        "`entityType` TEXT NOT NULL, " +
                        "`entityId` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`nextTriggerAtEpochMillis` INTEGER NOT NULL, " +
                        "`repeatType` TEXT NOT NULL, " +
                        "`repeatDaysCsv` TEXT, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`soundEnabled` INTEGER NOT NULL, " +
                        "`vibrationEnabled` INTEGER NOT NULL, " +
                        "`snoozeMinutes` INTEGER NOT NULL, " +
                        "`snoozeReturnAtEpochMillis` INTEGER, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_entityType` ON `reminders` (`entityType`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_entityId` ON `reminders` (`entityId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_nextTriggerAtEpochMillis` ON `reminders` (`nextTriggerAtEpochMillis`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_enabled` ON `reminders` (`enabled`)")
            }
        }

        /**
         * v4 → v5: add the diary "favourite" flag used by the entry detail
         * actions. Purely additive — a single `NOT NULL DEFAULT 0` column on
         * `diary_entries`, so every existing diary row (and every other table)
         * is preserved untouched, with existing entries correctly defaulting to
         * "not a favourite".
         *
         * `ALTER TABLE ... ADD COLUMN` with a NOT NULL constraint requires a
         * non-null default, which is what makes this safe to run against a
         * populated table in one statement. No table is dropped, no row is
         * rewritten, and the change is reversible in the sense that v4 data
         * read by a v4 build is unaffected.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `diary_entries` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Forces the one-time SQLCipher open (native load + Keystore passphrase
         * + Room build + PBKDF2 key derivation) to complete before returning.
         *
         * Call this from a background coroutine at startup; [MainActivity] gates
         * the first frame on `LifeOSApplication.databaseState` so no UI-owned
         * main-thread access ever triggers this. Exceptions (e.g.
         * [DatabaseKeyUnavailableException]) propagate to the caller so the
         * recovery screen can be shown instead of a background crash.
         */
        suspend fun warmUpOpen(context: Context) {
            // A trivial read is enough: the database opens lazily on the first
            // statement executed against it.
            getInstance(context).habitDao().getAllForBackup()
        }
    }
}
