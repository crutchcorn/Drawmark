package app.drawmark.ink

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp

class InkCanvasViewManager(
    private val reactContext: ReactApplicationContext
) : SimpleViewManager<InkCanvasView>() {

    companion object {
        const val REACT_CLASS = "InkCanvasView"
    }

    override fun getName(): String = REACT_CLASS

    override fun createViewInstance(context: ThemedReactContext): InkCanvasView {
        return InkCanvasView(context)
    }

    @ReactProp(name = "initialStrokes")
    fun setInitialStrokes(view: InkCanvasView, strokesJson: String?) {
        strokesJson?.let { view.loadStrokes(it) }
    }

    @ReactProp(name = "initialTextFields")
    fun setInitialTextFields(view: InkCanvasView, textFieldsJson: String?) {
        textFieldsJson?.let { view.loadTextFields(it) }
    }
}
