import 'react-native-gesture-handler';
import React from 'react';
import { NavigationContainer, DarkTheme } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import HomeScreen from './src/screens/HomeScreen';
import SearchScreen from './src/screens/SearchScreen';
import DetailsScreen from './src/screens/DetailsScreen';
import PlayerScreen from './src/screens/PlayerScreen';
import HeadlessScraper from './src/components/HeadlessScraper';

const Stack = createStackNavigator();

const theme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: '#1a73e8',
    background: '#121212',
    card: '#1e1e1e',
    text: '#f5f5f5',
    border: '#333333',
  },
};

export default function App() {
  return (
    <SafeAreaProvider>
      <NavigationContainer theme={theme}>
        <StatusBar style="light" />
        <Stack.Navigator
          initialRouteName="Home"
          screenOptions={{
            headerStyle: {
              backgroundColor: '#1e1e1e',
            },
            headerTintColor: '#fff',
            headerTitleStyle: {
              fontWeight: 'bold',
            },
            cardStyle: { backgroundColor: '#121212' },
          }}
        >
          <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'D-TECH ANIME' }} />
          <Stack.Screen name="Search" component={SearchScreen} />
          <Stack.Screen name="Details" component={DetailsScreen} options={{ title: 'Anime Details' }} />
          <Stack.Screen name="Player" component={PlayerScreen} options={{ headerShown: false }} />
        </Stack.Navigator>

        {/* Global background scraper */}
        <HeadlessScraper />
      </NavigationContainer>
    </SafeAreaProvider>
  );
}
