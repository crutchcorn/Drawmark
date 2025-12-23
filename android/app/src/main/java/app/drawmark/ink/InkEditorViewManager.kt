// SPDX-License-Identifier: MIT
//
// Drawmark - Ink Editor View Manager
// InkEditorViewManager.kt
//
// React Native ViewManager for the InkEditorView component (supports editing).

package app.drawmark.ink

import android.graphics.Color
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter

class InkEditorViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<InkEditorView>() {

    companion object {
        const val REACT_CLASS = "InkEditorView"
        const val COMMAND_CLEAR = 1
        const val COMMAND_LOAD_STROKES = 2
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): InkEditorView {
        val view = InkEditorView(context)
        
        // Set up the strokes change callback to emit events to React Native
        view.onStrokesChange = { serializedStrokes ->
            val event = Arguments.createMap().apply {
                putString("strokes", serializedStrokes)
            }
            context.getJSModule(RCTEventEmitter::class.java)
                .receiveEvent(view.id, "onStrokesChange", event)
        }
        
        return view
    }

    override fun getExportedCustomDirectEventTypeConstants(): Map<String, Any>? {
        return MapBuilder.builder<String, Any>()
            .put("onStrokesChange", MapBuilder.of("registrationName", "onStrokesChange"))
            .build()
    }

    override fun onAfterUpdateTransaction(view: InkEditorView) {
        super.onAfterUpdateTransaction(view)
        
        // Force layout update after React Native updates the view
        Choreographer.getInstance().postFrameCallback {
            manuallyLayoutChildren(view)
            view.invalidate()
        }
    }

    /**
     * Manually layout the view and its children since React Native doesn't 
     * automatically trigger Android's layout pass for native views.
     */
    private fun manuallyLayoutChildren(view: ViewGroup) {
        val width = view.width
        val height = view.height

        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, width, height)
        
        for (i in 0 until view.childCount) {
            val child = view.getChildAt(i)
            child.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            child.layout(0, 0, width, height)
        }
    }

    @ReactProp(name = "brushColor")
    fun setBrushColor(view: InkEditorView, color: String?) {
        color?.let {
            try {
                view.setBrushColor(Color.parseColor(it))
            } catch (e: IllegalArgumentException) {
                // Invalid color format, use default black
                view.setBrushColor(Color.BLACK)
            }
        }
    }

    @ReactProp(name = "brushSize", defaultFloat = 5f)
    fun setBrushSize(view: InkEditorView, size: Float) {
        view.setBrushSize(size)
    }

    @ReactProp(name = "brushFamily")
    fun setBrushFamily(view: InkEditorView, family: String?) {
        family?.let {
            view.setBrushFamily(it)
        }
    }

    override fun getCommandsMap(): Map<String, Int> {
        return mapOf(
            "clear" to COMMAND_CLEAR,
            "loadStrokes" to COMMAND_LOAD_STROKES
        )
    }

    override fun receiveCommand(view: InkEditorView, commandId: String?, args: ReadableArray?) {
        when (commandId) {
            "clear" -> view.clearCanvas()
            "loadStrokes" -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun receiveCommand(view: InkEditorView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            COMMAND_CLEAR -> view.clearCanvas()
            COMMAND_LOAD_STROKES -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }
}
