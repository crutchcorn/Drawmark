import React, {
  forwardRef,
  useImperativeHandle,
  useRef,
  useEffect,
} from 'react';
import {
  requireNativeComponent,
  UIManager,
  Platform,
  findNodeHandle,
  StyleSheet,
  ViewStyle,
  StyleProp,
} from 'react-native';

const COMPONENT_NAME = 'InkCanvasView';

interface InkCanvasNativeProps {
  style?: StyleProp<ViewStyle>;
}

export interface InkCanvasRef {
  loadStrokes: (strokesJson: string) => void;
}

export interface InkCanvasProps {
  style?: StyleProp<ViewStyle>;
  /**
   * Initial strokes to load when the canvas mounts.
   * This should be a JSON string from a previous save.
   */
  initialStrokes?: string;
}

const NativeInkCanvas =
  Platform.OS === 'android'
    ? requireNativeComponent<InkCanvasNativeProps>(COMPONENT_NAME)
    : null;

/**
 * InkCanvas is a React Native component that displays ink strokes (read-only).
 * For editing capabilities, use InkEditor instead.
 *
 * Note: This component is only available on Android.
 */
export const InkCanvas = forwardRef<InkCanvasRef, InkCanvasProps>(
  ({ style, initialStrokes }, ref) => {
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

    if (Platform.OS !== 'android' || !NativeInkCanvas) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkCanvas ref={nativeRef} style={[styles.container, style]} />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
