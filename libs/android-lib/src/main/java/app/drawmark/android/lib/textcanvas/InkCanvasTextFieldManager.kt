package app.drawmark.android.lib.textcanvas

import android.graphics.Matrix
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Manager for multiple text fields on an ink canvas.
 *
 * This class manages a collection of [CanvasTextFieldState] instances,
 * handling hit testing, focus management, and rendering coordination
 * with an ink canvas.
 */
@Stable
class InkCanvasTextFieldManager {
    /**
     * List of text field states managed by this manager.
     */
    val textFields: SnapshotStateList<CanvasTextFieldState> = mutableStateListOf()

    /**
     * The currently focused text field, if any.
     */
    var focusedTextField: CanvasTextFieldState? by mutableStateOf(null)
        private set

    /**
     * Whether any text field currently has focus.
     */
    val hasActiveFocus: Boolean
        get() = focusedTextField != null

    /**
     * Add a new text field at the specified position.
     *
     * @param position The canvas position for the text field
     * @param initialText Initial text content
     * @return The created text field state
     */
    fun addTextField(
        position: Offset,
        initialText: String = ""
    ): CanvasTextFieldState {
        val state = CanvasTextFieldState.withText(initialText, position)
        textFields.add(state)
        return state
    }

    /**
     * Remove a text field.
     *
     * @param state The text field state to remove
     * @return true if the text field was removed
     */
    fun removeTextField(state: CanvasTextFieldState): Boolean {
        if (focusedTextField == state) {
            focusedTextField = null
        }
        return textFields.remove(state)
    }

    /**
     * Remove all text fields.
     */
    fun clearTextFields() {
        focusedTextField = null
        textFields.clear()
    }

    /**
     * Find a text field at the given canvas position.
     *
     * @param position The position to test
     * @return The text field at the position, or null if none
     */
    fun hitTest(position: Offset): CanvasTextFieldState? {
        // Iterate in reverse order so topmost text fields are checked first
        return textFields.asReversed().firstOrNull { state ->
            state.containsPoint(position)
        }
    }

    /**
     * Request focus for a specific text field.
     *
     * @param state The text field to focus, or null to clear focus
     */
    fun requestFocus(state: CanvasTextFieldState?) {
        // Clear previous focus request
        focusedTextField?.focusRequested = false

        // Set new focus
        focusedTextField = state
        state?.focusRequested = true
    }

    /**
     * Clear focus from all text fields.
     */
    fun clearFocus() {
        requestFocus(null)
    }

    /**
     * Handle a tap on the canvas.
     *
     * This will check if the tap hits any text field and focus it,
     * or clear focus if tapping on empty space.
     *
     * @param position The tap position in canvas coordinates
     * @return true if a text field was tapped
     */
    fun handleTap(position: Offset): Boolean {
        val tappedTextField = hitTest(position)
        if (tappedTextField != null) {
            requestFocus(tappedTextField)

            // Position cursor within the text field
            val localPosition = tappedTextField.canvasToLocal(position)
            tappedTextField.getOffsetForPosition(localPosition)?.let { offset ->
                tappedTextField.placeCursor(offset)
            }
            return true
        } else {
            clearFocus()
            return false
        }
    }

    /**
     * Handle a double-tap on the canvas.
     *
     * This will select a word in the tapped text field.
     *
     * @param position The tap position in canvas coordinates
     * @return true if a text field was double-tapped
     */
    fun handleDoubleTap(position: Offset): Boolean {
        val tappedTextField = hitTest(position)
        if (tappedTextField != null) {
            requestFocus(tappedTextField)

            val localPosition = tappedTextField.canvasToLocal(position)
            tappedTextField.textLayoutResult?.let { layoutResult ->
                val offset = layoutResult.getOffsetForPosition(localPosition)
                val wordBoundary = layoutResult.getWordBoundary(offset)
                tappedTextField.updateSelection(
                    androidx.compose.ui.text.TextRange(wordBoundary.start, wordBoundary.end)
                )
            }
            return true
        }
        return false
    }

