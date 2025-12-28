package com.dtech.anime.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LivePatchService {

    /**
     * Bypasses Cloudflare and scrapes episode list.
     * MUST be called from the Main Thread (because it uses WebView).
     * Returns a list of scraped episodes or empty list on failure.
     */
    suspend fun scrapEpisodes(context: Context, url: String): List<Episode> = suspendCancellableCoroutine { continuation ->
        val webView = WebView(context)
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Wait for Cloudflare/DDoS check (5 seconds as per prompt)
                Handler(Looper.getMainLooper()).postDelayed({
                    val jsCode = """
                        (function() {
                            const episodes = [];
                            const playLinks = document.querySelectorAll('a[href*="/play/"]');
                            playLinks.forEach(link => {
                                const href = link.href || '';
                                const text = link.textContent?.trim() || '';
                                let episodeNum = '0';
                                const numMatch = text.match(/(\d+)/);
                                if (numMatch) {
                                    episodeNum = numMatch[1];
                                }
                                episodes.push({
                                    number: episodeNum,
                                    title: text,
                                    url: href,
                                    episode_id: href.split('/').pop() || '',
                                    iframe_url: null
                                });
                            });
                            return JSON.stringify(episodes);
                        })();
                    """.trimIndent()

                    webView.evaluateJavascript(jsCode) { result ->
                        try {
                            // result is a JSON string wrapped in quotes, e.g., "\"[{...}]\""
                            // or "null" if failed.
                            // We need to unescape it slightly if it's double encoded, but Gson/JSON parser usually handles the string content.
                            // evaluateJavascript returns the result as a JSON string.

                            val jsonRaw = if (result != null && result != "null") {
                                // Remove surrounding quotes if present (standard WebView behavior for string return)
                                if (result.startsWith("\"") && result.endsWith("\"")) {
                                    result.substring(1, result.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                                } else {
                                    result
                                }
                            } else {
                                "[]"
                            }

                            val gson = com.google.gson.Gson()
                            val listType = object : com.google.gson.reflect.TypeToken<List<Episode>>() {}.type
                            val episodes: List<Episode> = gson.fromJson(jsonRaw, listType)

                            if (continuation.isActive) {
                                continuation.resume(episodes)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        } finally {
                            // Cleanup is hard here without leaking, but we can destroy the webview
                            // usually you remove it from view hierarchy if attached. Here it's not attached.
                            webView.destroy()
                        }
                    }
                }, 5000) // 5 seconds wait
            }
        }

        webView.loadUrl(url)
    }
}
