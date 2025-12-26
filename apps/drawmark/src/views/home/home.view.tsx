import { ActivityIndicator } from 'react-native';
import { HomeUI } from './home.ui';
import { useInkCanvasPersistence } from '../../hooks/useInkCanvasPersistence';
import { useState } from 'react';
import { InkEditorBrushInfo } from '../../components/InkEditor';
import { Colors } from './constants/colors';

export function HomeView() {
  const { canvasRef, initialStrokes, isLoading, handleStrokesChange } =
    useInkCanvasPersistence('main-canvas');

  const [brushInfo, setBrushInfo] = useState({
    color: Colors.blue,
    size: 8,
    family: 'pen',
  } as InkEditorBrushInfo);

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
      brushInfo={brushInfo}
      setBrushInfo={setBrushInfo}
    />
  );
}