    /**
     * Handle a long press on the canvas.
     *
     * This will start selection mode in the pressed text field and show the context menu.
     *
     * @param position The press position in canvas coordinates
     * @return true if a text field was long-pressed
     */
    fun handleLongPress(position: Offset): Boolean {
        val pressedTextField = hitTest(position)
        if (pressedTextField != null) {
            requestFocus(pressedTextField)

            val localPosition = pressedTextField.canvasToLocal(position)
            pressedTextField.textLayoutResult?.let { layoutResult ->
                val offset = layoutResult.getOffsetForPosition(localPosition)
                val wordBoundary = layoutResult.getWordBoundary(offset)
                pressedTextField.updateSelection(
                    androidx.compose.ui.text.TextRange(wordBoundary.start, wordBoundary.end)
                )
                pressedTextField.handleState = HandleState.Selection
                
                // Show context menu above the selection
                showContextMenuForTextField(pressedTextField)
            }
            return true
        }
        return false
    }

    /**
     * Show the context menu for a text field.
     * Positions the menu above the selection or cursor.
     */
    fun showContextMenuForTextField(textField: CanvasTextFieldState) {
        val layoutResult = textField.textLayoutResult ?: return
        
        // Get the position for the context menu (above the selection/cursor)
        // Use a larger offset to give breathing room above handles
        val menuVerticalOffset = 98f
        val menuPosition = if (textField.hasSelection) {
            // Position above the start of the selection
            val startRect = layoutResult.getCursorRect(textField.selection.min)
            Offset(
                x = (startRect.left + layoutResult.getCursorRect(textField.selection.max).left) / 2,
                y = startRect.top - menuVerticalOffset
            )
        } else {
            // Position above the cursor
            val cursorRect = layoutResult.getCursorRect(textField.selection.start)
            Offset(
                x = cursorRect.left,
                y = cursorRect.top - menuVerticalOffset
            )
        }
        
        textField.contextMenuPosition = menuPosition
        textField.showContextMenu = true
    }

    /**
     * Hide the context menu for a text field.
     */
    fun hideContextMenu(textField: CanvasTextFieldState) {
        textField.showContextMenu = false
    }

    /**
     * Data class representing a handle hit test result.
     */
    data class HandleHitResult(
        val textField: CanvasTextFieldState,
        val handleType: DraggingHandle
    )

    /**
     * Helper function to inflate a Rect for touch tolerance.
     */
    private fun Rect.inflate(delta: Float): Rect {
        return Rect(
            left = left - delta,
            top = top - delta,
            right = right + delta,
            bottom = bottom + delta
        )
    }

    /**
     * Check if a position hits any selection handle.
     *
     * @param position The position in canvas coordinates
     * @return HandleHitResult if a handle was hit, null otherwise
     */
    fun hitTestHandle(position: Offset): HandleHitResult? {
        val touchTolerance = 24f // Extra touch area around handles

        for (textField in textFields.asReversed()) {
            val layoutResult = textField.textLayoutResult ?: continue
            if (!textField.hasFocus) continue

            // Convert position to text field local coordinates
            val localPosition = textField.canvasToLocal(position)

            when (textField.handleState) {
                HandleState.Selection -> {
                    // Check start handle
                    val startRect = CanvasTextDelegate.getStartHandleRect(
                        layoutResult,
                        textField.value.selection.start
                    ).inflate(touchTolerance)
                    if (startRect.contains(localPosition)) {
                        return HandleHitResult(textField, DraggingHandle.Start)
                    }

                    // Check end handle
                    val endRect = CanvasTextDelegate.getEndHandleRect(
                        layoutResult,
                        textField.value.selection.end
                    ).inflate(touchTolerance)
                    if (endRect.contains(localPosition)) {
                        return HandleHitResult(textField, DraggingHandle.End)
                    }
                }
                HandleState.Cursor -> {
                    // Check cursor handle
                    val cursorRect = CanvasTextDelegate.getCursorHandleRect(
                        layoutResult,
                        textField.value.selection.start
                    ).inflate(touchTolerance)
                    if (cursorRect.contains(localPosition)) {
                        return HandleHitResult(textField, DraggingHandle.Cursor)
                    }
                }
                HandleState.None -> {
                    // No handles to hit
                }
            }
        }
        return null
    }

    /**
     * Start dragging a handle.
     */
    fun startDraggingHandle(textField: CanvasTextFieldState, handleType: DraggingHandle) {
        textField.draggingHandle = handleType
    }

