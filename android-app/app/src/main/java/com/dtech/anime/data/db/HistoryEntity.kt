package com.dtech.anime.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_history")
data class HistoryEntity(
    @PrimaryKey val animeId: String,
    val animeTitle: String,
    val episodeNumber: Int,
    val sessionId: String,
    val timestamp: Long,
    val progressMs: Long,
    val totalDurationMs: Long
)
