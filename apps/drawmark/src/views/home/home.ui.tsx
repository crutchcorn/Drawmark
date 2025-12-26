import { RefObject } from 'react';
import { Pressable, Text, View } from 'react-native';
import {
  InkEditor,
  InkEditorBrushFamily,
  InkEditorBrushInfo,
  InkEditorMode,
  InkEditorRef,
} from '../../components/InkEditor';
import { InkCanvas } from '../../components/InkCanvas';
import { PenIcon } from '../../components/Icons/pen';
import { MarkerIcon } from '../../components/Icons/marker';
import { HighlighterIcon } from '../../components/Icons/highlighter';
import { BrushButton } from './components/BrushButton/BrushButton';
import Ionicons from '@react-native-vector-icons/ionicons';

interface HomeUIProps {
  canvasRef: RefObject<InkEditorRef | null>;
  initialStrokes: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  brushInfo: InkEditorBrushInfo;
  setBrushInfo: (info: InkEditorBrushInfo) => void;
  editingMode: InkEditorMode;
  setEditingMode: (mode: InkEditorMode) => void;
}

export function HomeUI({
  canvasRef,
  initialStrokes,
  handleStrokesChange,
  brushInfo,
  setBrushInfo,
  editingMode,
  setEditingMode,
}: HomeUIProps) {
  const handleClear = () => {
    canvasRef.current?.clear();
  };

  const handleEditToggle = () => {
    setEditingMode('draw');
  };

  const save = () => {
    setEditingMode(null);
  };

  const setFamily = (family: InkEditorBrushFamily) => {
    setBrushInfo({ ...brushInfo, family });
    setEditingMode('draw');
  };

  const setColor = (color: string) => setBrushInfo({ ...brushInfo, color });

  return (
    <View className="safe relative flex-1">
      {editingMode ? (
        <InkEditor
          initialStrokes={initialStrokes}
          onStrokesChange={handleStrokesChange}
          ref={canvasRef}
          brushColor={brushInfo.color}
          brushSize={brushInfo.size}
          brushFamily={brushInfo.family}
          mode={editingMode}
          style={{ flex: 1 }}
        />
      ) : (
        <InkCanvas initialStrokes={initialStrokes} style={{ flex: 1 }} />
      )}
      {editingMode ? (
        <View className="absolute bottom-12 w-full pr-10 pl-10">
          <View
            className={`flex flex-row items-center space-x-4 rounded-full border-2 border-gray-300 bg-white pr-4 pl-4 shadow-lg`}
          >
            <BrushButton
              Icon={PenIcon}
              family="pen"
              currentFamily={brushInfo.family}
              setFamily={setFamily}
              color={brushInfo.color}
              setColor={setColor}
              editingMode={editingMode}
            />
            <BrushButton
              Icon={MarkerIcon}
              family="marker"
              currentFamily={brushInfo.family}
              setFamily={setFamily}
              color={brushInfo.color}
              setColor={setColor}
              editingMode={editingMode}
            />
            <BrushButton
              Icon={HighlighterIcon}
              family="highlighter"
              currentFamily={brushInfo.family}
              setFamily={setFamily}
              color={brushInfo.color}
              setColor={setColor}
              editingMode={editingMode}
            />
            <Pressable
              onPress={() => setEditingMode('text')}
              className={`rounded-full p-2 ${
                editingMode === 'text' ? 'bg-gray-200' : ''
              }`}
            >
              <Text
                className={`text-xl font-bold ${
                  editingMode === 'text' ? 'text-black' : 'text-gray-500'
                }`}
              >
                T
              </Text>
            </Pressable>
            <View className="grow" />
            <Pressable
              className="ml-2 rounded-full border-2 border-orange-400 p-4 pr-8 pl-8"
              onPress={handleClear}
            >
              <Text>Clear</Text>
            </Pressable>
            <Pressable
              className="ml-2 rounded-full bg-orange-400 p-4 pr-8 pl-8"
              onPress={save}
            >
              <Text>Save</Text>
            </Pressable>
          </View>
        </View>
      ) : (
        <Pressable
          onPress={handleEditToggle}
          className="absolute right-14 bottom-14 flex items-center justify-center rounded-full bg-orange-300 p-4 shadow-lg"
        >
          <Ionicons name={'create-outline'} size={32} />
        </Pressable>
      )}
    </View>
  );
}
