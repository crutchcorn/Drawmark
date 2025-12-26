package app.drawmark.android.lib.textcanvas

import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * A text field that renders directly on a Canvas.
 *
 * This composable provides text editing functionality that can be positioned
 * anywhere on a canvas, making it suitable for integration with drawing surfaces
 * like InkCanvas.
 *
 * @param state The state holder for this text field
 * @param onValueChange Callback when the text value changes
 * @param modifier Modifier for the text field
 * @param textStyle Style for the text
 * @param cursorColor Color of the cursor
 * @param selectionColor Color of the selection highlight
 * @param enabled Whether the text field is enabled
 * @param readOnly Whether the text field is read-only
 * @param singleLine Whether the text field is single-line
 * @param maxLines Maximum number of lines (ignored if singleLine is true)
 * @param imeOptions IME options for the software keyboard
 * @param onImeAction Callback when an IME action is performed
 * @param handlePointerInput Whether this composable should handle pointer input.
 *                           Set to false when managed by InkCanvas which handles gestures.
 */
@Composable
fun CanvasTextField(
    state: CanvasTextFieldState,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    cursorColor: Color = Color.Black,
    selectionColor: Color = Color.Blue.copy(alpha = 0.4f),
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    imeOptions: ImeOptions = ImeOptions.Default,
    onImeAction: (ImeAction) -> Unit = {},
    handlePointerInput: Boolean = true
) {
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val textInputService = LocalTextInputService.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current

    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    // When state.focusRequested becomes true (set by manager), request Compose focus
    LaunchedEffect(state.focusRequested) {
        if (state.focusRequested) {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
                // Clear the request flag after handling
                state.focusRequested = false
            } catch (_: Exception) {
                // Focus request may fail if component not yet attached
            }
        }
    }

    // Track enabled/readOnly in state
    state.enabled = enabled
    state.readOnly = readOnly

    // Create/update the text delegate
    var textDelegate by remember {
        mutableStateOf<CanvasTextDelegate?>(null)
    }

    textDelegate = CanvasTextDelegate.updateDelegate(
        current = textDelegate,
        text = AnnotatedString(state.text),
        style = textStyle,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        softWrap = !singleLine,
        maxLines = maxLines
    )

    // Perform layout
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    textDelegate?.let { delegate ->
        val constraints = Constraints(
            minWidth = 0,
            maxWidth = if (singleLine) Constraints.Infinity else 500, // TODO: Get from actual size
            minHeight = 0,
            maxHeight = Constraints.Infinity
        )

        textLayoutResult = delegate.layout(
            constraints = constraints,
            layoutDirection = LayoutDirection.Ltr,
            prevResult = textLayoutResult
        )

        textLayoutResult?.let { result ->
            state.textLayoutResult = result
            // Ensure minimum size even for empty text
            val minWidth = with(density) { 100.dp.toPx() }
            val minHeight = with(density) { 24.dp.toPx() }
            state.layoutSize = Size(
                maxOf(result.size.width.toFloat(), minWidth),
                maxOf(result.size.height.toFloat(), minHeight)
            )
        }
        
        // If no layout result yet, still set a minimum size
        if (textLayoutResult == null) {
            val minWidth = with(density) { 100.dp.toPx() }
            val minHeight = with(density) { 24.dp.toPx() }
            state.layoutSize = Size(minWidth, minHeight)
        }
    }
    
    // Ensure minimum layout size even if delegate is null
    if (state.layoutSize == Size.Zero) {
        val minWidth = with(density) { 100.dp.toPx() }
        val minHeight = with(density) { 24.dp.toPx() }
        state.layoutSize = Size(minWidth, minHeight)
    }

    // IME session management
    var inputSession by remember { mutableStateOf<TextInputSession?>(null) }

    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Handle IME input
    val onEditCommand: (List<EditCommand>) -> Unit = remember(state) {
        { commands ->
            val preValue = state.value
            var currentValue = state.value
            commands.forEach { command ->
                currentValue = command.applyTo(currentValue)
            }
            // Record undo if text actually changed
            if (preValue.text != currentValue.text) {
                state.undoManager.recordChange(preValue, currentValue)
            }
            state.updateValue(currentValue)
            currentOnValueChange(currentValue)
        }
    }

    // Focus handling
    val focusModifier = Modifier
        .focusRequester(focusRequester)
        .onFocusChanged { focusState ->
            val wasFocused = state.hasFocus
            state.hasFocus = focusState.isFocused

            if (focusState.isFocused && !wasFocused) {
                // Gained focus - start IME session
                if (enabled && !readOnly && textInputService != null) {
                    inputSession = textInputService.startInput(
                        value = state.value,
                        imeOptions = imeOptions,
                        onEditCommand = onEditCommand,
                        onImeActionPerformed = onImeAction
                    )
                }
            } else if (!focusState.isFocused && wasFocused) {
                // Lost focus - end IME session
                inputSession?.let {
                    textInputService?.stopInput(it)
                }
                inputSession = null
                state.handleState = HandleState.None
                
                // Clear selection when losing focus (collapse to cursor at end of selection)
                if (state.hasSelection) {
                    state.clearSelection()
                }
                
                // Clear undo/redo history when losing focus
                state.undoManager.clear()
            }
        }
        .focusable(enabled = enabled)

    // Cursor blinking
    LaunchedEffect(state.hasFocus, state.hasSelection) {
        if (state.hasFocus && !state.hasSelection) {
            while (isActive) {
                delay(530)
                state.cursorVisible = !state.cursorVisible
            }
        } else {
            state.cursorVisible = true
        }
    }

    // Update IME when value changes
    LaunchedEffect(state.value) {
        inputSession?.updateState(
            oldValue = null,
            newValue = state.value
        )
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            inputSession?.let { session ->
                textInputService?.stopInput(session)
            }
        }
    }

    // Keyboard input handling
    val keyboardModifier = Modifier.onKeyEvent { keyEvent ->
        if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
        if (!enabled) return@onKeyEvent false

        val isCtrl = keyEvent.isCtrlPressed
        val isShift = keyEvent.isShiftPressed
        val isAlt = keyEvent.isAltPressed
        val isMeta = keyEvent.isMetaPressed

        when {
            // Backspace (check first to handle Ctrl+Backspace before other Ctrl shortcuts)
            keyEvent.key == Key.Backspace -> {
                if (!readOnly) {
                    when {
                        // Meta+Backspace (Cmd+Backspace on Mac) - delete to line start
                        isMeta -> state.deleteToLineStart()
                        // Alt+Backspace or Ctrl+Backspace - delete word backward
                        isAlt || isCtrl -> state.deleteWordBackward()
                        // Regular backspace
                        else -> state.deleteBackward()
                    }
                    currentOnValueChange(state.value)
                }
                true
            }

            // Copy (Ctrl+C)
            isCtrl && keyEvent.key == Key.C -> {
                if (state.hasSelection) {
                    clipboardManager.setText(AnnotatedString(state.selectedText))
                }
                true
            }

            // Cut (Ctrl+X)
            isCtrl && keyEvent.key == Key.X -> {
                if (state.hasSelection && !readOnly) {
                    clipboardManager.setText(AnnotatedString(state.selectedText))
                    state.deleteSelection(allowMerge = false)
                    currentOnValueChange(state.value)
                }
                true
            }

            // Paste (Ctrl+V)
            isCtrl && keyEvent.key == Key.V -> {
                if (!readOnly) {
                    clipboardManager.getText()?.let { text ->
                        state.insertText(text.text, allowMerge = false)
                        currentOnValueChange(state.value)
                    }
                }
                true
            }

            // Select All (Ctrl+A)
            isCtrl && keyEvent.key == Key.A -> {
                state.selectAll()
                true
            }

            // Undo (Ctrl+Z)
            isCtrl && !isShift && keyEvent.key == Key.Z -> {
                if (state.undo()) {
                    currentOnValueChange(state.value)
                }
                true
            }

            // Redo (Ctrl+Shift+Z or Ctrl+Y)
            (isCtrl && isShift && keyEvent.key == Key.Z) || (isCtrl && keyEvent.key == Key.Y) -> {
                if (state.redo()) {
                    currentOnValueChange(state.value)
                }
                true
            }

            // Arrow Left
            keyEvent.key == Key.DirectionLeft -> {
                if (isCtrl || isAlt) {
                    state.moveCursorLeftByWord(extendSelection = isShift)
                } else {
                    state.moveCursorLeft(extendSelection = isShift)
                }
                true
            }

            // Arrow Right
            keyEvent.key == Key.DirectionRight -> {
                if (isCtrl || isAlt) {
                    state.moveCursorRightByWord(extendSelection = isShift)
                } else {
                    state.moveCursorRight(extendSelection = isShift)
                }
                true
            }

            // Home
            keyEvent.key == Key.MoveHome -> {
                state.moveCursorToStart(extendSelection = isShift)
                true
            }

            // End
            keyEvent.key == Key.MoveEnd -> {
                state.moveCursorToEnd(extendSelection = isShift)
                true
            }

            // Delete
            keyEvent.key == Key.Delete -> {
                if (!readOnly) {
                    state.deleteForward()
                    currentOnValueChange(state.value)
                }
                true
            }

            // Enter
            keyEvent.key == Key.Enter -> {
                if (!singleLine && !readOnly) {
                    state.insertText("\n")
                    currentOnValueChange(state.value)
                }
                true
            }

            else -> false
        }
    }

    // Calculate size for the Box
    val layoutSize = state.layoutSize
    val minWidth = 100.dp
    val minHeight = 24.dp
    
    // Calculate extra height needed for handles (only if we're handling pointer input ourselves)
    val handleHeight = if (handlePointerInput) {
        CanvasTextDelegate.HANDLE_RADIUS * 2 + CanvasTextDelegate.HANDLE_STEM_HEIGHT
    } else {
        0f
    }

    // Build the pointer input modifier conditionally
    val pointerModifier = if (handlePointerInput) {
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = { offset ->
                    if (!enabled) return@detectTapGestures
                    focusRequester.requestFocus()
                    textLayoutResult?.let { layoutResult ->
                        val textOffset = layoutResult.getOffsetForPosition(offset)
                        state.placeCursor(textOffset)
                        state.handleState = HandleState.Cursor
                    }
                },
                onDoubleTap = { offset ->
                    textLayoutResult?.let { layoutResult ->
                        val textOffset = layoutResult.getOffsetForPosition(offset)
                        val wordBoundary = layoutResult.getWordBoundary(textOffset)
                        state.updateSelection(TextRange(wordBoundary.start, wordBoundary.end))
                        state.handleState = HandleState.Selection
                    }
                },
                onLongPress = { offset ->
                    focusRequester.requestFocus()
                    textLayoutResult?.let { layoutResult ->
                        val textOffset = layoutResult.getOffsetForPosition(offset)
                        val wordBoundary = layoutResult.getWordBoundary(textOffset)
                        state.updateSelection(TextRange(wordBoundary.start, wordBoundary.end))
                        state.handleState = HandleState.Selection
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(
                width = with(density) { layoutSize.width.coerceAtLeast(minWidth.toPx()).toDp() },
                height = with(density) { (layoutSize.height + handleHeight).coerceAtLeast(minHeight.toPx()).toDp() }
            )
            .then(focusModifier)
            .then(keyboardModifier)
            .then(pointerModifier)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            textLayoutResult?.let { layoutResult ->
                CanvasTextDelegate.draw(
                    canvas = drawContext.canvas,
                    state = state,
                    textLayoutResult = layoutResult,
                    position = Offset.Zero,
                    cursorColor = cursorColor,
                    selectionColor = selectionColor,
                    showCursor = state.hasFocus && state.cursorVisible && !state.hasSelection
                )
            }
        }
    }
}

