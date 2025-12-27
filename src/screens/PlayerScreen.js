import React, { useEffect, useState } from 'react';
import { View, StyleSheet, ActivityIndicator, Text } from 'react-native';
import { WebView } from 'react-native-webview';
import { DeviceEventEmitter } from 'react-native';
import { SCRAPER_EVENTS } from '../components/HeadlessScraper';
import * as ScreenOrientation from 'expo-screen-orientation';
import { StatusBar } from 'expo-status-bar';

const PlayerScreen = ({ route, navigation }) => {
  const { animeId, session } = route.params;
  const [iframeUrl, setIframeUrl] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Lock to landscape
    const lockLandscape = async () => {
      try {
        await ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.LANDSCAPE);
      } catch (e) {
        console.log('Orientation lock failed', e);
      }
    };
    lockLandscape();

    // Unlock on unmount
    return () => {
      const unlock = async () => {
        try {
          await ScreenOrientation.unlockAsync();
        } catch (e) {
          console.log('Orientation unlock failed', e);
        }
      };
      unlock();
    };
  }, []);

  useEffect(() => {
    // Trigger Scrape
    const sub = DeviceEventEmitter.addListener(`IFRAME_UPDATED_${session}`, (data) => {
      if (data.success && data.iframe_url) {
        setIframeUrl(data.iframe_url);
      } else {
        setError('Could not extract video. Try again.');
      }
    });

    DeviceEventEmitter.emit(SCRAPER_EVENTS.FETCH_IFRAME, { animeId, session });

    return () => sub.remove();
  }, [session]);

  if (error) {
    return (
      <View style={styles.center}>
        <Text style={styles.text}>{error}</Text>
        <Text style={[styles.text, { color: 'blue', marginTop: 20 }]} onPress={() => navigation.goBack()}>Go Back</Text>
      </View>
    );
  }

  if (!iframeUrl) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color="#1a73e8" />
        <Text style={styles.text}>Loading Stream...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <StatusBar hidden />
      <WebView
        source={{ uri: iframeUrl }}
        style={{ flex: 1, backgroundColor: '#000' }}
        allowsFullscreenVideo={true}
        javaScriptEnabled={true}
        domStorageEnabled={true}
        startInLoadingState={true}
        renderLoading={() => <ActivityIndicator size="large" color="#1a73e8" style={styles.loader} />}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#000' },
  center: { flex: 1, backgroundColor: '#000', alignItems: 'center', justifyContent: 'center' },
  text: { color: '#fff', marginTop: 10 },
  loader: { position: 'absolute', top: '50%', left: '50%' }
});

export default PlayerScreen;
