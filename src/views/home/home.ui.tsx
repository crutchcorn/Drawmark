import { useRef } from 'react';
import { InkCanvas, InkCanvasRef } from '../../components/InkCanvas';
import { Button, View } from 'react-native';

interface HomeUIProps {
  canvasRef: React.RefObject<InkCanvasRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
}

export function HomeUI({ canvasRef, initialStrokes, handleStrokesChange }: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  return (
    <View style={{ flex: 1 }}>
      <InkCanvas
        initialStrokes={initialStrokes}
        onStrokesChange={handleStrokesChange}
        ref={canvasRef}
        brushColor="#0000FF"
        brushSize={8}
        brushFamily="pen"
        style={{ flex: 1 }}
      />
      <Button title="Clear" onPress={handleClear} />
    </View>
  );
}