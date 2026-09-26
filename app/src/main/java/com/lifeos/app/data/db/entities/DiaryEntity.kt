package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/** Personal Diary — local-first entry model. */
@Serializable
@Entity(tableName = "diary_entries", indices = [Index(value = ["dateEpochDay"])])
data class DiaryEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val content: String,
    val mood: String? = null,
    val tagsCsv: String = "",
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val aiGenerated: Boolean = false,
    val isReviewed: Boolean = true,
    val attachmentsJson: String = "[]",
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
