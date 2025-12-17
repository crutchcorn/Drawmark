import { NativeModules } from 'react-native';

const { StatusBarModule } = NativeModules;

export function expandNotificationPanel(): Promise<boolean> {
  return StatusBarModule.expandNotificationPanel();
}

export function expandQuickSettings(): Promise<boolean> {
  return StatusBarModule.expandQuickSettings();
}
