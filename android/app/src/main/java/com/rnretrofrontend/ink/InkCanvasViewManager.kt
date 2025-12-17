// SPDX-License-Identifier: MIT
//
// RNRetroFrontend - Ink Canvas View Manager
// InkCanvasViewManager.kt
//
// React Native ViewManager for the InkCanvasView component.

package com.rnretrofrontend.ink

import android.graphics.Color
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.common.MapBuilder
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.events.RCTEventEmitter

class InkCanvasViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<InkCanvasView>() {

    companion object {
        const val REACT_CLASS = "InkCanvasView"
        const val COMMAND_CLEAR = 1
        const val COMMAND_LOAD_STROKES = 2
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): InkCanvasView {
        val view = InkCanvasView(context)
        
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

    @ReactProp(name = "brushColor")
    fun setBrushColor(view: InkCanvasView, color: String?) {
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
    fun setBrushSize(view: InkCanvasView, size: Float) {
        view.setBrushSize(size)
    }

    @ReactProp(name = "brushFamily")
    fun setBrushFamily(view: InkCanvasView, family: String?) {
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

    override fun receiveCommand(view: InkCanvasView, commandId: String?, args: ReadableArray?) {
        when (commandId) {
            "clear" -> view.clearCanvas()
            "loadStrokes" -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun receiveCommand(view: InkCanvasView, commandId: Int, args: ReadableArray?) {
        when (commandId) {
            COMMAND_CLEAR -> view.clearCanvas()
            COMMAND_LOAD_STROKES -> {
                val strokesJson = args?.getString(0) ?: ""
                view.loadStrokes(strokesJson)
            }
        }
    }
}
