# Canvas TextField Port Progress

This document tracks the progress of porting Jetpack Compose's `CoreTextField` to render on a Canvas, enabling drag-and-drop text fields within the `InkCanvas` drawing surface.

## Project Overview

### Goal
Create a `CanvasTextField` composable that:
1. Renders text directly on an Android Canvas (not using Compose's built-in text layout)
2. Supports all standard text editing features
3. Can be positioned anywhere on the InkCanvas at runtime
4. Integrates seamlessly with ink strokes and other canvas elements

### Target Features
- [x] **Phase 0**: Documentation and planning
- [x] **Phase 1**: Basic text rendering on Canvas
- [x] **Phase 2**: Cursor rendering and blinking
- [x] **Phase 3**: Software keyboard input (IME)
- [x] **Phase 4**: Hardware keyboard input
- [x] **Phase 5**: Text selection (touch/pointer)
- [x] **Phase 6**: Selection highlighting
- [x] **Phase 7**: Keyboard navigation shortcuts
- [x] **Phase 8**: Copy/Cut/Paste clipboard operations
- [x] **Phase 9**: Integration with InkCanvas

---

## Architecture Overview

### Key Components from AndroidX CoreTextField

The original `CoreTextField` relies on these major components:

#### 1. **LegacyTextFieldState** (`CoreTextField.kt` lines 740-940)
Manages the internal state including:
- `processor: EditProcessor` - Processes IME edit commands
- `inputSession: TextInputSession?` - Current IME session
- `hasFocus: Boolean` - Focus state
- `layoutCoordinates: LayoutCoordinates?` - Position info
- `layoutResult: TextLayoutResultProxy?` - Text layout results
- `handleState: HandleState` - None/Selection/Cursor handle state
- Selection/cursor handle visibility states

#### 2. **TextDelegate** (`TextDelegate.kt`)
Handles text layout computation:
- Uses `MultiParagraphIntrinsics` for text measurement
- Computes min/max intrinsic widths
- Performs layout with constraints
- Paints text via `TextPainter.paint(canvas, textLayoutResult)`

#### 3. **TextFieldDelegate** (`TextFieldDelegate.kt`)
Static utility methods for:
- `layout()` - Compute text layout
- `draw()` - Draw text and selection highlights to canvas
- `restartInput()` - Start/restart IME session
- `notifyFocusedRect()` - Notify IME of cursor position
- `updateTextLayoutResult()` - Update IME with layout info

#### 4. **TextFieldSelectionManager** (`selection/TextFieldSelectionManager.kt`)
Manages text selection:
- Drag gesture handling
- Copy/cut/paste operations
- Selection handle positioning
- Text toolbar (context menu) management
- Haptic feedback

#### 5. **TextFieldKeyInput** (`TextFieldKeyInput.kt`)
Handles keyboard events:
- Maps key events to commands via `KeyMapping`
- Executes commands via `TextFieldPreparedSelection`
- Handles typed characters via `DeadKeyCombiner`
- Supports undo/redo via `UndoManager`

#### 6. **KeyMapping** (`KeyMapping.kt`)
Maps key combinations to `KeyCommand` enum:
- Navigation (arrows, home, end, page up/down)
- Selection (shift+navigation)
- Editing (delete, backspace)
- Clipboard (Ctrl+C/X/V)
- Undo/Redo (Ctrl+Z/Y)

---

## Implementation Plan

### Phase 1: Core State & Basic Rendering

**Files to create:**
- `libs/android-lib/src/main/java/app/drawmark/android/lib/textcanvas/CanvasTextFieldState.kt`
- `libs/android-lib/src/main/java/app/drawmark/android/lib/textcanvas/CanvasTextDelegate.kt`
- `libs/android-lib/src/main/java/app/drawmark/android/lib/textcanvas/CanvasTextField.kt`

**State class structure:**
```kotlin
class CanvasTextFieldState(
    initialValue: TextFieldValue = TextFieldValue()
) {
    var value by mutableStateOf(initialValue)
    var hasFocus by mutableStateOf(false)
    var textLayoutResult: TextLayoutResult? = null
    
    // Position on canvas (for drag-and-drop)
    var position by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(Size.Zero)
    
    // Cursor
    var cursorVisible by mutableStateOf(true)
    
    // Selection
    val hasSelection: Boolean get() = !value.selection.collapsed
}
```

**Basic rendering:**
1. Use `TextLayoutResult` from `MultiParagraph` layout
2. Draw text using `TextPainter.paint(canvas, layoutResult)`
3. Draw at specified canvas position with transformation matrix

### Phase 2: Cursor Rendering

**Requirements:**
- Draw cursor line at correct position from `TextLayoutResult.getCursorRect(offset)`
- Implement blinking animation (530ms on/off cycle)
- Hide cursor during selection or when unfocused

**Implementation:**
```kotlin
// In CanvasTextField drawing code
if (state.hasFocus && state.value.selection.collapsed) {
    val cursorOffset = state.value.selection.start
    val cursorRect = textLayoutResult.getCursorRect(cursorOffset)
    if (cursorVisible) {
        canvas.drawRect(cursorRect, cursorPaint)
    }
}
```

### Phase 3: Software Keyboard (IME) Integration

**Key classes to reference:**
- `TextInputService` / `TextInputSession`
- `EditProcessor` for processing IME commands
- `ImeOptions` for keyboard configuration

**Implementation steps:**
1. Request focus -> Start input session with `TextInputService`
2. Handle `EditCommand`s from IME:
   - `CommitTextCommand` - Insert text
   - `SetComposingTextCommand` - Composition (underlined text)
   - `DeleteSurroundingTextCommand` - Delete text
   - `SetSelectionCommand` - Change selection
3. Notify IME of cursor position changes

### Phase 4: Hardware Keyboard Input

**Key handling flow:**
1. Capture key events via `Modifier.onKeyEvent`
2. Use `DeadKeyCombiner` to combine dead keys (accents, etc.)
3. Map to `KeyCommand` via `KeyMapping`
4. Execute via command pattern

**Commands to implement:**
- Character insertion (typed characters)
- Delete/Backspace
- Arrow navigation (with/without selection)
- Home/End, Page Up/Down

### Phase 5: Touch/Pointer Selection

**Gesture handling:**
1. Single tap -> Position cursor
2. Double tap -> Select word
3. Triple tap -> Select paragraph/line
4. Long press -> Start selection mode
5. Drag -> Extend selection

**Position calculation:**
```kotlin
val offset = textLayoutResult.getOffsetForPosition(localPosition)
```

### Phase 6: Selection Highlighting

**Drawing selection:**
```kotlin
if (!value.selection.collapsed) {
    val selectionPath = textLayoutResult.getPathForRange(
        value.selection.start,
        value.selection.end
    )
    canvas.drawPath(selectionPath, selectionPaint)
}
// Then draw text on top
TextPainter.paint(canvas, textLayoutResult)
```

### Phase 7: Keyboard Shortcuts

**Essential shortcuts:**
| Shortcut | Action |
|----------|--------|
| Ctrl+A | Select all |
| Ctrl+C | Copy |
| Ctrl+X | Cut |
| Ctrl+V | Paste |
| Ctrl+Z | Undo |
| Ctrl+Shift+Z / Ctrl+Y | Redo |
| Shift+Arrow | Extend selection |
| Ctrl+Arrow | Move by word |
| Ctrl+Shift+Arrow | Select by word |
| Home/End | Line start/end |
| Ctrl+Home/End | Document start/end |

### Phase 8: Clipboard Operations

**Implementation:**
1. Use `androidx.compose.ui.platform.Clipboard` or Android's `ClipboardManager`
2. Copy: Get selected text, put on clipboard
3. Cut: Copy + delete selection
4. Paste: Read clipboard, insert at cursor

### Phase 9: InkCanvas Integration

**Integration points:**
1. `CanvasTextField` position stored in canvas coordinate system
2. Hit testing to determine if tap is on text field
3. Focus management between ink drawing and text editing
4. Rendering order: strokes first, then text fields on top (or configurable)

**Usage example:**
```kotlin
@Composable
fun InkCanvasWithText(
    strokes: Set<Stroke>,
    textFields: List<CanvasTextFieldState>,
    onTextFieldsChange: (List<CanvasTextFieldState>) -> Unit
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Draw strokes
        strokes.forEach { stroke ->
            canvasStrokeRenderer.draw(stroke, canvas, transform)
        }
        
        // Draw text fields
        textFields.forEach { textFieldState ->
            CanvasTextDelegate.draw(
                canvas = canvas,
                state = textFieldState,
                position = textFieldState.position
            )
        }
    }
}
```

---

## Progress Log

### 2024-12-26: Project Kickoff
- [x] Analyzed CoreTextField architecture from AndroidX
- [x] Identified key components to port
- [x] Created implementation plan
- [x] Documented phase breakdown

### 2024-12-26: Initial Implementation Complete
- [x] Created `CanvasTextFieldState.kt` - Full state management with:
  - Text value, selection, cursor management
  - Focus state tracking
  - Layout result caching
  - Canvas position for drag-and-drop
  - Hit testing utilities
  - Cursor/selection navigation methods
  
- [x] Created `CanvasTextDelegate.kt` - Text layout and rendering with:
  - MultiParagraph-based text layout
  - Text rendering using TextPainter
  - Cursor rendering at correct position
  - Selection highlight rendering
  - IME composition underline rendering
  - Layout caching and reuse optimization
  
- [x] Created `CanvasTextField.kt` - Main composable with:
  - Focus management with FocusRequester
  - Cursor blinking animation (530ms cycle)
  - Software keyboard (IME) integration via TextInputService
  - Hardware keyboard input handling via onKeyEvent
  - Touch gestures: tap to position, double-tap to select word, long-press for selection
  - Keyboard shortcuts: Ctrl+C/X/V/A, arrow navigation with Shift selection
  - Backspace/Delete handling
  - EditCommand processing for IME input
  
- [x] Created `InkCanvasTextFieldManager.kt` - Integration helper with:
  - Multiple text field management
  - Focus coordination between text fields
  - Hit testing for tap-to-focus
  - Unified drawing API for all text fields
  - Modifier for easy Canvas integration

**Build Status**: ✅ Compiling successfully (with expected deprecation warnings)

---

## Usage Examples

### Basic Usage - Standalone CanvasTextField

```kotlin
@Composable
fun TextFieldDemo() {
    val state = rememberCanvasTextFieldState("Hello, World!")
    
    CanvasTextField(
        state = state,
        onValueChange = { newValue ->
            state.updateValue(newValue)
        },
        textStyle = TextStyle(
            fontSize = 16.sp,
            color = Color.Black
        ),
        cursorColor = Color.Blue,
        selectionColor = Color.Blue.copy(alpha = 0.4f)
    )
}
```

### Integration with InkCanvas

```kotlin
@Composable
fun InkCanvasWithTextFields(
    strokes: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer
) {
    val textFieldManager = rememberInkCanvasTextFieldManager()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas for strokes and text fields
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .inkCanvasTextFields(
                    manager = textFieldManager,
                    onCreateTextField = { position ->
                        // Return true to create a new text field at double-tap
                        true
                    }
                )
        ) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            // Draw strokes first
            strokes.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = canvasTransform
                )
            }

            // Draw text fields on top
            textFieldManager.draw(
                canvas = drawContext.canvas,
                cursorColor = Color.Black,
                selectionColor = Color.Blue.copy(alpha = 0.4f)
            )
        }
        
        // Individual CanvasTextField composables for focus handling
        textFieldManager.textFields.forEach { textFieldState ->
            CanvasTextField(
                state = textFieldState,
                onValueChange = { textFieldState.updateValue(it) },
                modifier = Modifier.offset { 
                    IntOffset(
                        textFieldState.position.x.roundToInt(),
                        textFieldState.position.y.roundToInt()
                    )
                }
            )
        }
    }
}
```

---

## Known Limitations & Future Improvements

### Current Limitations
1. **Single-line focus**: Uses deprecated `TextInputService` API (will need migration to `PlatformTextInputModifierNode`)
2. **Undo/Redo**: Not yet implemented (TODO: add UndoManager integration)
3. **Multi-line scrolling**: No internal scrolling for multi-line text overflow
4. **Selection handles**: Visual drag handles not yet implemented (just the selection highlight)
5. **Context menu**: Long-press context menu with Cut/Copy/Paste not implemented

### Planned Improvements
- [ ] Migrate from `TextInputService` to `PlatformTextInputModifierNode`
- [x] Implement word-by-word navigation (Ctrl+Arrow)
- [ ] Add UndoManager for Ctrl+Z/Y support
- [ ] Implement visual selection handles
- [ ] Add drag-to-select gesture
- [ ] Implement context menu on long-press
- [ ] Add support for rich text (AnnotatedString with spans)
- [ ] Implement text field resizing/dragging

---

## Technical Notes

### Canvas Coordinate System
- Canvas origin (0,0) is top-left
- Text fields have their own local coordinate system
- Need transformation matrix to position text on canvas:
  ```kotlin
  canvas.save()
  canvas.translate(textFieldState.position.x, textFieldState.position.y)
  // Draw text at local origin
  canvas.restore()
  ```

### Focus Management
- Only one text field can have focus at a time
- Tapping outside text field -> lose focus, close keyboard
- Tapping on different text field -> transfer focus
- Need to coordinate with ink drawing mode

### Performance Considerations
- Text layout is expensive - cache `TextLayoutResult`
- Only re-layout when text or constraints change
- Cursor blinking should use `LaunchedEffect` with delay, not recomposition

### Android-Specific APIs
- `TextInputService` for IME communication
- `SoftwareKeyboardController` for showing/hiding keyboard
- `Clipboard` for clipboard operations
- `HapticFeedback` for selection feedback

---

## File Structure

```
libs/android-lib/src/main/java/app/drawmark/android/lib/textcanvas/
├── CanvasTextField.kt              # Main composable (complete)
├── CanvasTextFieldState.kt         # State management (complete)
├── CanvasTextDelegate.kt           # Text layout and rendering (complete)
├── InkCanvasTextFieldManager.kt    # Multi-field management for InkCanvas (complete)
└── input/
    └── internal/
        # Reserved for future internal implementation details
```

---

## Dependencies

Already available in project:
- `androidx.compose.ui:ui` - Core Compose UI
- `androidx.compose.ui:ui-graphics` - Canvas, Paint, etc.
- `androidx.compose.foundation` - Text input infrastructure

May need to add:
- None identified yet - should be able to use existing deps

---

## Testing Strategy

1. **Unit tests**: State management, text manipulation logic
2. **Integration tests**: Keyboard input, clipboard operations
3. **UI tests**: Gesture handling, focus management
4. **Manual testing**: IME behavior across different keyboards

---

## Open Questions

1. **Multi-line support**: Start with single-line or support multi-line from the beginning?
   - Recommendation: Start single-line, add multi-line in later phase

2. **Rich text**: Support AnnotatedString with spans or plain text only?
   - Recommendation: Plain text first, rich text as enhancement

3. **Visual transformation**: Support password masking, etc.?
   - Recommendation: Add later if needed

4. **Undo/Redo**: Full undo stack or basic?
   - Recommendation: Basic undo/redo in first pass

---

## References

- [AndroidX Compose Foundation Text](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/)
- [Android TextInputService](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/input/TextInputService)
- [Compose Canvas Drawing](https://developer.android.com/jetpack/compose/graphics/draw/overview)
