// SPDX-License-Identifier: MIT
//
// Drawmark - Ink Canvas View
// InkCanvasView.kt
//
// A Jetpack Compose-based view for displaying ink strokes (read-only).
// For editing capabilities, use InkEditorView instead.

package app.drawmark.android.lib.ink

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke

/**
 * InkCanvasView is a FrameLayout that embeds a Jetpack Compose surface
 * for displaying ink strokes (read-only, no drawing capabilities).
 * For editing capabilities, use InkEditorView instead.
 */
@SuppressLint("ViewConstructor")
class InkCanvasView(context: Context) : FrameLayout(context) {
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val strokeSerializer = StrokeSerializer()
    private val composeView: ComposeView
    private var wasDetached = false

    init {
        composeView = ComposeView(context).apply {
            // Keep composition alive across detach/attach cycles (e.g., tab switches)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    private fun setComposeContent() {
        composeView.setContent {
            InkDisplaySurface(
                finishedStrokesState = finishedStrokesState.value,
                canvasStrokeRenderer = canvasStrokeRenderer
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Post to ensure ComposeView is fully attached before setting content
        post {
            if (composeView.isAttachedToWindow) {
                if (wasDetached) {
                    // Dispose old composition first for re-attachment
                    composeView.disposeComposition()
                }
                setComposeContent()
                // Force a full layout pass to trigger initial render
                composeView.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                )
                composeView.layout(0, 0, width, height)
                composeView.invalidate()
            }
        }
    }

    override fun onDetachedFromWindow() {
        wasDetached = true
        super.onDetachedFromWindow()
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
}

@Composable
private fun InkDisplaySurface(
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
