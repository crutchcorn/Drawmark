// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Ink Canvas View
// InkCanvasView.kt
//
// A Jetpack Compose-based drawing surface using the Android Ink API.

package com.rnretrofrontend.ink

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor

/**
 * InkCanvasView is a FrameLayout that embeds a Jetpack Compose drawing surface
 * using the Android Ink API for low-latency stylus/touch input.
 */
class InkCanvasView(context: Context) : FrameLayout(context), InProgressStrokesFinishedListener {

    private val inProgressStrokesView = InProgressStrokesView(context)
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val strokeSerializer = StrokeSerializer()
    private var composeView: ComposeView? = null

    // Callback for stroke changes
    var onStrokesChange: ((String) -> Unit)? = null

    // Brush configuration
    private var brushColor: Int = Color.Black.toArgb()
    private var brushSize: Float = 5f
    private var brushFamily = StockBrushes.pressurePen()

    init {
        inProgressStrokesView.addFinishedStrokesListener(this)
        setupComposeView()
    }

    private fun setupComposeView() {
        composeView = ComposeView(context).apply {
            // Dispose composition when view is detached - clean slate each time
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                InkDrawingSurface(
                    inProgressStrokesView = inProgressStrokesView,
                    finishedStrokesState = finishedStrokesState.value,
                    canvasStrokeRenderer = canvasStrokeRenderer,
                    getBrush = { createBrush() }
                )
            }
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onDetachedFromWindow() {
        // Clean up the listener to avoid memory leaks
        inProgressStrokesView.removeFinishedStrokesListener(this)
        super.onDetachedFromWindow()
    }

    private fun createBrush(): Brush {
        return Brush.createWithColorIntArgb(
            family = brushFamily,
            colorIntArgb = brushColor,
            size = brushSize,
            epsilon = 0.1f
        )
    }

    /**
     * Sets the brush color using ARGB integer format.
     */
    fun setBrushColor(color: Int) {
        brushColor = color
    }

    /**
     * Sets the brush stroke size.
     */
    fun setBrushSize(size: Float) {
        brushSize = size
    }

    /**
     * Sets the brush family type.
     * Supported values: "pen", "marker", "highlighter"
     */
    fun setBrushFamily(family: String) {
        brushFamily = when (family.lowercase()) {
            "marker" -> StockBrushes.marker()
            "highlighter" -> StockBrushes.highlighter()
            else -> StockBrushes.pressurePen()
        }
    }

    /**
     * Clears all strokes from the canvas.
     */
    fun clearCanvas() {
        finishedStrokesState.value = emptySet()
        notifyStrokesChanged()
    }

    /**
     * Loads strokes from a serialized JSON string.
     */
    fun loadStrokes(json: String) {
        if (json.isEmpty()) return
        val strokes = strokeSerializer.deserializeStrokes(json)
        finishedStrokesState.value = strokes
    }

    /**
     * Gets the current strokes as a serialized JSON string.
     */
    fun getSerializedStrokes(): String {
        return strokeSerializer.serializeStrokes(finishedStrokesState.value)
    }

    /**
     * Notifies the listener that strokes have changed.
     */
    private fun notifyStrokesChanged() {
        val serialized = strokeSerializer.serializeStrokes(finishedStrokesState.value)
        onStrokesChange?.invoke(serialized)
    }

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokesState.value = finishedStrokesState.value + strokes.values
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
        notifyStrokesChanged()
    }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
private fun InkDrawingSurface(
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: Set<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    getBrush: () -> Brush
) {
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)
                
                // Remove from existing parent if any (needed for view reuse)
                (inProgressStrokesView.parent as? android.view.ViewGroup)?.removeView(inProgressStrokesView)
                
                inProgressStrokesView.apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }

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
                                    brush = getBrush()
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
            }
        )

        // Canvas for rendering finished strokes
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
}
