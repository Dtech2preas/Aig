package com.dtech.anime.data

import android.content.Context
import com.dtech.anime.data.db.AppDatabase
import com.dtech.anime.data.db.HistoryEntity
import kotlinx.coroutines.flow.Flow

class WatchHistoryRepository(context: Context) {
    private val historyDao = AppDatabase.getDatabase(context).historyDao()

    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun saveHistory(
        animeId: String,
        animeTitle: String,
        episodeNumber: Int,
        sessionId: String,
        progressMs: Long,
        totalDurationMs: Long
    ) {
        val entity = HistoryEntity(
            animeId = animeId,
            animeTitle = animeTitle,
            episodeNumber = episodeNumber,
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            progressMs = progressMs,
            totalDurationMs = totalDurationMs
        )
        historyDao.insertOrUpdate(entity)
    }

    suspend fun getHistory(animeId: String): HistoryEntity? {
        return historyDao.getHistoryForAnime(animeId)
    }
}
