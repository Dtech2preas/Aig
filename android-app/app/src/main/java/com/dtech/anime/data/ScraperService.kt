package com.dtech.anime.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object ScraperService {

    /**
     * Scrapes "Fresh Episodes" from animepahe.si using a hidden WebView.
     * MUST be called from the Main Thread.
     * Saves result to 'fresh_episodes.json' in internal storage.
     */
    suspend fun scrapeFreshEpisodes(context: Context): List<FreshEpisode> = suspendCancellableCoroutine { continuation ->
        val webView = WebView(context)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val url = "https://animepahe.si"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Wait for Cloudflare/DDoS check (10 seconds as per airing.py logic "wait_for_timeout(10000)")
                Handler(Looper.getMainLooper()).postDelayed({
                    val jsCode = """
                        (function() {
                            const episodes = [];
                            // Selectors from airing.py
                            const selectors = [
                                '.episode-list .episode-item a[href*="/play/"]',
                                '.tab-content .episode-item a[href*="/play/"]',
                                'a.episode-link[href*="/play/"]',
                                '.main-content a[href*="/play/"]',
                                'a[href*="/play/"]' // fallback
                            ];

                            let links = [];
                            for (let sel of selectors) {
                                const found = document.querySelectorAll(sel);
                                if (found && found.length > 0) {
                                    links = Array.from(found);
                                    break;
                                }
                            }

                            const seenHrefs = new Set();

                            links.forEach(link => {
                                const href = link.href;
                                if (!href || seenHrefs.has(href)) return;
                                seenHrefs.add(href);

                                // Extract anime ID and session ID
                                // /play/anime_id/session_id
                                const match = href.match(/\/play\/([a-f0-9-]+)\/([a-f0-9]+)/);
                                let animeId = null;
                                let sessionId = null;
                                if (match) {
                                    animeId = match[1];
                                    sessionId = match[2];
                                }

                                const text = link.innerText || link.textContent || "";
                                // Try to get parent text for context if link text is just "Episode X"
                                let fullText = text;
                                if (link.parentElement) {
                                    fullText = link.parentElement.innerText || fullText;
                                }

                                // Simple parsing of "Anime Name - Episode X"
                                // This is a simplified version of the python regex
                                let animeName = "Unknown Anime";
                                let episodeNumber = 0;

                                // Clean text
                                let clean = fullText.replace(/\s+/g, ' ').trim();

                                // Regex for "Name - Episode Num"
                                const epMatch = clean.match(/^(.+?)\s*-\s*(?:Episode|Ep)\s*(\d+)/i);
                                if (epMatch) {
                                    animeName = epMatch[1].trim();
                                    episodeNumber = parseInt(epMatch[2]);
                                } else {
                                    // Fallback: Name 123
                                    const numMatch = clean.match(/(\d+)$/);
                                    if (numMatch) {
                                        episodeNumber = parseInt(numMatch[1]);
                                        animeName = clean.replace(numMatch[0], "").trim();
                                    }
                                }

                                if (animeName && episodeNumber > 0) {
                                    episodes.push({
                                        anime_name: animeName,
                                        episode_number: episodeNumber,
                                        episode_title: animeName + " - Episode " + episodeNumber,
                                        episode_url: href,
                                        anime_id: animeId,
                                        session_id: sessionId
                                    });
                                }
                            });
                            return JSON.stringify(episodes);
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(jsCode) { result ->
                        try {
                            val jsonRaw = if (result != null && result != "null") {
                                if (result.startsWith("\"") && result.endsWith("\"")) {
                                    result.substring(1, result.length - 1)
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                } else {
                                    result
                                }
                            } else {
                                "[]"
                            }

                            val gson = Gson()
                            val listType = object : TypeToken<List<FreshEpisode>>() {}.type
                            val episodes: List<FreshEpisode> = gson.fromJson(jsonRaw, listType)

                            // Save to file if we got results
                            if (episodes.isNotEmpty()) {
                                saveEpisodesToFile(context, episodes)
                            }

                            if (continuation.isActive) {
                                continuation.resume(episodes)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        } finally {
                            webView.destroy()
                        }
                    }
                }, 10000) // Wait 10s
            }
        }
        webView.loadUrl(url)
    }

    private fun saveEpisodesToFile(context: Context, episodes: List<FreshEpisode>) {
        try {
            val file = File(context.filesDir, "fresh_episodes.json")
            val gson = Gson()
            val json = gson.toJson(episodes)
            file.writeText(json)

            // Also update timestamp in preferences
            val prefs = context.getSharedPreferences("dtech_prefs", Context.MODE_PRIVATE)
            prefs.edit().putLong("last_scrape_time", System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
