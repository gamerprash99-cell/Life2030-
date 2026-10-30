package com.lifeos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeos.app.core.security.DatabasePassphraseProvider
import com.lifeos.app.data.db.dao.CaptureDao
import com.lifeos.app.data.db.dao.DiaryDao
import com.lifeos.app.data.db.dao.ExpenseDao
import com.lifeos.app.data.db.dao.HabitChecklistDao
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.dao.NoteDao
import com.lifeos.app.data.db.dao.NoteFolderDao
import com.lifeos.app.data.db.dao.ReminderDao
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitChecklistCheckEntity
import com.lifeos.app.data.db.entities.HabitChecklistItemEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.NoteFolderEntity
import com.lifeos.app.data.db.entities.ReminderEntity
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        HabitChecklistItemEntity::class,
        HabitChecklistCheckEntity::class,
        ExpenseEntity::class,
        DiaryEntity::class,
        ReminderEntity::class,
        NoteEntity::class,
        NoteFolderEntity::class,
        CaptureEntity::class,
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun habitChecklistDao(): HabitChecklistDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun diaryDao(): DiaryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao
    abstract fun noteFolderDao(): NoteFolderDao
    abstract fun captureDao(): CaptureDao

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
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
         * v5 → v6: reintroduce the Smart Notes and Life Capture modules.
         *
         * v1 shipped `notes`/`captures`, v2→v3 dropped them, and this version
         * brings them back with a richer, purpose-built schema. Purely additive:
         * three brand-new tables (plus their indexes) are created and no
         * retained table is touched, so every task, habit, expense and diary
         * row survives the upgrade byte-for-byte.
         *
         * The column list and index names mirror Room's generated SQL exactly
         * (booleans → INTEGER, Double → REAL, non-null Kotlin fields → NOT
         * NULL, nullable fields with no default), so the migrated database
         * validates against the exported v6 schema.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ---- Smart Notes ----
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes` (" +
                        "`id` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`content` TEXT NOT NULL, " +
                        "`contentJson` TEXT NOT NULL, " +
                        "`tagsCsv` TEXT NOT NULL, " +
                        "`folderId` TEXT, " +
                        "`isPinned` INTEGER NOT NULL, " +
                        "`isFavorite` INTEGER NOT NULL, " +
                        "`isArchived` INTEGER NOT NULL, " +
                        "`isTrashed` INTEGER NOT NULL, " +
                        "`trashedAt` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_folderId` ON `notes` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`)")

                // ---- Note folders ----
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `note_folders` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_note_folders_name` ON `note_folders` (`name`)")

                // ---- Life Capture ----
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `captures` (" +
                        "`id` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`text` TEXT, " +
                        "`filePath` TEXT, " +
                        "`mimeType` TEXT, " +
                        "`durationMillis` INTEGER, " +
                        "`latitude` REAL, " +
                        "`longitude` REAL, " +
                        "`placeLabel` TEXT, " +
                        "`moodKey` TEXT, " +
                        "`tagsCsv` TEXT NOT NULL, " +
                        "`isFavorite` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_type` ON `captures` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_createdAt` ON `captures` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_captures_isFavorite` ON `captures` (`isFavorite`)")
            }
        }

        /**
         * v6 → v7: the permanent Habits experience.
         *
         * Purely additive. Every existing habit and completion row is kept
         * exactly as written; the new columns carry safe defaults that preserve
         * the pre-v7 meaning:
         *  - scheduleType = LEGACY keeps DAILY/WEEKLY/CUSTOM semantics intact,
         *  - trackingMode = TICK, habitType = BUILD match how old rows behaved,
         *  - a completion's outcome defaults to DONE (it was a logged success),
         *  - targetCount = 0 means "use the habit's current goal" for old rows.
         *
         * Two new checklist tables are created for the new tracking mode. They
         * start empty, so no historical row is rewritten. Column/index names
         * mirror Room's generated SQL so the migrated schema validates against
         * the exported v7 schema.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `description` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `colorArgb` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `habitType` TEXT NOT NULL DEFAULT 'BUILD'")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `trackingMode` TEXT NOT NULL DEFAULT 'TICK'")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `scheduleType` TEXT NOT NULL DEFAULT 'LEGACY'")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `daysOfWeekCsv` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `daysOfMonthCsv` TEXT")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `timesPerPeriod` INTEGER")
                db.execSQL("ALTER TABLE `habits` ADD COLUMN `showOnTimeline` INTEGER NOT NULL DEFAULT 1")

                db.execSQL("ALTER TABLE `habit_completions` ADD COLUMN `outcome` TEXT NOT NULL DEFAULT 'DONE'")
                db.execSQL("ALTER TABLE `habit_completions` ADD COLUMN `targetCount` INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_checklist_items` (" +
                        "`id` TEXT NOT NULL, " +
                        "`habitId` TEXT NOT NULL, " +
                        "`label` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, " +
                        "`isArchived` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checklist_items_habitId` ON `habit_checklist_items` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checklist_items_habitId_position` ON `habit_checklist_items` (`habitId`, `position`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `habit_checklist_checks` (" +
                        "`itemId` TEXT NOT NULL, " +
                        "`habitId` TEXT NOT NULL, " +
                        "`dateEpochDay` INTEGER NOT NULL, " +
                        "`isChecked` INTEGER NOT NULL, " +
                        "`checkedAtEpochMillis` INTEGER NOT NULL, " +
                        "`labelSnapshot` TEXT NOT NULL, " +
                        "PRIMARY KEY(`itemId`, `dateEpochDay`))"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checklist_checks_habitId` ON `habit_checklist_checks` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checklist_checks_habitId_dateEpochDay` ON `habit_checklist_checks` (`habitId`, `dateEpochDay`)")
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
         *
         * The probe is `SELECT 1`, not a real query. It used to be
         * `habitDao().getAllForBackup()`, which forced the open but also
         * deserialised every row in the habits table on the startup path — pure
         * waste that grew with the user's data and was paid for while the splash
         * was still on screen. `SELECT 1` reaches the same code path (the
         * SupportSQLiteOpenHelper is created and the first statement is
         * executed) at a constant cost.
         */
        suspend fun warmUpOpen(context: Context) {
            // `openHelper.writableDatabase` is a blocking call, so the context
            // switch is what guarantees the open never lands on the main thread
            // regardless of who calls this.
            withContext(Dispatchers.IO) {
                getInstance(context).openHelper.writableDatabase
                    .query("SELECT 1")
                    .use { it.moveToFirst() }
            }
        }
    }
}
