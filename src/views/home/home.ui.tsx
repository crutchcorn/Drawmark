import { useRef } from 'react';
import { InkEditor, InkEditorRef } from '../../components/InkEditor';
import { Button, View } from 'react-native';
import { InkCanvas } from '../../components/InkCanvas';

interface HomeUIProps {
  canvasRef: React.RefObject<InkEditorRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
}

export function HomeUI({
  canvasRef,
  initialStrokes,
  handleStrokesChange,
  isEditing,
  setIsEditing,
}: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  const handleEditToggle = () => {
    setIsEditing(!isEditing);
  };

  return (
    <View style={{ flex: 1 }}>
      {isEditing ? <InkEditor
        initialStrokes={initialStrokes}
        onStrokesChange={handleStrokesChange}
        ref={canvasRef}
        brushColor="#0000FF"
        brushSize={8}
        brushFamily="pen"
        style={{ flex: 1 }}
      /> : <InkCanvas initialStrokes={initialStrokes} style={{ flex: 1 }} />}
      <Button title="Clear" onPress={handleClear} />
      <Button title={isEditing ? "Stop Editing" : "Edit"} onPress={handleEditToggle} />
    </View>
  );
}
