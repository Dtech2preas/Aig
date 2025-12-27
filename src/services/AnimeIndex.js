// Since we might not have the full JSON index on the client (it's too big or was missing),
// this service will primarily rely on the ScraperService but can provide offline fallback
// if we bundle some data.

// We will attempt to load the bundled "anime_episodes_optimized.json" as a starter
import freshEpisodesData from '../../assets/data/anime_episodes_optimized.json';
import { getFullLibrary } from './FullLibraryLoader';

class AnimeIndex {
  constructor() {
    this.freshEpisodes = freshEpisodesData || [];
    this.allAnime = getFullLibrary(); // Load full library
  }

  // Normalize text for search (simplified version of Python's)
  normalizeText(text) {
    if (!text) return "";
    return text.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").trim();
  }

  // Simple search implementation
  search(query) {
    if (!query) return [];

    // Search in full library
    const normalizedQuery = this.normalizeText(query);
    const results = this.allAnime.filter(anime => {
      const title = this.normalizeText(anime.title);
      return title.includes(normalizedQuery);
    }).map(anime => {
      // Get latest episode for session id
      const episodes = anime.episodes || [];
      const latestEp = episodes.length > 0 ? episodes[episodes.length - 1] : null;

      return {
        id: anime.id,
        title: anime.title,
        session_id: latestEp ? latestEp.episode_id : null,
        latest_episode: anime.episodes_count
      };
    });

    return results;
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
