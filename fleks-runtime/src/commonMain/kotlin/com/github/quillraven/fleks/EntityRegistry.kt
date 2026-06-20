package com.github.quillraven.fleks

import kotlin.math.max

/**
 * Manages allocation, recycling, and metadata indexing for Entity IDs.
 * Uses flat primitive arrays for O(1) performance and zero heap allocations.
 */
class EntityRegistry(initialCapacity: Int = 1024) {
    // Stores the generation/version of each slot index.
    private var versions = IntArray(initialCapacity)

    // Implicit free-list linking: nextFree[index] holds the next free slot index.
    private var nextFree = IntArray(initialCapacity)
    private var freeListHead: Int = -1

    // Monotonically increasing index for allocating fresh slots when free list is empty.
    private var nextIndex: Int = 0

    // Metadata arrays mapping entity indices to their archetype and row.
    private var archetypeIds = IntArray(initialCapacity)
    private var rows = IntArray(initialCapacity)

    val capacity: Int get() = versions.size
    val activeEntityCount: Int get() = nextIndex - freeListCount()

    private fun freeListCount(): Int {
        var count = 0
        var current = freeListHead
        while (current != -1) {
            count++
            current = nextFree[current]
        }
        return count
    }

    /**
     * Checks if a given [Entity] is currently active and alive in this registry.
     */
    fun isAlive(entity: Entity): Boolean {
        val index = entity.index
        return index in 0 until nextIndex && versions[index] == entity.version
    }

    /**
     * Reconstructs an [Entity] handle from a raw slot index using its current version.
     */
    fun getEntity(index: Int): Entity {
        return Entity.of(index, versions[index])
    }


    /**
     * Allocates a new entity index (either recycling from free list or grabbing a new slot).
     * Defaults to the root archetype (ID 0) and row -1.
     */
    fun create(): Entity {
        val index: Int
        if (freeListHead != -1) {
            index = freeListHead
            freeListHead = nextFree[index]
        } else {
            index = nextIndex++
            if (index >= versions.size) {
                grow(versions.size * 2)
            }
        }

        val version = versions[index]
        archetypeIds[index] = 0 // Root archetype ID (usually 0)
        rows[index] = -1        // Default row before archetype insertion

        return Entity.of(index, version)
    }

    /**
     * Destroys an entity by invalidating its version and returning its index to the free list.
     */
    fun destroy(entity: Entity): Boolean {
        if (!isAlive(entity)) return false

        val index = entity.index
        // Increment version to immediately invalidate any existing Entity handles holding this index.
        versions[index] = (versions[index] + 1) and Int.MAX_VALUE

        // Push index back onto the free list
        nextFree[index] = freeListHead
        freeListHead = index

        // Reset metadata
        archetypeIds[index] = 0
        rows[index] = -1

        return true
    }

    /**
     * Returns the archetype ID of the entity.
     */
    fun getArchetypeId(entity: Entity): Int {
        if (!isAlive(entity)) return -1
        return archetypeIds[entity.index]
    }

    /**
     * Sets the archetype ID of the entity.
     */
    fun setArchetypeId(entity: Entity, archetypeId: Int) {
        if (isAlive(entity)) {
            archetypeIds[entity.index] = archetypeId
        }
    }

    /**
     * Returns the row index of the entity within its archetype table.
     */
    fun getRow(entity: Entity): Int {
        if (!isAlive(entity)) return -1
        return rows[entity.index]
    }

    /**
     * Sets the row index of the entity within its archetype table.
     */
    fun setRow(entity: Entity, row: Int) {
        if (isAlive(entity)) {
            rows[entity.index] = row
        }
    }

    /**
     * Direct unsafe updates for batch operations (flushing).
     */
    internal fun setRecord(index: Int, archetypeId: Int, row: Int) {
        archetypeIds[index] = archetypeId
        rows[index] = row
    }

    private fun grow(newCapacity: Int) {
        val finalCapacity = max(nextIndex + 1, newCapacity)
        versions = versions.copyOf(finalCapacity)
        nextFree = nextFree.copyOf(finalCapacity)
        archetypeIds = archetypeIds.copyOf(finalCapacity)
        rows = rows.copyOf(finalCapacity)
    }
}
