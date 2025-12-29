package com.dtech.anime.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dtech.anime.R
import com.dtech.anime.data.Anime

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
