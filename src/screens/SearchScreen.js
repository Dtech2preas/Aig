import React, { useState } from 'react';
import { View, TextInput, FlatList, Text, StyleSheet, TouchableOpacity } from 'react-native';
import AnimeIndex from '../services/AnimeIndex';

const SearchScreen = ({ navigation }) => {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);

  const handleSearch = (text) => {
    setQuery(text);
    if (text.length > 2) {
      const res = AnimeIndex.search(text);
      setResults(res);
    } else {
      setResults([]);
    }
  };

  const renderItem = ({ item }) => (
    <TouchableOpacity
      style={styles.item}
      onPress={() => {
        // If we have an ID, go to details
        // If it's a "fresh episode" result, it might be an episode link.
        // But Search logic currently returns generic objects.
        // Let's assume they are generic anime objects or episodes.

        if (item.id) {
             // It's an episode or anime? The logic in AnimeIndex maps to episodes mostly.
             // If we have session_id, it's a specific episode.
             if (item.session_id) {
                 navigation.navigate('Player', {
                     animeId: item.id,
                     session: item.session_id,
                     title: item.title
                 });
             } else {
                 navigation.navigate('Details', { animeId: item.id, title: item.title });
             }
        }
      }}
    >
      <Text style={styles.title}>{item.title}</Text>
      {item.latest_episode && <Text style={styles.subtitle}>Latest: Ep {item.latest_episode}</Text>}
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <TextInput
        style={styles.input}
        placeholder="Search anime..."
        placeholderTextColor="#888"
        value={query}
        onChangeText={handleSearch}
        autoFocus
      />
      <FlatList
        data={results}
        renderItem={renderItem}
        keyExtractor={(item, idx) => idx.toString()}
        contentContainerStyle={{ padding: 15 }}
        ListEmptyComponent={
          query.length > 2 ? <Text style={styles.empty}>No offline results. Try exact matches.</Text> : null
        }
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  input: { backgroundColor: '#333', color: '#fff', padding: 15, margin: 15, borderRadius: 8, fontSize: 16 },
  item: { padding: 15, borderBottomWidth: 1, borderBottomColor: '#333' },
  title: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  subtitle: { color: '#aaa', fontSize: 14, marginTop: 4 },
  empty: { color: '#666', textAlign: 'center', marginTop: 20 }
});

export default SearchScreen;
