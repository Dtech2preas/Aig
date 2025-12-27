import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, Image, TouchableOpacity, ScrollView, RefreshControl } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { DeviceEventEmitter } from 'react-native';
import CacheManager from '../services/CacheManager';
import { SCRAPER_EVENTS } from '../components/HeadlessScraper';

const HomeScreen = ({ navigation }) => {
  const [freshEpisodes, setFreshEpisodes] = useState([]);
  const [popularAnime, setPopularAnime] = useState([]);
  const [refreshing, setRefreshing] = useState(false);

  // Kitsu Cover Cache
  const [covers, setCovers] = useState({});

  useEffect(() => {
    loadData();

    // Listen for updates from Scraper
    const sub = DeviceEventEmitter.addListener('HOME_UPDATED', (data) => {
      console.log('Home Updated via Scraper');
      if (data.fresh && data.fresh.length > 0) {
        setFreshEpisodes(data.fresh);
        fetchCovers(data.fresh, 'anime_name');
      }
      if (data.popular && data.popular.length > 0) {
        setPopularAnime(data.popular);
        fetchCovers(data.popular, 'title');
      }
      setRefreshing(false);
    });

    // Trigger initial scrape if needed (or just rely on cache)
    if (freshEpisodes.length === 0) {
       onRefresh();
    }

    return () => sub.remove();
  }, []);

  const loadData = async () => {
    const fresh = await CacheManager.getFreshEpisodes();
    const popular = await CacheManager.getPopularAnime();

    if (fresh) {
      setFreshEpisodes(fresh);
      fetchCovers(fresh, 'anime_name');
    }
    if (popular) {
      setPopularAnime(popular);
      fetchCovers(popular, 'title');
    }
  };

  const onRefresh = () => {
    setRefreshing(true);
    DeviceEventEmitter.emit(SCRAPER_EVENTS.FETCH_HOME);
    // Timeout to stop refreshing spinner if scrape fails/takes too long
    setTimeout(() => setRefreshing(false), 10000);
  };

  const fetchCovers = async (list, nameKey) => {
    // Simple batch processing to avoid flooding
    for (const item of list) {
      const name = item[nameKey];
      if (!name || covers[name]) continue;

      try {
        const cleanName = name.replace(/[^\w\s]/gi, '').trim();
        const res = await fetch(`https://kitsu.io/api/edge/anime?filter[text]=${encodeURIComponent(cleanName)}&page[limit]=1`);
        const data = await res.json();
        const url = data.data?.[0]?.attributes?.posterImage?.medium;
        if (url) {
          setCovers(prev => ({ ...prev, [name]: url }));
        }
      } catch (e) {
        // ignore
      }
    }
  };

  const renderFreshItem = ({ item }) => (
    <TouchableOpacity
      style={styles.card}
      onPress={() => navigation.navigate('Player', {
        animeId: item.anime_id || item.id, // Handle mismatch
        session: item.session_id || item.session,
        title: item.episode_title
      })}
    >
      <Image
        source={{ uri: covers[item.anime_name] || 'https://via.placeholder.com/150x225?text=Loading' }}
        style={styles.cover}
      />
      <View style={styles.badge}><Text style={styles.badgeText}>Ep {item.episode_number || '?'}</Text></View>
      <Text style={styles.cardTitle} numberOfLines={2}>{item.anime_name}</Text>
    </TouchableOpacity>
  );

  const renderPopularItem = ({ item }) => (
    <TouchableOpacity
      style={styles.card}
      onPress={() => navigation.navigate('Details', { animeId: item.id, title: item.title })}
    >
       <Image
        source={{ uri: covers[item.title] || 'https://via.placeholder.com/150x225?text=Loading' }}
        style={styles.cover}
      />
      <Text style={styles.cardTitle} numberOfLines={2}>{item.title}</Text>
    </TouchableOpacity>
  );

  return (
    <ScrollView
      style={styles.container}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor="#fff" />}
    >
      <StatusBar style="light" />

      {/* Search Bar Trigger */}
      <View style={styles.searchContainer}>
        <TouchableOpacity style={styles.searchBtn} onPress={() => navigation.navigate('Search')}>
          <Text style={styles.searchText}>Search anime...</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>Fresh Episodes</Text>
      <FlatList
        horizontal
        data={freshEpisodes}
        renderItem={renderFreshItem}
        keyExtractor={(item, index) => index.toString()}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.listContent}
      />

      <Text style={styles.sectionTitle}>Popular Anime</Text>
      <FlatList
        horizontal
        data={popularAnime}
        renderItem={renderPopularItem}
        keyExtractor={(item) => item.id}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.listContent}
      />

      <View style={{ height: 50 }} />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  searchContainer: { padding: 15, backgroundColor: '#1e1e1e', marginBottom: 10 },
  searchBtn: { backgroundColor: '#333', padding: 10, borderRadius: 8 },
  searchText: { color: '#aaa' },
  sectionTitle: { fontSize: 20, fontWeight: 'bold', color: '#fff', marginLeft: 15, marginTop: 20, marginBottom: 10, borderLeftWidth: 4, borderLeftColor: '#1a73e8', paddingLeft: 10 },
  listContent: { paddingHorizontal: 15 },
  card: { width: 140, marginRight: 15 },
  cover: { width: 140, height: 210, borderRadius: 8, backgroundColor: '#333' },
  cardTitle: { color: '#f5f5f5', fontSize: 14, marginTop: 5, fontWeight: '600' },
  badge: { position: 'absolute', bottom: 45, right: 5, backgroundColor: '#1a73e8', paddingHorizontal: 6, paddingVertical: 2, borderRadius: 4 },
  badgeText: { color: '#fff', fontSize: 10, fontWeight: 'bold' }
});

export default HomeScreen;
