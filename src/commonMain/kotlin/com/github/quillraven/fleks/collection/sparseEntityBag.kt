package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

/**
 * A sparse-set implementation for [entities][Entity] (=integer) values in Kotlin to avoid autoboxing.
 *
 * - **O(1)** membership tests via a sparse array that is indexed by entity id and stores the
 *   position of the entity within a dense array.
 * - **O(1)** adding and removing of entities. Removing uses a swap-and-pop operation to keep
 *   the dense array tightly packed (no holes).
 * - **O(N)** iterations over the densely packed [dense] array which is cache friendly and
 *   does not require any null checks.
 *
 * This makes the [SparseEntityBag] the single data structure needed for membership checks
 * and iterations of a set of entities, e.g. for a [com.github.quillraven.fleks.Family].
 * It is always up to date and does not need any lazy update or "dirty" flag.
 */
class SparseEntityBag(
    initialCapacity: Int = 64
) {
    /**
     * The densely packed [entities][Entity] of the bag. It is used for iterations and
     * also determines the [size] of the bag.
     */
    @PublishedApi
    internal val dense = MutableEntityBag(initialCapacity)

    /**
     * Sparse array indexed by entity id that stores the position of the entity within the
     * [dense] array or [Entity.NONE] if the entity is not part of the bag. The array
     * grows on demand whenever an entity id exceeds the array's [size][IntArray.size].
     */
    @PublishedApi
    internal var sparse = IntArray(initialCapacity) { Entity.NONE.id }

    /**
     * Monotonic counter that gets incremented with every structural modification of the bag
     * (add, remove, sort, clear). It can be used to detect if the bag changed without the
     * need for an explicit "is dirty" flag.
     */
    @PublishedApi
    internal var version = 0

    /**
     * Returns the number of [entities][Entity] that belong to the bag.
     */
    val size: Int
        get() = dense.size

    /**
     * Returns true if and only if the given [entity] is part of the bag.
     */
    operator fun contains(entity: Entity): Boolean {
        val denseIdx = sparse.getOrNull(entity.id) ?: return false
        return denseIdx != Entity.NONE.id
    }

    /**
     * Adds the [entity] to the bag by appending it to the [dense] array and recording
     * its position in the [sparse] array. If the [entity] is already part of the bag
     * then it will be added a second time.
     */
    operator fun plusAssign(entity: Entity) {
        ensureSparseCapacity(entity.id)
        sparse[entity.id] = dense.size
        dense += entity
        version++
    }

    /**
     * Removes the [entity] from the bag using a swap-and-pop operation. The last [entity][Entity]
     * of the [dense] array moves into the position of the removed entity to keep the dense
     * array tightly packed, and its [sparse] entry gets updated accordingly.
     */
    operator fun minusAssign(entity: Entity) {
        val denseIdx = sparse.getOrNull(entity.id) ?: return
        if (denseIdx == Entity.NONE.id) {
            return
        }
        val moved = dense.removeAt(denseIdx)
        if (moved.id != entity.id) {
            sparse[moved.id] = denseIdx
        }
        sparse[entity.id] = Entity.NONE.id
        version++
    }

    /**
     * Sorts the [entities][Entity] of the bag by the given [comparator] and rebuilds the
     * [sparse] array to reflect the new positions within the [dense] array.
     */
    fun sort(comparator: EntityComparator) {
        dense.sort(comparator)
        for (i in 0 until dense.size) {
            sparse[dense.values[i]] = i
        }
        version++
    }

    /**
     * Resets the bag by removing all [entities][Entity].
     */
    fun clear() {
        dense.clear()
        sparse.fill(Entity.NONE.id)
        version++
    }

    /**
     * Performs the given [action] for all [entities][Entity] of the bag in the order of the
     * [dense] array.
     */
    inline fun forEach(action: (Entity) -> Unit) {
        dense.forEach(action)
    }

    private fun ensureSparseCapacity(entityId: Int) {
        if (entityId >= sparse.size) {
            sparse = sparse.copyOf(entityId + 1) { Entity.NONE.id }
        }
    }

    override fun toString(): String {
        return "SparseEntityBag(size=$size, dense=$dense)"
    }
}
