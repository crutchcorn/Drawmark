package app.drawmark.android.prototype

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Data class representing a brush option in the selector.
 */
data class BrushOption(
    val family: String,
    val color: Int,
    val size: Float,
    val displayColor: Color
)

/**
 * Predefined brush options for the selector.
 */
val defaultBrushOptions = listOf(
    BrushOption("pen", Color.Black.toArgb(), 5f, Color.Black),
    BrushOption("pen", Color(0xFF4CAF50).toArgb(), 5f, Color(0xFF4CAF50)), // Green
    BrushOption("marker", Color(0xFF2196F3).toArgb(), 8f, Color(0xFF2196F3)), // Blue
    BrushOption("pen", Color(0xFFFF5722).toArgb(), 5f, Color(0xFFFF5722)), // Orange
    BrushOption("highlighter", Color(0xFFFFEB3B).toArgb(), 12f, Color(0xFFFFEB3B)), // Yellow
    BrushOption("pen", Color.White.toArgb(), 5f, Color.White),
)

/**
 * A single brush item in the selector.
 */
@Composable
private fun BrushItem(
    option: BrushOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Draw a simple pen/marker shape
        PenShape(
            color = option.displayColor,
            brushFamily = option.family,
            modifier = Modifier.size(36.dp)
        )
    }
}

/**
 * Draws a pen/marker shape with the given color.
 */
@Composable
private fun PenShape(
    color: Color,
    brushFamily: String,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeColor = Color.White
        val fillColor = color
        
        // Scale factor based on canvas size
        val width = size.width
        val height = size.height
        
        // Centered pen coordinates (relative to canvas size)
        val bodyLeft = width * 0.22f
        val bodyRight = width * 0.78f
        val bodyTop = height * 0.08f
        val bodyBottom = height * 0.55f
        val tipBottom = height * 0.92f
        val centerX = width * 0.5f

        // Draw pen outline
        drawPath(
            path = Path().apply {
                // Pen body - trapezoidal shape
                moveTo(bodyLeft, bodyTop)
                lineTo(bodyRight, bodyTop)
                lineTo(bodyRight - width * 0.05f, bodyBottom)
                lineTo(bodyLeft + width * 0.05f, bodyBottom)
                close()
            },
            color = strokeColor,
            style = Stroke(width = 2.5f)
        )

        // Draw the triangular tip
        drawPath(
            path = Path().apply {
                moveTo(bodyLeft + width * 0.05f, bodyBottom)
                lineTo(centerX, tipBottom)
                lineTo(bodyRight - width * 0.05f, bodyBottom)
                close()
            },
            color = strokeColor,
            style = Stroke(width = 2.5f)
        )

        // Fill the tip with the brush color
        drawPath(
            path = Path().apply {
                moveTo(bodyLeft + width * 0.1f, bodyBottom)
                lineTo(centerX, tipBottom - height * 0.08f)
                lineTo(bodyRight - width * 0.1f, bodyBottom)
                close()
            },
            color = fillColor
        )

        // Add a small divider line if it's a marker
        if (brushFamily == "marker" || brushFamily == "highlighter") {
            drawLine(
                color = strokeColor,
                start = Offset(bodyLeft, bodyTop + height * 0.15f),
                end = Offset(bodyRight, bodyTop + height * 0.15f),
                strokeWidth = 2f
            )
        }
    }
}

/**
 * A text mode button that displays "T" and indicates selection state.
 */
@Composable
private fun TextModeButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "T",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * A horizontal brush selector bar with rounded pill shape.
 * Displays multiple brush options that can be selected.
 * Tapping on an already-selected brush opens a color picker popup.
 * Includes a text mode button.
 */
@Composable
fun InkEditorBrushSelector(
    selectedBrushIndex: Int,
    isTextMode: Boolean,
    onBrushSelected: (index: Int, option: BrushOption) -> Unit,
    onColorChanged: (index: Int, newColor: Color) -> Unit,
    onTextModeSelected: () -> Unit,
    modifier: Modifier = Modifier,
    brushOptions: List<BrushOption> = defaultBrushOptions
) {
    val showColorPicker = remember { mutableStateOf(false) }
    val colorPickerBrushIndex = remember { mutableStateOf(-1) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF2D2D2D))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            brushOptions.forEachIndexed { index, option ->
                BrushItem(
                    option = option,
                    isSelected = index == selectedBrushIndex && !isTextMode,
                    onClick = {
                        if (index == selectedBrushIndex && !isTextMode) {
                            // Tapped on already-selected brush, show color picker
                            colorPickerBrushIndex.value = index
                            showColorPicker.value = true
                        } else {
                            // Select this brush (also exits text mode)
                            onBrushSelected(index, option)
                        }
                    }
                )
            }
            
            // Text mode button
            TextModeButton(
                isSelected = isTextMode,
                onClick = onTextModeSelected
            )
        }

        // Show color picker popup when triggered
        if (showColorPicker.value && colorPickerBrushIndex.value >= 0) {
            val currentOption = brushOptions.getOrNull(colorPickerBrushIndex.value)
            if (currentOption != null) {
                InkEditorBrushColorSelector(
                    selectedColor = currentOption.displayColor,
                    onColorSelected = { newColor ->
                        onColorChanged(colorPickerBrushIndex.value, newColor)
                    },
                    onDismiss = {
                        showColorPicker.value = false
                        colorPickerBrushIndex.value = -1
                    }
                )
            }
        }
    }
}