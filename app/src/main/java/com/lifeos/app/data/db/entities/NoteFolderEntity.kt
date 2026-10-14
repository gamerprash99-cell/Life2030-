package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * A user-created folder for Notes. [name] is unique so the "new folder" action
 * can reject duplicates instead of silently creating a second "Ideas".
 * [position] keeps a stable, user-reorderable order.
 */
@Serializable
@Entity(
    tableName = "note_folders",
    indices = [Index(value = ["name"], unique = true)]
)
data class NoteFolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val position: Int = 0,
    val createdAt: Long
)