/**
 * Apply an EditCommand to a TextFieldValue.
 */
private fun EditCommand.applyTo(value: TextFieldValue): TextFieldValue {
    return when (this) {
        is androidx.compose.ui.text.input.CommitTextCommand -> {
            val beforeSelection = value.text.substring(0, value.selection.min)
            val afterSelection = value.text.substring(value.selection.max)
            val newText = beforeSelection + text + afterSelection
            val newCursor = beforeSelection.length + text.length
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        }

        is androidx.compose.ui.text.input.SetComposingTextCommand -> {
            val beforeSelection = value.text.substring(0, value.selection.min)
            val afterSelection = value.text.substring(value.selection.max)
            val newText = beforeSelection + text + afterSelection
            val compositionStart = beforeSelection.length
            val compositionEnd = compositionStart + text.length
            TextFieldValue(
                text = newText,
                selection = TextRange(compositionEnd),
                composition = TextRange(compositionStart, compositionEnd)
            )
        }

        is androidx.compose.ui.text.input.DeleteSurroundingTextCommand -> {
            val deleteStart = (value.selection.min - lengthBeforeCursor).coerceAtLeast(0)
            val deleteEnd = (value.selection.max + lengthAfterCursor).coerceAtMost(value.text.length)
            val newText = value.text.removeRange(deleteStart, deleteEnd)
            TextFieldValue(
                text = newText,
                selection = TextRange(deleteStart)
            )
        }

        is androidx.compose.ui.text.input.SetSelectionCommand -> {
            value.copy(selection = TextRange(start, end))
        }

        is androidx.compose.ui.text.input.FinishComposingTextCommand -> {
            value.copy(composition = null)
        }

        is androidx.compose.ui.text.input.BackspaceCommand -> {
            if (value.selection.collapsed && value.selection.start > 0) {
                val newText = value.text.removeRange(value.selection.start - 1, value.selection.start)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(value.selection.start - 1)
                )
            } else if (!value.selection.collapsed) {
                val newText = value.text.removeRange(value.selection.min, value.selection.max)
                TextFieldValue(
                    text = newText,
                    selection = TextRange(value.selection.min)
                )
            } else {
                value
            }
        }

        else -> value // Handle other commands as no-op for now
    }
}

/**
 * Remembers and creates a [CanvasTextFieldState].
 */
@Composable
fun rememberCanvasTextFieldState(
    initialValue: TextFieldValue = TextFieldValue(),
    initialPosition: Offset = Offset.Zero
): CanvasTextFieldState {
    return remember {
        CanvasTextFieldState(
            initialValue = initialValue,
            initialPosition = initialPosition
        )
    }
}

/**
 * Remembers and creates a [CanvasTextFieldState] with initial text.
 */
@Composable
fun rememberCanvasTextFieldState(
    initialText: String,
    initialPosition: Offset = Offset.Zero
): CanvasTextFieldState {
    return remember {
        CanvasTextFieldState.withText(initialText, initialPosition)
    }
}

/**
 * Expands a Rect by the given amount in all directions.
 */
private fun androidx.compose.ui.geometry.Rect.inflate(delta: Float): androidx.compose.ui.geometry.Rect {
    return androidx.compose.ui.geometry.Rect(
        left = left - delta,
        top = top - delta,
        right = right + delta,
        bottom = bottom + delta
    )
}
