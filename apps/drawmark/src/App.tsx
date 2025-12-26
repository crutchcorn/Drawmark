import { StatusBar, useColorScheme, View, Text } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Navigation } from './constants/routes';
import { DefaultTheme } from '@react-navigation/native';
import { db } from './constants/db';
import { useMigrations } from 'drizzle-orm/expo-sqlite/migrator';
import migrations from './drizzle/migrations';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { PortalHost } from '@rn-primitives/portal';
import "./global.css"

const queryClient = new QueryClient();

const MyTheme = {
  ...DefaultTheme,
  colors: {
    ...DefaultTheme.colors,
    background: 'transparent',
  },
};

export function App() {
  const { success, error } = useMigrations(db, migrations);

  const isDarkMode = useColorScheme() === 'dark';

  if (error) {
    return (
      <GestureHandlerRootView style={{ flex: 1 }}>
        <View>
          <Text>Migration error: {error.message}</Text>
        </View>
      </GestureHandlerRootView>
    );
  }
  if (!success) {
    return (
      <GestureHandlerRootView style={{ flex: 1 }}>
        <View>
          <Text>Migration is in progress...</Text>
        </View>
      </GestureHandlerRootView>
    );
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <QueryClientProvider client={queryClient}>
        <SafeAreaProvider>
          <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
          <Navigation theme={MyTheme} />
          <PortalHost />
        </SafeAreaProvider>
      </QueryClientProvider>
    </GestureHandlerRootView>
  );
}
