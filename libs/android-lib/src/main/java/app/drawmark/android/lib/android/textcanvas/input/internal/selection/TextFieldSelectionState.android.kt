/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.drawmark.android.lib.textcanvas.input.internal.selection

import android.os.Build
import androidx.compose.foundation.interaction.MutableInteractionSource
import app.drawmark.android.lib.textcanvas.TextContextMenuItems
import app.drawmark.android.lib.textcanvas.TextContextMenuItems.Autofill
import app.drawmark.android.lib.textcanvas.TextContextMenuItems.Copy
import app.drawmark.android.lib.textcanvas.TextContextMenuItems.Cut
import app.drawmark.android.lib.textcanvas.TextContextMenuItems.Paste
import app.drawmark.android.lib.textcanvas.TextContextMenuItems.SelectAll
import app.drawmark.android.lib.textcanvas.TextDragObserver
import app.drawmark.android.lib.textcanvas.contextmenu.builder.TextContextMenuBuilderScope
import app.drawmark.android.lib.textcanvas.contextmenu.modifier.addTextContextMenuComponentsWithContext
import app.drawmark.android.lib.textcanvas.selection.MouseSelectionObserver
import app.drawmark.android.lib.textcanvas.selection.addPlatformTextContextMenuItems
import app.drawmark.android.lib.textcanvas.textItem
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

// TODO(halilibo): Add a new TextToolbar option "paste as plain text".
internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier = addTextContextMenuComponentsWithContext { context ->
    fun TextContextMenuBuilderScope.textFieldItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        desiredState: TextToolbarState = TextToolbarState.None,
        closePredicate: (() -> Boolean)? = null,
        onClick: () -> Unit,
    ) {
        textItem(context.resources, item, enabled) {
            onClick()
            if (closePredicate?.invoke() ?: true) close()
            state.updateTextToolbarState(desiredState)
        }
    }

    fun TextContextMenuBuilderScope.textFieldSuspendItem(
        item: TextContextMenuItems,
        enabled: Boolean,
        onClick: suspend () -> Unit,
    ) {
        textFieldItem(item, enabled) {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { onClick() }
        }
    }

    addPlatformTextContextMenuItems(
        context = context,
        editable = state.editable,
        text = state.textFieldState.visualText.text,
        selection = state.textFieldState.visualText.selection,
        platformSelectionBehaviors = state.platformSelectionBehaviors,
    ) {
        with(state) {
            separator()
            textFieldSuspendItem(Cut, enabled = canShowCutMenuItem()) { cut() }
            textFieldSuspendItem(Copy, enabled = canShowCopyMenuItem()) {
                copy(cancelSelection = textToolbarShown)
            }
            textFieldSuspendItem(Paste, enabled = canShowPasteMenuItem()) { paste() }
            textFieldItem(
                item = SelectAll,
                enabled = canShowSelectAllMenuItem(),
                desiredState = TextToolbarState.Selection,
                closePredicate = { !textToolbarShown },
            ) {
                selectAll()
            }
            if (Build.VERSION.SDK_INT >= 26) {
                textFieldItem(Autofill, enabled = canShowAutofillMenuItem()) { autofill() }
            }
            separator()
        }
    }
}

/** Runs platform-specific text tap gestures logic. */
internal actual suspend fun TextFieldSelectionState.detectTextFieldTapGestures(
    pointerInputScope: PointerInputScope,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit,
) =
    defaultDetectTextFieldTapGestures(
        pointerInputScope,
        interactionSource,
        requestFocus,
        showKeyboard,
    )

/** Runs platform-specific text selection gestures logic. */
internal actual suspend fun TextFieldSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver,
) = pointerInputScope.defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip: Boolean = false
    private var _hasText: Boolean = false

    actual val hasText: Boolean
        get() = _hasText

    actual val hasClip: Boolean
        get() = _hasClip

    actual suspend fun update() {
        // On Android, we don't need to read `clipEntry` to evaluate `canPaste`.
        // Reading `clipEntry` directly can trigger a "App pasted from Clipboard" system warning.
        _hasClip = clipboard.nativeClipboard.hasPrimaryClip()
        _hasText =
            _hasClip &&
                clipboard.nativeClipboard.primaryClipDescription?.hasMimeType("text/*") == true
    }
}
