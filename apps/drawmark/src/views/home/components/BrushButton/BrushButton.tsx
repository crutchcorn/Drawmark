import * as React from 'react';
import { StyleSheet, Pressable, Text, View } from 'react-native';
import * as PopoverPrimitive from '@rn-primitives/popover';
import Animated, { FadeIn, FadeOut } from 'react-native-reanimated';
import { InkEditorBrushFamily } from '../../../../components/InkEditor';
import { useRef } from 'react';
import { Colors } from '../../constants/colors';

const ColorValues = Object.values(Colors);

interface BrushColorButtonProps {
  color: string;
  setColor: (color: string) => void;
}

function BrushColorButton({
  color,
  setColor
}: BrushColorButtonProps) {
  return <View className="bg-white p-12">
    {ColorValues.map((Color) =>
      <Pressable key={Color} onPress={() => setColor(Color)} className={"relative w-12 h-12 rounded-full"} style={{backgroundColor: Color}}>
        {color === Color ? <View className={"absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-4 h-4 rounded-full bg-white"}/> : null}
      </Pressable>)
    }
  </View>
}

interface BrushButtonProps {
  family: InkEditorBrushFamily;
  currentFamily: InkEditorBrushFamily;
  setFamily: (family: InkEditorBrushFamily) => void;
  color: string;
  setColor: (color: string) => void;
  Icon: React.FC<{activeColor: string, className: string}>;
}

export function BrushButton({family, currentFamily, setFamily, color, setColor, Icon}: BrushButtonProps) {
  const ref = useRef<PopoverPrimitive.TriggerRef>(null);
  return (
    <PopoverPrimitive.Root>
      <PopoverPrimitive.Trigger ref={ref}>
        <Pressable onPress={() => {
            if (family === currentFamily) {
              ref.current?.open();
              return;
            }
            setFamily(family)
          }}
          className={`flex columns relative`}
        >
          <Icon activeColor={color} className={`w-12 h-12 mb-12`}/>
          {currentFamily === family ? <Text className="text-6xl absolute bottom-0 left-1/2 transform -translate-x-1/2">⋅</Text> : null}
        </Pressable>
      </PopoverPrimitive.Trigger>
      <PopoverPrimitive.Portal>
        <PopoverPrimitive.Overlay style={StyleSheet.absoluteFill}>
          <Animated.View entering={FadeIn.duration(200)} exiting={FadeOut}>
            <PopoverPrimitive.Content
              align={'center'}
              sideOffset={4}
            >
              <BrushColorButton color={color} setColor={setColor}/>
            </PopoverPrimitive.Content>
          </Animated.View>
        </PopoverPrimitive.Overlay>
      </PopoverPrimitive.Portal>
    </PopoverPrimitive.Root>
  );
}
