package com.dtech.anime.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.dtech.anime.R

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    companion object {
        private const val EXTRA_VIDEO_URL = "video_url"
        private const val EXTRA_USER_AGENT = "user_agent"
        private const val EXTRA_COOKIE = "cookie"

        fun newIntent(context: Context, videoUrl: String, userAgent: String? = null, cookie: String? = null): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_USER_AGENT, userAgent)
                putExtra(EXTRA_COOKIE, cookie)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer() {
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: return
        val userAgent = intent.getStringExtra(EXTRA_USER_AGENT) ?: "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        val cookie = intent.getStringExtra(EXTRA_COOKIE)

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(userAgent)
            .setAllowCrossProtocolRedirects(true)

        if (!cookie.isNullOrEmpty()) {
            dataSourceFactory.setDefaultRequestProperties(mapOf("Cookie" to cookie))
        }

        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

        player = ExoPlayer.Builder(this).build().apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
        playerView.player = player
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }
}
