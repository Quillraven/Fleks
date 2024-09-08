package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.math.max
import kotlin.math.min

/**
 * Returns a [List] containing only [entities][Entity] matching the given [predicate].
 */
public inline fun EntityBag.filter(predicate: (Entity) -> Boolean): EntityBag {
    val result = ArrayEntityBag((size * 0.25f).toInt())
    for (i in 0 until size) {
        val entity = this[i]
        if (predicate(entity)) {
            result += entity
        }
    }
    return result
}

/**
 * Returns a [List] containing all [entities][Entity] not matching the given [predicate].
 */
public inline fun EntityBag.filterNot(predicate: (Entity) -> Boolean): EntityBag {
    val result = ArrayEntityBag((size * 0.25f).toInt())
    for (i in 0 until size) {
        val entity = this[i]
        if (!predicate(entity)) {
            result += entity
        }
    }
    return result
}

/**
 * Returns a [List] containing only [entities][Entity] matching the given [predicate].
 */
public inline fun EntityBag.filterIndexed(predicate: (index: Int, entity: Entity) -> Boolean): EntityBag {
    val result = ArrayEntityBag((size * 0.25f).toInt())
    for (i in 0 until size) {
        val entity = this[i]
        if (predicate(i, entity)) {
            result += entity
        }
    }
    return result
}

/**
 * Returns a single [List] of all elements yielded from the results of [transform] function
 * being invoked on each [entity][Entity] of the bag.
 */
public inline fun <R> EntityBag.flatMapSequence(transform: (Entity) -> Sequence<R>): List<R> {
    val result = mutableListOf<R>()
    for (i in 0 until size) {
        result.addAll(transform(this[i]))
    }
    return result
}

/**
 * Returns a new bag of all elements yielded from the results of [transform] function
 * being invoked on each [entity][Entity] of the bag.
 */
public inline fun EntityBag.flatMapBag(transform: (Entity) -> EntityBag): EntityBag {
    val result = ArrayEntityBag(size)
    for (i in 0 until size) {
        transform(this[i]).forEach { result += it }
    }
    return result
}

/**
 * Returns a single [List] of all non-null elements yielded from the results of [transform] function
 * being invoked on each [entity][Entity] of the bag.
 */
public inline fun <R> EntityBag.flatMapNotNull(transform: (Entity) -> Iterable<R?>?): List<R> {
    val result = mutableListOf<R>()
    for (i in 0 until size) {
        transform(this[i])?.forEach {
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
public inline fun <R> EntityBag.flatMapSequenceNotNull(transform: (Entity) -> Sequence<R?>?): List<R> {
    val result = mutableListOf<R>()
    for (i in 0 until size) {
        transform(this[i])?.forEach {
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
public inline fun EntityBag.flatMapBagNotNull(transform: (Entity) -> EntityBag?): EntityBag {
    val result = ArrayEntityBag(size)
    for (i in 0 until size) {
        val transformVal = transform(this[i]) ?: continue
        transformVal.forEach { result += it }
    }
    return result
}

/**
 * Groups [entities][Entity] by the key returned by the given [keySelector] function
 * applied to each [entity][Entity] and puts to the [destination] map each group key associated with
 * an [EntityBag] of corresponding elements.
 */
public inline fun <K, M : MutableMap<in K, MutableEntityBag>> EntityBag.groupByTo(
    destination: M,
    keySelector: (Entity) -> K
): M {
    for (i in 0 until size) {
        val entity = this[i]
        val key = keySelector(entity)
        destination.getOrPut(key) { ArrayEntityBag() } += entity
    }
    return destination
}

/**
 * Groups [entities][Entity] by the key returned by the given [keySelector] function
 * applied to each [entity][Entity] and returns a map where each group key is associated with an [EntityBag]
 * of corresponding [entities][Entity].
 */
public inline fun <K> EntityBag.groupBy(keySelector: (Entity) -> K): Map<K, MutableEntityBag> {
    val result = mutableMapOf<K, MutableEntityBag>()
    for (i in 0 until size) {
        val entity = this[i]
        val key = keySelector(entity)
        result.getOrPut(key) { ArrayEntityBag() } += entity
    }
    return result
}

/**
 * Splits the original bag into a pair of bags,
 * where the first bag contains elements for which predicate yielded true,
 * while the second bag contains elements for which predicate yielded false.
 */
public inline fun EntityBag.partition(predicate: (Entity) -> Boolean): Pair<EntityBag, EntityBag> {
    val first = ArrayEntityBag()
    val second = ArrayEntityBag()
    for (i in 0 until size) {
        val entity = this[i]
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
public inline fun EntityBag.partitionTo(
    first: MutableEntityBag,
    second: MutableEntityBag,
    predicate: (Entity) -> Boolean
) {
    first.clear()
    second.clear()
    for (i in 0 until size) {
        val entity = this[i]
        if (predicate(entity)) {
            first += entity
        } else {
            second += entity
        }
    }
}

/**
 * Returns a [List] containing the first [n] [entities][Entity].
 */
public inline fun EntityBag.take(n: Int): EntityBag {
    val result = ArrayEntityBag(max(min(n, size), 0))
    for (i in 0 until min(n, size)) {
        result += this[i]
    }
    return result
}
