package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A bag implementation for [entities][Entity] (=integer) values in Kotlin to avoid autoboxing.
 * It contains necessary functions for Fleks and some additional Kotlin standard library utilities.
 */
class EntityBag(
    size: Int = 64
) : EntityCollection {
    @PublishedApi
    internal var values: Array<Entity> = Array(size) { Entity(-1) }

    /**
     * Returns the size of the [EntityBag].
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
    internal operator fun plusAssign(entity: Entity) {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity(-1) })
        }
        values[size++] = entity
    }

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    operator fun get(index: Int): Entity {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index]
    }

    /**
     * Resets [size] to zero and clears any [entity][Entity] of the bag.
     */
    internal fun clear() {
        values.fill(Entity(-1))
        size = 0
    }

    /**
     * Resizes the bag to fit in the given [capacity] of [entities][Entity] if necessary.
     */
    internal fun ensureCapacity(capacity: Int) {
        if (capacity >= values.size) {
            values = values.copyInto(Array(capacity + 1) { Entity(-1) })
        }
    }

    /**
     * Sorts the bag according to the given [comparator].
     */
    internal fun sort(comparator: EntityComparator) {
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
     * Returns true if and only if all given [entities] are part of the bag.
     */
    override fun containsAll(entities: Collection<Entity>): Boolean {
        for (i in 0 until size) {
            if (values[i] !in entities) {
                return false
            }
        }
        return true
    }

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    override fun containsAll(entities: EntityBag): Boolean {
        for (i in 0 until size) {
            if (values[i] !in entities) {
                return false
            }
        }
        return true
    }

    /**
     * Returns true if and only if the bag is empty and contains no [entities][Entity].
     */
    override fun isEmpty(): Boolean = size == 0

    /**
     * Returns true if and only if the bag is not empty and contains at least one [entity][Entity].
     */
    override fun isNotEmpty(): Boolean = size > 0

    /**
     * Returns true if all [entities][Entity] of the bag match the given [predicate].
     */
    override fun all(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (!predicate(values[i])) {
                return false
            }
        }
        return true
    }

    /**
     * Returns true if at least one [entity][Entity] of the bag matches the given [predicate].
     */
    override fun any(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (predicate(values[i])) {
                return true
            }
        }
        return false
    }

    /**
     * Returns true if no [entity][Entity] of the bag matches the given [predicate].
     */
    override fun none(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (predicate(values[i])) {
                return false
            }
        }
        return true
    }

    /**
     * Returns a [Map] containing key-value pairs provided by the [transform] function applied to
     * each [entity][Entity] of the bag.
     */
    override fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for (i in 0 until size) {
            result += transform(values[i])
        }
        return result
    }

    /**
     * Returns a [Map] containing the [entities][Entity] of the bag indexed by the key
     * returned from [keySelector] function applied to each [entity][Entity] of the bag.
     */
    override fun <K> associateBy(keySelector: (Entity) -> K): Map<K, Entity> {
        val result = mutableMapOf<K, Entity>()
        for (i in 0 until size) {
            val entity = values[i]
            result[keySelector(entity)] = entity
        }
        return result
    }

    /**
     * Returns a [Map] containing the values provided by [valueTransform] and indexed by the
     * [keySelector] function applied to each [entity][Entity] of the bag.
     */
    override fun <K, V> associateBy(keySelector: (Entity) -> K, valueTransform: (Entity) -> V): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for (i in 0 until size) {
            val entity = values[i]
            result[keySelector(entity)] = valueTransform(entity)
        }
        return result
    }

    /**
     * Populates and returns the [destination] mutable map containing key-value pairs
     * provided by the [transform] function applied to each [entity][Entity] of the bag.
     */
    override fun <K, V, M : MutableMap<in K, in V>> associateTo(destination: M, transform: (Entity) -> Pair<K, V>): M {
        destination.clear()
        for (i in 0 until size) {
            destination += transform(values[i])
        }
        return destination
    }

    /**
     * Populates and returns the [destination] mutable map containing the [entities][Entity]
     * of the bag indexed by the key returned from [keySelector] function applied to
     * each [entity][Entity] of the bag.
     */
    override fun <K, M : MutableMap<in K, Entity>> associateByTo(destination: M, keySelector: (Entity) -> K): M {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            destination[keySelector(entity)] = entity
        }
        return destination
    }

    /**
     * Populates and returns the [destination] mutable map containing the values
     * provided by [valueTransform] and indexed by the [keySelector] function applied
     * to each [entity][Entity] of the bag.
     */
    override fun <K, V, M : MutableMap<in K, in V>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            destination[keySelector(entity)] = valueTransform(entity)
        }
        return destination
    }

    /**
     * Returns the number of [entities][Entity] in this bag.
     */
    override fun count(): Int = size

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    override fun count(predicate: (Entity) -> Boolean): Int {
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
    override fun filter(predicate: (Entity) -> Boolean): List<Entity> {
        val result = mutableListOf<Entity>()
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
    override fun filterNot(predicate: (Entity) -> Boolean): List<Entity> {
        val result = mutableListOf<Entity>()
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
    override fun filterIndexed(predicate: (index: Int, Entity) -> Boolean): List<Entity> {
        val result = mutableListOf<Entity>()
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
    override fun <C : MutableCollection<Entity>> filterTo(destination: C, predicate: (Entity) -> Boolean): C {
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
    override fun <C : MutableCollection<Entity>> filterNotTo(destination: C, predicate: (Entity) -> Boolean): C {
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
    override fun <C : MutableCollection<Entity>> filterIndexedTo(
        destination: C,
        predicate: (index: Int, Entity) -> Boolean
    ): C {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(i, entity)) {
                destination += entity
            }
        }
        return destination
    }

    /**
     * Returns the first [entity][Entity] matching the given [predicate], or null if no such
     * [entity][Entity] was found.
     */
    override fun find(predicate: (Entity) -> Boolean): Entity? {
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                return entity
            }
        }
        return null
    }

    /**
     * Returns the first [entity][Entity].
     *
     * @throws [NoSuchElementException] if the bag is empty.
     */
    override fun first(): Entity {
        if (isEmpty()) {
            throw NoSuchElementException("EntityBag is empty!")
        }
        return values[0]
    }

    /**
     * Returns the first [entity][Entity] matching the given [predicate].
     *
     * @throws [NoSuchElementException] if the bag is empty or there is no such [entity][Entity].
     */
    override fun first(predicate: (Entity) -> Boolean): Entity {
        return find(predicate) ?: throw NoSuchElementException("There is no entity matching the given predicate!")
    }

    /**
     * Returns the first [entity][Entity], or null if the bag is empty.
     */
    override fun firstOrNull(): Entity? {
        if (isEmpty()) {
            return null
        }
        return values[0]
    }

    /**
     * Returns the first [entity][Entity] matching the given [predicate], or null
     * if the bag is empty or no such [entity][Entity] was found.
     */
    override fun firstOrNull(predicate: (Entity) -> Boolean): Entity? = find(predicate)

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity].
     */
    override fun <R> fold(initial: R, operation: (acc: R, Entity) -> R): R {
        var accumulator = initial
        for (i in 0 until size) {
            accumulator = operation(accumulator, values[i])
        }
        return accumulator
    }

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity] with its index in the original bag.
     */
    override fun <R> foldIndexed(initial: R, operation: (index: Int, acc: R, Entity) -> R): R {
        var accumulator = initial
        for (i in 0 until size) {
            accumulator = operation(i, accumulator, values[i])
        }
        return accumulator
    }

    /**
     * Performs the given [action] on each [entity][Entity].
     */
    override fun forEach(action: (Entity) -> Unit) {
        for (i in 0 until size) {
            action(values[i])
        }
    }

    /**
     * Performs the given [action] on each [entity][Entity], providing sequential
     * index with the [entity][Entity].
     */
    override fun forEachIndexed(action: (index: Int, Entity) -> Unit) {
        for (i in 0 until size) {
            action(i, values[i])
        }
    }

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] of the bag.
     */
    override fun <R> map(transform: (Entity) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            result += transform(values[i])
        }
        return result
    }

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] and its index of the bag.
     */
    override fun <R> mapIndexed(transform: (index: Int, Entity) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            result += transform(i, values[i])
        }
        return result
    }

    /**
     * Applies the given [transform] function to each [entity][Entity] of the bag and appends
     * the results to the given [destination].
     */
    override fun <R, C : MutableCollection<in R>> mapTo(destination: C, transform: (Entity) -> R): C {
        for (i in 0 until size) {
            destination += transform(values[i])
        }
        return destination
    }

    /**
     * Applies the given [transform] function to each [entity][Entity] and its index of the bag and appends
     * the results to the given [destination].
     */
    override fun <R, C : MutableCollection<in R>> mapIndexedTo(
        destination: C,
        transform: (index: Int, Entity) -> R
    ): C {
        destination.clear()
        for (i in 0 until size) {
            destination += transform(i, values[i])
        }
        return destination
    }

    /**
     * Returns a random [entity][Entity] of the bag.
     *
     * @throws [NoSuchElementException] if the bag is empty.
     */
    override fun random(): Entity {
        if (isEmpty()) {
            throw NoSuchElementException("EntityBag is empty!")
        }
        return values[Random.Default.nextInt(size)]
    }

    /**
     * Returns a random [entity][Entity] of the bag, or null if the bag is empty.
     */
    override fun randomOrNull(): Entity? {
        if (isEmpty()) {
            return null
        }
        return values[Random.Default.nextInt(size)]
    }

    /**
     * Returns a [List] containing first [n] [entities][Entity].
     */
    override fun take(n: Int): List<Entity> {
        val result = mutableListOf<Entity>()
        for (i in 0 until min(n, size)) {
            result += values[i]
        }
        return result
    }
}
