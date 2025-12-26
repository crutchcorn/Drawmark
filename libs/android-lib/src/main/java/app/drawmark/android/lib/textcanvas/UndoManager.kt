package app.drawmark.android.lib.textcanvas

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Manages undo/redo history for text field changes.
 *
 * This class maintains a stack-based history of text field states,
 * allowing users to undo and redo their changes.
 *
 * @param maxHistorySize Maximum number of states to keep in history
 */
@Stable
class UndoManager(
    private val maxHistorySize: Int = 100
) {
    // Undo stack - most recent state is at the end
    private val undoStack = mutableListOf<TextFieldValue>()
    
    // Redo stack - states that were undone
    private val redoStack = mutableListOf<TextFieldValue>()
    
    // The last saved state, used to detect meaningful changes
    private var lastSavedValue: TextFieldValue? = null
    
    // Flag to prevent recording during undo/redo operations
    private var isUndoRedoInProgress = false

    /**
     * Whether undo is available.
     */
    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    /**
     * Whether redo is available.
     */
    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Record a state change for undo history.
     * 
     * This should be called before making a change to save the current state.
     * Only records if the text content has actually changed (ignores selection-only changes).
     *
     * @param value The current value before the change
     */
    fun recordChange(value: TextFieldValue) {
        if (isUndoRedoInProgress) return
        
        // Only record if text content changed (not just selection)
        val lastValue = lastSavedValue
        if (lastValue != null && lastValue.text == value.text) {
            return
        }
        
        // Save the current state
        undoStack.add(value)
        
        // Clear redo stack when new changes are made
        redoStack.clear()
        
        // Trim history if it exceeds max size
        while (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }
        
        lastSavedValue = value
    }

    /**
     * Undo the last change.
     *
     * @param currentValue The current value to save for redo
     * @return The previous value to restore, or null if nothing to undo
     */
    fun undo(currentValue: TextFieldValue): TextFieldValue? {
        if (!canUndo) return null
        
        isUndoRedoInProgress = true
        try {
            // Save current state for redo
            redoStack.add(currentValue)
            
            // Pop and return the previous state
            val previousValue = undoStack.removeAt(undoStack.lastIndex)
            lastSavedValue = previousValue
            return previousValue
        } finally {
            isUndoRedoInProgress = false
        }
    }

    /**
     * Redo the last undone change.
     *
     * @param currentValue The current value to save for undo
     * @return The next value to restore, or null if nothing to redo
     */
    fun redo(currentValue: TextFieldValue): TextFieldValue? {
        if (!canRedo) return null
        
        isUndoRedoInProgress = true
        try {
            // Save current state for undo
            undoStack.add(currentValue)
            
            // Pop and return the redo state
            val redoValue = redoStack.removeAt(redoStack.lastIndex)
            lastSavedValue = redoValue
            return redoValue
        } finally {
            isUndoRedoInProgress = false
        }
    }

    /**
     * Clear all undo/redo history.
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastSavedValue = null
    }

    /**
     * Initialize the undo manager with an initial state.
     * This sets the baseline and clears any existing history.
     */
    fun initialize(value: TextFieldValue) {
        clear()
        lastSavedValue = value
    }
}
