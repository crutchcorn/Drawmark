package app.drawmark.android.lib.textcanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A floating context menu for text field operations (Cut, Copy, Paste).
 *
 * @param hasSelection Whether there is text selected (enables Cut/Copy)
 * @param hasClipboardContent Whether there is content on the clipboard (enables Paste)
 * @param onCut Called when Cut is pressed
 * @param onCopy Called when Copy is pressed
 * @param onPaste Called when Paste is pressed
 * @param onSelectAll Called when Select All is pressed
 * @param onDismiss Called when the menu should be dismissed
 * @param modifier Modifier for the menu container
 */
@Composable
fun TextFieldContextMenu(
    hasSelection: Boolean,
    hasClipboardContent: Boolean,
    onCut: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        if (hasSelection) {
            ContextMenuItem(
                text = "Cut",
                onClick = {
                    onCut()
                    onDismiss()
                }
            )
            ContextMenuItem(
                text = "Copy",
                onClick = {
                    onCopy()
                    onDismiss()
                }
            )
        }
        
        if (hasClipboardContent) {
            ContextMenuItem(
                text = "Paste",
                onClick = {
                    onPaste()
                    onDismiss()
                }
            )
        }
        
        ContextMenuItem(
            text = "Select All",
            onClick = {
                onSelectAll()
                // Don't dismiss - keep menu open after select all
            }
        )
    }
}

@Composable
private fun ContextMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Black,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
