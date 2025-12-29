package com.dtech.anime.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dtech.anime.R
import com.dtech.anime.data.Anime
import com.dtech.anime.data.Episode

class DetailsActivity : AppCompatActivity() {

    private lateinit var anime: Anime

    companion object {
        private const val EXTRA_ANIME = "extra_anime"

        fun newIntent(context: Context, anime: Anime): Intent {
            return Intent(context, DetailsActivity::class.java).apply {
                putExtra(EXTRA_ANIME, anime)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        anime = intent.getSerializableExtra(EXTRA_ANIME) as Anime

        setupViews()
    }

    private fun setupViews() {
        val tvTitle = findViewById<TextView>(R.id.tv_title)
        val tvEpisodeCount = findViewById<TextView>(R.id.tv_episode_count)
        val rvEpisodes = findViewById<RecyclerView>(R.id.rv_episodes)

        tvTitle.text = anime.title
        tvEpisodeCount.text = "${anime.episodesCount} Episodes"

        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = EpisodeAdapter(anime.episodes) { episode ->
            // Use dummy video URL if scrape failed/null, or prompt user
            // Ideally we scrape the specific episode page here if iframe_url is null
            val urlToPlay = episode.iframeUrl ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            startActivity(PlayerActivity.newIntent(this, urlToPlay))
        }
    }

    class EpisodeAdapter(private val episodes: List<Episode>, private val onClick: (Episode) -> Unit) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_episode, parent, false)
            return EpisodeViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
            holder.bind(episodes[position])
        }

        override fun getItemCount(): Int = episodes.size

        class EpisodeViewHolder(itemView: View, val onClick: (Episode) -> Unit) : RecyclerView.ViewHolder(itemView) {
            private val tvNumber: TextView = itemView.findViewById(R.id.tv_episode_number)
            private val tvTitle: TextView = itemView.findViewById(R.id.tv_episode_title)

            fun bind(episode: Episode) {
                tvNumber.text = "Episode ${episode.number}"
                tvTitle.text = episode.title
                itemView.setOnClickListener { onClick(episode) }
            }
        }
    }
}
