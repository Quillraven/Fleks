package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.math.max

/**
 * A bag implementation for [entities][Entity] (=integer) values in Kotlin to avoid autoboxing.
 * It contains necessary functions for Fleks and some additional Kotlin standard library utilities.
 */
class ArrayEntityBag(
    size: Int = 64
) : MutableEntityBag {
    @PublishedApi
    internal var values: Array<Entity> = Array(size) { Entity.NONE }

    /**
     * Returns the size of the [MutableEntityBag].
     */
    override var size: Int = 0
        private set

    private lateinit var iterable: EntityBagIterable


    override fun add(element: Entity): Boolean {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity.NONE })
        }
        values[size++] = element

        return true
    }

    override fun addAll(elements: Collection<Entity>): Boolean {
        if (elements.isEmpty()) return false
        ensureCapacity(size + elements.size)
        elements.forEach {
            add(it)
        }
        return true
    }

    /**
     * Returns the size of the underlying [Array] that reflects the maximum amount
     * of [entities][Entity] that can be stored before resizing the [Array].
     */
    val capacity: Int
        get() = values.size

    /**
     * Adds the [entity] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    override operator fun plusAssign(entity: Entity) {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity.NONE })
        }
        values[size++] = entity
    }

    /**
     * Removes the [entity] of the bag.
     */
    override operator fun minusAssign(entity: Entity) {
        for (i in 0 until size) {
            if (values[i] == entity) {
                values[i] = values[--size]
                values[size] = Entity.NONE
                return
            }
        }
    }

    override fun removeAt(index: Int): Entity {
        if (index >= size) throw IndexOutOfBoundsException("index can't be >= size: $index >= $size")
        val value = values[index]
        values[index] = values[--size]
        values[size] = Entity.NONE
        return value
    }

    /**
     * Resets [size] to zero and clears any [entity][Entity] of the bag.
     */
    override fun clear() {
        values.fill(Entity.NONE)
        size = 0
    }

    /**
     * Resizes the bag to fit in the given [capacity] of [entities][Entity] if necessary.
     */
    fun ensureCapacity(capacity: Int) {
        if (capacity > values.size) {
            values = values.copyInto(Array(capacity + 1) { Entity.NONE })
        }
    }

    override fun containsAll(elements: Collection<Entity>): Boolean = elements.all { it in this }

    /**
     * Resets [size] to zero, clears any [entity][Entity] of the bag, and if necessary,
     * resizes the bag to be able to fit the given [capacity] of [entities][Entity].
     */
    fun clearEnsuringCapacity(capacity: Int) {
        if (capacity > values.size) {
            values = Array(capacity + 1) { Entity.NONE }
        } else {
            values.fill(Entity.NONE)
        }
        size = 0
    }

    /**
     * Sorts the bag according to the given [comparator].
     */
    override fun sort(comparator: EntityComparator) {
        values.quickSort(0, size, comparator)
    }

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    override fun get(index: Int): Entity {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index]
    }

    /**
     * Returns true if and only if the given [element] is part of the bag.
     */
    override fun contains(element: Entity): Boolean {
        for (i in 0 until size) {
            if (values[i] == element) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if and only if the bag is empty and contains no [entities][Entity].
     */
    override fun isEmpty(): Boolean = size == 0

    override fun remove(element: Entity): Boolean {
        for (i in 0 until size) {
            if (values[i] == element) {
                values[i] = values[--size]
                values[size] = Entity.NONE
                return true
            }
        }

        return false
    }

    override fun removeAll(elements: Collection<Entity>): Boolean {
        var changed = false

        elements.forEach {
            if(remove(it)) changed = true
        }

        return changed
    }

    override fun retainAll(elements: Collection<Entity>): Boolean {
        return removeAll { !elements.contains(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as MutableEntityBag

        if (size != other.size) return false
        return containsAll(other)
    }

    override fun hashCode(): Int {
        var result = values.contentHashCode()
        result = 31 * result + size
        return result
    }

    override fun iterator(): MutableIterator<Entity> {
        if (!::iterable.isInitialized) iterable = EntityBagIterable(this)
        return iterable.iterator()
    }
}
