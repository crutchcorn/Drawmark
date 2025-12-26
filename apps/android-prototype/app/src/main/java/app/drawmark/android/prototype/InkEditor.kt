package app.drawmark.android.prototype

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import app.drawmark.android.lib.ink.InkEditorMode
import app.drawmark.android.lib.ink.InkEditorSurface
import app.drawmark.android.lib.textcanvas.rememberInkCanvasTextFieldManager

@Composable
fun InkEditor(
    context: Context,
    modifier: Modifier = Modifier,
) {
    val inProgressStrokesView = remember { InProgressStrokesView(context) }
    val finishedStrokesState = remember { mutableStateOf(emptySet<Stroke>()) }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

    // Brush options state (mutable to allow color changes)
    val brushOptions = remember { mutableStateOf(defaultBrushOptions) }

    // Brush configuration state
    val selectedBrushIndex = remember { mutableStateOf(0) }
    val brushFamily = remember { mutableStateOf(StockBrushes.pressurePen()) }
    val brushColor = remember { mutableStateOf(Color.Black.toArgb()) }
    val brushSize = remember { mutableStateOf(5f) }

    // Editor mode state (Draw or Text)
    val editorMode = remember { mutableStateOf(InkEditorMode.Draw) }

    // Text field manager for text mode
    val textFieldManager = rememberInkCanvasTextFieldManager()

    val getBrush: () -> Brush = {
        Brush.createWithColorIntArgb(
            family = brushFamily.value,
            colorIntArgb = brushColor.value,
            size = brushSize.value,
            epsilon = 0.1f
        )
    }

    // Set up the finished strokes listener
    val finishedStrokesListener = remember {
        object : InProgressStrokesFinishedListener {
            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                finishedStrokesState.value = finishedStrokesState.value + strokes.values
                inProgressStrokesView.removeFinishedStrokes(strokes.keys)
            }
        }
    }

    // Add/remove listener based on composition lifecycle
    androidx.compose.runtime.DisposableEffect(inProgressStrokesView) {
        inProgressStrokesView.addFinishedStrokesListener(finishedStrokesListener)
        onDispose {
            inProgressStrokesView.removeFinishedStrokesListener(finishedStrokesListener)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Drawing surface
        InkEditorSurface(
            inProgressStrokesView = inProgressStrokesView,
            finishedStrokesState = finishedStrokesState.value,
            canvasStrokeRenderer = canvasStrokeRenderer,
            getBrush = getBrush,
            mode = editorMode.value,
            textFieldManager = textFieldManager,
        )

        // Mode toggle buttons at the top
        Row(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color.White.copy(alpha = 0.9f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            Button(
                onClick = { editorMode.value = InkEditorMode.Draw },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editorMode.value == InkEditorMode.Draw) Color.Blue else Color.Gray
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("✏️ Draw", color = Color.White)
            }
            Button(
                onClick = { editorMode.value = InkEditorMode.Text },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editorMode.value == InkEditorMode.Text) Color.Blue else Color.Gray
                )
            ) {
                Text("📝 Text", color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            InkEditorBrushSelector(
                selectedBrushIndex = selectedBrushIndex.value,
                brushOptions = brushOptions.value,
                onBrushSelected = { index, option ->
                    selectedBrushIndex.value = index
                    brushColor.value = option.color
                    brushSize.value = option.size
                    brushFamily.value = when (option.family.lowercase()) {
                        "marker" -> StockBrushes.marker()
                        "highlighter" -> StockBrushes.highlighter()
                        else -> StockBrushes.pressurePen()
                    }
                },
                onColorChanged = { index, newColor ->
                    // Update the brush option with the new color
                    val updatedOptions = brushOptions.value.toMutableList()
                    val oldOption = updatedOptions[index]
                    updatedOptions[index] = oldOption.copy(
                        color = newColor.toArgb(),
                        displayColor = newColor
                    )
                    brushOptions.value = updatedOptions

                    // Also update the current brush color if this is the selected brush
                    if (index == selectedBrushIndex.value) {
                        brushColor.value = newColor.toArgb()
                    }
                }
            )
        }
    }
}
