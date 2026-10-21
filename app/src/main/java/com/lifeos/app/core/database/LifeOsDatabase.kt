package com.lifeos.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.lifeos.app.core.database.dao.CaptureDao
import com.lifeos.app.core.database.dao.DiaryDao
import com.lifeos.app.core.database.dao.ExpenseDao
import com.lifeos.app.core.database.dao.HabitDao
import com.lifeos.app.core.database.dao.LifeEventDao
import com.lifeos.app.core.database.dao.NoteDao
import com.lifeos.app.core.database.dao.SearchDao
import com.lifeos.app.core.database.dao.TaskDao
import com.lifeos.app.core.database.entity.AttachmentEntity
import com.lifeos.app.core.database.entity.CaptureEntity
import com.lifeos.app.core.database.entity.DiaryEntryEntity
import com.lifeos.app.core.database.entity.EntityTagCrossRef
import com.lifeos.app.core.database.entity.ExpenseEntity
import com.lifeos.app.core.database.entity.FolderEntity
import com.lifeos.app.core.database.entity.HabitCompletionEntity
import com.lifeos.app.core.database.entity.HabitEntity
import com.lifeos.app.core.database.entity.LifeEventEntity
import com.lifeos.app.core.database.entity.NoteEntity
import com.lifeos.app.core.database.entity.TagEntity
import com.lifeos.app.core.database.entity.TaskEntity

/**
 * Local-first store. Everything the app shows originates here — there is no
 * network data source in this version (spec: offline-first, no fake content).
 *
 * Schema v1 is frozen at first release: from v1 onward every change ships as an
 * explicit, tested forward migration (see `MigrationTest`). Exported schemas are
 * written to `app/schemas/` for that comparison.
 */
@Database(
    entities = [
        FolderEntity::class,
        TagEntity::class,
        EntityTagCrossRef::class,
        NoteEntity::class,
        TaskEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        ExpenseEntity::class,
        DiaryEntryEntity::class,
        CaptureEntity::class,
        AttachmentEntity::class,
        LifeEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun diaryDao(): DiaryDao
    abstract fun captureDao(): CaptureDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun searchDao(): SearchDao

    companion object {
        const val NAME = "lifeos.db"
    }
}

/**
 * Room type converters.
 *
 * Nearly every column is already a primitive with a stable encoding (String/Long),
 * so converters exist only for nullable Boolean-free edge cases. Keeping the
 * conversion surface tiny is deliberate: it makes the on-disk schema auditable.
 */
class DatabaseConverters {
    @TypeConverter
    fun fromBooleanOrNull(value: Boolean?): Int? = value?.let { if (it) 1 else 0 }

    @TypeConverter
    fun toBooleanOrNull(value: Int?): Boolean? = value?.let { it != 0 }
}
