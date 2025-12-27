import AsyncStorage from '@react-native-async-storage/async-storage';

const KEYS = {
  FRESH_EPISODES: 'dtech_fresh_episodes',
  POPULAR_ANIME: 'dtech_popular_anime',
  ANIME_DETAILS: 'dtech_anime_details_', // prefix
  WATCH_HISTORY: 'dtech_watch_history',
  FAVORITES: 'dtech_favorites',
};

class CacheManager {
  async getFreshEpisodes() {
    try {
      const data = await AsyncStorage.getItem(KEYS.FRESH_EPISODES);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error('Error reading fresh episodes cache', e);
      return null;
    }
  }

  async setFreshEpisodes(episodes) {
    try {
      await AsyncStorage.setItem(KEYS.FRESH_EPISODES, JSON.stringify(episodes));
    } catch (e) {
      console.error('Error saving fresh episodes cache', e);
    }
  }

  async getPopularAnime() {
    try {
      const data = await AsyncStorage.getItem(KEYS.POPULAR_ANIME);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error('Error reading popular anime cache', e);
      return null;
    }
  }

  async setPopularAnime(animeList) {
    try {
      await AsyncStorage.setItem(KEYS.POPULAR_ANIME, JSON.stringify(animeList));
    } catch (e) {
      console.error('Error saving popular anime cache', e);
    }
  }

  async getAnimeDetails(animeId) {
    try {
      const data = await AsyncStorage.getItem(KEYS.ANIME_DETAILS + animeId);
      return data ? JSON.parse(data) : null;
    } catch (e) {
      console.error('Error reading anime details cache', e);
      return null;
    }
  }

  async setAnimeDetails(animeId, data) {
    try {
      await AsyncStorage.setItem(KEYS.ANIME_DETAILS + animeId, JSON.stringify(data));
    } catch (e) {
      console.error('Error saving anime details cache', e);
    }
  }

  // --- User Data ---

  async addToHistory(animeId, animeName, episodeNumber, sessionId) {
    try {
      const historyJson = await AsyncStorage.getItem(KEYS.WATCH_HISTORY);
      let history = historyJson ? JSON.parse(historyJson) : [];

      // Remove existing entry for this anime if exists (to move to top)
      history = history.filter(item => item.animeId !== animeId);

      const newItem = {
        animeId,
        animeName,
        episodeNumber,
        sessionId,
        timestamp: Date.now(),
      };

      history.unshift(newItem);

      // Limit history to 50 items
      if (history.length > 50) history = history.slice(0, 50);

      await AsyncStorage.setItem(KEYS.WATCH_HISTORY, JSON.stringify(history));
    } catch (e) {
      console.error('Error updating history', e);
    }
  }

  async getHistory() {
    try {
      const data = await AsyncStorage.getItem(KEYS.WATCH_HISTORY);
      return data ? JSON.parse(data) : [];
    } catch (e) {
      return [];
    }
  }
}

export default new CacheManager();
