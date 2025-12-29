package com.dtech.anime.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader

class AssetManagerRepository(private val context: Context) {
    private val gson = Gson()

    suspend fun loadMasterIndex(): MasterIndex? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("data/master_index.json")
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, MasterIndex::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun loadShard(letter: String): List<Anime> = withContext(Dispatchers.IO) {
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

    suspend fun loadPopularAnime(): List<Anime> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.assets.open("popular_anime.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<Anime>>() {}.type
            gson.fromJson(reader, type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun loadFreshEpisodes(): List<FreshEpisode> = withContext(Dispatchers.IO) {
        val allEpisodes = mutableListOf<FreshEpisode>()

        // 1. Load Initial Bundled Episodes
        try {
            val inputStream = context.assets.open("initial_fresh_episodes.json")
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<List<FreshEpisode>>() {}.type
            val initial: List<FreshEpisode> = gson.fromJson(reader, type)
            allEpisodes.addAll(initial)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Load Scraped Episodes from Internal Storage
        try {
            val file = File(context.filesDir, "fresh_episodes.json")
            if (file.exists()) {
                val content = file.readText()
                val type = object : TypeToken<List<FreshEpisode>>() {}.type
                val scraped: List<FreshEpisode> = gson.fromJson(content, type)

                // Merge strategies could be complex, for now, just prepend newer ones or simple distinct
                // Assuming scraped is newer.
                // Simple de-dupe by episode_url
                val existingUrls = allEpisodes.map { it.episodeUrl }.toSet()
                val newUnique = scraped.filter { !existingUrls.contains(it.episodeUrl) }

                // Add new ones to the top
                allEpisodes.addAll(0, newUnique)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        allEpisodes
    }

    suspend fun searchAnime(query: String): List<Anime> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val firstChar = query.uppercase().first()
        // Map firstChar to valid shard letter (A-Z, #)
        val shardLetter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"

        val shard = loadShard(shardLetter)

        shard.filter { anime ->
            anime.title.contains(query, ignoreCase = true)
        }
    }
}
