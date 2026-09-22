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
import com.lifeos.app.data.db.dao.HabitCompletionDao
import com.lifeos.app.data.db.dao.HabitDao
import com.lifeos.app.data.db.dao.NoteDao
import com.lifeos.app.data.db.dao.TaskDao
import com.lifeos.app.data.db.entities.CaptureEntity
import com.lifeos.app.data.db.entities.DiaryEntity
import com.lifeos.app.data.db.entities.ExpenseEntity
import com.lifeos.app.data.db.entities.HabitCompletionEntity
import com.lifeos.app.data.db.entities.HabitEntity
import com.lifeos.app.data.db.entities.NoteEntity
import com.lifeos.app.data.db.entities.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * The single Room database backing all of LifeOS (Section 57: Data/Room layer).
 * All data lives here, on-device, encrypted at rest with SQLCipher. The
 * passphrase is random per install and wrapped by an Android Keystore key
 * (see core/security/DatabasePassphraseProvider.kt).
 */
@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        ExpenseEntity::class,
        DiaryEntity::class,
        CaptureEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun diaryDao(): DiaryDao
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
                        .addMigrations(MIGRATION_1_2)
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
         * SQLCipher performs a one-time key derivation (PBKDF2) the first time the
         * database is opened. Kick it off on a background dispatcher at app startup
         * so that cost never lands on the first Home query after the UI is shown.
         */
        fun warmUpOpen(context: Context, scope: CoroutineScope) {
            scope.launch(Dispatchers.IO) {
                // Force the one-time SQLCipher key derivation (PBKDF2) to run on a
                // background thread instead of on the first Home query after the UI
                // is shown. A trivial read is enough: the database opens lazily on
                // the first statement executed against it.
                runCatching { getInstance(context).habitDao().getAllForBackup() }
            }
        }
    }
}
