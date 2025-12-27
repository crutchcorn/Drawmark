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
import type { ToolbarState, BrushSettings } from '../../constants/toolbar';

interface HomeUIProps {
  canvasRef: RefObject<InkEditorRef | null>;
  strokes: string | undefined;
  textFields: string | undefined;
  handleStrokesChange: (strokesJson: string) => void;
  handleTextFieldsChange: (textFieldsJson: string) => void;
  toolbarState: ToolbarState;
  activeBrushInfo: InkEditorBrushInfo;
  setActiveFamily: (family: InkEditorBrushFamily) => void;
  setEditingMode: (mode: InkEditorMode) => void;
  setBrushSettings: (family: InkEditorBrushFamily, settings: Partial<BrushSettings>) => void;
}

export function HomeUI({
  canvasRef,
  strokes,
  textFields,
  handleStrokesChange,
  handleTextFieldsChange,
  toolbarState,
  activeBrushInfo,
  setActiveFamily,
  setEditingMode,
  setBrushSettings,
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

  const handleSetFamily = (family: InkEditorBrushFamily) => {
    setActiveFamily(family);
    setEditingMode('draw');
  };

  return (
    <View className="safe relative flex-1">
      {toolbarState.editingMode ? (
        <InkEditor
          strokes={strokes}
          textFields={textFields}
          onStrokesChange={handleStrokesChange}
          onTextFieldsChange={handleTextFieldsChange}
          ref={canvasRef}
          brushColor={activeBrushInfo.color}
          brushSize={activeBrushInfo.size}
          brushFamily={activeBrushInfo.family}
          brushOpacity={activeBrushInfo.opacity}
          mode={toolbarState.editingMode}
          style={{ flex: 1 }}
        />
      ) : (
        <InkCanvas strokes={strokes} textFields={textFields} style={{ flex: 1 }} />
      )}
      {toolbarState.editingMode ? (
        <View className="absolute bottom-12 w-full pr-10 pl-10">
          <View
            className={`flex flex-row items-center space-x-4 rounded-full border-2 border-gray-300 bg-white pr-4 pl-4 shadow-lg`}
          >
            <BrushButton
              Icon={PenIcon}
              family="pen"
              currentFamily={toolbarState.activeFamily}
              setFamily={handleSetFamily}
              color={toolbarState.brushes.pen.color}
              setColor={(color) => setBrushSettings('pen', { color })}
              editingMode={toolbarState.editingMode}
            />
            <BrushButton
              Icon={MarkerIcon}
              family="marker"
              currentFamily={toolbarState.activeFamily}
              setFamily={handleSetFamily}
              color={toolbarState.brushes.marker.color}
              setColor={(color) => setBrushSettings('marker', { color })}
              editingMode={toolbarState.editingMode}
            />
            <BrushButton
              Icon={HighlighterIcon}
              family="highlighter"
              currentFamily={toolbarState.activeFamily}
              setFamily={handleSetFamily}
              color={toolbarState.brushes.highlighter.color}
              setColor={(color) => setBrushSettings('highlighter', { color })}
              editingMode={toolbarState.editingMode}
            />
            <Pressable
              onPress={() => setEditingMode('text')}
              className={`rounded-full p-2 ${
                toolbarState.editingMode === 'text' ? 'bg-gray-200' : ''
              }`}
            >
              <Text
                className={`text-xl font-bold ${
                  toolbarState.editingMode === 'text' ? 'text-black' : 'text-gray-500'
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
