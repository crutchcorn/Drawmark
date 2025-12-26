import * as React from 'react';
import { StyleSheet, Pressable, Text, View } from 'react-native';
import * as PopoverPrimitive from '@rn-primitives/popover';
import Animated, { FadeIn, FadeOut } from 'react-native-reanimated';
import { InkEditorBrushFamily } from '../../../../components/InkEditor';
import { useRef } from 'react';
import { Colors } from '../../constants/colors';
import { VariableContextProvider } from 'nativewind';
import Color from 'color';
import { splitInto } from '../../../../utils/split-into';

const ColorValues = Object.values(Colors);

interface BrushColorButtonProps {
  currentColor: string;
  setColor: (color: string) => void;
}

function BrushColorButton({ currentColor, setColor }: BrushColorButtonProps) {
  const ColorValuesRows = splitInto(ColorValues, 4);

  return (
    <View className="flex flex-col space-y-4 rounded-lg bg-white p-12 shadow-lg">
      {ColorValuesRows.map((row, i) => (
        <View key={i} className="flex flex-row space-x-4">
          {row.map(color => {
            const isColorDark = new Color(color).isDark();
            return (
              <VariableContextProvider
                key={color}
                value={{
                  '--background': color,
                }}
              >
                <Pressable
                  onPress={() => setColor(color)}
                  className={`relative flex h-12 w-12 items-center justify-center rounded-full bg-(--background)`}
                >
                  {currentColor === color ? (
                    <View
                      className={`h-4 w-4 rounded-full ${isColorDark ? 'bg-white' : 'bg-black'}`}
                    />
                  ) : null}
                </Pressable>
              </VariableContextProvider>
            );
          })}
        </View>
      ))}
    </View>
  );
}

interface BrushButtonProps {
  family: InkEditorBrushFamily;
  currentFamily: InkEditorBrushFamily;
  setFamily: (family: InkEditorBrushFamily) => void;
  color: string;
  setColor: (color: string) => void;
  Icon: React.FC<{ activeColor: string; className: string }>;
}

export function BrushButton({
  family,
  currentFamily,
  setFamily,
  color,
  setColor,
  Icon,
}: BrushButtonProps) {
  const ref = useRef<PopoverPrimitive.TriggerRef>(null);
  return (
    <PopoverPrimitive.Root>
      <PopoverPrimitive.Trigger ref={ref}>
        <Pressable
          onPress={() => {
            if (family === currentFamily) {
              ref.current?.open();
              return;
            }
            setFamily(family);
          }}
          className={`columns relative flex`}
        >
          <Icon activeColor={color} className={`mb-12 h-12 w-12`} />
          {currentFamily === family ? (
            <Text className="absolute bottom-0 left-1/2 -translate-x-1/2 transform text-6xl">
              ⋅
            </Text>
          ) : null}
        </Pressable>
      </PopoverPrimitive.Trigger>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Overlay style={StyleSheet.absoluteFill}>
          <Animated.View entering={FadeIn.duration(200)} exiting={FadeOut}>
            <PopoverPrimitive.Content align={'center'} sideOffset={4}>
              <BrushColorButton currentColor={color} setColor={setColor} />
            </PopoverPrimitive.Content>
          </Animated.View>
        </PopoverPrimitive.Overlay>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
}
