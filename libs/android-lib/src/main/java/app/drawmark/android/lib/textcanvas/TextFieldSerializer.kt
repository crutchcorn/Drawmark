package app.drawmark.android.lib.textcanvas

import androidx.compose.ui.geometry.Offset
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Serialized representation of a text field for persistent storage.
 */
data class SerializedTextField(
    val text: String,
    val positionX: Float,
    val positionY: Float
)

/**
 * Utility class for serializing and deserializing text fields.
 */
class TextFieldSerializer {

    private val gson: Gson = GsonBuilder().create()

    /**
     * Serializes a list of text field states to a JSON string.
     */
    fun serializeTextFields(textFields: List<CanvasTextFieldState>): String {
        val serializedTextFields = textFields.map { state ->
            SerializedTextField(
                text = state.text,
                positionX = state.position.x,
                positionY = state.position.y
            )
        }
        return gson.toJson(serializedTextFields)
    }

    /**
     * Deserializes a JSON string to a list of text field states.
     */
    fun deserializeTextFields(json: String): List<CanvasTextFieldState> {
        if (json.isBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<SerializedTextField>>() {}.type
            val serializedTextFields: List<SerializedTextField> = gson.fromJson(json, type)
            serializedTextFields.map { serialized ->
                CanvasTextFieldState.withText(
                    text = serialized.text,
                    position = Offset(serialized.positionX, serialized.positionY)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
