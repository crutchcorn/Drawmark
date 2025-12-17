import React, { useCallback, useRef } from 'react';
import {
  requireNativeComponent,
  ViewStyle,
  StyleProp,
  UIManager,
  findNodeHandle,
  Platform,
} from 'react-native';

interface AndroidWidgetHostProps {
  widgetId: number;
  style?: StyleProp<ViewStyle>;
}

const AndroidWidgetHostNative =
  Platform.OS === 'android'
    ? requireNativeComponent<AndroidWidgetHostProps>('AndroidWidgetHost')
    : null;

interface WidgetHostProps {
  widgetId: number;
  style?: StyleProp<ViewStyle>;
}

export function WidgetHost({ widgetId, style }: WidgetHostProps) {
  const ref = useRef(null);

  const refresh = useCallback(() => {
    if (Platform.OS !== 'android' || !ref.current) return;

    const handle = findNodeHandle(ref.current);
    if (handle) {
      UIManager.dispatchViewManagerCommand(handle, 'refresh', []);
    }
  }, []);

  if (Platform.OS !== 'android' || !AndroidWidgetHostNative) {
    return null;
  }

  return (
    <AndroidWidgetHostNative
      ref={ref}
      widgetId={widgetId}
      style={style}
    />
  );
}

export default WidgetHost;
