package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.binarySearch
import kotlin.math.max

/**
 * Stores component arrays and entity indices in a contiguous Columnar/SoA layout.
 * All entities in an archetype have the exact same set of components.
 */
class Archetype(
    val id: Int,
    val componentIds: IntArray, // Sorted array of component type IDs
    initialCapacity: Int = 16
) {
    var size: Int = 0
        private set

    // Transition graph edges for O(1) transitions
    internal val addTransitions = HashMap<Int, Archetype>()
    internal val removeTransitions = HashMap<Int, Archetype>()


    // Contiguous array of entity indices currently belonging to this archetype.
    var entities: IntArray = IntArray(initialCapacity)
        private set

    // Columns of component instances: columns[c] holds the array of components matching componentIds[c].
    // Each column is of type Array<Any?>.
    var columns: Array<Array<Any?>> = Array(componentIds.size) {
        arrayOfNulls(initialCapacity)
    }
        private set

    /**
     * Checks if this archetype contains a specific component type ID.
     */
    fun hasComponent(componentId: Int): Boolean {
        return binarySearch(componentIds, componentIds.size, componentId) >= 0
    }

    /**
     * Gets the column index for a given component type ID, or -1 if not present.
     */
    fun getColumnIndex(componentId: Int): Int {
        return binarySearch(componentIds, componentIds.size, componentId)
    }

    /**
     * Inserts an entity into the archetype, allocating a row.
     * Caller is responsible for writing component values to columns at the returned row.
     */
    fun addEntity(entityIndex: Int): Int {
        val row = size++
        if (row >= entities.size) {
            grow()
        }
        entities[row] = entityIndex
        return row
    }

    /**
     * Removes the entity at the given row using the swap-and-pop pattern.
     * The last entity in the archetype is moved into the freed row.
     * Returns the index of the entity that was moved to fill the gap, or -1 if no move occurred (last row was popped).
     */
    fun removeEntity(row: Int): Int {
        val lastRow = --size
        if (row != lastRow) {
            val movedEntity = entities[lastRow]
            entities[row] = movedEntity

            // Move components from the last row to the target row
            for (c in columns.indices) {
                columns[c][row] = columns[c][lastRow]
                columns[c][lastRow] = null // Avoid reference leaks
            }
            return movedEntity
        } else {
            // Popped the last element; just clear its component references
            for (c in columns.indices) {
                columns[c][row] = null
            }
            return -1
        }
    }

    private fun grow() {
        val newCapacity = max(16, entities.size * 2)
        entities = entities.copyOf(newCapacity)
        for (c in columns.indices) {
            columns[c] = columns[c].copyOf(newCapacity)
        }
    }
}
