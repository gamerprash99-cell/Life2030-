package com.lifeos.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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

/**
 * The single Room database backing all of LifeOS (Section 57: Data/Room layer).
 * All data lives here, on-device, unencrypted-at-rest by default with an
 * optional SQLCipher upgrade path noted in README (Phase 6 hardening item).
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
    version = 1,
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
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeos.db"
                )
                    // No destructive fallback in production; migrations must be added
                    // explicitly as the schema evolves post-v1.
                    .build().also { INSTANCE = it }
            }
        }
    }
}
