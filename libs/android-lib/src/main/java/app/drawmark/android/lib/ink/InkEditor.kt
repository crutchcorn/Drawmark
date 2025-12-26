package app.drawmark.android.lib.ink

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import app.drawmark.android.lib.textcanvas.InkCanvasTextFieldManager

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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)

                inProgressStrokesView.eagerInit()

                val predictor = MotionEventPredictor.newInstance(rootView)
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
                rootView.addView(inProgressStrokesView)
                rootView
            },
            // Update the touch listener when mode changes
            update = { rootView ->
                // In text mode, disable the ink touch listener to allow text field interaction
                if (mode == InkEditorMode.Text) {
                    rootView.setOnTouchListener(null)
                } else {
                    val predictor = MotionEventPredictor.newInstance(rootView)
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

        InkDisplaySurfaceWithText(
            finishedStrokesState = finishedStrokesState,
            canvasStrokeRenderer = canvasStrokeRenderer,
            textFieldManager = textFieldManager,
            isTextMode = mode == InkEditorMode.Text
        )
    }
}
