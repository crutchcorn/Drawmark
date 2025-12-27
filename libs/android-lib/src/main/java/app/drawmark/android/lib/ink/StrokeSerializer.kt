package app.drawmark.android.lib.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.InputToolType
import androidx.ink.brush.StockBrushes
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import androidx.ink.strokes.StrokeInputBatch
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Serialized representation of a stroke for persistent storage.
 */
data class SerializedStroke(
    val inputs: SerializedStrokeInputBatch,
    val brush: SerializedBrush,
    val zIndex: Long = 0L,
    val lastModified: Long = 0L
)

/**
 * A stroke with its z-index and lastModified timestamp for proper ordering with other canvas elements.
 */
data class StrokeWithZIndex(
    val stroke: Stroke,
    val zIndex: Long,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Serialized representation of a brush.
 */
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)

/**
 * Enum representing available stock brushes.
 */
enum class SerializedStockBrush {
    MARKER_V1,
    PRESSURE_PEN_V1,
    HIGHLIGHTER_V1
}

/**
 * Serialized representation of stroke input batch.
 */
data class SerializedStrokeInputBatch(
    val toolType: SerializedToolType,
    val strokeUnitLengthCm: Float,
    val inputs: List<SerializedStrokeInput>
)

/**
 * Serialized representation of a single stroke input point.
 */
data class SerializedStrokeInput(
    val x: Float,
    val y: Float,
    val timeMillis: Float,
    val pressure: Float,
    val tiltRadians: Float,
    val orientationRadians: Float,
    val strokeUnitLengthCm: Float
)

/**
 * Enum representing input tool types.
 */
enum class SerializedToolType {
    STYLUS,
    TOUCH,
    MOUSE,
    UNKNOWN
}

/**
 * Utility class for serializing and deserializing Ink strokes.
 */
class StrokeSerializer {

    private val gson: Gson = GsonBuilder().create()

    companion object {
        private val stockBrushToEnumValues: Map<BrushFamily, SerializedStockBrush> by lazy {
            mapOf(
                StockBrushes.marker() to SerializedStockBrush.MARKER_V1,
                StockBrushes.pressurePen() to SerializedStockBrush.PRESSURE_PEN_V1,
                StockBrushes.highlighter() to SerializedStockBrush.HIGHLIGHTER_V1,
            )
        }

        private val enumToStockBrush: Map<SerializedStockBrush, BrushFamily> by lazy {
            mapOf(
                SerializedStockBrush.MARKER_V1 to StockBrushes.marker(),
                SerializedStockBrush.PRESSURE_PEN_V1 to StockBrushes.pressurePen(),
                SerializedStockBrush.HIGHLIGHTER_V1 to StockBrushes.highlighter(),
            )
        }
    }

    /**
     * Serializes a set of strokes to a JSON string.
     * Note: This method does not preserve z-index. Use serializeStrokesWithZIndex for z-ordering support.
     */
    fun serializeStrokes(strokes: Set<Stroke>): String {
        val serializedStrokes = strokes.map { stroke ->
            SerializedStroke(
                inputs = serializeStrokeInputBatch(stroke.inputs),
                brush = serializeBrush(stroke.brush),
                zIndex = 0L
            )
        }
        return gson.toJson(serializedStrokes)
    }

    /**
     * Serializes a list of strokes with z-index information to a JSON string.
     */
    fun serializeStrokesWithZIndex(strokes: List<StrokeWithZIndex>): String {
        val serializedStrokes = strokes.map { strokeWithZIndex ->
            SerializedStroke(
                inputs = serializeStrokeInputBatch(strokeWithZIndex.stroke.inputs),
                brush = serializeBrush(strokeWithZIndex.stroke.brush),
                zIndex = strokeWithZIndex.zIndex,
                lastModified = strokeWithZIndex.lastModified
            )
        }
        return gson.toJson(serializedStrokes)
    }

    /**
     * Deserializes a JSON string to a set of strokes.
     * Note: This method does not preserve z-index. Use deserializeStrokesWithZIndex for z-ordering support.
     */
    fun deserializeStrokes(json: String): Set<Stroke> {
        if (json.isBlank()) return emptySet()

        return try {
            val type = object : TypeToken<List<SerializedStroke>>() {}.type
            val serializedStrokes: List<SerializedStroke> = gson.fromJson(json, type)
            serializedStrokes.mapNotNull { deserializeStroke(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Deserializes a JSON string to a list of strokes with z-index information.
     */
    fun deserializeStrokesWithZIndex(json: String): List<StrokeWithZIndex> {
        if (json.isBlank()) return emptyList()

        return try {
            val type = object : TypeToken<List<SerializedStroke>>() {}.type
            val serializedStrokes: List<SerializedStroke> = gson.fromJson(json, type)
            serializedStrokes.mapNotNull { serialized ->
                deserializeStroke(serialized)?.let { stroke ->
                    StrokeWithZIndex(stroke, serialized.zIndex, serialized.lastModified)
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.PRESSURE_PEN_V1,
        )
    }

    private fun serializeStrokeInputBatch(inputs: StrokeInputBatch): SerializedStrokeInputBatch {
        val serializedInputs = mutableListOf<SerializedStrokeInput>()
        val scratchInput = StrokeInput()

        for (i in 0 until inputs.size) {
            inputs.populate(i, scratchInput)
            serializedInputs.add(
                SerializedStrokeInput(
                    x = scratchInput.x,
                    y = scratchInput.y,
                    timeMillis = scratchInput.elapsedTimeMillis.toFloat(),
                    pressure = scratchInput.pressure,
                    tiltRadians = scratchInput.tiltRadians,
                    orientationRadians = scratchInput.orientationRadians,
                    strokeUnitLengthCm = scratchInput.strokeUnitLengthCm,
                )
            )
        }

        val toolType = when (inputs.getToolType()) {
            InputToolType.STYLUS -> SerializedToolType.STYLUS
            InputToolType.TOUCH -> SerializedToolType.TOUCH
            InputToolType.MOUSE -> SerializedToolType.MOUSE
            else -> SerializedToolType.UNKNOWN
        }

        return SerializedStrokeInputBatch(
            toolType = toolType,
            strokeUnitLengthCm = inputs.getStrokeUnitLengthCm(),
            inputs = serializedInputs,
        )
    }

    private fun deserializeStroke(serializedStroke: SerializedStroke): Stroke? {
        return try {
            val inputs = deserializeStrokeInputBatch(serializedStroke.inputs)
            val brush = deserializeBrush(serializedStroke.brush)
            Stroke(brush = brush, inputs = inputs)
        } catch (e: Exception) {
            null
        }
    }

    private fun deserializeBrush(serializedBrush: SerializedBrush): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush] ?: StockBrushes.pressurePen()

        return Brush.createWithColorLong(
            family = stockBrushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }

    private fun deserializeStrokeInputBatch(serializedBatch: SerializedStrokeInputBatch): StrokeInputBatch {
        val toolType = when (serializedBatch.toolType) {
            SerializedToolType.STYLUS -> InputToolType.STYLUS
            SerializedToolType.TOUCH -> InputToolType.TOUCH
            SerializedToolType.MOUSE -> InputToolType.MOUSE
            else -> InputToolType.UNKNOWN
        }

        val batch = MutableStrokeInputBatch()

        serializedBatch.inputs.forEach { input ->
            batch.add(
                type = toolType,
                x = input.x,
                y = input.y,
                elapsedTimeMillis = input.timeMillis.toLong(),
                pressure = input.pressure,
                tiltRadians = input.tiltRadians,
                orientationRadians = input.orientationRadians,
            )
        }

        return batch
    }
}
