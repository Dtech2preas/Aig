package com.dtech.anime.data

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MasterIndex(
    @SerializedName("total_anime") val totalAnime: Int,
    @SerializedName("letters") val letters: Map<String, Int>
)

data class AnimeShard(
    @SerializedName("letter") val letter: String,
    @SerializedName("count") val count: Int,
    @SerializedName("anime") val animeList: List<Anime>
)

data class Anime(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    @SerializedName("episodes") val episodes: List<Episode>,
    @SerializedName("episodes_count") val episodesCount: Int
) : Serializable

data class Episode(
    @SerializedName("number") val number: String,
    @SerializedName("title") val title: String,
    @SerializedName("url") val url: String,
    @SerializedName("episode_id") val episodeId: String,
    @SerializedName("iframe_url") val iframeUrl: String?
) : Serializable
