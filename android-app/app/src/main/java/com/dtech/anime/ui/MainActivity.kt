package com.dtech.anime.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dtech.anime.R
import com.dtech.anime.data.Anime
import com.dtech.anime.data.AssetManagerRepository
import com.dtech.anime.data.LivePatchService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var repository: AssetManagerRepository
    private val allAnimeList = mutableListOf<Anime>()
    private val popularAnimeList = mutableListOf<Anime>()

    private lateinit var popularAdapter: AnimeAdapter
    private lateinit var allAnimeAdapter: AnimeAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AssetManagerRepository(this)

        setupViews()
        loadData()
    }

    private fun setupViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh)

        val rvPopular = findViewById<RecyclerView>(R.id.rv_popular)
        // Setup LayoutManager for rvPopular
        rvPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularAdapter = AnimeAdapter { anime -> openDetails(anime) }
        rvPopular.adapter = popularAdapter

        val rvAllAnime = findViewById<RecyclerView>(R.id.rv_all_anime)
        // Setup LayoutManager for rvAllAnime
        rvAllAnime.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        allAnimeAdapter = AnimeAdapter { anime -> openDetails(anime) }
        rvAllAnime.adapter = allAnimeAdapter

        swipeRefresh.setOnRefreshListener {
            performLiveCheck()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val masterIndex = repository.loadMasterIndex()
            if (masterIndex == null) return@launch

            val shardsToLoad = listOf("A", "B", "C")
            val loadedAnime = mutableListOf<Anime>()

            shardsToLoad.forEach { letter ->
                loadedAnime.addAll(repository.loadShard(letter))
            }

            allAnimeList.clear()
            allAnimeList.addAll(loadedAnime)
            allAnimeAdapter.submitList(allAnimeList)

            popularAnimeList.clear()
            if (allAnimeList.isNotEmpty()) {
                 popularAnimeList.addAll(allAnimeList.shuffled().take(10))
            }
            popularAdapter.submitList(popularAnimeList)
        }
    }

    private fun performLiveCheck() {
        lifecycleScope.launch {
            val latestEpisodes = LivePatchService.scrapEpisodes(this@MainActivity, "https://animepahe.si")
            swipeRefresh.isRefreshing = false
        }
    }

    private fun openDetails(anime: Anime) {
        startActivity(DetailsActivity.newIntent(this, anime))
    }
}
