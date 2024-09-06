@file:Suppress("OVERRIDE_BY_INLINE")

package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.math.max
import kotlin.math.min

class ListMutableEntityBag(
    @PublishedApi
    internal val values: MutableList<Entity>
) : MutableEntityBag {

    override val size: Int
        get() = values.size

    /**
     * Adds the [entity] to the bag. If the [capacity] is not sufficient then a resize is happening.
     */
    override operator fun plusAssign(entity: Entity) {
        values.add(entity)
    }

    /**
     * Removes the [entity] of the bag.
     */
    override operator fun minusAssign(entity: Entity) {
        values.remove(entity)
    }

    /**
     * Resets [size] to zero and clears any [entity][Entity] of the bag.
     */
    override fun clear() {
        values.clear()
    }

    /**
     * Resizes the bag to fit in the given [capacity] of [entities][Entity] if necessary.
     */
    override fun ensureCapacity(capacity: Int) {
        throw NotImplementedError("Not Supported")
    }

    /**
     * Resets [size] to zero, clears any [entity][Entity] of the bag, and if necessary,
     * resizes the bag to be able to fit the given [capacity] of [entities][Entity].
     */
    override fun clearEnsuringCapacity(capacity: Int) {
        throw NotImplementedError("Not Supported")
    }

    /**
     * Sorts the bag according to the given [comparator].
     */
    override fun sort(comparator: EntityComparator) {
        values.sortWith(comparator)
    }

    /**
     * Returns true if and only if the given [entity] is part of the bag.
     */
    override fun contains(entity: Entity): Boolean {
        return values.contains(entity)
    }

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    override fun containsAll(entities: Collection<Entity>): Boolean = entities.all { it in this }

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    override fun containsAll(entities: EntityBag): Boolean = entities.all { it in this }

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
    override inline fun all(crossinline predicate: (Entity) -> Boolean): Boolean {
        return values.all(predicate)
    }

    /**
     * Returns true if at least one [entity][Entity] of the bag matches the given [predicate].
     */
    override inline fun any(predicate: (Entity) -> Boolean): Boolean {
        return values.any(predicate)
    }

    /**
     * Returns true if no [entity][Entity] of the bag matches the given [predicate].
     */
    override inline fun none(predicate: (Entity) -> Boolean): Boolean {
        return values.none(predicate)
    }

    /**
     * Returns a [Map] containing key-value pairs provided by the [transform] function applied to
     * each [entity][Entity] of the bag.
     */
    override inline fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V> {
        return values.associate(transform)
    }

    /**
     * Returns a [Map] containing the [entities][Entity] of the bag indexed by the key
     * returned from [keySelector] function applied to each [entity][Entity] of the bag.
     */
    override inline fun <K> associateBy(keySelector: (Entity) -> K): Map<K, Entity> {
        return values.associateBy(keySelector)
    }

    /**
     * Returns a [Map] containing the values provided by [valueTransform] and indexed by the
     * [keySelector] function applied to each [entity][Entity] of the bag.
     */
    override inline fun <K, V> associateBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, V> {
        return values.associateBy(keySelector, valueTransform)
    }

    /**
     * Populates and returns the [destination] mutable map containing key-value pairs
     * provided by the [transform] function applied to each [entity][Entity] of the bag.
     */
    override inline fun <K, V, M : MutableMap<in K, in V>> associateTo(
        destination: M,
        transform: (Entity) -> Pair<K, V>
    ): M {
        return values.associateTo(destination, transform)
    }

    /**
     * Populates and returns the [destination] mutable map containing the [entities][Entity]
     * of the bag indexed by the key returned from [keySelector] function applied to
     * each [entity][Entity] of the bag.
     */
    override inline fun <K, M : MutableMap<in K, Entity>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M {
        return values.associateByTo(destination, keySelector)
    }

    /**
     * Populates and returns the [destination] mutable map containing the values
     * provided by [valueTransform] and indexed by the [keySelector] function applied
     * to each [entity][Entity] of the bag.
     */
    override inline fun <K, V, M : MutableMap<in K, in V>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M {
        return values.associateByTo(destination, keySelector, valueTransform)
    }

    /**
     * Returns the number of [entities][Entity] in this bag.
     */
    override fun count(): Int = size

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    override inline fun count(predicate: (Entity) -> Boolean): Int {
        return values.count(predicate)
    }

    /**
     * Returns a [List] containing only [entities][Entity] matching the given [predicate].
     */
    override inline fun filter(predicate: (Entity) -> Boolean): EntityBag {
        val result = ArrayMutableEntityBag((size * 0.25f).toInt())
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
        val result = ArrayMutableEntityBag((size * 0.25f).toInt())
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
        val result = ArrayMutableEntityBag((size * 0.25f).toInt())
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
     * Returns the first [entity][Entity] matching the given [predicate], or null if no such
     * [entity][Entity] was found.
     */
    override inline fun find(predicate: (Entity) -> Boolean): Entity? {
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
        return values.first()
    }

    /**
     * Returns the first [entity][Entity] matching the given [predicate].
     *
     * @throws [NoSuchElementException] if the bag is empty or there is no such [entity][Entity].
     */
    override inline fun first(predicate: (Entity) -> Boolean): Entity {
        return find(predicate) ?: throw NoSuchElementException("There is no entity matching the given predicate!")
    }

    /**
     * Returns the first [entity][Entity], or null if the bag is empty.
     */
    override fun firstOrNull(): Entity? {
        return values.firstOrNull()
    }

    /**
     * Returns the first [entity][Entity] matching the given [predicate], or null
     * if the bag is empty or no such [entity][Entity] was found.
     */
    override inline fun firstOrNull(predicate: (Entity) -> Boolean): Entity? = find(predicate)

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun <R> flatMap(transform: (Entity) -> Iterable<R>): List<R> {
        return values.flatMap(transform)
    }

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun <R> flatMapSequence(transform: (Entity) -> Sequence<R>): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            result.addAll(transform(values[i]))
        }
        return result
    }

    /**
     * Returns a new bag of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    override inline fun flatMapBag(transform: (Entity) -> EntityBag): EntityBag {
        val result = ArrayMutableEntityBag(size)
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
        val result = ArrayMutableEntityBag(size)
        for (i in 0 until size) {
            val transformVal = transform(values[i]) ?: continue
            transformVal.forEach { result += it }
        }
        return result
    }

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity].
     */
    override inline fun <R> fold(
        initial: R,
        operation: (acc: R, entity: Entity) -> R
    ): R {
        return values.fold(initial, operation)
    }

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity] with its index in the original bag.
     */
    override inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, entity: Entity) -> R
    ): R {
        return values.foldIndexed(initial, operation)
    }

    /**
     * Performs the given [action] on each [entity][Entity].
     */
    override inline fun forEach(action: (Entity) -> Unit) {
        for (i in 0 until size) {
            action(values[i])
        }
    }

    /**
     * Performs the given [action] on each [entity][Entity], providing sequential
     * index with the [entity][Entity].
     */
    override inline fun forEachIndexed(action: (index: Int, entity: Entity) -> Unit) {
        for (i in 0 until size) {
            action(i, values[i])
        }
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
            result.getOrPut(key) { ArrayMutableEntityBag() } += entity
        }
        return result
    }

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    override operator fun get(index: Int): Entity {
        return values.get(index)
    }

    /**
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the bag
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and returns
     * a map where each group key is associated with a list of corresponding values.
     */
    override inline fun <K, V> groupBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, List<V>> {
        return values.groupBy(keySelector, valueTransform)
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
            destination.getOrPut(key) { ArrayMutableEntityBag() } += entity
        }
        return destination
    }

    /**
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the bag
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and puts
     * to the [destination] map each group key associated with a list of corresponding values.
     */
    override inline fun <K, V, M : MutableMap<in K, MutableList<V>>> groupByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M {
        return values.groupByTo(destination, keySelector, valueTransform)
    }

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] of the bag.
     */
    override inline fun <R> map(transform: (Entity) -> R): List<R> {
        return values.map(transform)
    }

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] and its index of the bag.
     */
    override inline fun <R> mapIndexed(transform: (index: Int, entity: Entity) -> R): List<R> {
        return values.mapIndexed(transform)
    }

    /**
     * Applies the given [transform] function to each [entity][Entity] of the bag and appends
     * the results to the given [destination].
     */
    override inline fun <R, C : MutableCollection<in R>> mapTo(
        destination: C,
        transform: (Entity) -> R
    ): C {
        for (i in 0 until size) {
            destination += transform(values[i])
        }
        return destination
    }

    /**
     * Applies the given [transform] function to each [entity][Entity] and its index of the bag and appends
     * the results to the given [destination].
     */
    override inline fun <R, C : MutableCollection<in R>> mapIndexedTo(
        destination: C,
        transform: (index: Int, Entity) -> R
    ): C {
        for (i in 0 until size) {
            destination += transform(i, values[i])
        }
        return destination
    }

    /**
     * Returns a list containing only the non-null results of applying the given [transform] function
     * to each [entity][Entity] of the bag.
     */
    override inline fun <R> mapNotNull(transform: (Entity) -> R?): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            val transformVal = transform(values[i]) ?: continue
            result += transformVal
        }
        return result
    }

    /**
     * Applies the given [transform] function to each [entity][Entity] of the bag and appends only
     * the non-null results to the given [destination].
     */
    override inline fun <R, C : MutableCollection<in R>> mapNotNullTo(
        destination: C,
        transform: (Entity) -> R?
    ): C {
        for (i in 0 until size) {
            val transformVal = transform(values[i]) ?: continue
            destination += transformVal
        }
        return destination
    }

    /**
     * Splits the original bag into a pair of bags,
     * where the first bag contains elements for which predicate yielded true,
     * while the second bag contains elements for which predicate yielded false.
     */
    override inline fun partition(predicate: (Entity) -> Boolean): Pair<EntityBag, EntityBag> {
        val first = ArrayMutableEntityBag()
        val second = ArrayMutableEntityBag()
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
     * Returns a random [entity][Entity] of the bag.
     *
     * @throws [NoSuchElementException] if the bag is empty.
     */
    override fun random(): Entity {
        return values.random()
    }

    /**
     * Returns a random [entity][Entity] of the bag, or null if the bag is empty.
     */
    override fun randomOrNull(): Entity? {
        return values.randomOrNull()
    }

    /**
     * Returns a [List] containing the first [n] [entities][Entity].
     */
    override fun take(n: Int): EntityBag {
        val result = ArrayMutableEntityBag(max(min(n, size), 0))
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
        return values.hashCode()
    }
}
