package app.drawmark.android.lib.ink

import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.textcanvas.CanvasTextFieldState

/**
 * Represents a drawable element on the canvas with z-ordering support.
 * 
 * This sealed class allows strokes and text fields to be rendered
 * in their creation order, ensuring proper visual layering where
 * elements drawn later appear on top of elements drawn earlier.
 * 
 * Elements are sorted by zIndex first, then by lastModified for elements
 * with the same zIndex. This allows manual z-index assignment while
 * maintaining predictable ordering.
 */
sealed class CanvasElement {
    /**
     * The z-index of this element, used for ordering during rendering.
     * Higher values are drawn on top of lower values.
     */
    abstract val zIndex: Long

    /**
     * Timestamp of when this element was last modified.
     * Used as a secondary sort key when elements have the same zIndex.
     * Higher values (more recent) are drawn on top.
     */
    abstract val lastModified: Long

    /**
     * A stroke element on the canvas.
     */
    data class StrokeElement(
        val stroke: Stroke,
        override val zIndex: Long,
        override val lastModified: Long = System.currentTimeMillis()
    ) : CanvasElement()

    /**
     * A text field element on the canvas.
     */
    data class TextFieldElement(
        val textField: CanvasTextFieldState,
        override val zIndex: Long,
        override val lastModified: Long = System.currentTimeMillis()
    ) : CanvasElement()
}

/**
 * Counter for generating unique z-indices.
 * This should be managed at the editor/manager level.
 */
class ZIndexCounter(initialValue: Long = 0L) {
    private var current: Long = initialValue

    /**
     * Gets the next z-index value and increments the counter.
     */
    fun next(): Long = current++

    /**
     * Gets the current value without incrementing.
     */
    fun current(): Long = current

    /**
     * Resets the counter to a specific value.
     * Useful when loading saved state.
     */
    fun reset(value: Long = 0L) {
        current = value
    }
}
