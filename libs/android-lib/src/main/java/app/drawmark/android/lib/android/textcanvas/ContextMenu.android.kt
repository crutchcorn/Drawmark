/*
 * Copyright 2021 The Android Open Source Project
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

package app.drawmark.android.lib.textcanvas

import android.content.res.Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import app.drawmark.android.lib.textcanvas.contextmenu.builder.TextContextMenuBuilderScope
import app.drawmark.android.lib.textcanvas.contextmenu.builder.item
import app.drawmark.android.lib.textcanvas.contextmenu.data.TextContextMenuSession
import app.drawmark.android.lib.textcanvas.input.internal.selection.TextFieldSelectionState
import app.drawmark.android.lib.textcanvas.selection.SelectionManager
import app.drawmark.android.lib.textcanvas.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit,
) {
    CommonContextMenuArea(manager, content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    CommonContextMenuArea(selectionState, enabled, content)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun ContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
    CommonContextMenuArea(manager, content)
}

internal fun TextContextMenuBuilderScope.textItem(
    resources: Resources,
    item: TextContextMenuItems,
    enabled: Boolean,
    onClick: TextContextMenuSession.() -> Unit,
) {
    if (enabled) {
        item(
            key = item.key,
            label = resources.getString(item.stringId.value),
            leadingIcon = item.drawableId.value,
            onClick = onClick,
        )
    }
}
