import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, FlatList, TouchableOpacity, ActivityIndicator, Image } from 'react-native';
import { DeviceEventEmitter } from 'react-native';
import { SCRAPER_EVENTS } from '../components/HeadlessScraper';

const DetailsScreen = ({ route, navigation }) => {
  const { animeId, title } = route.params;
  const [loading, setLoading] = useState(true);
  const [episodes, setEpisodes] = useState([]);
  const [cover, setCover] = useState(null);

  useEffect(() => {
    // 1. Fetch Cover
    fetchCover();

    // 2. Setup Listener
    const sub = DeviceEventEmitter.addListener(`DETAILS_UPDATED_${animeId}`, (data) => {
      setEpisodes(data.episodes);
      setLoading(false);
    });

    // 3. Trigger Scrape
    DeviceEventEmitter.emit(SCRAPER_EVENTS.FETCH_DETAILS, { animeId });

    return () => sub.remove();
  }, [animeId]);

  const fetchCover = async () => {
    try {
      const cleanName = title.replace(/[^\w\s]/gi, '').trim();
      const res = await fetch(`https://kitsu.io/api/edge/anime?filter[text]=${encodeURIComponent(cleanName)}&page[limit]=1`);
      const data = await res.json();
      setCover(data.data?.[0]?.attributes?.posterImage?.large);
    } catch (e) {}
  };

  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.epItem}
      onPress={() => navigation.navigate('Player', {
        animeId,
        session: item.session,
        title: `${title} - Ep ${item.number}`
      })}
    >
      <View style={styles.epBadge}><Text style={styles.epText}>{item.number}</Text></View>
      <View style={{flex: 1}}>
        <Text style={styles.epTitle}>{item.title || `Episode ${item.number}`}</Text>
      </View>
      <View><Text style={styles.playBtn}>▶</Text></View>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <FlatList
        ListHeaderComponent={
          <View>
             {cover && <Image source={{ uri: cover }} style={styles.cover} />}
             <View style={styles.header}>
               <Text style={styles.title}>{title}</Text>
             </View>
             {loading && <ActivityIndicator size="large" color="#1a73e8" style={{ marginTop: 20 }} />}
          </View>
        }
        data={episodes}
        renderItem={renderItem}
        keyExtractor={(item) => item.session}
        contentContainerStyle={{ paddingBottom: 20 }}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  cover: { width: '100%', height: 250, resizeMode: 'cover', opacity: 0.7 },
  header: { padding: 20, borderBottomWidth: 1, borderBottomColor: '#333' },
  title: { fontSize: 24, fontWeight: 'bold', color: '#fff' },
  epItem: { flexDirection: 'row', alignItems: 'center', padding: 15, borderBottomWidth: 1, borderBottomColor: '#222' },
  epBadge: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#1a73e8', alignItems: 'center', justifyContent: 'center', marginRight: 15 },
  epText: { color: '#fff', fontWeight: 'bold' },
  epTitle: { color: '#eee', fontSize: 16 },
  playBtn: { color: '#1a73e8', fontSize: 20 }
});

export default DetailsScreen;
