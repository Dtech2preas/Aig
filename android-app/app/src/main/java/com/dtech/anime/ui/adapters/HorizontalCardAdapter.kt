package com.dtech.anime.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dtech.anime.R
import com.dtech.anime.data.Anime
import com.dtech.anime.data.FreshEpisode
import com.dtech.anime.data.db.HistoryEntity

sealed class CardItem {
    data class AnimeItem(val anime: Anime) : CardItem()
    data class EpisodeItem(val episode: FreshEpisode) : CardItem()
    data class HistoryItem(val history: HistoryEntity) : CardItem()
}

class HorizontalCardAdapter(
    private val onItemClick: (CardItem) -> Unit
) : ListAdapter<CardItem, HorizontalCardAdapter.CardViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPoster: ImageView = itemView.findViewById(R.id.iv_poster)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle) // Assuming layout has this or we use episode text

        fun bind(item: CardItem) {
            when (item) {
                is CardItem.AnimeItem -> {
                    tvTitle.text = item.anime.title
                    tvSubtitle.text = "Series"
                    // Load image... for now just default
                    // In a real app we would use Glide/Coil.
                    // Assuming no Coil yet, we rely on default drawable or set it if we had a URL loader.
                }
                is CardItem.EpisodeItem -> {
                    tvTitle.text = item.episode.animeName
                    tvSubtitle.text = "Ep ${item.episode.episodeNumber}"
                }
                is CardItem.HistoryItem -> {
                    tvTitle.text = item.history.animeTitle
                    val progress = if (item.history.totalDurationMs > 0) {
                        "${(item.history.progressMs * 100 / item.history.totalDurationMs)}%"
                    } else {
                        "Ep ${item.history.episodeNumber}"
                    }
                    tvSubtitle.text = "Ep ${item.history.episodeNumber} • $progress"
                }
            }

            itemView.setOnClickListener { onItemClick(item) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CardItem>() {
        override fun areItemsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
            return when {
                oldItem is CardItem.AnimeItem && newItem is CardItem.AnimeItem -> oldItem.anime.id == newItem.anime.id
                oldItem is CardItem.EpisodeItem && newItem is CardItem.EpisodeItem -> oldItem.episode.episodeUrl == newItem.episode.episodeUrl
                oldItem is CardItem.HistoryItem && newItem is CardItem.HistoryItem -> oldItem.history.animeId == newItem.history.animeId
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: CardItem, newItem: CardItem): Boolean {
            return oldItem == newItem
        }
    }
}
