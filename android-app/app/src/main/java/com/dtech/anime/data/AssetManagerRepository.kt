package com.dtech.anime.data

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

class AssetManagerRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun loadMasterIndex(): MasterIndex = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open("data/master_index.json")
        val reader = InputStreamReader(inputStream)
        gson.fromJson(reader, MasterIndex::class.java)
    }

    suspend fun loadShard(letter: String): List<Anime> = withContext(Dispatchers.IO) {
        // Map special characters if necessary, but files are named like anime_#.json, anime_A.json
        val filename = "data/anime_${letter}.json"
        try {
            val inputStream = context.assets.open(filename)
            val reader = InputStreamReader(inputStream)
            val shard = gson.fromJson(reader, AnimeShard::class.java)
            shard.animeList
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
