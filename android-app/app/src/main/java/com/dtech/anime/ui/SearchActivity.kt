package com.dtech.anime.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dtech.anime.R
import com.dtech.anime.data.Anime
import com.dtech.anime.data.AssetManagerRepository
import com.dtech.anime.ui.adapters.CardItem
import com.dtech.anime.ui.adapters.HorizontalCardAdapter
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var repository: AssetManagerRepository
    private lateinit var adapter: HorizontalCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        repository = AssetManagerRepository(this)

        val query = intent.getStringExtra("query") ?: ""

        setupViews(query)
        performSearch(query)
    }

    private fun setupViews(query: String) {
        val tvTitle = findViewById<TextView>(R.id.tv_search_query)
        tvTitle.text = "Results for \"$query\""

        val rvResults = findViewById<RecyclerView>(R.id.rv_search_results)
        rvResults.layoutManager = GridLayoutManager(this, 3) // 3 columns
        adapter = HorizontalCardAdapter { item ->
            if (item is CardItem.AnimeItem) {
                startActivity(DetailsActivity.newIntent(this, item.anime))
            }
        }
        rvResults.adapter = adapter
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            val results = repository.searchAnime(query)
            adapter.submitList(results.map { CardItem.AnimeItem(it) })
        }
    }
}
