package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

interface EntityBag : Collection<Entity> {

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    operator fun get(index: Int): Entity
}
