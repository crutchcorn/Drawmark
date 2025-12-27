package app.drawmark.ink

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.ink.InkEditorMode
import app.drawmark.android.lib.ink.InkEditorSurface
import app.drawmark.android.lib.ink.StrokeSerializer
import app.drawmark.android.lib.textcanvas.InkCanvasTextFieldManager

@SuppressLint("ViewConstructor")
class InkEditorView(context: Context) : FrameLayout(context), InProgressStrokesFinishedListener {

    private val inProgressStrokesView = InProgressStrokesView(context)
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())
    private val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    private val strokeSerializer = StrokeSerializer()
    
    // Text field manager - held at view level for serialization access
    private val textFieldManager = InkCanvasTextFieldManager()

    // Callback for stroke changes
    var onStrokesChange: ((String) -> Unit)? = null
    
    // Callback for text field changes
    var onTextFieldsChange: ((String) -> Unit)? = null

    // Brush configuration - using mutableStateOf for reactive updates from React Native
    private var brushColorState = mutableStateOf(Color.Black.toArgb())
    private var brushSizeState = mutableStateOf(5f)
    private var brushFamilyState = mutableStateOf(StockBrushes.pressurePen())
    private var brushOpacityState = mutableStateOf(1f)
    
    // Editor mode - Draw or Text
    private var editorModeState = mutableStateOf(InkEditorMode.Draw)

    private val composeView: ComposeView
    private var wasDetached = false

    init {
        inProgressStrokesView.addFinishedStrokesListener(this)
        
        // Set up text field change callback
        textFieldManager.onTextFieldsChange = {
            notifyTextFieldsChanged()
        }

        composeView = ComposeView(context).apply {
            // Keep composition alive across detach/attach cycles (e.g., tab switches)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }


    private fun setComposeContent() {
        composeView.setContent {
            // Read state values inside the composable to trigger recomposition when they change
            val currentBrushColor = brushColorState.value
            val currentBrushSize = brushSizeState.value
            val currentBrushFamily = brushFamilyState.value
            val currentBrushOpacity = brushOpacityState.value
            val currentMode = editorModeState.value

            InkEditorSurface(
                inProgressStrokesView = inProgressStrokesView,
                finishedStrokesState = finishedStrokesState.value,
                canvasStrokeRenderer = canvasStrokeRenderer,
                getBrush = {
                    // Apply opacity by modifying the alpha channel of the color
                    val alpha = (currentBrushOpacity * 255).toInt().coerceIn(0, 255)
                    val colorWithOpacity = (currentBrushColor and 0x00FFFFFF) or (alpha shl 24)
                    Brush.createWithColorIntArgb(
                        family = currentBrushFamily,
                        colorIntArgb = colorWithOpacity,
                        size = currentBrushSize,
                        epsilon = 0.1f
                    )
                },
                mode = currentMode,
                textFieldManager = textFieldManager
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

    private fun createBrush(): Brush {
        return Brush.createWithColorIntArgb(
            family = brushFamilyState.value,
            colorIntArgb = brushColorState.value,
            size = brushSizeState.value,
            epsilon = 0.1f
        )
    }

    /**
     * Sets the brush color using ARGB integer format.
     */
    fun setBrushColor(color: Int) {
        brushColorState.value = color
    }

    /**
     * Sets the brush stroke size.
     */
    fun setBrushSize(size: Float) {
        brushSizeState.value = size
    }

    /**
     * Sets the brush family type.
     * Supported values: "pen", "marker", "highlighter"
     */
    fun setBrushFamily(family: String) {
        brushFamilyState.value = when (family.lowercase()) {
            "marker" -> StockBrushes.marker()
            "highlighter" -> StockBrushes.highlighter()
            else -> StockBrushes.pressurePen()
        }
    }

    /**
     * Sets the brush opacity (0.0 to 1.0).
     * This affects the alpha channel of the brush color.
     */
    fun setBrushOpacity(opacity: Float) {
        brushOpacityState.value = opacity.coerceIn(0f, 1f)
    }

    /**
     * Sets the editor mode.
     * Supported values: "draw", "text"
     */
    fun setEditorMode(mode: String) {
        editorModeState.value = when (mode.lowercase()) {
            "text" -> InkEditorMode.Text
            else -> InkEditorMode.Draw
        }
    }

    /**
     * Clears all strokes from the canvas.
     */
    fun clearCanvas() {
        finishedStrokesState.value = emptySet()
        textFieldManager.clearTextFields()
        notifyStrokesChanged()
        notifyTextFieldsChanged()
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
     * Loads text fields from a serialized JSON string.
     */
    fun loadTextFields(json: String) {
        if (json.isEmpty()) return
        textFieldManager.loadTextFields(json)
    }

    /**
     * Gets the current text fields as a serialized JSON string.
     */
    fun getSerializedTextFields(): String {
        return textFieldManager.serializeTextFields()
    }

    /**
     * Notifies the listener that strokes have changed.
     */
    private fun notifyStrokesChanged() {
        val serialized = strokeSerializer.serializeStrokes(finishedStrokesState.value)
        onStrokesChange?.invoke(serialized)
    }

    /**
     * Notifies the listener that text fields have changed.
     */
    private fun notifyTextFieldsChanged() {
        val serialized = textFieldManager.serializeTextFields()
        onTextFieldsChange?.invoke(serialized)
    }

    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokesState.value += strokes.values
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
        notifyStrokesChanged()
    }
}
