import React, { Component } from 'react';
import { View, StyleSheet } from 'react-native';
import { WebView } from 'react-native-webview';
import CacheManager from '../services/CacheManager';

// We use a singleton-like pattern or global event emitter to communicate requests
// to this hidden WebView component.
import { DeviceEventEmitter } from 'react-native';

export const SCRAPER_EVENTS = {
  FETCH_HOME: 'SCRAPER_FETCH_HOME',
  FETCH_SEARCH: 'SCRAPER_FETCH_SEARCH',
  FETCH_DETAILS: 'SCRAPER_FETCH_DETAILS',
  FETCH_IFRAME: 'SCRAPER_FETCH_IFRAME',
};

const BASE_URL = 'https://animepahe.si';

class HeadlessScraper extends Component {
  constructor(props) {
    super(props);
    this.state = {
      url: BASE_URL,
      mode: 'IDLE', // IDLE, HOME, SEARCH, DETAILS, IFRAME
      pendingData: null,
    };
    this.webViewRef = React.createRef();
  }

  componentDidMount() {
    this.subs = [
      DeviceEventEmitter.addListener(SCRAPER_EVENTS.FETCH_HOME, this.handleFetchHome),
      DeviceEventEmitter.addListener(SCRAPER_EVENTS.FETCH_SEARCH, this.handleFetchSearch),
      DeviceEventEmitter.addListener(SCRAPER_EVENTS.FETCH_DETAILS, this.handleFetchDetails),
      DeviceEventEmitter.addListener(SCRAPER_EVENTS.FETCH_IFRAME, this.handleFetchIframe),
    ];
  }

  componentWillUnmount() {
    this.subs.forEach(s => s.remove());
  }

  handleFetchHome = () => {
    console.log('Scraper: Fetching Home...');
    this.setState({ url: BASE_URL, mode: 'HOME' });
  };

  handleFetchSearch = (query) => {
    // Note: AnimePahe doesn't have a direct search URL parameter easily scrapable?
    // Actually typically sites have ?q=... but animepahe API uses internal calls.
    // We might need to load the home page and use the search API if possible,
    // or scraping the search results page if there is one.
    // Looking at app.py, `flexible_search` was purely internal index based.
    // The scraper didn't seem to implement search?
    // Wait, `app.py` has `backend.search_anime` which strictly uses `anime_index`.
    // It does NOT scrape for search results.
    // SO: For now, I will skip scraping search and rely on the fallback logic in `AnimeIndex`
    // or implement a basic scrape if I can find the URL pattern.
    // Usually `https://animepahe.si/api?m=search&q=...`
    // Let's try to hit the API directly via WebView fetch?
    console.log('Scraper: Fetching Search (Not fully impl yet)...', query);
  };

  handleFetchDetails = ({ animeId, page = 1 }) => {
    const url = `${BASE_URL}/anime/${animeId}?page=${page}`;
    console.log('Scraper: Fetching Details...', url);
    this.setState({ url, mode: 'DETAILS', pendingData: { animeId, page } });
  };

  handleFetchIframe = ({ animeId, session }) => {
    const url = `${BASE_URL}/play/${animeId}/${session}`;
    console.log('Scraper: Fetching Iframe...', url);
    this.setState({ url, mode: 'IFRAME', pendingData: { animeId, session } });
  };

  onMessage = async (event) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      console.log('Scraper: Received Message', data.type);

