// Since we might not have the full JSON index on the client (it's too big or was missing),
// this service will primarily rely on the ScraperService but can provide offline fallback
// if we bundle some data.

// We will attempt to load the bundled "anime_episodes_optimized.json" as a starter
import freshEpisodesData from '../../assets/data/anime_episodes_optimized.json';

class AnimeIndex {
  constructor() {
    this.freshEpisodes = freshEpisodesData || [];
    this.allAnime = []; // We don't have the full index currently
  }

  // Normalize text for search (simplified version of Python's)
  normalizeText(text) {
    if (!text) return "";
    return text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
  }

  // Simple search implementation
  search(query) {
    if (!query) return [];

    // If we had a local full index, we would search it here.
    // Since we don't, we return empty so the UI knows to trigger a "Live Search".
    // Or we can search the "fresh episodes" list for partial matches?

    const normalizedQuery = this.normalizeText(query);
    const results = this.freshEpisodes.filter(ep => {
      const title = this.normalizeText(ep.anime_name);
      return title.includes(normalizedQuery);
    }).map(ep => ({
      id: null, // We might not have ID in this optimized file?
               // Wait, the file has URLs: "https://animepahe.si/play/..."
               // The URL contains the ID.
      title: ep.anime_name,
      // extract ID from URL: /play/ANIME_ID/SESSION_ID
      id: this.extractAnimeIdFromUrl(ep.episode_url),
      session_id: this.extractSessionIdFromUrl(ep.episode_url),
      latest_episode: ep.episode_number
    }));

    // Deduplicate by ID
    const unique = [];
    const ids = new Set();
    for (const r of results) {
      if (r.id && !ids.has(r.id)) {
        ids.add(r.id);
        unique.push(r);
      }
    }

    return unique;
  }

  extractAnimeIdFromUrl(url) {
    if (!url) return null;
    const match = url.match(/\/play\/([a-f0-9-]+)\//);
    return match ? match[1] : null;
  }

  extractSessionIdFromUrl(url) {
    if (!url) return null;
    const match = url.match(/\/play\/[a-f0-9-]+\/([a-f0-9]+)/);
    return match ? match[1] : null;
  }
}

export default new AnimeIndex();
