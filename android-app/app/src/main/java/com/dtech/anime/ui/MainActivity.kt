package com.dtech.anime.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dtech.anime.R

class MainActivity : AppCompatActivity() {

    private lateinit var visibleWebView: WebView
    private lateinit var hiddenWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup layout programmatically to avoid XML dependency issues after deletions
        val layout = FrameLayout(this)
        layout.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )

        visibleWebView = WebView(this)
        visibleWebView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        layout.addView(visibleWebView)

        hiddenWebView = WebView(this)
        hiddenWebView.layoutParams = FrameLayout.LayoutParams(1, 1) // Tiny, invisible
        hiddenWebView.visibility = View.INVISIBLE
        layout.addView(hiddenWebView)

        setContentView(layout)

        setupVisibleWebView()
        setupHiddenWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupVisibleWebView() {
        visibleWebView.settings.javaScriptEnabled = true
        visibleWebView.settings.domStorageEnabled = true
        visibleWebView.webViewClient = WebViewClient()
        visibleWebView.webChromeClient = WebChromeClient()
        visibleWebView.addJavascriptInterface(WebAppInterface(), "Android")

        // Load the local HTML
        visibleWebView.loadUrl("file:///android_asset/www/index.html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupHiddenWebView() {
        hiddenWebView.settings.javaScriptEnabled = true
        hiddenWebView.settings.domStorageEnabled = true
        hiddenWebView.settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        hiddenWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // When page loads, if we are in scraping mode, inject the scraper script
                // The injection is actually triggered by scrapeUpdates() calling loadUrl
                // But we can also inject the scraper.js logic here if we navigated.
            }
        }
    }

    private inner class WebAppInterface {
        @JavascriptInterface
        fun scrapeUpdates() {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Starting background scrape...", Toast.LENGTH_SHORT).show()
                startScrapingProcess()
            }
        }
    }

    private fun startScrapingProcess() {
        // 1. Load the target site in hidden webview
        hiddenWebView.loadUrl("https://animepahe.si/")

        // 2. Wait a bit then inject scraper
        // We use a simple delayed runnable. In a robust app, we'd wait for onPageFinished and check logic.
        hiddenWebView.postDelayed({
            injectScraperScript()
        }, 8000) // Wait 8 seconds for initial load + DDOS guard
    }

    private fun injectScraperScript() {
        // Read scraper.js content
        val scraperJs = try {
            assets.open("www/js/scraper.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        // Inject the script and call the function
        val injection = """
            $scraperJs
            (function() {
                var result = scrapeFreshEpisodes();
                return JSON.stringify(result);
            })();
        """.trimIndent()

        hiddenWebView.evaluateJavascript(injection) { result ->
            // result is the JSON string returned by scrapeFreshEpisodes
            // result might be quoted string like "\"{\"status\":\"...\"}\""

            // Clean up the string (remove quotes added by evaluateJavascript)
            var jsonStr = result
            if (jsonStr != null) {
                if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                    jsonStr = jsonStr.substring(1, jsonStr.length - 1)
                }
                // Unescape
                jsonStr = jsonStr.replace("\\\"", "\"").replace("\\\\", "\\")

                handleScraperResult(jsonStr)
            }
        }
    }

    private fun handleScraperResult(jsonStr: String) {
        if (jsonStr == "null" || jsonStr.isEmpty()) return

        // We need to parse it partially to check status
        // Simple string check is safer than importing GSON just for this check if we want to keep it simple
        if (jsonStr.contains("\"status\":\"WAIT\"") || jsonStr.contains("\"status\":\"RETRY\"")) {
            // Retry after delay
            hiddenWebView.postDelayed({
                injectScraperScript()
            }, 5000)
        } else if (jsonStr.contains("\"status\":\"SUCCESS\"")) {
            // Extract data part manually or pass whole thing to UI
            // We need to extract the 'data' array from the JSON object
            // Let's pass the array directly to the UI

            // We need to parse it to get the 'data' field content
            // Since we deleted Gson (or maybe not? dependencies might still be there),
            // let's do a trick: we will just pass the raw episodes array string to the visible WebView.

            // Actually, let's just pass the data object string if we can.
            // But doing regex on JSON is fragile.
            // Let's rely on the fact that we can call visibleWebView to parse it.

            runOnUiThread {
                 // Pass the whole JSON string to the UI and let it handle parsing
                 // We first need to parse it in JS context of visibleWebView or pass it as string.

                 // Ideally: visibleWebView.evaluateJavascript("onEpisodesFound('" + escape(jsonStr) + "')", null)
                 // But passing huge string can be tricky.

                 // Let's try to extract just the data part using string manipulation if possible,
                 // OR assuming 'scraper.js' returns {status:..., data:[...]}

                 // Let's just pass the full JSON to a helper in UI which extracts .data

                 // Escape single quotes for JS injection
                 val safeJson = jsonStr.replace("'", "\\'")

                 // We need to call: onEpisodesFound(data_json_string)
                 // But our JSON has {status:..., data:...}
                 // Let's modify scraper.js or index.html to handle this format.
                 // I'll update index.html's onEpisodesFound to expect the episodes array,
                 // so I need to extract it here OR update index.html to handle the full object.
                 // Updating index.html to handle full object is easier? No, I already wrote it to expect episodes.
                 // Let's update this function to pass the "data" property.

                 // Actually, it's safer to let the UI parse the outer object.
                 // I will update the logic to pass the 'data' property content.

                 // Wait, I can't easily parse JSON in Kotlin without GSON/Kotlinx.Serialization.
                 // Are they available? Build.gradle says?
                 // I should check build.gradle.kts.
                 // If not, I can just substring.

                 // Hacky substring: find "data": and the rest.
                 val dataIndex = jsonStr.indexOf("\"data\":")
                 if (dataIndex != -1) {
                     var dataStr = jsonStr.substring(dataIndex + 7)
                     if (dataStr.endsWith("}")) dataStr = dataStr.substring(0, dataStr.length - 1)

                     // Send to UI
                     val jsCall = "onEpisodesFound('${dataStr.replace("'", "\\'")}')"
                     visibleWebView.evaluateJavascript(jsCall, null)
                     Toast.makeText(this@MainActivity, "Episodes synced to UI", Toast.LENGTH_SHORT).show()
                 }
            }
        }
    }
}
