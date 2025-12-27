import React, { forwardRef, useImperativeHandle, useRef } from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  findNodeHandle,
  StyleSheet,
  ViewStyle,
  StyleProp,
  NativeSyntheticEvent,
} from 'react-native';

const COMPONENT_NAME = 'InkEditorView';

interface StrokesChangeEvent {
  strokes: string;
}

interface TextFieldsChangeEvent {
  textFields: string;
}

interface InkEditorNativeProps {
  style?: StyleProp<ViewStyle>;
  brushColor?: string;
  brushSize?: number;
  brushFamily?: 'pen' | 'marker' | 'highlighter';
  brushOpacity?: number;
  mode?: 'draw' | 'text';
  strokes?: string;
  textFields?: string;
  onStrokesChange?: (event: NativeSyntheticEvent<StrokesChangeEvent>) => void;
  onTextFieldsChange?: (
    event: NativeSyntheticEvent<TextFieldsChangeEvent>,
  ) => void;
}

export interface InkEditorRef {
  clear: () => void;
}

export type InkEditorBrushFamily = 'pen' | 'marker' | 'highlighter';

export type InkEditorMode = 'draw' | 'text' | null;

export interface InkEditorBrushInfo {
  color: string;
  size: number;
  family: InkEditorBrushFamily;
  opacity?: number;
}

export interface InkEditorProps {
  style?: StyleProp<ViewStyle>;
  /**
   * The color of the brush stroke in hex format (e.g., "#000000").
   * @default "#000000"
   */
  brushColor?: string;
  /**
   * The size of the brush stroke in pixels.
   * @default 5
   */
  brushSize?: number;
  /**
   * The type of brush family to use.
   * - "pen": Pressure-sensitive pen (default)
   * - "marker": Solid marker
   * - "highlighter": Semi-transparent highlighter
   * @default "pen"
   */
  brushFamily?: InkEditorBrushFamily;
  /**
   * The opacity of the brush stroke (0.0 to 1.0).
   * Currently only applies to the highlighter brush.
   * @default 1.0 (or 0.5 for highlighter)
   */
  brushOpacity?: number;
  /**
   * The editor mode.
   * - "draw": Drawing mode - touch creates ink strokes (default)
   * - "text": Text mode - tap creates/edits text fields
   * @default "draw"
   */
  mode?: InkEditorMode;
  /**
   * The strokes to display/edit.
   * This should be a JSON string.
   */
  strokes?: string;
  /**
   * The text fields to display/edit.
   * This should be a JSON string.
   */
  textFields?: string;
  /**
   * Callback fired when strokes change (after a stroke is finished or cleared).
   * The callback receives a serialized JSON string of all strokes.
   */
  onStrokesChange?: (strokesJson: string) => void;
  /**
   * Callback fired when text fields change.
   * The callback receives a serialized JSON string of all text fields.
   */
  onTextFieldsChange?: (textFieldsJson: string) => void;
}

const NativeInkEditor =
  Platform.OS === 'android'
    ? requireNativeComponent<InkEditorNativeProps>(COMPONENT_NAME)
    : null;

/**
 * InkEditor is a React Native component that wraps the Android Ink API
 * for low-latency stylus/touch drawing with editing capabilities.
 * For display-only, use InkCanvas instead.
 *
 * Note: This component is only available on Android.
 */
export const InkEditor = forwardRef<InkEditorRef, InkEditorProps>(
  (
    {
      style,
      brushColor = '#000000',
      brushSize = 5,
      brushFamily = 'pen',
      brushOpacity,
      mode = 'draw',
      strokes,
      textFields,
      onStrokesChange,
      onTextFieldsChange,
    },
    ref,
  ) => {
    const nativeRef = useRef(null);

    const dispatchCommand = (commandName: string, args: unknown[] = []) => {
      if (Platform.OS === 'android' && nativeRef.current) {
        const commands =
          UIManager.getViewManagerConfig(COMPONENT_NAME)?.Commands;
        if (commands?.[commandName] !== undefined) {
          UIManager.dispatchViewManagerCommand(
            findNodeHandle(nativeRef.current),
            commands[commandName],
            args,
          );
        }
      }
    };

    useImperativeHandle(ref, () => ({
      clear: () => {
        dispatchCommand('clear');
      },
    }));

    const handleStrokesChange = (
      event: NativeSyntheticEvent<StrokesChangeEvent>,
    ) => {
      onStrokesChange?.(event.nativeEvent.strokes);
    };

    const handleTextFieldsChange = (
      event: NativeSyntheticEvent<TextFieldsChangeEvent>,
    ) => {
      onTextFieldsChange?.(event.nativeEvent.textFields);
    };

    if (Platform.OS !== 'android' || !NativeInkEditor) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkEditor
        ref={nativeRef}
        style={[styles.container, style]}
        brushColor={brushColor}
        brushSize={brushSize}
        brushFamily={brushFamily}
        brushOpacity={brushOpacity}
        mode={mode ?? undefined}
        strokes={strokes}
        textFields={textFields}
        onStrokesChange={handleStrokesChange}
        onTextFieldsChange={handleTextFieldsChange}
      />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
