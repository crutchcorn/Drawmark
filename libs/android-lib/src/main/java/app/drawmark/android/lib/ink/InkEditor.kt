package app.drawmark.android.lib.ink

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import app.drawmark.android.lib.textcanvas.InkCanvasTextFieldManager
import app.drawmark.android.lib.textcanvas.TextFieldContextMenu
import kotlin.math.roundToInt

/**
 * Editing mode for the InkEditor.
 */
enum class InkEditorMode {
    /** Drawing mode - touch creates ink strokes */
    Draw,
    /** Text mode - tap creates/edits text fields */
    Text
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun InkEditorSurface(
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    getBrush: () -> Brush,
    mode: InkEditorMode = InkEditorMode.Draw,
    textFieldManager: InkCanvasTextFieldManager? = null
) {
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }
    // Use rememberUpdatedState to always have the latest getBrush lambda
    // This ensures the touch listener always uses the current brush settings
    val currentGetBrush = rememberUpdatedState(getBrush)
    
    // Store predictor reference
    val predictorRef = remember { mutableStateOf<MotionEventPredictor?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)

                inProgressStrokesView.eagerInit()
                
                // Create predictor once and store it
                val predictor = MotionEventPredictor.newInstance(rootView)
                predictorRef.value = predictor

                rootView.addView(inProgressStrokesView)
                rootView
            },
            // Update the touch listener when mode changes
            update = { rootView ->
                val predictor = predictorRef.value
                
                // In text mode, disable the ink touch listener to allow text field interaction
                if (mode == InkEditorMode.Text) {
                    // Return false to not consume touch events, allowing them to pass through
                    rootView.setOnTouchListener { _, _ -> false }
                    inProgressStrokesView.setOnTouchListener { _, _ -> false }
                } else if (predictor != null) {
                    // Draw mode - set up the ink touch listener
                    inProgressStrokesView.setOnTouchListener(null)
                    val touchListener = View.OnTouchListener { view, event ->
                        predictor.record(event)
                        val predictedEvent = predictor.predict()

                        try {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    view.requestUnbufferedDispatch(event)
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    currentPointerId.value = pointerId
                                    currentStrokeId.value = inProgressStrokesView.startStroke(
                                        event = event,
                                        pointerId = pointerId,
                                        brush = currentGetBrush.value()
                                    )
                                    true
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val pointerId = currentPointerId.value ?: return@OnTouchListener false
                                    val strokeId = currentStrokeId.value ?: return@OnTouchListener false

                                    for (pointerIndex in 0 until event.pointerCount) {
                                        if (event.getPointerId(pointerIndex) != pointerId) continue
                                        inProgressStrokesView.addToStroke(
                                            event,
                                            pointerId,
                                            strokeId,
                                            predictedEvent
                                        )
                                    }
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    if (pointerId == currentPointerId.value) {
                                        val strokeId = currentStrokeId.value
                                        if (strokeId != null) {
                                            inProgressStrokesView.finishStroke(
                                                event,
                                                pointerId,
                                                strokeId
                                            )
                                        }
                                        view.performClick()
                                    }
                                    true
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    if (pointerId == currentPointerId.value) {
                                        val strokeId = currentStrokeId.value
                                        if (strokeId != null) {
                                            inProgressStrokesView.cancelStroke(strokeId, event)
                                        }
                                    }
                                    true
                                }

                                else -> false
                            }
                        } finally {
                            predictedEvent?.recycle()
                        }
                    }
                    rootView.setOnTouchListener(touchListener)
                }
            }
        )

        // Render strokes and text fields  
        InkDisplaySurfaceWithText(
            finishedStrokesState = finishedStrokesState,
            canvasStrokeRenderer = canvasStrokeRenderer,
            textFieldManager = textFieldManager,
            isTextMode = mode == InkEditorMode.Text
        )
        
        // In text mode, add an invisible touch layer on top to capture taps and handle drags
        // This is needed because AndroidView intercepts events before Compose modifiers
        if (mode == InkEditorMode.Text && textFieldManager != null) {
            // Constants for gesture detection
            val longPressTimeoutMillis = 500L
            val doubleTapTimeoutMillis = 300L

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(textFieldManager) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downPosition = down.position

                            // First, check if this is a handle hit
                            val handleHit = textFieldManager.hitTestHandle(downPosition)
                            if (handleHit != null) {
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
                            var upOrCancel: androidx.compose.ui.geometry.Offset? = null

                            // Try to detect long press
                            try {
                                withTimeout(longPressTimeoutMillis) {
                                    val up = waitForUpOrCancellation()
                                    upOrCancel = up?.position
                                }
                            } catch (e: PointerEventTimeoutCancellationException) {
                                // Long press detected
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
                                        textFieldManager.handleDoubleTap(secondDown.position)
                                    }
                                }
                            } catch (e: PointerEventTimeoutCancellationException) {
                                // Single tap - hide context menu first
                                textFieldManager.textFields.forEach { it.showContextMenu = false }
                                
                                if (!textFieldManager.handleTap(downPosition)) {
                                    // No text field was tapped - create a new one
                                    val newTextField = textFieldManager.addTextField(downPosition, "")
                                    textFieldManager.requestFocus(newTextField)
                                }
                            }
                        }
                    }
            )
            
            // Context menu overlay for text fields
            val clipboardManager = LocalClipboardManager.current
            val density = LocalDensity.current
            
            textFieldManager.textFields.forEach { textFieldState ->
                if (textFieldState.showContextMenu) {
                    val menuX = textFieldState.position.x + textFieldState.contextMenuPosition.x
                    val menuY = textFieldState.position.y + textFieldState.contextMenuPosition.y
                    
                    TextFieldContextMenu(
                        hasSelection = textFieldState.hasSelection,
                        hasClipboardContent = clipboardManager.getText() != null,
                        onCut = {
                            if (textFieldState.hasSelection) {
                                clipboardManager.setText(AnnotatedString(textFieldState.selectedText))
                                textFieldState.deleteSelection(allowMerge = false)
                            }
                        },
                        onCopy = {
                            if (textFieldState.hasSelection) {
                                clipboardManager.setText(AnnotatedString(textFieldState.selectedText))
                            }
                        },
                        onPaste = {
                            clipboardManager.getText()?.let { text ->
                                textFieldState.insertText(text.toString(), allowMerge = false)
                            }
                        },
                        onSelectAll = {
                            textFieldState.selectAll()
                            // Reposition menu after select all
                            textFieldManager.showContextMenuForTextField(textFieldState)
                        },
                        onDismiss = {
                            textFieldState.showContextMenu = false
                        },
                        modifier = Modifier.offset {
                            IntOffset(
                                menuX.roundToInt(),
                                menuY.roundToInt().coerceAtLeast(0)
                            )
                        }
                    )
                }
            }
        }
    }
}
