import { ActivityIndicator } from 'react-native';
import { HomeUI } from './home.ui';
import { useInkCanvasPersistence } from '../../hooks/useInkCanvasPersistence';
import { useState } from 'react';

export function HomeView() {
  const { canvasRef, initialStrokes, isLoading, handleStrokesChange } =
    useInkCanvasPersistence('main-canvas');

  const [isEditing, setIsEditing] = useState(false);

  if (isLoading) {
    return <ActivityIndicator />;
  }

  return (
    <HomeUI
      canvasRef={canvasRef}
      initialStrokes={initialStrokes}
      handleStrokesChange={handleStrokesChange}
      isEditing={isEditing}
      setIsEditing={setIsEditing}
    />
  );
}
