package com.dtech.anime.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.dtech.anime.R
import com.dtech.anime.data.Anime
import com.dtech.anime.data.AssetManagerRepository
import com.dtech.anime.data.FreshEpisode
import com.dtech.anime.data.ScraperService
import com.dtech.anime.data.WatchHistoryRepository
import com.dtech.anime.data.db.HistoryEntity
import com.dtech.anime.ui.adapters.CardItem
import com.dtech.anime.ui.adapters.HorizontalCardAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var repository: AssetManagerRepository
    private lateinit var historyRepository: WatchHistoryRepository

    private lateinit var popularAdapter: HorizontalCardAdapter
    private lateinit var freshAdapter: HorizontalCardAdapter
    private lateinit var historyAdapter: HorizontalCardAdapter

    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repository = AssetManagerRepository(this)
        historyRepository = WatchHistoryRepository(this)

        setupViews()
        setupSearch()
        loadData()

        // Check if we need to run scraper
        checkAndRunScraper()
    }

    private fun setupViews() {
        swipeRefresh = findViewById(R.id.swipe_refresh)

        // Popular Anime
        val rvPopular = findViewById<RecyclerView>(R.id.rv_popular_anime)
        rvPopular.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        popularAdapter = HorizontalCardAdapter { item ->
            if (item is CardItem.AnimeItem) openDetails(item.anime)
        }
        rvPopular.adapter = popularAdapter

        // Fresh Episodes
        val rvFresh = findViewById<RecyclerView>(R.id.rv_fresh_episodes)
        rvFresh.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        freshAdapter = HorizontalCardAdapter { item ->
            if (item is CardItem.EpisodeItem) openPlayerForEpisode(item.episode)
        }
        rvFresh.adapter = freshAdapter

        // Continue Watching
        val rvHistory = findViewById<RecyclerView>(R.id.rv_continue_watching)
        rvHistory.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        historyAdapter = HorizontalCardAdapter { item ->
            if (item is CardItem.HistoryItem) openPlayerForHistory(item.history)
        }
        rvHistory.adapter = historyAdapter

        swipeRefresh.setOnRefreshListener {
            checkAndRunScraper(force = true)
        }
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.et_search)
        val btnSearch = findViewById<ImageView>(R.id.btn_search)

        fun doSearch() {
            val query = etSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("query", query)
                startActivity(intent)
            }
        }

        btnSearch.setOnClickListener { doSearch() }
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else {
                false
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            // Load Popular
            val popular = repository.loadPopularAnime()
            popularAdapter.submitList(popular.map { CardItem.AnimeItem(it) })

            // Load Fresh
            val fresh = repository.loadFreshEpisodes()
            freshAdapter.submitList(fresh.map { CardItem.EpisodeItem(it) })
        }

        lifecycleScope.launch {
            // Observe History
            historyRepository.allHistory.collectLatest { historyList ->
                val container = findViewById<View>(R.id.layout_continue_watching)
                if (historyList.isNotEmpty()) {
                    container.visibility = View.VISIBLE
                    historyAdapter.submitList(historyList.map { CardItem.HistoryItem(it) })
                } else {
                    container.visibility = View.GONE
                }
            }
        }
    }

    private fun checkAndRunScraper(force: Boolean = false) {
        val prefs = getSharedPreferences("dtech_prefs", MODE_PRIVATE)
        val lastRun = prefs.getLong("last_scrape_time", 0)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000

        if (force || (now - lastRun > oneDayMs)) {
            Toast.makeText(this, "Checking for fresh episodes...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val newEpisodes = ScraperService.scrapeFreshEpisodes(this@MainActivity)
                if (newEpisodes.isNotEmpty()) {
                    // Refresh the list
                    val fresh = repository.loadFreshEpisodes()
                    freshAdapter.submitList(fresh.map { CardItem.EpisodeItem(it) })
                    Toast.makeText(this@MainActivity, "Episodes updated!", Toast.LENGTH_SHORT).show()
                }
                swipeRefresh.isRefreshing = false
            }
        } else {
            swipeRefresh.isRefreshing = false
        }
    }

    private fun openDetails(anime: Anime) {
        startActivity(DetailsActivity.newIntent(this, anime))
    }

    private fun openPlayerForEpisode(episode: FreshEpisode) {
        // Need to convert FreshEpisode to something PlayerActivity can handle
        // or update PlayerActivity to handle FreshEpisode
        // For now, let's assume we pass ID and SessionID
        if (episode.animeId != null && episode.sessionId != null) {
            // TODO: Ensure PlayerActivity can take raw IDs.
            // Current PlayerActivity likely expects an Anime object.
            // We might need to fetch the anime first? Or just pass necessary data.
            // Given "Don't lazy load entire DB", fetching anime by ID might be hard if we don't know the shard.
            // But we can construct a dummy Anime object if needed.
             val intent = PlayerActivity.newIntent(this, episode.animeId, episode.sessionId, episode.episodeNumber.toString(), episode.animeName)
             startActivity(intent)
        } else {
            // If it's a raw link (not parsed correctly), we might need another strategy.
            // Scraper logic ensures we have IDs.
            Toast.makeText(this, "Cannot play this episode directly yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayerForHistory(history: HistoryEntity) {
        val intent = PlayerActivity.newIntent(this, history.animeId, history.sessionId, history.episodeNumber.toString(), history.animeTitle)
        startActivity(intent)
    }
}
