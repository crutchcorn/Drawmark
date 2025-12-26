/*
 * Copyright 2023 The Android Open Source Project
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

package app.drawmark.android.lib.textcanvas.input.internal.undo

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.util.fastForEach
import kotlin.collections.removeFirst as removeFirstKt
import kotlin.collections.removeLast as removeLastKt

/**
 * A generic purpose undo/redo stack manager.
 *
 * @param initialUndoStack Previous undo stack if this manager is being restored from a saved state.
 * @param initialRedoStack Previous redo stack if this manager is being restored from a saved state.
 * @param capacity Maximum number of elements that can be hosted by this UndoManager. Total element
 *   count is the sum of undo and redo stack sizes.
 */
internal class UndoManager<T>(
    initialUndoStack: List<T> = emptyList(),
    initialRedoStack: List<T> = emptyList(),
    private val capacity: Int = 100,
) {
    private var undoStack = SnapshotStateList<T>().apply { addAll(initialUndoStack) }
    private var redoStack = SnapshotStateList<T>().apply { addAll(initialRedoStack) }

    internal val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    internal val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    val size: Int
        get() = undoStack.size + redoStack.size

    fun record(undoableAction: T) {
        // First clear the redo stack.
        redoStack.clear()

        while (size > capacity - 1) { // leave room for the immediate `add`
            undoStack.removeFirstKt()
        }
        undoStack.add(undoableAction)
    }

    /**
     * Request undo.
     *
     * This method returns the item that was on top of the undo stack. By the time this function
     * returns, the given item has already been carried to the redo stack.
     */
    fun undo(): T {
        val topOperation = undoStack.removeLastKt()

        redoStack.add(topOperation)
        return topOperation
    }

    /**
     * Request redo.
     *
     * This method returns the item that was on top of the redo stack. By the time this function
     * returns, the given item has already been carried back to the undo stack.
     */
    fun redo(): T {
        val topOperation = redoStack.removeLastKt()

        undoStack.add(topOperation)
        return topOperation
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    companion object {

        /**
         * Saver factory for a generic [UndoManager].
         *
         * @param itemSaver Since [UndoManager] is defined as a generic class, a specific item saver
         *   is required to _serialize_ each individual item in undo and redo stacks.
         */
        inline fun <reified T> createSaver(itemSaver: Saver<T, Any>) =
            object : Saver<UndoManager<T>, Any> {
                /**
                 * Saves the contents of given [value] to a list.
                 *
                 * List's structure is
                 * - Capacity
                 * - n; Number of items in undo stack
                 * - m; Number of items in redo stack
                 * - n items in order from undo stack
                 * - m items in order from redo stack
                 */
                override fun SaverScope.save(value: UndoManager<T>): Any = buildList {
                    add(value.capacity)
                    add(value.undoStack.size)
                    add(value.redoStack.size)
                    value.undoStack.fastForEach { with(itemSaver) { add(save(it)) } }
                    value.redoStack.fastForEach { with(itemSaver) { add(save(it)) } }
                }

                @Suppress("UNCHECKED_CAST")
                override fun restore(value: Any): UndoManager<T> {
                    val list = value as List<Any>
                    val (capacity, undoSize, redoSize) = (list as List<Int>)
                    var i = 3
                    val undoStackItems = buildList {
                        while (i < undoSize + 3) {
                            add(itemSaver.restore(list[i])!!)
                            i++
                        }
                    }
                    val redoStackItems = buildList {
                        while (i < undoSize + redoSize + 3) {
                            add(itemSaver.restore(list[i])!!)
                            i++
                        }
                    }
                    return UndoManager(undoStackItems, redoStackItems, capacity)
                }
            }
    }
}