      switch (data.type) {
        case 'HOME_DATA':
          await CacheManager.setFreshEpisodes(data.fresh);
          await CacheManager.setPopularAnime(data.popular);
          DeviceEventEmitter.emit('HOME_UPDATED', data);
          break;

        case 'DETAILS_DATA':
          // Merge with pendingData logic if needed
          DeviceEventEmitter.emit(`DETAILS_UPDATED_${this.state.pendingData?.animeId}`, data);
          break;

        case 'IFRAME_DATA':
           DeviceEventEmitter.emit(`IFRAME_UPDATED_${this.state.pendingData?.session}`, data);
           break;

        case 'DDOS_WAIT':
          console.log('Scraper: DDoS Guard detected, waiting...');
          // The JS inside handles the reload, we just log
          break;
      }
    } catch (e) {
      console.error('Scraper Message Error:', e);
    }
  };

  // The injection script that runs in the WebView
  injectedJS = `
    (function() {
      // Helper to send data back
      function send(type, payload) {
        window.ReactNativeWebView.postMessage(JSON.stringify({ type, ...payload }));
      }

      // 1. Check DDoS
      if (document.title.includes('DDoS-Guard') || document.title.includes('Just a moment')) {
        send('DDOS_WAIT');
        setTimeout(() => location.reload(), 6000); // Retry after 6s
        return;
      }

      // 2. Parse Logic based on URL/Content
      const path = window.location.pathname;

      // HOME PAGE
      if (path === '/' || path === '') {
        // Scrape Fresh Episodes
        const fresh = [];
        // Note: Selectors must match site. Using generic robust ones.
        // Replicating app.py logic roughly

        // ... scraping logic ...
        // Since I can't debug the DOM live, I'll use broad selectors
        document.querySelectorAll('.episode-wrap .episode .episode-snapshot img').forEach(img => {
           // This structure is hypothetical based on common layouts or what I recall/guess
           // I'll stick to the anchor tags which usually have the data
        });

        // Let's look for links with /play/
        const playLinks = Array.from(document.querySelectorAll('a[href*="/play/"]'));
        playLinks.slice(0, 30).forEach(a => {
           const href = a.getAttribute('href');
           const titleEl = a.querySelector('.episode-title, .title') || a;
           const title = titleEl.textContent.trim();
           // Try to parse ID
           const match = href.match(/\\/play\\/([a-f0-9-]+)\\/([a-f0-9]+)/);
           if (match) {
             fresh.push({
               anime_id: match[1],
               session_id: match[2],
               episode_title: title,
               // anime_name usually needs to be parsed from title "Anime Name - Episode X"
               anime_name: title.split(/\\s*-[\\s\\d]*Episode/i)[0].trim()
             });
           }
        });

        // Popular
        const popular = [];
        const popLinks = Array.from(document.querySelectorAll('a[href*="/anime/"]'));
        // Filter those that look like cover items
        popLinks.slice(0, 20).forEach(a => {
           const href = a.getAttribute('href');
           const title = a.getAttribute('title') || a.textContent.trim();
           const match = href.match(/\\/anime\\/([a-f0-9-]+)/);
           if (match && title.length > 2) {
             popular.push({
               id: match[1],
               title: title
             });
           }
        });

        if (fresh.length > 0 || popular.length > 0) {
          send('HOME_DATA', { fresh, popular });
        }
      }

      // DETAILS PAGE
      if (path.includes('/anime/')) {
        const episodes = [];
        // Look for episode table rows
        document.querySelectorAll('.episode-list-wrapper .episode-list .episode').forEach(row => {
           const a = row.querySelector('a');
           if (a) {
             const href = a.getAttribute('href');
             const num = row.getAttribute('data-number') || row.querySelector('.episode-number')?.textContent;
             const title = row.querySelector('.episode-title')?.textContent;
             const match = href.match(/\\/([a-f0-9]+)$/); // session is last part
             if (match) {
               episodes.push({
                 session: match[1],
                 number: num ? parseInt(num) : 0,
                 title: title,
                 url: href
               });
             }
           }
        });

        // Also look for pagination
        const hasNext = !!document.querySelector('.pagination .next');

        send('DETAILS_DATA', {
           title: document.title.replace(':: animepahe', '').trim(),
           episodes: episodes,
           has_next_page: hasNext
        });
      }

      // WATCH PAGE
      if (path.includes('/play/')) {
        // Extract iframe
        // Strategy 1: Iframe tag
        const iframe = document.querySelector('iframe[src*="kwik"], iframe[src*="embed"]');
        if (iframe) {
           send('IFRAME_DATA', { iframe_url: iframe.src, success: true });
           return;
        }

        // Strategy 2: JS variable
        // Often in a script tag: let url = "https://...";
        // We can scan scripts

        // Fallback: If we can't find it immediately, we might need to click a button?
        // But for now, basic check.

        send('IFRAME_DATA', { iframe_url: null, success: false });
      }

    })();
  `;

  render() {
    return (
      <View style={{ height: 0, width: 0, overflow: 'hidden' }}>
        <WebView
          ref={this.webViewRef}
          source={{ uri: this.state.url }}
          onMessage={this.onMessage}
          injectedJavaScript={this.injectedJS}
          userAgent="Mozilla/5.0 (Linux; Android 10; SM-G960F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36"
          javaScriptEnabled={true}
          domStorageEnabled={true}
          onError={(e) => console.log('WebView Error', e.nativeEvent)}
        />
      </View>
    );
  }
}

export default HeadlessScraper;
