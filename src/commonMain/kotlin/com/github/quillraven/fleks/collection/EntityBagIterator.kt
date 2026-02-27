package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

/**
 * Creates an [EntityBagIterator] for the bag. If the bag gets updated
 * during iteration then [EntityBagIterator.reset] must be called to guarantee correct iterator behavior.
 */
operator fun EntityBag.iterator(): EntityBagIterator = EntityBagIterator(this)

/**
 * An iterator over an [EntityBag]. Allows iterating in a forward and backward direction.
 * Also, supports looping iteration which means that if the iterator is at the end/beginning of
 * the bag, then it will go to the beginning/end of the bag.
 * The iterator returns [Entity.NONE] in case an [entity][Entity] does not exist.
 */
data class EntityBagIterator(private val bag: EntityBag) {
    private var currentIdx = -1

    /**
     * Returns **true** if and only if there is a next [entity][Entity] in the bag.
     */
    operator fun hasNext(): Boolean = currentIdx < bag.size - 1

    /**
     * Returns the next [entity][Entity] of the bag and moves the iterator forward.
     * If [loop] is true then the iterator starts again from the beginning if it is at the end.
     */
    fun next(loop: Boolean = false): Entity = when {
        hasNext() -> bag[++currentIdx]

        loop && bag.isNotEmpty() -> {
            currentIdx = -1
            bag[++currentIdx]
        }

        else -> Entity.NONE
    }

    /**
     * Returns the next [entity][Entity] of the bag and moves the iterator forward.
     */
    operator fun next(): Entity = next(false)

    /**
     * Returns **true** if and only if there is a previous [entity][Entity] in the bag.
     */
    fun hasPrevious(): Boolean = currentIdx > 0

    /**
     * Returns the previous [entity][Entity] of the bag and moves the iterator backward.
     * If [loop] is true then the iterator starts again at the end if it is at the beginning.
     */
    fun previous(loop: Boolean = false): Entity = when {
        hasPrevious() -> bag[--currentIdx]

        loop && bag.isNotEmpty() -> {
            currentIdx = bag.size
            bag[--currentIdx]
        }

        else -> Entity.NONE
    }

    /**
     * Resets the iterator to the beginning of the bag.
     */
    fun reset() {
        currentIdx = -1
    }

    /**
     * Moves the iterator to the first [Entity] matching the given [predicate] and returns it.
     * If there is no such [Entity] then the iterator is reset and [Entity.NONE] is returned instead.
     */
    fun goToFirst(predicate: (Entity) -> Boolean): Entity {
        currentIdx = bag.indexOfFirst(predicate)
        if (currentIdx == -1) {
            reset()
            return Entity.NONE
        }

        return bag[currentIdx]
    }

    /**
     * Moves the iterator to the last [Entity] matching the given [predicate] and returns it.
     * If there is no such [Entity] then the iterator is reset and [Entity.NONE] is returned instead.
     */
    fun goToLast(predicate: (Entity) -> Boolean): Entity {
        currentIdx = bag.indexOfLast(predicate)
        if (currentIdx == -1) {
            reset()
            return Entity.NONE
        }

        return bag[currentIdx]
    }
}
