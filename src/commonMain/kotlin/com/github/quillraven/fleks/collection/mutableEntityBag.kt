@file:Suppress("OVERRIDE_BY_INLINE")

package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import kotlin.math.max
import kotlin.math.min

/**
 * Returns a new [MutableEntityBag] with the given [entities].
 */
fun mutableEntityBagOf(vararg entities: Entity): MutableEntityBag {
    return MutableEntityBag(entities.size).apply {
        entities.forEach { this += it }
    }
}

/**
 * Returns a new empty [MutableEntityBag] of the given [size].
 */
fun mutableEntityBagOf(size: Int): MutableEntityBag = MutableEntityBag(size)

/**
 * A bag implementation for [entities][Entity] (=integer) values in Kotlin to avoid autoboxing.
 * It contains necessary functions for Fleks and some additional Kotlin standard library utilities.
 */
class MutableEntityBag(
    size: Int = 64
) : EntityBag {
    @PublishedApi
    internal var values: Array<Entity> = Array(size) { Entity.NONE }

    /**
     * Returns the size of the [MutableEntityBag].
     */
    override var size: Int = 0
        private set

    /**
     * Returns the size of the underlying [Array] that reflects the maximum amount
     * of [entities][Entity] that can be stored before resizing the [Array].
     */
    val capacity: Int
        get() = values.size

    /**
     * Adds the [entity] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    operator fun plusAssign(entity: Entity) {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity.NONE })
        }
        values[size++] = entity
    }

    /**
     * Removes the [entity] of the bag.
     */
    operator fun minusAssign(entity: Entity) {
        for (i in 0 until size) {
            if (values[i] == entity) {
                values[i] = values[--size]
                values[size] = Entity.NONE
                return
            }
        }
    }

    /**
     * Adds all [entities] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    operator fun plusAssign(entities: EntityBag) {
        entities.forEach { plusAssign(it) }
    }

    /**
     * Adds all entities of the [family] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    operator fun plusAssign(family: Family) {
        family.entities.forEach { plusAssign(it) }
    }

    /**
     * Removes all [entities] of the bag.
     */
    operator fun minusAssign(entities: EntityBag) {
        entities.forEach { minusAssign(it) }
    }

    /**
     * Resets [size] to zero and clears any [entity][Entity] of the bag.
     */
    fun clear() {
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
    fun sort(comparator: EntityComparator) {
        values.quickSort(0, size, comparator)
    }

    /**
     * Returns true if and only if the given [entity] is part of the bag.
     */
    override fun contains(entity: Entity): Boolean {
        for (i in 0 until size) {
            if (values[i] == entity) {
                return true
            }
        }
        return false
    }

    /**
     * Creates an [EntityBagIterator] for the bag. If the bag gets updated
     * during iteration then [EntityBagIterator.reset] must be called to guarantee correct iterator behavior.
     */
    override fun iterator(): Iterator<Entity> = EntityBagIterator(this)

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    override fun containsAll(entities: Collection<Entity>): Boolean = entities.all { it in this }

    /**
     * Returns true if and only if the bag is empty and contains no [entities][Entity].
     */
    override fun isEmpty(): Boolean = size == 0

    /**
     * Returns the number of [entities][Entity] in this bag.
     */
    override fun count(): Int = size

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    override inline fun count(predicate: (Entity) -> Boolean): Int {
        var result = 0
        for (i in 0 until size) {
            if (predicate(values[i])) {
                ++result
            }
        }
        return result
    }

    /**
     * Returns a [List] containing only [entities][Entity] matching the given [predicate].
     */
    override inline fun filter(predicate: (Entity) -> Boolean): EntityBag {
        val result = MutableEntityBag((size * 0.25f).toInt())
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                result += entity
            }
        }
        return result
    }

    /**
     * Returns a [List] containing all [entities][Entity] not matching the given [predicate].
     */
    override inline fun filterNot(predicate: (Entity) -> Boolean): EntityBag {
        val result = MutableEntityBag((size * 0.25f).toInt())
        for (i in 0 until size) {
            val entity = values[i]
            if (!predicate(entity)) {
                result += entity
            }
        }
        return result
    }

    /**
     * Returns a [List] containing only [entities][Entity] matching the given [predicate].
     */
    override inline fun filterIndexed(predicate: (index: Int, entity: Entity) -> Boolean): EntityBag {
        val result = MutableEntityBag((size * 0.25f).toInt())
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(i, entity)) {
                result += entity
            }
        }
        return result
    }

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    override inline fun filterTo(
        destination: MutableEntityBag,
        predicate: (Entity) -> Boolean
    ): MutableEntityBag {
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                destination += entity
            }
        }
        return destination
    }

    /**
     * Appends all [entities][Entity] not matching the given [predicate] to the given [destination].
     */
    override inline fun filterNotTo(
        destination: MutableEntityBag,
        predicate: (Entity) -> Boolean
    ): MutableEntityBag {
        for (i in 0 until size) {
            val entity = values[i]
            if (!predicate(entity)) {
                destination += entity
            }
        }
        return destination
    }

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    override inline fun filterIndexedTo(
        destination: MutableEntityBag,
        predicate: (index: Int, Entity) -> Boolean
    ): MutableEntityBag {
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(i, entity)) {
                destination += entity
            }
        }
        return destination
    }

    /**
     * Returns a new bag of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun flatMapBag(transform: (Entity) -> EntityBag): EntityBag {
        val result = MutableEntityBag(size)
        for (i in 0 until size) {
            transform(values[i]).forEach { result += it }
        }
        return result
    }

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun <R> flatMapNotNull(transform: (Entity) -> Iterable<R?>?): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            transform(values[i])?.forEach {
                if (it == null) return@forEach
                result += it
            }
        }
        return result
    }

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun <R> flatMapSequenceNotNull(transform: (Entity) -> Sequence<R?>?): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            transform(values[i])?.forEach {
                if (it == null) return@forEach
                result += it
            }
        }
        return result
    }

    /**
     * Returns a new bag of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun flatMapBagNotNull(transform: (Entity) -> EntityBag?): EntityBag {
        val result = MutableEntityBag(size)
        for (i in 0 until size) {
            val transformVal = transform(values[i]) ?: continue
            transformVal.forEach { result += it }
        }
        return result
    }

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and returns a map where each group key is associated with an [EntityBag]
     * of corresponding [entities][Entity].
     */
    override inline fun <K> groupBy(keySelector: (Entity) -> K): Map<K, MutableEntityBag> {
        val result = mutableMapOf<K, MutableEntityBag>()
        for (i in 0 until size) {
            val entity = values[i]
            val key = keySelector(entity)
            result.getOrPut(key) { MutableEntityBag() } += entity
        }
        return result
    }

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    override operator fun get(index: Int): Entity {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index]
    }

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and puts to the [destination] map each group key associated with
     * an [EntityBag] of corresponding elements.
     */
    override inline fun <K, M : MutableMap<in K, MutableEntityBag>> groupByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M {
        for (i in 0 until size) {
            val entity = values[i]
            val key = keySelector(entity)
            destination.getOrPut(key) { MutableEntityBag() } += entity
        }
        return destination
    }

    /**
     * Splits the original bag into a pair of bags,
     * where the first bag contains elements for which predicate yielded true,
     * while the second bag contains elements for which predicate yielded false.
     */
    override inline fun partition(predicate: (Entity) -> Boolean): Pair<EntityBag, EntityBag> {
        val first = MutableEntityBag()
        val second = MutableEntityBag()
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                first += entity
            } else {
                second += entity
            }
        }
        return Pair(first, second)
    }

    /**
     * Splits the original bag into two bags,
     * where [first] contains elements for which predicate yielded true,
     * while [second] contains elements for which predicate yielded false.
     */
    override inline fun partitionTo(
        first: MutableEntityBag,
        second: MutableEntityBag,
        predicate: (Entity) -> Boolean
    ) {
        first.clear()
        second.clear()
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                first += entity
            } else {
                second += entity
            }
        }
    }

    /**
     * Returns a [List] containing the first [n][] [entities][Entity].
     */
    override fun take(n: Int): EntityBag {
        val result = MutableEntityBag(max(min(n, size), 0))
        for (i in 0 until min(n, size)) {
            result += values[i]
        }
        return result
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

    override fun toString(): String {
        return "MutableEntityBag(size=$size, values=${values.contentToString()})"
    }
}
