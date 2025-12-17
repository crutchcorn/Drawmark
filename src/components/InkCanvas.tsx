import React, {forwardRef, useImperativeHandle, useRef, useEffect} from 'react';
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

const COMPONENT_NAME = 'InkCanvasView';

interface StrokesChangeEvent {
  strokes: string;
}

interface InkCanvasNativeProps {
  style?: StyleProp<ViewStyle>;
  brushColor?: string;
  brushSize?: number;
  brushFamily?: 'pen' | 'marker' | 'highlighter';
  onStrokesChange?: (event: NativeSyntheticEvent<StrokesChangeEvent>) => void;
}

export interface InkCanvasRef {
  clear: () => void;
  loadStrokes: (strokesJson: string) => void;
}

export interface InkCanvasProps {
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
  brushFamily?: 'pen' | 'marker' | 'highlighter';
  /**
   * Initial strokes to load when the canvas mounts.
   * This should be a JSON string from a previous save.
   */
  initialStrokes?: string;
  /**
   * Callback fired when strokes change (after a stroke is finished or cleared).
   * The callback receives a serialized JSON string of all strokes.
   */
  onStrokesChange?: (strokesJson: string) => void;
}

const NativeInkCanvas =
  Platform.OS === 'android'
    ? requireNativeComponent<InkCanvasNativeProps>(COMPONENT_NAME)
    : null;

/**
 * InkCanvas is a React Native component that wraps the Android Ink API
 * for low-latency stylus/touch drawing.
 *
 * Note: This component is only available on Android.
 */
export const InkCanvas = forwardRef<InkCanvasRef, InkCanvasProps>(
  (
    {
      style,
      brushColor = '#000000',
      brushSize = 5,
      brushFamily = 'pen',
      initialStrokes,
      onStrokesChange,
    },
    ref,
  ) => {
    const nativeRef = useRef(null);
    const hasLoadedInitialStrokes = useRef(false);

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
      loadStrokes: (strokesJson: string) => {
        dispatchCommand('loadStrokes', [strokesJson]);
      },
    }));

    // Load initial strokes when the component mounts
    useEffect(() => {
      if (
        initialStrokes &&
        !hasLoadedInitialStrokes.current &&
        nativeRef.current
      ) {
        // Small delay to ensure the native view is ready
        const timer = setTimeout(() => {
          dispatchCommand('loadStrokes', [initialStrokes]);
          hasLoadedInitialStrokes.current = true;
        }, 100);
        return () => clearTimeout(timer);
      }
    }, [initialStrokes]);

    const handleStrokesChange = (
      event: NativeSyntheticEvent<StrokesChangeEvent>,
    ) => {
      onStrokesChange?.(event.nativeEvent.strokes);
    };

    if (Platform.OS !== 'android' || !NativeInkCanvas) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkCanvas
        ref={nativeRef}
        style={[styles.container, style]}
        brushColor={brushColor}
        brushSize={brushSize}
        brushFamily={brushFamily}
        onStrokesChange={handleStrokesChange}
      />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
