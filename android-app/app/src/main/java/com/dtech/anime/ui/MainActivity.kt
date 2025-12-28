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
    private val popularAnimeList = mutableListOf<Anime>() // In real app, this comes from pop.py logic or popularity flag

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
        popularAdapter = AnimeAdapter { anime -> openDetails(anime) }
        rvPopular.adapter = popularAdapter

        val rvAllAnime = findViewById<RecyclerView>(R.id.rv_all_anime)
        allAnimeAdapter = AnimeAdapter { anime -> openDetails(anime) }
        rvAllAnime.adapter = allAnimeAdapter

        swipeRefresh.setOnRefreshListener {
            performLiveCheck()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Load Master Index first
            val masterIndex = repository.loadMasterIndex()

            // Load a few shards for "All Anime" (e.g., A, B, C for demo)
            // Real app would implement pagination or load all async
            val shardsToLoad = listOf("A", "B", "C")
            val loadedAnime = mutableListOf<Anime>()

            shardsToLoad.forEach { letter ->
                loadedAnime.addAll(repository.loadShard(letter))
            }

            allAnimeList.clear()
            allAnimeList.addAll(loadedAnime)
            allAnimeAdapter.submitList(allAnimeList)

            // Simulate Popular (random 10 from loaded)
            popularAnimeList.clear()
            popularAnimeList.addAll(allAnimeList.shuffled().take(10))
            popularAdapter.submitList(popularAnimeList)
        }
    }

    private fun performLiveCheck() {
        // Trigger scraping
        lifecycleScope.launch {
            // For now, scrape a sample URL or search
            // The prompt says "Live Check... scrape 'Latest Releases' grid"
            // We'll simulate checking the main site.
            val latestEpisodes = LivePatchService.scrapEpisodes(this@MainActivity, "https://animepahe.si")

            if (latestEpisodes.isNotEmpty()) {
                // Logic to merge/show new episodes
                // For MVP, just log or toast, or update a "Fresh" section
                // Here we stop refreshing
            }
            swipeRefresh.isRefreshing = false
        }
    }

    private fun openDetails(anime: Anime) {
        startActivity(DetailsActivity.newIntent(this, anime))
    }

    // Inner Adapter Class
    class AnimeAdapter(private val onClick: (Anime) -> Unit) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {
        private var items: List<Anime> = emptyList()

        fun submitList(newItems: List<Anime>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anime_card, parent, false)
            return AnimeViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class AnimeViewHolder(itemView: View, val onClick: (Anime) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            private val ivPoster: ImageView = itemView.findViewById(R.id.iv_poster)

            fun bind(anime: Anime) {
                tvTitle.text = anime.title
                // Set poster if URL available, use placeholder for now or Coil/Glide
                // ivPoster.load(anime.imageUrl)
                itemView.setOnClickListener { onClick(anime) }
            }
        }
    }
}