    /**
     * Update handle position during drag.
     *
     * @param textField The text field being edited
     * @param handleType Which handle is being dragged
     * @param position The new position in canvas coordinates
     */
    fun updateHandleDrag(
        textField: CanvasTextFieldState,
        handleType: DraggingHandle,
        position: Offset
    ) {
        val layoutResult = textField.textLayoutResult ?: return
        val localPosition = textField.canvasToLocal(position)
        val newOffset = layoutResult.getOffsetForPosition(localPosition)

        when (handleType) {
            DraggingHandle.Start -> {
                textField.updateSelectionStart(newOffset)
            }
            DraggingHandle.End -> {
                textField.updateSelectionEnd(newOffset)
            }
            DraggingHandle.Cursor -> {
                textField.placeCursor(newOffset)
            }
        }
    }

    /**
     * Stop dragging a handle.
     */
    fun stopDraggingHandle(textField: CanvasTextFieldState) {
        textField.draggingHandle = null
    }

    /**
     * Draw all text fields on the canvas.
     *
     * @param canvas The Compose canvas to draw on
     * @param cursorColor The cursor color
     * @param selectionColor The selection highlight color
     */
    fun draw(
        canvas: androidx.compose.ui.graphics.Canvas,
        cursorColor: Color = Color.Black,
        selectionColor: Color = Color.Blue.copy(alpha = 0.4f)
    ) {
        textFields.forEach { state ->
            state.textLayoutResult?.let { layoutResult ->
                CanvasTextDelegate.draw(
                    canvas = canvas.nativeCanvas,
                    state = state,
                    textLayoutResult = layoutResult,
                    position = state.position,
                    cursorColor = cursorColor,
                    selectionColor = selectionColor,
                    showCursor = state.hasFocus && state.cursorVisible && !state.hasSelection
                )
            }
        }
    }

    /**
     * Draw all text fields on an Android native canvas.
     *
     * @param canvas The native Android canvas to draw on
     * @param cursorColor The cursor color
     * @param selectionColor The selection highlight color
     */
    fun draw(
        canvas: android.graphics.Canvas,
        cursorColor: Color = Color.Black,
        selectionColor: Color = Color.Blue.copy(alpha = 0.4f)
    ) {
        textFields.forEach { state ->
            state.textLayoutResult?.let { layoutResult ->
                CanvasTextDelegate.draw(
                    canvas = canvas,
                    state = state,
                    textLayoutResult = layoutResult,
                    position = state.position,
                    cursorColor = cursorColor,
                    selectionColor = selectionColor,
                    showCursor = state.hasFocus && state.cursorVisible && !state.hasSelection
                )
            }
        }
    }
}

/**
 * Remember and create an [InkCanvasTextFieldManager].
 */
@Composable
fun rememberInkCanvasTextFieldManager(): InkCanvasTextFieldManager {
    return remember { InkCanvasTextFieldManager() }
}

/**
 * Modifier that handles text field interactions on an ink canvas.
 *
 * This modifier should be applied to the canvas container to enable
 * tap-to-create, focus management, and interaction with text fields.
 *
 * @param manager The text field manager
 * @param onCreateTextField Callback to create a new text field at a position.
 *                          Return true to create a text field at that position.
 * @param enabled Whether text field interactions are enabled
 */
fun Modifier.inkCanvasTextFields(
    manager: InkCanvasTextFieldManager,
    onCreateTextField: ((Offset) -> Boolean)? = null,
    enabled: Boolean = true
): Modifier = this.then(
    if (enabled) {
        Modifier.pointerInput(manager) {
            detectTapGestures(
                onTap = { position ->
                    // First, try to tap an existing text field
                    if (!manager.handleTap(position)) {
                        // No text field was tapped - optionally create one
                        if (onCreateTextField?.invoke(position) == true) {
                            val newTextField = manager.addTextField(position)
                            manager.requestFocus(newTextField)
                        }
                    }
                },
                onDoubleTap = { position ->
                    manager.handleDoubleTap(position)
                },
                onLongPress = { position ->
                    manager.handleLongPress(position)
                }
            )
        }
    } else {
        Modifier
    }
)
