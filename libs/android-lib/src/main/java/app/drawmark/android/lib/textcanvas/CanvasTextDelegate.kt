package app.drawmark.android.lib.textcanvas

import android.graphics.Canvas
import android.graphics.Matrix
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.MultiParagraphIntrinsics
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.resolveDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Handles text layout and rendering for Canvas-based text fields.
 *
 * This class is modeled after AndroidX's TextDelegate but adapted for
 * direct Canvas rendering with support for positioning on an ink canvas.
 *
 * @param text The text to display
 * @param style The text style
 * @param density The density for layout calculations
 * @param fontFamilyResolver The font family resolver
 * @param softWrap Whether text should wrap at soft line breaks
 * @param overflow How to handle text overflow
 * @param maxLines Maximum number of lines
 */
@Stable
class CanvasTextDelegate(
    val text: AnnotatedString,
    val style: TextStyle,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val maxLines: Int = Int.MAX_VALUE
) {
    private var paragraphIntrinsics: MultiParagraphIntrinsics? = null
    private var intrinsicsLayoutDirection: LayoutDirection? = null

    private val nonNullIntrinsics: MultiParagraphIntrinsics
        get() = paragraphIntrinsics
            ?: throw IllegalStateException("layoutIntrinsics must be called first")

    /**
     * The minimum intrinsic width of the text.
     */
    val minIntrinsicWidth: Int
        get() = nonNullIntrinsics.minIntrinsicWidth.ceilToInt()

    /**
     * The maximum intrinsic width of the text.
     */
    val maxIntrinsicWidth: Int
        get() = nonNullIntrinsics.maxIntrinsicWidth.ceilToInt()

    /**
     * Compute intrinsics for the given layout direction.
     */
    fun layoutIntrinsics(layoutDirection: LayoutDirection) {
        val localIntrinsics = paragraphIntrinsics
        val intrinsics = if (
            localIntrinsics == null ||
            layoutDirection != intrinsicsLayoutDirection ||
            localIntrinsics.hasStaleResolvedFonts
        ) {
            intrinsicsLayoutDirection = layoutDirection
            MultiParagraphIntrinsics(
                annotatedString = text,
                style = resolveDefaults(style, layoutDirection),
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                placeholders = emptyList()
            )
        } else {
            localIntrinsics
        }

        paragraphIntrinsics = intrinsics
    }

    /**
     * Perform text layout with the given constraints.
     */
    fun layout(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        prevResult: TextLayoutResult? = null
    ): TextLayoutResult {
        layoutIntrinsics(layoutDirection)

        // Check if we can reuse the previous result
        if (prevResult != null && canReuseLayout(prevResult, constraints, layoutDirection)) {
            return prevResult
        }

        val multiParagraph = layoutText(constraints, layoutDirection)

        val rawSize = IntSize(
            multiParagraph.width.ceilToInt(),
            multiParagraph.height.ceilToInt()
        )
        val size = IntSize(
            rawSize.width.coerceIn(constraints.minWidth, constraints.maxWidth),
            rawSize.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        )

        return TextLayoutResult(
            layoutInput = TextLayoutInput(
                text = text,
                style = style,
                placeholders = emptyList(),
                maxLines = maxLines,
                softWrap = softWrap,
                overflow = overflow,
                density = density,
                layoutDirection = layoutDirection,
                fontFamilyResolver = fontFamilyResolver,
                constraints = constraints
            ),
            multiParagraph = multiParagraph,
            size = size
        )
    }

    private fun canReuseLayout(
        prevResult: TextLayoutResult,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): Boolean {
        val input = prevResult.layoutInput
        return input.text == text &&
                input.style == style &&
                input.maxLines == maxLines &&
                input.softWrap == softWrap &&
                input.overflow == overflow &&
                input.density == density &&
                input.layoutDirection == layoutDirection &&
                input.fontFamilyResolver === fontFamilyResolver &&
                input.constraints == constraints
    }

    private fun layoutText(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MultiParagraph {
        val minWidth = constraints.minWidth
        val widthMatters = softWrap || overflow == TextOverflow.Ellipsis
        val maxWidth = if (widthMatters && constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            Constraints.Infinity
        }

        // Handle ellipsis for non-wrapping text
        val overwriteMaxLines = !softWrap && overflow == TextOverflow.Ellipsis
        val finalMaxLines = if (overwriteMaxLines) 1 else maxLines

        val width = if (minWidth == maxWidth) {
            maxWidth
        } else {
            maxIntrinsicWidth.coerceIn(minWidth, maxWidth)
        }

        return MultiParagraph(
            intrinsics = nonNullIntrinsics,
            constraints = Constraints.fitPrioritizingWidth(
                minWidth = 0,
                maxWidth = width,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            ),
            maxLines = finalMaxLines,
            overflow = overflow
        )
    }

    companion object {
        /**
         * Default cursor width in pixels.
         */
        const val DEFAULT_CURSOR_WIDTH = 2f

        /**
         * Default selection highlight color alpha.
         */
        const val SELECTION_ALPHA = 0.4f

        /**
         * Default handle radius in pixels.
         */
        const val HANDLE_RADIUS = 12f

        /**
         * Default handle stem height in pixels.
         */
        const val HANDLE_STEM_HEIGHT = 8f

        /**
         * Draw a text field onto an Android Canvas.
         *
         * @param canvas The native Android canvas to draw on
         * @param state The text field state containing text and selection info
         * @param textLayoutResult The result of text layout
         * @param position The position to draw at (top-left corner)
         * @param cursorColor The color for the cursor
         * @param selectionColor The color for selection highlight
         * @param handleColor The color for selection/cursor handles
         * @param showCursor Whether to draw the cursor
         * @param showHandles Whether to draw selection/cursor handles
         */
        fun draw(
            canvas: Canvas,
            state: CanvasTextFieldState,
            textLayoutResult: TextLayoutResult,
            position: Offset = Offset.Zero,
            cursorColor: Color = Color.Black,
            selectionColor: Color = Color.Blue.copy(alpha = SELECTION_ALPHA),
            handleColor: Color = Color.Blue,
            showCursor: Boolean = true,
            showHandles: Boolean = true
        ) {
            canvas.save()
            canvas.translate(position.x, position.y)

            val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)

            // Draw selection highlight first (behind text)
            if (state.hasSelection) {
                drawSelection(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    selection = state.selection,
                    selectionColor = selectionColor
                )
            }

            // Draw the text
            TextPainter.paint(composeCanvas, textLayoutResult)

            // Draw cursor on top
            if (showCursor && state.hasFocus && !state.hasSelection && state.cursorVisible) {
                drawCursor(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    cursorOffset = state.value.selection.start,
                    cursorColor = cursorColor
                )
            }

            // Draw composition underline if active
            state.composition?.let { composition ->
                drawCompositionUnderline(
                    canvas = composeCanvas,
                    textLayoutResult = textLayoutResult,
                    composition = composition,
                    color = cursorColor
                )
            }

            // Draw selection handles
            if (showHandles && state.hasFocus && state.handleState != HandleState.None) {
                when (state.handleState) {
                    HandleState.Selection -> {
                        if (state.hasSelection) {
                            // Draw start handle (left side, pointing left)
                            val startRect = textLayoutResult.getCursorRect(state.selection.min)
                            drawSelectionHandle(
                                canvas = composeCanvas,
                                position = Offset(startRect.left, startRect.bottom),
                                color = handleColor,
                                isStart = true
                            )
                            
                            // Draw end handle (right side, pointing right)
                            val endRect = textLayoutResult.getCursorRect(state.selection.max)
                            drawSelectionHandle(
                                canvas = composeCanvas,
                                position = Offset(endRect.left, endRect.bottom),
                                color = handleColor,
                                isStart = false
                            )
                        }
                    }
                    HandleState.Cursor -> {
                        // Draw cursor handle (centered below cursor)
                        val cursorRect = textLayoutResult.getCursorRect(state.selection.start)
                        drawCursorHandle(
                            canvas = composeCanvas,
                            position = Offset(cursorRect.left + DEFAULT_CURSOR_WIDTH / 2, cursorRect.bottom),
                            color = handleColor
                        )
                    }
                    HandleState.None -> { /* No handles */ }
                }
            }

            canvas.restore()
        }

        /**
         * Draw using a Compose Canvas instead of native Android Canvas.
         */
        fun draw(
            canvas: androidx.compose.ui.graphics.Canvas,
            state: CanvasTextFieldState,
            textLayoutResult: TextLayoutResult,
            position: Offset = Offset.Zero,
            cursorColor: Color = Color.Black,
            selectionColor: Color = Color.Blue.copy(alpha = SELECTION_ALPHA),
            handleColor: Color = Color.Blue,
            showCursor: Boolean = true,
            showHandles: Boolean = true
        ) {
            draw(
                canvas = canvas.nativeCanvas,
                state = state,
                textLayoutResult = textLayoutResult,
                position = position,
                cursorColor = cursorColor,
                selectionColor = selectionColor,
                handleColor = handleColor,
                showCursor = showCursor,
                showHandles = showHandles
            )
        }

        /**
         * Draw the selection highlight.
         */
        private fun drawSelection(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            selection: androidx.compose.ui.text.TextRange,
            selectionColor: Color
        ) {
            if (selection.collapsed) return

            val selectionPath = textLayoutResult.getPathForRange(
                selection.min,
                selection.max
            )

            val paint = Paint().apply {
                color = selectionColor
                style = PaintingStyle.Fill
            }

            canvas.drawPath(selectionPath, paint)
        }

        /**
         * Draw the cursor at the specified offset.
         */
        private fun drawCursor(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            cursorOffset: Int,
            cursorColor: Color,
            cursorWidth: Float = DEFAULT_CURSOR_WIDTH
        ) {
            val cursorRect = textLayoutResult.getCursorRect(cursorOffset)

            val paint = Paint().apply {
                color = cursorColor
                style = PaintingStyle.Fill
            }

            // Draw cursor as a thin rectangle
            canvas.drawRect(
                Rect(
                    left = cursorRect.left,
                    top = cursorRect.top,
                    right = cursorRect.left + cursorWidth,
                    bottom = cursorRect.bottom
                ),
                paint
            )
        }

        /**
         * Draw the composition underline for IME input.
         */
        private fun drawCompositionUnderline(
            canvas: androidx.compose.ui.graphics.Canvas,
            textLayoutResult: TextLayoutResult,
            composition: androidx.compose.ui.text.TextRange,
            color: Color
        ) {
            if (composition.collapsed) return

            // Get the bounding boxes for the composition range
            val startRect = textLayoutResult.getCursorRect(composition.start)
            val endRect = textLayoutResult.getCursorRect(composition.end)

            val paint = Paint().apply {
                this.color = color
                style = PaintingStyle.Stroke
                strokeWidth = 2f
            }

            // Draw underline
            // For single-line text, draw a simple line
            // For multi-line, we'd need to draw multiple segments
            val y = maxOf(startRect.bottom, endRect.bottom) - 1f
            canvas.drawLine(
                p1 = Offset(startRect.left, y),
                p2 = Offset(endRect.right, y),
                paint = paint
            )
        }

        /**
         * Draw a selection handle (teardrop shape pointing to the text).
         *
         * @param canvas The canvas to draw on
         * @param position The position where the handle connects to text (top of stem)
         * @param color The handle color
         * @param isStart True for start handle (points left), false for end handle (points right)
         * @param radius The radius of the circular part
         * @param stemHeight The height of the stem connecting to text
         */
        private fun drawSelectionHandle(
            canvas: androidx.compose.ui.graphics.Canvas,
            position: Offset,
            color: Color,
            isStart: Boolean,
            radius: Float = HANDLE_RADIUS,
            stemHeight: Float = HANDLE_STEM_HEIGHT
        ) {
            val paint = Paint().apply {
                this.color = color
                style = PaintingStyle.Fill
            }

            // Draw stem (thin rectangle from text to circle)
            val stemWidth = 3f
            canvas.drawRect(
                Rect(
                    left = position.x - stemWidth / 2,
                    top = position.y,
                    right = position.x + stemWidth / 2,
                    bottom = position.y + stemHeight
                ),
                paint
            )

            // Draw circle at bottom of stem
            // For start handle, circle is offset to the left
            // For end handle, circle is offset to the right
            val circleOffset = if (isStart) -radius / 2 else radius / 2
            val circleCenter = Offset(
                x = position.x + circleOffset,
                y = position.y + stemHeight + radius
            )
            canvas.drawCircle(circleCenter, radius, paint)
        }

        /**
         * Draw a cursor handle (teardrop shape centered below cursor).
         *
         * @param canvas The canvas to draw on
         * @param position The position where the handle connects to cursor (top of stem)
         * @param color The handle color
         * @param radius The radius of the circular part
         * @param stemHeight The height of the stem connecting to cursor
         */
        private fun drawCursorHandle(
            canvas: androidx.compose.ui.graphics.Canvas,
            position: Offset,
            color: Color,
            radius: Float = HANDLE_RADIUS,
            stemHeight: Float = HANDLE_STEM_HEIGHT
        ) {
            val paint = Paint().apply {
                this.color = color
                style = PaintingStyle.Fill
            }

            // Draw stem
            val stemWidth = 3f
            canvas.drawRect(
                Rect(
                    left = position.x - stemWidth / 2,
                    top = position.y,
                    right = position.x + stemWidth / 2,
                    bottom = position.y + stemHeight
                ),
                paint
            )

            // Draw circle centered below stem
            val circleCenter = Offset(
                x = position.x,
                y = position.y + stemHeight + radius
            )
            canvas.drawCircle(circleCenter, radius, paint)
        }

        /**
         * Get the bounding rect for a selection start handle.
         * Used for hit testing.
         */
        fun getStartHandleRect(
            textLayoutResult: TextLayoutResult,
            selectionStart: Int,
            radius: Float = HANDLE_RADIUS,
            stemHeight: Float = HANDLE_STEM_HEIGHT
        ): Rect {
            val cursorRect = textLayoutResult.getCursorRect(selectionStart)
            val handleTop = cursorRect.bottom
            val circleCenter = Offset(
                x = cursorRect.left - radius / 2,
                y = handleTop + stemHeight + radius
            )
            return Rect(
                left = circleCenter.x - radius,
                top = handleTop,
                right = circleCenter.x + radius,
                bottom = circleCenter.y + radius
            )
        }

        /**
         * Get the bounding rect for a selection end handle.
         * Used for hit testing.
         */
        fun getEndHandleRect(
            textLayoutResult: TextLayoutResult,
            selectionEnd: Int,
            radius: Float = HANDLE_RADIUS,
            stemHeight: Float = HANDLE_STEM_HEIGHT
        ): Rect {
            val cursorRect = textLayoutResult.getCursorRect(selectionEnd)
            val handleTop = cursorRect.bottom
            val circleCenter = Offset(
                x = cursorRect.left + radius / 2,
                y = handleTop + stemHeight + radius
            )
            return Rect(
                left = circleCenter.x - radius,
                top = handleTop,
                right = circleCenter.x + radius,
                bottom = circleCenter.y + radius
            )
        }

        /**
         * Get the bounding rect for a cursor handle.
         * Used for hit testing.
         */
        fun getCursorHandleRect(
            textLayoutResult: TextLayoutResult,
            cursorOffset: Int,
            radius: Float = HANDLE_RADIUS,
            stemHeight: Float = HANDLE_STEM_HEIGHT
        ): Rect {
            val cursorRect = textLayoutResult.getCursorRect(cursorOffset)
            val handleTop = cursorRect.bottom
            val circleCenter = Offset(
                x = cursorRect.left + DEFAULT_CURSOR_WIDTH / 2,
                y = handleTop + stemHeight + radius
            )
            return Rect(
                left = circleCenter.x - radius,
                top = handleTop,
                right = circleCenter.x + radius,
                bottom = circleCenter.y + radius
            )
        }

        /**
         * Create or update a CanvasTextDelegate if parameters changed.
         */
        fun updateDelegate(
            current: CanvasTextDelegate?,
            text: AnnotatedString,
            style: TextStyle,
            density: Density,
            fontFamilyResolver: FontFamily.Resolver,
            softWrap: Boolean = true,
            overflow: TextOverflow = TextOverflow.Clip,
            maxLines: Int = Int.MAX_VALUE
        ): CanvasTextDelegate {
            if (current != null &&
                current.text == text &&
                current.style == style &&
                current.density == density &&
                current.fontFamilyResolver === fontFamilyResolver &&
                current.softWrap == softWrap &&
                current.overflow == overflow &&
                current.maxLines == maxLines
            ) {
                return current
            }

            return CanvasTextDelegate(
                text = text,
                style = style,
                density = density,
                fontFamilyResolver = fontFamilyResolver,
                softWrap = softWrap,
                overflow = overflow,
                maxLines = maxLines
            )
        }
    }
}

/**
 * Extension to ceil a float to int.
 */
private fun Float.ceilToInt(): Int = ceil(this).roundToInt()
