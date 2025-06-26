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

interface EntityBag {
    /**
     * Returns the size of the [EntityBag].
     */
    val size: Int

    /**
     * Returns true if and only if the given [entity] is part of the bag.
     */
    operator fun contains(entity: Entity): Boolean

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    fun containsAll(entities: Collection<Entity>): Boolean

    /**
     * Returns true if and only if all given [entities] are part of the bag.
     */
    fun containsAll(entities: EntityBag): Boolean

    /**
     * Returns true if and only if the bag is empty and contains no [entities][Entity].
     */
    fun isEmpty(): Boolean

    /**
     * Returns true if and only if the bag is not empty and contains at least one [entity][Entity].
     */
    fun isNotEmpty(): Boolean

    /**
     * Returns true if all [entities][Entity] of the bag match the given [predicate].
     */
    fun all(predicate: (Entity) -> Boolean): Boolean

    /**
     * Returns true if at least one [entity][Entity] of the bag matches the given [predicate].
     */
    fun any(predicate: (Entity) -> Boolean): Boolean

    /**
     * Returns true if no [entity][Entity] of the bag matches the given [predicate].
     */
    fun none(predicate: (Entity) -> Boolean): Boolean

    /**
     * Returns a [Map] containing key-value pairs provided by the [transform] function applied to
     * each [entity][Entity] of the bag.
     */
    fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V>

    /**
     * Returns a [Map] containing the [entities][Entity] of the bag indexed by the key
     * returned from [keySelector] function applied to each [entity][Entity] of the bag.
     */
    fun <K> associateBy(keySelector: (Entity) -> K): Map<K, Entity>

    /**
     * Returns a [Map] containing the values provided by [valueTransform] and indexed by the
     * [keySelector] function applied to each [entity][Entity] of the bag.
     */
    fun <K, V> associateBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, V>

    /**
     * Populates and returns the [destination] mutable map containing key-value pairs
     * provided by the [transform] function applied to each [entity][Entity] of the bag.
     */
    fun <K, V, M : MutableMap<in K, in V>> associateTo(
        destination: M,
        transform: (Entity) -> Pair<K, V>
    ): M

    /**
     * Populates and returns the [destination] mutable map containing the [entities][Entity]
     * of the bag indexed by the key returned from [keySelector] function applied to
     * each [entity][Entity] of the bag.
     */
    fun <K, M : MutableMap<in K, Entity>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M

    /**
     * Populates and returns the [destination] mutable map containing the values
     * provided by [valueTransform] and indexed by the [keySelector] function applied
     * to each [entity][Entity] of the bag.
     */
    fun <K, V, M : MutableMap<in K, in V>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M

    /**
     * Returns the number of [entities][Entity] in this bag.
     */
    fun count(): Int

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    fun count(predicate: (Entity) -> Boolean): Int

    /**
     * Returns the index of the first [Entity] matching the given [predicate],
     * or -1 if the bag does not contain such an [Entity].
     */
    fun indexOfFirst(predicate: (Entity) -> Boolean): Int

    /**
     * Returns the index of the last [Entity] matching the given [predicate],
     * or -1 if the bag does not contain such an [Entity].
     */
    fun indexOfLast(predicate: (Entity) -> Boolean): Int

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
     * Returns the first [entity][Entity] matching the given [predicate], or null if no such
     * [entity][Entity] was found.
     */
    fun find(predicate: (Entity) -> Boolean): Entity?

    /**
     * Returns the first [entity][Entity].
     *
     * @throws [NoSuchElementException] if the bag is empty.
     */
    fun first(): Entity

    /**
     * Returns the first [entity][Entity] matching the given [predicate].
     *
     * @throws [NoSuchElementException] if the bag is empty or there is no such [entity][Entity].
     */
    fun first(predicate: (Entity) -> Boolean): Entity

    /**
     * Returns the first [entity][Entity], or null if the bag is empty.
     */
    fun firstOrNull(): Entity?

    /**
     * Returns the first [entity][Entity] matching the given [predicate], or null
     * if the bag is empty or no such [entity][Entity] was found.
     */
    fun firstOrNull(predicate: (Entity) -> Boolean): Entity?

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun <R> flatMap(transform: (Entity) -> Iterable<R>): List<R>

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the bag.
     */
    fun <R> flatMapSequence(transform: (Entity) -> Sequence<R>): List<R>

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
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity].
     */
    fun <R> fold(
        initial: R,
        operation: (acc: R, Entity) -> R
    ): R

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity] with its index in the original bag.
     */
    fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, Entity) -> R
    ): R

    /**
     * Performs the given [action] on each [entity][Entity].
     */
    fun forEach(action: (Entity) -> Unit)

    /**
     * Performs the given [action] on each [entity][Entity].
     */
    suspend fun suspendForEach(action: suspend (Entity) -> Unit)

    /**
     * Performs the given [action] on each [entity][Entity], providing sequential
     * index with the [entity][Entity].
     */
    fun forEachIndexed(action: (index: Int, Entity) -> Unit)

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
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the bag
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and returns
     * a map where each group key is associated with a list of corresponding values.
     */
    fun <K, V> groupBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, List<V>>

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
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the bag
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and puts
     * to the [destination] map each group key associated with a list of corresponding values.
     */
    fun <K, V, M : MutableMap<in K, MutableList<V>>> groupByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] of the bag.
     */
    fun <R> map(transform: (Entity) -> R): List<R>

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] and its index of the bag.
     */
    fun <R> mapIndexed(transform: (index: Int, Entity) -> R): List<R>

    /**
     * Applies the given [transform] function to each [entity][Entity] of the bag and appends
     * the results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapTo(
        destination: C,
        transform: (Entity) -> R
    ): C

    /**
     * Applies the given [transform] function to each [entity][Entity] and its index of the bag and appends
     * the results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapIndexedTo(
        destination: C,
        transform: (index: Int, Entity) -> R
    ): C

    /**
     * Returns a list containing only the non-null results of applying the given [transform] function
     * to each [entity][Entity] of the bag.
     */
    fun <R> mapNotNull(transform: (Entity) -> R?): List<R>

    /**
     * Applies the given [transform] function to each [entity][Entity] of the bag and appends only
     * the non-null results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapNotNullTo(
        destination: C,
        transform: (Entity) -> R?
    ): C

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
     * Returns a random [entity][Entity] of the bag.
     *
     * @throws [NoSuchElementException] if the bag is empty.
     */
    fun random(): Entity

    /**
     * Returns a random [entity][Entity] of the bag, or null if the bag is empty.
     */
    fun randomOrNull(): Entity?

    /**
     * Returns the single [entity][Entity] of the bag, or throws an exception
     * if the bag is empty or has more than one [entity][Entity].
     */
    fun single(): Entity

    /**
     * Returns the single [entity][Entity] of the bag matching the given [predicate],
     * or throws an exception if the bag is empty or has more than one [entity][Entity].
     */
    fun single(predicate: (Entity) -> Boolean): Entity

    /**
     * Returns single [entity][Entity] of the bag, or null
     * if the bag is empty or has more than one [entity][Entity].
     */
    fun singleOrNull(): Entity?

    /**
     * Returns the single [entity][Entity] of the bag matching the given [predicate],
     * or null if the bag is empty or has more than one [entity][Entity].
     */
    fun singleOrNull(predicate: (Entity) -> Boolean): Entity?

    /**
     * Returns a [List] containing the first [n][] [entities][Entity].
     */
    fun take(n: Int): EntityBag
}
