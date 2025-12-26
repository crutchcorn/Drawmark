import { RefObject } from 'react';
import { Pressable, Text, View } from 'react-native';
import {
  InkEditor, InkEditorBrushFamily,
  InkEditorBrushInfo,
  InkEditorRef,
} from '../../components/InkEditor';
import { InkCanvas } from '../../components/InkCanvas';
import { PenIcon } from '../../components/Icons/pen';
import { MarkerIcon } from '../../components/Icons/marker';
import { HighlighterIcon } from '../../components/Icons/highlighter';
import { BrushButton } from './components/BrushButton/BrushButton';

interface HomeUIProps {
  canvasRef: RefObject<InkEditorRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  isEditing: boolean;
  setIsEditing: (val: boolean) => void;
  brushInfo: InkEditorBrushInfo;
  setBrushInfo: (info: InkEditorBrushInfo) => void;
}

export function HomeUI({
  canvasRef,
  initialStrokes,
  handleStrokesChange,
  isEditing,
  setIsEditing,
  brushInfo,
  setBrushInfo,
}: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  const handleEditToggle = () => {
    setIsEditing(!isEditing);
  };

  const setFamily = (family: InkEditorBrushFamily) => setBrushInfo({ ...brushInfo, family });

  const setColor = (color: string) => setBrushInfo({ ...brushInfo, color });

  return (
    <View className="flex-1">
      {isEditing ? (
        <InkEditor
          initialStrokes={initialStrokes}
          onStrokesChange={handleStrokesChange}
          ref={canvasRef}
          brushColor={brushInfo.color}
          brushSize={brushInfo.size}
          brushFamily={brushInfo.family}
          style={{ flex: 1 }}
        />
      ) : (
        <InkCanvas initialStrokes={initialStrokes} style={{ flex: 1 }} />
      )}
      <Pressable onPress={handleClear}><Text>Clear</Text></Pressable>
      <View className={`flex flex-row space-x-4`}>
        <BrushButton Icon={PenIcon} family="pen" currentFamily={brushInfo.family} setFamily={setFamily} color={brushInfo.color} setColor={setColor} />
        <BrushButton Icon={MarkerIcon} family="marker" currentFamily={brushInfo.family} setFamily={setFamily} color={brushInfo.color} setColor={setColor} />
        <BrushButton Icon={HighlighterIcon} family="highlighter" currentFamily={brushInfo.family} setFamily={setFamily} color={brushInfo.color} setColor={setColor} />
      </View>
      <View>
        <Pressable
          onPress={() => setBrushInfo({ ...brushInfo, color: '#FF0000' })}
        ><Text>Red</Text></Pressable>
        <Pressable
          onPress={() => setBrushInfo({ ...brushInfo, color: '#0000FF' })}
        ><Text>Blue</Text></Pressable>
      </View>
      <Pressable
        onPress={handleEditToggle}
      ><Text>{isEditing ? 'Stop Editing' : 'Edit'}</Text></Pressable>
    </View>
  );
}
