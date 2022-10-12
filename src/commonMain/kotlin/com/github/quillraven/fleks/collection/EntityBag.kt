package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * A bag implementation for [entities][Entity] (=integer) values in Kotlin to avoid autoboxing.
 * It contains only the necessary functions for Fleks.
 */
class EntityBag(
    size: Int = 64
) : EntityCollection {
    @PublishedApi
    internal var values: Array<Entity> = Array(size) { Entity(-1) }

    override var size: Int = 0
        private set

    val capacity: Int
        get() = values.size

    internal fun add(entity: Entity) {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity(-1) })
        }
        values[size++] = entity
    }

    internal fun unsafeAdd(entity: Entity) {
        if (size >= values.size) throw IndexOutOfBoundsException("Cannot add value because of insufficient size")
        values[size++] = entity
    }

    operator fun get(index: Int): Entity {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index]
    }

    internal fun clear() {
        values.fill(Entity(-1))
        size = 0
    }

    internal fun ensureCapacity(capacity: Int) {
        if (capacity >= values.size) {
            values = values.copyInto(Array(capacity + 1) { Entity(-1) })
        }
    }

    internal fun sort(comparator: EntityComparator) {
        values.quickSort(0, size, comparator)
    }

    override fun contains(entity: Entity): Boolean {
        for (i in 0 until size) {
            if (values[i] == entity) {
                return true
            }
        }
        return false
    }

    override fun containsAll(entities: Collection<Entity>): Boolean {
        for (i in 0 until size) {
            if (values[i] !in entities) {
                return false
            }
        }
        return true
    }

    override fun isEmpty(): Boolean = size == 0

    override fun isNotEmpty(): Boolean = size > 0

    override fun all(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (!predicate(values[i])) {
                return false
            }
        }
        return true
    }

    override fun any(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (predicate(values[i])) {
                return true
            }
        }
        return false
    }

    override fun none(predicate: (Entity) -> Boolean): Boolean {
        for (i in 0 until size) {
            if (predicate(values[i])) {
                return false
            }
        }
        return true
    }

    override fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for (i in 0 until size) {
            result += transform(values[i])
        }
        return result
    }

    override fun <K> associateBy(keySelector: (Entity) -> K): Map<K, Entity> {
        val result = mutableMapOf<K, Entity>()
        for (i in 0 until size) {
            val entity = values[i]
            result[keySelector(entity)] = entity
        }
        return result
    }

    override fun <K, V> associateBy(keySelector: (Entity) -> K, valueTransform: (Entity) -> V): Map<K, V> {
        val result = mutableMapOf<K, V>()
        for (i in 0 until size) {
            val entity = values[i]
            result[keySelector(entity)] = valueTransform(entity)
        }
        return result
    }

    override fun <K, V, M : MutableMap<in K, in V>> associateTo(destination: M, transform: (Entity) -> Pair<K, V>): M {
        destination.clear()
        for (i in 0 until size) {
            destination += transform(values[i])
        }
        return destination
    }

    override fun <K, M : MutableMap<in K, Entity>> associateByTo(destination: M, keySelector: (Entity) -> K): M {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            destination[keySelector(entity)] = entity
        }
        return destination
    }

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

    override fun count(): Int = size

    override fun count(predicate: (Entity) -> Boolean): Int {
        var result = 0
        for (i in 0 until size) {
            if (predicate(values[i])) {
                ++result
            }
        }
        return result
    }

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

    override fun <C : MutableCollection<Entity>> filterTo(destination: C, predicate: (Entity) -> Boolean): C {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                destination += entity
            }
        }
        return destination
    }

    override fun <C : MutableCollection<Entity>> filterNotTo(destination: C, predicate: (Entity) -> Boolean): C {
        destination.clear()
        for (i in 0 until size) {
            val entity = values[i]
            if (!predicate(entity)) {
                destination += entity
            }
        }
        return destination
    }

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

    override fun find(predicate: (Entity) -> Boolean): Entity? {
        for (i in 0 until size) {
            val entity = values[i]
            if (predicate(entity)) {
                return entity
            }
        }
        return null
    }

    override fun first(): Entity {
        if (isEmpty()) {
            throw NoSuchElementException("EntityBag is empty!")
        }
        return values[0]
    }

    override fun first(predicate: (Entity) -> Boolean): Entity {
        return find(predicate) ?: throw NoSuchElementException("There is no entity matching the given predicate!")
    }

    override fun firstOrNull(): Entity? {
        if (isEmpty()) {
            return null
        }
        return values[0]
    }

    override fun firstOrNull(predicate: (Entity) -> Boolean): Entity? = find(predicate)

    override fun <R> fold(initial: R, operation: (acc: R, Entity) -> R): R {
        var accumulator = initial
        for (i in 0 until size) {
            accumulator = operation(accumulator, values[i])
        }
        return accumulator
    }

    override fun <R> foldIndexed(initial: R, operation: (index: Int, acc: R, Entity) -> R): R {
        var accumulator = initial
        for (i in 0 until size) {
            accumulator = operation(i, accumulator, values[i])
        }
        return accumulator
    }

    override fun forEach(action: (Entity) -> Unit) {
        for (i in 0 until size) {
            action(values[i])
        }
    }

    override fun forEachIndexed(action: (index: Int, Entity) -> Unit) {
        for (i in 0 until size) {
            action(i, values[i])
        }
    }

    override fun <R> map(transform: (Entity) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            result += transform(values[i])
        }
        return result
    }

    override fun <R> mapIndexed(transform: (index: Int, Entity) -> R): List<R> {
        val result = mutableListOf<R>()
        for (i in 0 until size) {
            result += transform(i, values[i])
        }
        return result
    }

    override fun <R, C : MutableCollection<in R>> mapTo(destination: C, transform: (Entity) -> R): C {
        destination.clear()
        for (i in 0 until size) {
            destination += transform(values[i])
        }
        return destination
    }

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

    override fun random(): Entity {
        if (isEmpty()) {
            throw NoSuchElementException("EntityBag is empty!")
        }
        return values[Random.Default.nextInt(size)]
    }

    override fun randomOrNull(): Entity? {
        if (isEmpty()) {
            return null
        }
        return values[Random.Default.nextInt(size)]
    }

    override fun take(n: Int): List<Entity> {
        val result = mutableListOf<Entity>()
        for (i in 0 until min(n, size)) {
            result += values[i]
        }
        return result
    }
}
