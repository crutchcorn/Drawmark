package app.drawmark.android.lib.ink

import android.graphics.Matrix
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.textcanvas.CanvasTextField
import app.drawmark.android.lib.textcanvas.InkCanvasTextFieldManager
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope

// TODO: This composable was originally meant to handle displaying our contents for us, but now I'm
//   wondering if we should use this instead:
//   @see https://developer.android.com/reference/kotlin/androidx/ink/rendering/android/view/ViewStrokeRenderer
@Composable
fun InkDisplaySurface(
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer
) {
    // Canvas for rendering finished strokes (read-only display)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasTransform = Matrix()
        drawContext.canvas.nativeCanvas.concat(canvasTransform)
        val canvas = drawContext.canvas.nativeCanvas

        finishedStrokesState.forEach { stroke ->
            canvasStrokeRenderer.draw(
                stroke = stroke,
                canvas = canvas,
                strokeToScreenTransform = canvasTransform
            )
        }
    }
}

/**
 * Enhanced display surface that renders both strokes and text fields.
 */
@Composable
fun InkDisplaySurfaceWithText(
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    textFieldManager: InkCanvasTextFieldManager?,
    isTextMode: Boolean = false,
    cursorColor: Color = Color.Black,
    selectionColor: Color = Color.Blue.copy(alpha = 0.4f)
) {
    // Constants for gesture detection
    val longPressTimeoutMillis = 500L
    val doubleTapTimeoutMillis = 300L

    Box(modifier = Modifier.fillMaxSize()) {
        val canvasModifier = if (isTextMode && textFieldManager != null) {
            Modifier
                .fillMaxSize()
                .pointerInput(textFieldManager) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downPosition = down.position
                        Log.d("InkCanvasGesture", "Down at $downPosition")

                        // First, check if this is a handle hit
                        val handleHit = textFieldManager.hitTestHandle(downPosition)
                        if (handleHit != null) {
                            Log.d("InkCanvasGesture", "Handle hit: ${handleHit.handleType}")
                            down.consume()

                            // Start dragging the handle
                            textFieldManager.startDraggingHandle(handleHit.textField, handleHit.handleType)

                            // Follow the drag
                            drag(down.id) { change ->
                                change.consume()
                                textFieldManager.updateHandleDrag(
                                    handleHit.textField,
                                    handleHit.handleType,
                                    change.position
                                )
                            }

                            // Drag ended
                            textFieldManager.stopDraggingHandle(handleHit.textField)
                            return@awaitEachGesture
                        }

                        // Not a handle hit - detect tap, double-tap, or long-press
                        var upOrCancel: Offset? = null
                        var isLongPress = false

                        // Try to detect long press
                        try {
                            withTimeout(longPressTimeoutMillis) {
                                val up = waitForUpOrCancellation()
                                upOrCancel = up?.position
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            // Long press detected
                            isLongPress = true
                            Log.d("InkCanvasGesture", "Long press at $downPosition")
                            textFieldManager.handleLongPress(downPosition)
                            // Wait for up to complete the gesture
                            waitForUpOrCancellation()
                            return@awaitEachGesture
                        }

                        if (upOrCancel == null) {
                            // Cancelled
                            return@awaitEachGesture
                        }

                        // It was a tap - now check for double tap
                        try {
                            withTimeout(doubleTapTimeoutMillis) {
                                val secondDown = awaitFirstDown()
                                // Got a second tap - wait for up
                                val secondUp = waitForUpOrCancellation()
                                if (secondUp != null) {
                                    Log.d("InkCanvasGesture", "Double tap at ${secondDown.position}")
                                    textFieldManager.handleDoubleTap(secondDown.position)
                                }
                            }
                        } catch (e: PointerEventTimeoutCancellationException) {
                            // Single tap
                            Log.d("InkCanvasGesture", "Single tap at $downPosition")
                            if (!textFieldManager.handleTap(downPosition)) {
                                // No text field was tapped - create a new one
                                val newTextField = textFieldManager.addTextField(downPosition, "")
                                textFieldManager.requestFocus(newTextField)
                            }
                        }
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }

        // Canvas for strokes and text field visuals
        Canvas(modifier = canvasModifier) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            // Draw strokes first
            finishedStrokesState.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = canvasTransform
                )
            }

            // Draw text fields on top (visual rendering only)
            textFieldManager?.draw(
                canvas = drawContext.canvas,
                cursorColor = cursorColor,
                selectionColor = selectionColor
            )
        }

        // Overlay actual CanvasTextField composables for focus/keyboard handling
        if (isTextMode && textFieldManager != null) {
            textFieldManager.textFields.forEach { textFieldState ->
                CanvasTextField(
                    state = textFieldState,
                    onValueChange = { newValue ->
                        textFieldState.updateValue(newValue)
                    },
                    modifier = Modifier.offset {
                        IntOffset(
                            textFieldState.position.x.roundToInt(),
                            textFieldState.position.y.roundToInt()
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    ),
                    cursorColor = cursorColor,
                    selectionColor = selectionColor,
                    // Disable pointer handling - InkCanvas handles all gestures including handle drags
                    handlePointerInput = false
                )
            }
        }
    }
}
