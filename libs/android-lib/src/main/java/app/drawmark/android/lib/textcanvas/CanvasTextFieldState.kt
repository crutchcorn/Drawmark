package app.drawmark.android.lib.textcanvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * State holder for a Canvas-rendered text field.
 *
 * This class manages all state needed to render and interact with a text field
 * drawn on an Android Canvas, enabling integration with InkCanvas for
 * drag-and-drop text editing within a drawing surface.
 *
 * @param initialValue The initial text field value
 * @param initialPosition The initial position on the canvas (top-left corner)
 */
@Stable
class CanvasTextFieldState(
    initialValue: TextFieldValue = TextFieldValue(),
    initialPosition: Offset = Offset.Zero
) {
    // ============ Text Content State ============

    /**
     * The current text field value including text content, selection, and composition.
     */
    var value by mutableStateOf(initialValue)
        internal set

    /**
     * The text content as a simple string.
     */
    val text: String get() = value.text

    /**
     * The current selection range.
     */
    val selection: TextRange get() = value.selection

    /**
     * Whether there is an active selection (non-collapsed).
     */
    val hasSelection: Boolean get() = !value.selection.collapsed

    /**
     * The currently selected text, or empty string if no selection.
     */
    val selectedText: String
        get() = if (hasSelection) {
            value.text.substring(value.selection.min, value.selection.max)
        } else {
            ""
        }

    // ============ Focus State ============

    /**
     * Whether this text field currently has focus.
     */
    var hasFocus by mutableStateOf(false)
        internal set
    
    /**
     * Whether focus has been requested externally (by the manager).
     * This is separate from hasFocus to avoid race conditions with Compose's focus system.
     */
    var focusRequested by mutableStateOf(false)

    /**
     * Whether the cursor should be visible (for blinking animation).
     */
    var cursorVisible by mutableStateOf(true)
        internal set

    // ============ Layout State ============

    /**
     * The result of text layout computation. Null until layout is performed.
     */
    var textLayoutResult: TextLayoutResult? by mutableStateOf(null)
        internal set

    /**
     * The computed size of the text field after layout.
     */
    var layoutSize: Size by mutableStateOf(Size.Zero)
        internal set

    // ============ Canvas Position State ============

    /**
     * Position of the text field on the canvas (top-left corner).
     * Used for positioning the text field within the InkCanvas.
     */
    var position by mutableStateOf(initialPosition)

    /**
     * The bounding rectangle of this text field on the canvas.
     */
    val bounds: androidx.compose.ui.geometry.Rect
        get() = androidx.compose.ui.geometry.Rect(
            offset = position,
            size = layoutSize
        )

    // ============ Handle State ============

    /**
     * Current handle state for selection/cursor handles.
     */
    var handleState by mutableStateOf(HandleState.None)
        internal set

    /**
     * Whether the start selection handle should be shown.
     */
    var showSelectionHandleStart by mutableStateOf(false)
        internal set

    /**
     * Whether the end selection handle should be shown.
     */
    var showSelectionHandleEnd by mutableStateOf(false)
        internal set

    /**
     * Whether the cursor handle should be shown.
     */
    var showCursorHandle by mutableStateOf(false)
        internal set

    // ============ IME Composition State ============

    /**
     * Whether there is an active IME composition.
     */
    val hasComposition: Boolean get() = value.composition != null

    /**
     * The active composition range, if any.
     */
    val composition: TextRange? get() = value.composition

    // ============ Editing State ============

    /**
     * Whether the text field is enabled for editing.
     */
    var enabled by mutableStateOf(true)

    /**
     * Whether the text field is read-only (focusable but not editable).
     */
    var readOnly by mutableStateOf(false)

    /**
     * Whether the text field is currently editable (enabled and not read-only).
     */
    val isEditable: Boolean get() = enabled && !readOnly

    // ============ State Modification Methods ============

    /**
     * Update the text field value.
     * This is the primary method for modifying text content.
     */
    fun updateValue(newValue: TextFieldValue) {
        value = newValue
    }

    /**
     * Update just the text content, preserving selection if possible.
     */
    fun updateText(newText: String) {
        val currentSelection = value.selection
        val newSelection = if (currentSelection.start <= newText.length &&
            currentSelection.end <= newText.length) {
            currentSelection
        } else {
            TextRange(newText.length)
        }
        value = TextFieldValue(
            text = newText,
            selection = newSelection
        )
    }

    /**
     * Update the selection range.
     */
    fun updateSelection(newSelection: TextRange) {
        value = value.copy(selection = newSelection)
    }

    /**
     * Place the cursor at a specific offset.
     */
    fun placeCursor(offset: Int) {
        val clampedOffset = offset.coerceIn(0, value.text.length)
        value = value.copy(selection = TextRange(clampedOffset))
    }

    /**
     * Select all text.
     */
    fun selectAll() {
        value = value.copy(selection = TextRange(0, value.text.length))
    }

    /**
     * Clear the selection, placing cursor at the end of the previous selection.
     */
    fun clearSelection() {
        if (hasSelection) {
            value = value.copy(selection = TextRange(value.selection.max))
        }
    }

    /**
     * Insert text at the current cursor position, replacing any selection.
     */
    fun insertText(textToInsert: String) {
        val beforeSelection = value.text.substring(0, value.selection.min)
        val afterSelection = value.text.substring(value.selection.max)
        val newText = beforeSelection + textToInsert + afterSelection
        val newCursorPosition = beforeSelection.length + textToInsert.length
        value = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
    }

    /**
     * Delete the selected text or the character before the cursor (backspace).
     * @return true if text was deleted, false if nothing to delete
     */
    fun deleteBackward(): Boolean {
        return if (hasSelection) {
            deleteSelection()
            true
        } else if (value.selection.start > 0) {
            val beforeCursor = value.text.substring(0, value.selection.start - 1)
            val afterCursor = value.text.substring(value.selection.start)
            value = TextFieldValue(
                text = beforeCursor + afterCursor,
                selection = TextRange(beforeCursor.length)
            )
            true
        } else {
            false
        }
    }

    /**
     * Delete the selected text or the character after the cursor (delete key).
     * @return true if text was deleted, false if nothing to delete
     */
    fun deleteForward(): Boolean {
        return if (hasSelection) {
            deleteSelection()
            true
        } else if (value.selection.start < value.text.length) {
            val beforeCursor = value.text.substring(0, value.selection.start)
            val afterCursor = value.text.substring(value.selection.start + 1)
            value = TextFieldValue(
                text = beforeCursor + afterCursor,
                selection = TextRange(beforeCursor.length)
            )
            true
        } else {
            false
        }
    }

    /**
     * Delete the currently selected text.
     */
    fun deleteSelection() {
        if (hasSelection) {
            val beforeSelection = value.text.substring(0, value.selection.min)
            val afterSelection = value.text.substring(value.selection.max)
            value = TextFieldValue(
                text = beforeSelection + afterSelection,
                selection = TextRange(beforeSelection.length)
            )
        }
    }

    // ============ Cursor/Selection Navigation ============

    /**
     * Move cursor left by one character.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorLeft(extendSelection: Boolean = false) {
        val currentPos = if (extendSelection) value.selection.end else value.selection.start
        val newPos = (currentPos - 1).coerceAtLeast(0)
        
        if (extendSelection) {
            val newSelection = TextRange(value.selection.start, newPos)
            value = value.copy(selection = newSelection)
        } else {
            if (hasSelection) {
                // Collapse to start of selection
                value = value.copy(selection = TextRange(value.selection.min))
            } else {
                value = value.copy(selection = TextRange(newPos))
            }
        }
    }

    /**
     * Move cursor right by one character.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorRight(extendSelection: Boolean = false) {
        val currentPos = if (extendSelection) value.selection.end else value.selection.end
        val newPos = (currentPos + 1).coerceAtMost(value.text.length)
        
        if (extendSelection) {
            val newSelection = TextRange(value.selection.start, newPos)
            value = value.copy(selection = newSelection)
        } else {
            if (hasSelection) {
                // Collapse to end of selection
                value = value.copy(selection = TextRange(value.selection.max))
            } else {
                value = value.copy(selection = TextRange(newPos))
            }
        }
    }

    /**
     * Move cursor to the start of the text.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorToStart(extendSelection: Boolean = false) {
        if (extendSelection) {
            value = value.copy(selection = TextRange(value.selection.start, 0))
        } else {
            value = value.copy(selection = TextRange(0))
        }
    }

    /**
     * Move cursor to the end of the text.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorToEnd(extendSelection: Boolean = false) {
        if (extendSelection) {
            value = value.copy(selection = TextRange(value.selection.start, value.text.length))
        } else {
            value = value.copy(selection = TextRange(value.text.length))
        }
    }

    // ============ Hit Testing ============

    /**
     * Check if a point (in canvas coordinates) is within this text field's bounds.
     */
    fun containsPoint(point: Offset): Boolean {
        return bounds.contains(point)
    }

    /**
     * Convert a canvas position to a local position within this text field.
     */
    fun canvasToLocal(canvasPosition: Offset): Offset {
        return canvasPosition - position
    }

    /**
     * Convert a local position to canvas coordinates.
     */
    fun localToCanvas(localPosition: Offset): Offset {
        return localPosition + position
    }

    /**
     * Get the text offset for a position within this text field.
     * Returns null if no layout result is available.
     */
    fun getOffsetForPosition(localPosition: Offset): Int? {
        return textLayoutResult?.getOffsetForPosition(localPosition)
    }

    companion object {
        /**
         * Create a new state with the given initial text.
         */
        fun withText(text: String, position: Offset = Offset.Zero): CanvasTextFieldState {
            return CanvasTextFieldState(
                initialValue = TextFieldValue(text = text, selection = TextRange(text.length)),
                initialPosition = position
            )
        }
    }
}

/**
 * Represents the state of selection/cursor handles.
 */
enum class HandleState {
    /**
     * No handles are shown. Selection can still exist but handles are hidden
     * (e.g., during scrolling or when touch mode is off).
     */
    None,

    /**
     * Selection handles are shown for a text range selection.
     */
    Selection,

    /**
     * A single cursor handle is shown for cursor positioning.
     */
    Cursor
}
