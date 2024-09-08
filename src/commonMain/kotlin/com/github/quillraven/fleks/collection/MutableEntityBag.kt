package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

interface MutableEntityBag : EntityBag, MutableCollection<Entity> {

    /**
     * Adds the [entity] to the bag.
     */
    operator fun plusAssign(entity: Entity)

    /**
     * Removes the [entity] of the bag.
     */
    operator fun minusAssign(entity: Entity)


    /** Removes and returns the [Entity] at the specified index. */
    fun removeAt(index: Int): Entity

    /**
     * Sorts the bag according to the given [comparator].
     */
    fun sort(comparator: EntityComparator)
}
