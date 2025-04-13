package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

/**
 * Returns a new read-only [EntityBag] of given [entities].
 */
fun entityBagOf(vararg entities: Entity): EntityBag {
    return MutableEntityBag(entities.size).apply {
        entities.forEach { this += it }
    }
}

/**
 * Returns a new read-only empty [EntityBag].
 */
fun emptyEntityBag(): EntityBag = MutableEntityBag(0)

interface EntityBag : Collection<Entity> {
    /**
     * Returns the size of the [EntityBag].
     */
    override val size: Int

    /**
     * Returns true if and only if the bag is empty and contains no [entities][Entity].
     */
    override fun isEmpty(): Boolean

    /**
     * Returns the number of [entities][Entity] in this bag.
     */
    fun count(): Int

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    fun count(predicate: (Entity) -> Boolean): Int

    /**
     * Returns a [List] containing only [entities][Entity] matching the given [predicate].
     */
    fun filter(predicate: (Entity) -> Boolean): EntityBag

    /**
     * Returns a [List] containing all [entities][Entity] not matching the given [predicate].
     */
    fun filterNot(predicate: (Entity) -> Boolean): EntityBag

    /**
     * Returns a [List] containing only [entities][Entity] matching the given [predicate].
     */
    fun filterIndexed(predicate: (index: Int, Entity) -> Boolean): EntityBag

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    fun filterTo(
        destination: MutableEntityBag,
        predicate: (Entity) -> Boolean
    ): MutableEntityBag

    /**
     * Appends all [entities][Entity] not matching the given [predicate] to the given [destination].
     */
    fun filterNotTo(
        destination: MutableEntityBag,
        predicate: (Entity) -> Boolean
    ): MutableEntityBag

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    fun filterIndexedTo(
        destination: MutableEntityBag,
        predicate: (index: Int, Entity) -> Boolean
    ): MutableEntityBag

    /**
     * Returns a new bag of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun flatMapBag(transform: (Entity) -> EntityBag): EntityBag

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun <R> flatMapNotNull(transform: (Entity) -> Iterable<R?>?): List<R>

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun <R> flatMapSequenceNotNull(transform: (Entity) -> Sequence<R?>?): List<R>

    /**
     * Returns a new bag of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun flatMapBagNotNull(transform: (Entity) -> EntityBag?): EntityBag

    /**
     * Returns the [entity][Entity] of the given [index].
     *
     * @throws [IndexOutOfBoundsException] if [index] is less than zero or greater equal [size].
     */
    operator fun get(index: Int): Entity

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and returns a map where each group key is associated with an [EntityBag]
     * of corresponding [entities][Entity].
     */
    fun <K> groupBy(keySelector: (Entity) -> K): Map<K, MutableEntityBag>

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and puts to the [destination] map each group key associated with
     * an [EntityBag] of corresponding elements.
     */
    fun <K, M : MutableMap<in K, MutableEntityBag>> groupByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M

    /**
     * Splits the original bag into a pair of bags,
     * where the first bag contains elements for which predicate yielded true,
     * while the second bag contains elements for which predicate yielded false.
     */
    fun partition(predicate: (Entity) -> Boolean): Pair<EntityBag, EntityBag>

    /**
     * Splits the original bag into two bags,
     * where [first] contains elements for which predicate yielded true,
     * while [second] contains elements for which predicate yielded false.
     */
    fun partitionTo(
        first: MutableEntityBag,
        second: MutableEntityBag,
        predicate: (Entity) -> Boolean
    )

    /**
     * Returns a [List] containing the first [n][] [entities][Entity].
     */
    fun take(n: Int): EntityBag
}
