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
     * Undo manager for tracking text changes.
     */
    val undoManager = UndoManager()

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
     * Which handle is currently being dragged, if any.
     */
    var draggingHandle by mutableStateOf<DraggingHandle?>(null)
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
     * Update the selection start position during handle dragging.
     * The start can move past the end, which will swap the selection direction.
     */
    fun updateSelectionStart(offset: Int) {
        val clampedOffset = offset.coerceIn(0, value.text.length)
        val currentEnd = value.selection.max
        
        // If dragging start past end, swap roles
        if (clampedOffset > currentEnd) {
            value = value.copy(selection = TextRange(currentEnd, clampedOffset))
            // Swap to dragging end handle
            draggingHandle = DraggingHandle.End
        } else if (clampedOffset == currentEnd) {
            // Collapsed to cursor
            value = value.copy(selection = TextRange(clampedOffset))
            handleState = HandleState.Cursor
            draggingHandle = DraggingHandle.Cursor
        } else {
            value = value.copy(selection = TextRange(clampedOffset, currentEnd))
        }
    }

    /**
     * Update the selection end position during handle dragging.
     * The end can move past the start, which will swap the selection direction.
     */
    fun updateSelectionEnd(offset: Int) {
        val clampedOffset = offset.coerceIn(0, value.text.length)
        val currentStart = value.selection.min
        
        // If dragging end past start, swap roles
        if (clampedOffset < currentStart) {
            value = value.copy(selection = TextRange(clampedOffset, currentStart))
            // Swap to dragging start handle
            draggingHandle = DraggingHandle.Start
        } else if (clampedOffset == currentStart) {
            // Collapsed to cursor
            value = value.copy(selection = TextRange(clampedOffset))
            handleState = HandleState.Cursor
            draggingHandle = DraggingHandle.Cursor
        } else {
            value = value.copy(selection = TextRange(currentStart, clampedOffset))
        }
    }

    /**
     * Start dragging a handle.
     */
    fun startDraggingHandle(handle: DraggingHandle) {
        draggingHandle = handle
    }

    /**
     * Stop dragging a handle.
     */
    fun stopDraggingHandle() {
        draggingHandle = null
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
     * @param textToInsert The text to insert
     * @param allowMerge Whether to allow merging with previous undo operations (false for paste)
     */
    fun insertText(textToInsert: String, allowMerge: Boolean = true) {
        val preValue = value
        val beforeSelection = value.text.substring(0, value.selection.min)
        val afterSelection = value.text.substring(value.selection.max)
        val newText = beforeSelection + textToInsert + afterSelection
        val newCursorPosition = beforeSelection.length + textToInsert.length
        val postValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorPosition)
        )
        undoManager.recordChange(preValue, postValue, allowMerge)
        value = postValue
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
            val preValue = value
            val beforeCursor = value.text.substring(0, value.selection.start - 1)
            val afterCursor = value.text.substring(value.selection.start)
            val postValue = TextFieldValue(
                text = beforeCursor + afterCursor,
                selection = TextRange(beforeCursor.length)
            )
            undoManager.recordChange(preValue, postValue)
            value = postValue
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
            val preValue = value
            val beforeCursor = value.text.substring(0, value.selection.start)
            val afterCursor = value.text.substring(value.selection.start + 1)
            val postValue = TextFieldValue(
                text = beforeCursor + afterCursor,
                selection = TextRange(beforeCursor.length)
            )
            undoManager.recordChange(preValue, postValue)
            value = postValue
            true
        } else {
            false
        }
    }

    /**
     * Delete the word before the cursor (Alt+Backspace).
     * @return true if text was deleted, false if nothing to delete
     */
    fun deleteWordBackward(): Boolean {
        return if (hasSelection) {
            deleteSelection()
            true
        } else if (value.selection.start > 0) {
            val preValue = value
            val wordStart = findPreviousWordBoundary(value.text, value.selection.start)
            val beforeWord = value.text.substring(0, wordStart)
            val afterCursor = value.text.substring(value.selection.start)
            val postValue = TextFieldValue(
                text = beforeWord + afterCursor,
                selection = TextRange(wordStart)
            )
            undoManager.recordChange(preValue, postValue, allowMerge = false)
            value = postValue
            true
        } else {
            false
        }
    }

    /**
     * Delete from cursor to the beginning of the line (Meta/Cmd+Backspace).
     * @return true if text was deleted, false if nothing to delete
     */
    fun deleteToLineStart(): Boolean {
        return if (hasSelection) {
            deleteSelection()
            true
        } else if (value.selection.start > 0) {
            val preValue = value
            val lineStart = findLineStart(value.text, value.selection.start)
            val beforeLine = value.text.substring(0, lineStart)
            val afterCursor = value.text.substring(value.selection.start)
            val postValue = TextFieldValue(
                text = beforeLine + afterCursor,
                selection = TextRange(lineStart)
            )
            undoManager.recordChange(preValue, postValue, allowMerge = false)
            value = postValue
            true
        } else {
            false
        }
    }

    /**
     * Find the start of the current line from the given position.
     */
    private fun findLineStart(text: String, position: Int): Int {
        if (position <= 0) return 0
        
        var pos = position - 1
        while (pos > 0 && text[pos] != '\n') {
            pos--
        }
        
        // If we found a newline, the line starts after it
        return if (pos > 0 || text[pos] == '\n') pos + 1 else 0
    }

    /**
     * Delete the currently selected text.
     * @param allowMerge Whether to allow merging with previous undo operations (false for cut)
     */
    fun deleteSelection(allowMerge: Boolean = true) {
        if (hasSelection) {
            val preValue = value
            val beforeSelection = value.text.substring(0, value.selection.min)
            val afterSelection = value.text.substring(value.selection.max)
            val postValue = TextFieldValue(
                text = beforeSelection + afterSelection,
                selection = TextRange(beforeSelection.length)
            )
            undoManager.recordChange(preValue, postValue, allowMerge)
            value = postValue
        }
    }

    // ============ Undo/Redo ============

    /**
     * Undo the last text change.
     * @return true if undo was performed, false if nothing to undo
     */
    fun undo(): Boolean {
        val previousValue = undoManager.undo(value)
        return if (previousValue != null) {
            value = previousValue
            true
        } else {
            false
        }
    }

    /**
     * Redo the last undone change.
     * @return true if redo was performed, false if nothing to redo
     */
    fun redo(): Boolean {
        val nextValue = undoManager.redo(value)
        return if (nextValue != null) {
            value = nextValue
            true
        } else {
            false
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

    /**
     * Move cursor left by one word.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorLeftByWord(extendSelection: Boolean = false) {
        val text = value.text
        val currentPos = if (extendSelection) value.selection.end else value.selection.start
        val newPos = findPreviousWordBoundary(text, currentPos)
        
        if (extendSelection) {
            value = value.copy(selection = TextRange(value.selection.start, newPos))
        } else {
            if (hasSelection) {
                // Collapse to start of selection, then move by word
                val collapsedPos = value.selection.min
                val wordPos = findPreviousWordBoundary(text, collapsedPos)
                value = value.copy(selection = TextRange(wordPos))
            } else {
                value = value.copy(selection = TextRange(newPos))
            }
        }
    }

    /**
     * Move cursor right by one word.
     * @param extendSelection If true, extend selection instead of moving cursor
     */
    fun moveCursorRightByWord(extendSelection: Boolean = false) {
        val text = value.text
        val currentPos = if (extendSelection) value.selection.end else value.selection.end
        val newPos = findNextWordBoundary(text, currentPos)
        
        if (extendSelection) {
            value = value.copy(selection = TextRange(value.selection.start, newPos))
        } else {
            if (hasSelection) {
                // Collapse to end of selection, then move by word
                val collapsedPos = value.selection.max
                val wordPos = findNextWordBoundary(text, collapsedPos)
                value = value.copy(selection = TextRange(wordPos))
            } else {
                value = value.copy(selection = TextRange(newPos))
            }
        }
    }

    /**
     * Find the previous word boundary from the given position.
     * A word boundary is defined as a transition between word and non-word characters.
     */
    private fun findPreviousWordBoundary(text: String, position: Int): Int {
        if (position <= 0) return 0
        
        var pos = position - 1
        
        // Skip any whitespace/punctuation before the current position
        while (pos > 0 && !text[pos].isLetterOrDigit()) {
            pos--
        }
        
        // Now skip the word characters to find the start of the word
        while (pos > 0 && text[pos - 1].isLetterOrDigit()) {
            pos--
        }
        
        return pos
    }

    /**
     * Find the next word boundary from the given position.
     * A word boundary is defined as a transition between word and non-word characters.
     */
    private fun findNextWordBoundary(text: String, position: Int): Int {
        if (position >= text.length) return text.length
        
        var pos = position
        
        // If we're in a word, skip to the end of it
        while (pos < text.length && text[pos].isLetterOrDigit()) {
            pos++
        }
        
        // Skip any whitespace/punctuation after the word
        while (pos < text.length && !text[pos].isLetterOrDigit()) {
            pos++
        }
        
        return pos
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

/**
 * Represents which handle is currently being dragged.
 */
enum class DraggingHandle {
    /**
     * The start (left) selection handle is being dragged.
     */
    Start,

    /**
     * The end (right) selection handle is being dragged.
     */
    End,

    /**
     * The cursor handle is being dragged.
     */
    Cursor
}
