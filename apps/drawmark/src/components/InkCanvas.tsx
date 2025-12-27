import React, { forwardRef } from 'react';
import {
  requireNativeComponent,
  Platform,
  StyleSheet,
  ViewStyle,
  StyleProp,
} from 'react-native';

const COMPONENT_NAME = 'InkCanvasView';

interface InkCanvasNativeProps {
  style?: StyleProp<ViewStyle>;
  initialStrokes?: string;
  initialTextFields?: string;
}

export interface InkCanvasRef {
  // Currently no imperative methods needed for read-only canvas
}

export interface InkCanvasProps {
  style?: StyleProp<ViewStyle>;
  /**
   * Initial strokes to load when the canvas mounts.
   * This should be a JSON string from a previous save.
   */
  initialStrokes?: string;
  /**
   * Initial text fields to load when the editor mounts.
   * This should be a JSON string from a previous save.
   */
  initialTextFields?: string;
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
  ({ style, initialStrokes, initialTextFields }, ref) => {
    if (Platform.OS !== 'android' || !NativeInkCanvas) {
      // Return null or a placeholder for non-Android platforms
      return null;
    }

    return (
      <NativeInkCanvas
        style={[styles.container, style]}
        initialStrokes={initialStrokes}
        initialTextFields={initialTextFields}
      />
    );
  },
);

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
});
