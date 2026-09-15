package com.lifeos.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Personal Diary. One entry per moment (not strictly one per day — users can
 * write multiple entries in a day, mirroring how "captures" work).
 * `aiGenerated` + `isReviewed` enforce Rule #8: "AI-generated diary entries
 * must be reviewable" — an AI-drafted entry starts unreviewed and must be
 * confirmed by the user before it counts toward streaks/insights.
 */
@Entity(tableName = "diary_entries")
data class DiaryEntity(
    @PrimaryKey val id: String,
    val title: String? = null,
    val content: String,
    val mood: String? = null,          // e.g. "happy" | "calm" | "stressed" | "sad" | "excited"
    val tagsCsv: String = "",
    val dateEpochDay: Long,
    val timeMinutes: Int,
    val aiGenerated: Boolean = false,
    val isReviewed: Boolean = true,    // false only for unreviewed AI drafts
    val attachmentsJson: String = "[]",
    val createdAt: Long,
    val updatedAt: Long
)
