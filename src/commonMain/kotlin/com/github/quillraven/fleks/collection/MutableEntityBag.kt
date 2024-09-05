package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

interface MutableEntityBag : EntityBag {

    /**
     * Adds the [entity] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    operator fun plusAssign(entity: Entity)

    /**
     * Removes the [entity] of the bag.
     */
    operator fun minusAssign(entity: Entity)

    /**
     * Resets [size] to zero and clears any [entity][Entity] of the bag.
     */
    fun clear()

    /**
     * Resizes the bag to fit in the given [capacity] of [entities][Entity] if necessary.
     */
    fun ensureCapacity(capacity: Int)

    /**
     * Resets [size] to zero, clears any [entity][Entity] of the bag, and if necessary,
     * resizes the bag to be able to fit the given [capacity] of [entities][Entity].
     */
    fun clearEnsuringCapacity(capacity: Int)

    /**
     * Sorts the bag according to the given [comparator].
     */
    fun sort(comparator: EntityComparator)
}
