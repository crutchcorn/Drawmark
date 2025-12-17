import { ActivityIndicator } from 'react-native';
import { HomeUI } from './home.ui';
import { useInkCanvasPersistence } from '../../hooks/useInkCanvasPersistence';

export function HomeView() {
  const { canvasRef, initialStrokes, isLoading, handleStrokesChange } =
    useInkCanvasPersistence('main-canvas');

  if (isLoading) {
    return <ActivityIndicator />;
  }

  return <HomeUI
    canvasRef={canvasRef}
    initialStrokes={initialStrokes}
    handleStrokesChange={handleStrokesChange}
  />;
}
