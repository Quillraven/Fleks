package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.EntityBag
import com.github.quillraven.fleks.collection.EntityBagIterator
import com.github.quillraven.fleks.collection.EntityComparator
import com.github.quillraven.fleks.collection.MutableEntityBag
import com.github.quillraven.fleks.collection.SparseEntityBag
import com.github.quillraven.fleks.collection.isNullOrEmpty

/**
 * Type alias for an optional hook function for a [Family].
 * Such a function runs within a [World] and takes the [Entity] as an argument.
 */
typealias FamilyHook = World.(Entity) -> Unit

/**
 * A class to define the configuration of a [Family]. A [family][Family] contains of three parts:
 *
 * - **allOf**: an [entity][Entity] must have all specified [components][Component] to be part of the [family][Family].
 * - **noneOf**: an [entity][Entity] must not have any of the specified [components][Component] to be part of the [family][Family].
 * - **anyOf**: an [entity][Entity] must have at least one of the specified [components][Component] to be part of the [family][Family].
 *
 * It is not mandatory to specify all three parts, but **at least one** part must be provided.
 */
data class FamilyDefinition(
    internal var allOf: BitArray? = null,
    internal var noneOf: BitArray? = null,
    internal var anyOf: BitArray? = null,
) {

    /**
     * Any [entity][Entity] must have all given [types] to be part of the [family][Family].
     */
    fun all(vararg types: UniqueId<*>): FamilyDefinition {
        allOf = BitArray(types.size).also { bits ->
            types.forEach { bits.set(it.id) }
        }
        return this
    }

    /**
     * Any [entity][Entity] must not have any of the given [types] to be part of the [family][Family].
     */
    fun none(vararg types: UniqueId<*>): FamilyDefinition {
        noneOf = BitArray(types.size).also { bits ->
            types.forEach { bits.set(it.id) }
        }
        return this
    }

    /**
     * Any [entity][Entity] must have at least one of the given [types] to be part of the [family][Family].
     */
    fun any(vararg types: UniqueId<*>): FamilyDefinition {
        anyOf = BitArray(types.size).also { bits ->
            types.forEach { bits.set(it.id) }
        }
        return this
    }

    /**
     * Returns true if and only if [allOf], [noneOf] and [anyOf] are either null or empty.
     */
    internal fun isEmpty(): Boolean {
        return allOf.isNullOrEmpty() && noneOf.isNullOrEmpty() && anyOf.isNullOrEmpty()
    }
}

/**
 * A family of [entities][Entity]. It stores [entities][Entity] that have a specific configuration of components.
 * A configuration is defined via the a [FamilyDefinition].
 * Each [component][Component] is assigned to a unique index via its [ComponentType].
 * That index is set in the [allOf], [noneOf] or [anyOf][] [BitArray].
 *
 * A family gets notified when an [entity][Entity] is added, updated, or removed of the [world][World].
 *
 * Every [IteratingSystem] is linked to exactly one family, but a family can also exist outside of systems.
 * It gets created via the [World.family] function.
 */
data class Family(
    internal val allOf: BitArray? = null,
    internal val noneOf: BitArray? = null,
    internal val anyOf: BitArray? = null,
    private val world: World,
    @PublishedApi
    internal val entityService: EntityService = world.entityService,
) : EntityComponentContext(world.componentService) {
    /**
     * An optional [FamilyHook] that gets called whenever an [entity][Entity] enters the family.
     */
    internal var addHook: FamilyHook? = null

    /**
     * An optional [FamilyHook] that gets called whenever an [entity][Entity] leaves the family.
     */
    internal var removeHook: FamilyHook? = null

    /**
     * The [entities][Entity] that belong to this family. It is a sparse set that provides
     * O(1) membership tests via its sparse array and O(N) iterations over its densely
     * packed dense array. Therefore, it is always up to date and does not need any lazy
     * update mechanism.
     */
    @PublishedApi
    internal val activeEntities = SparseEntityBag(world.capacity)

    /**
     * Returns true if an iteration of this family is currently in process.
     */
    @PublishedApi
    internal var isIterating = false

    /**
     * Snapshot of [activeEntities] that is used for iterations to guarantee that a running
     * iteration is not affected by any structural change of the family. The snapshot is
     * only refreshed whenever the family changed since the last iteration.
     */
    @PublishedApi
    internal val snapshotEntities = MutableEntityBag()

    /**
     * The [version][SparseEntityBag.version] of [activeEntities] that the [snapshotEntities]
     * is based on.
     */
    @PublishedApi
    internal var lastIterationVersion = -1

    /**
     * Returns the [entities][Entity] that belong to this family.
     * Be aware that the returned bag is the snapshot used for the current iteration and is not
     * updated while a family iteration is in progress. When no iteration is in progress, then
     * the returned bag is always up to date.
     *
     * It is typed as a [MutableEntityBag] so that the various inline delegation functions of
     * this family can keep their inline lambda parameters.
     */
    @PublishedApi
    internal val iterationEntities: MutableEntityBag
        get() = if (isIterating) snapshotEntities else activeEntities.dense

    /**
     * Returns the [entities][Entity] that belong to this family.
     * Be aware that the returned [EntityBag] is the snapshot used for the current iteration
     * and is not updated while a family iteration is in progress. When no iteration is in
     * progress, then the returned [EntityBag] is always up to date.
     */
    val entities: EntityBag
        get() = iterationEntities

    /**
     * Returns the number of [entities][Entity] that belong to this family.
     */
    val numEntities: Int
        get() = activeEntities.size

    /**
     * Returns true if and only if this [Family] does not contain any entity.
     */
    val isEmpty: Boolean
        get() = activeEntities.size == 0

    /**
     * Returns true if and only if this [Family] contains at least one entity.
     */
    val isNotEmpty: Boolean
        get() = activeEntities.size > 0

    /**
     * Returns true if the specified [compMask] matches the family's component configuration.
     *
     * @param compMask the component configuration of an [entity][Entity].
     */
    internal operator fun contains(compMask: BitArray): Boolean {
        return (allOf == null || compMask.contains(allOf))
            && (noneOf == null || !compMask.intersects(noneOf))
            && (anyOf == null || compMask.intersects(anyOf))
    }

    /**
     * Returns true if and only if the given [entity] is part of the family.
     */
    operator fun contains(entity: Entity): Boolean = entity in activeEntities

    /**
     * Returns true if and only if all given [entities] are part of the family.
     */
    fun containsAll(entities: Collection<Entity>): Boolean = this.iterationEntities.containsAll(entities)

    /**
     * Returns true if and only if all given [entities] are part of the family.
     */
    fun containsAll(entities: EntityBag): Boolean = this.iterationEntities.containsAll(entities)

    /**
     * Returns true if and only if all entities of the given [family] are part of this family.
     */
    fun containsAll(family: Family): Boolean = iterationEntities.containsAll(family.entities)

    /**
     * Updates this family if needed and runs the given [action] for all [entities][Entity].
     *
     * **Important note**: There is a potential risk when iterating over entities, and one of those entities
     * gets removed. Removing the entity immediately and cleaning up its components could
     * cause problems because if you access a component which is mandatory for the family, you will get
     * a [FleksNoSuchEntityComponentException]. To avoid that, you could check if an entity really has the component
     * before accessing it, but that is redundant in the context of a family.
     *
     * To avoid these kinds of issues, entity removals are delayed until the end of the iteration. This also means
     * that a removed entity of this family will still be part of the [action] for the current iteration.
     */
    inline fun forEach(crossinline action: Family.(Entity) -> Unit) {
        // Refresh the iteration snapshot only if the family changed since the last iteration.
        // During an iteration (e.g. a nested iteration of this family) the snapshot is reused
        // to guarantee a stable iteration. Check the 'snapshotEntities' documentation for more details.
        if (!isIterating && lastIterationVersion != activeEntities.version) {
            lastIterationVersion = activeEntities.version
            snapshotEntities.clearEnsuringCapacity(activeEntities.size)
            activeEntities.forEach { snapshotEntities += it }
        }

        val entitiesForIteration = snapshotEntities

        if (!entityService.delayRemoval) {
            entityService.delayRemoval = true
            isIterating = true
            entitiesForIteration.forEach { action(it) }
            isIterating = false
            entityService.cleanupDelays()
        } else {
            val origIterating = isIterating
            isIterating = true
            entitiesForIteration.forEach { this.action(it) }
            isIterating = origIterating
        }
    }

    /**
     * Updates this family if needed and returns its first [Entity].
     * @throws [NoSuchElementException] if the family has no entities.
     */
    fun first(): Entity = iterationEntities.first()

    /**
     * Updates this family if needed and returns its first [Entity] matching the given [predicate].
     * @throws [NoSuchElementException] if the family has no such entity.
     */
    fun first(predicate: (Entity) -> Boolean): Entity = iterationEntities.first(predicate)

    /**
     * Updates this family if needed and returns its first [Entity] or null if the family has no entities.
     */
    fun firstOrNull(): Entity? = iterationEntities.firstOrNull()

    /**
     * Updates this family if needed and returns its first [Entity] matching the given [predicate],
     * or null if the family has no such entity.
     */
    fun firstOrNull(predicate: (Entity) -> Boolean): Entity? = iterationEntities.firstOrNull(predicate)

    /**
     * Updates this family if needed and returns the first non-null value produced by [transform]
     * applied to each [Entity].
     * @throws [NoSuchElementException] if no non-null value was produced.
     */
    fun <R : Any> firstNotNullOf(transform: (Entity) -> R?): R = iterationEntities.firstNotNullOf(transform)

    /**
     * Updates this family if needed and returns the first non-null value produced by [transform]
     * applied to each [Entity], or null if no non-null value was produced.
     */
    fun <R : Any> firstNotNullOfOrNull(transform: (Entity) -> R?): R? =
        iterationEntities.firstNotNullOfOrNull(transform)

    /**
     * Sorts the [entities][Entity] of this family by the given [comparator].
     */
    fun sort(comparator: EntityComparator) = activeEntities.sort(comparator)

    /**
     * Returns true if all [entities][Entity] of the family match the given [predicate].
     */
    fun all(predicate: (Entity) -> Boolean): Boolean = iterationEntities.all(predicate)

    /**
     * Returns true if at least one [entity][Entity] of the family matches the given [predicate].
     */
    fun any(predicate: (Entity) -> Boolean): Boolean = iterationEntities.any(predicate)

    /**
     * Returns true if no [entity][Entity] of the family matches the given [predicate].
     */
    fun none(predicate: (Entity) -> Boolean): Boolean = iterationEntities.none(predicate)

    /**
     * Returns the number of [entities][Entity] matching the given [predicate].
     */
    fun count(predicate: (Entity) -> Boolean): Int = iterationEntities.count(predicate)

    /**
     * Returns the index of the first [Entity] matching the given [predicate],
     * or -1 if the family does not contain such an [Entity].
     */
    fun indexOfFirst(predicate: (Entity) -> Boolean): Int = iterationEntities.indexOfFirst(predicate)

    /**
     * Returns the index of the last [Entity] matching the given [predicate],
     * or -1 if the family does not contain such an [Entity].
     */
    fun indexOfLast(predicate: (Entity) -> Boolean): Int = iterationEntities.indexOfLast(predicate)

    /**
     * Creates an [EntityBagIterator] for the family. If the family gets updated
     * during iteration then [EntityBagIterator.reset] must be called to guarantee correct iterator behavior.
     */
    fun iterator(): EntityBagIterator = EntityBagIterator(entities)

    /**
     * Returns a [Map] containing key-value pairs provided by the [transform] function applied to
     * each [entity][Entity] of the family.
     */
    inline fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V> = iterationEntities.associate(transform)

    /**
     * Returns a [Map] containing the [entities][Entity] of the family indexed by the key
     * returned from [keySelector] function applied to each [entity][Entity] of the family.
     */
    inline fun <K> associateBy(keySelector: (Entity) -> K): Map<K, Entity> = iterationEntities.associateBy(keySelector)

    /**
     * Returns a [Map] containing the values provided by [valueTransform] and indexed by the
     * [keySelector] function applied to each [entity][Entity] of the family.
     */
    inline fun <K, V> associateBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, V> = iterationEntities.associateBy(keySelector, valueTransform)

    /**
     * Populates and returns the [destination] mutable map containing key-value pairs
     * provided by the [transform] function applied to each [entity][Entity] of the family.
     */
    inline fun <K, V, M : MutableMap<in K, in V>> associateTo(
        destination: M,
        transform: (Entity) -> Pair<K, V>
    ): M = iterationEntities.associateTo(destination, transform)

    /**
     * Populates and returns the [destination] mutable map containing the [entities][Entity]
     * of the family indexed by the key returned from [keySelector] function applied to
     * each [entity][Entity] of the family.
     */
    inline fun <K, M : MutableMap<in K, Entity>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M = iterationEntities.associateByTo(destination, keySelector)

    /**
     * Populates and returns the [destination] mutable map containing the values
     * provided by [valueTransform] and indexed by the [keySelector] function applied
     * to each [entity][Entity] of the family.
     */
    inline fun <K, V, M : MutableMap<in K, in V>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M = iterationEntities.associateByTo(destination, keySelector, valueTransform)

    /**
     * Returns an [EntityBag] containing only [entities][Entity] matching the given [predicate].
     */
    fun filter(predicate: (Entity) -> Boolean): EntityBag = iterationEntities.filter(predicate)

    /**
     * Returns an [EntityBag] containing all [entities][Entity] not matching the given [predicate].
     */
    fun filterNot(predicate: (Entity) -> Boolean): EntityBag = iterationEntities.filterNot(predicate)

    /**
     * Returns an [EntityBag] containing only [entities][Entity] matching the given [predicate].
     */
    fun filterIndexed(predicate: (index: Int, Entity) -> Boolean): EntityBag =
        iterationEntities.filterIndexed(predicate)

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    fun filterTo(destination: MutableEntityBag, predicate: (Entity) -> Boolean): MutableEntityBag =
        iterationEntities.filterTo(destination, predicate)

    /**
     * Appends all [entities][Entity] not matching the given [predicate] to the given [destination].
     */
    fun filterNotTo(destination: MutableEntityBag, predicate: (Entity) -> Boolean): MutableEntityBag =
        iterationEntities.filterNotTo(destination, predicate)

    /**
     * Appends all [entities][Entity] matching the given [predicate] to the given [destination].
     */
    fun filterIndexedTo(
        destination: MutableEntityBag,
        predicate: (index: Int, Entity) -> Boolean
    ): MutableEntityBag = iterationEntities.filterIndexedTo(destination, predicate)

    /**
     * Returns the first [entity][Entity] matching the given [predicate], or null if no such
     * [entity][Entity] was found.
     */
    fun find(predicate: (Entity) -> Boolean): Entity? = iterationEntities.find(predicate)

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun <R> flatMap(transform: (Entity) -> Iterable<R>) = iterationEntities.flatMap(transform)

    /**
     * Returns a single [List] of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun <R> flatMapSequence(transform: (Entity) -> Sequence<R>) = iterationEntities.flatMapSequence(transform)

    /**
     * Returns a new bag of all elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun flatMapBag(transform: (Entity) -> EntityBag) = iterationEntities.flatMapBag(transform)

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun <R> flatMapNotNull(transform: (Entity) -> Iterable<R?>?) = iterationEntities.flatMapNotNull(transform)

    /**
     * Returns a single [List] of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun <R> flatMapSequenceNotNull(transform: (Entity) -> Sequence<R?>?) =
        iterationEntities.flatMapSequenceNotNull(transform)

    /**
     * Returns a new bag of all non-null elements yielded from the results of [transform] function
     * being invoked on each [entity][Entity] of the family.
     */
    inline fun flatMapBagNotNull(transform: (Entity) -> EntityBag?) = iterationEntities.flatMapBagNotNull(transform)

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity].
     */
    inline fun <R> fold(
        initial: R,
        operation: (acc: R, entity: Entity) -> R
    ): R = iterationEntities.fold(initial, operation)

    /**
     * Accumulates value starting with [initial] value and applying [operation] from left to right to
     * current accumulator value and each [entity][Entity] with its index in the original family.
     */
    inline fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, entity: Entity) -> R
    ): R = iterationEntities.foldIndexed(initial, operation)

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and returns a map where each group key is associated with an [EntityBag]
     * of corresponding [entities][Entity].
     */
    fun <K> groupBy(keySelector: (Entity) -> K): Map<K, MutableEntityBag> = iterationEntities.groupBy(keySelector)

    /**
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the family
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and returns
     * a map where each group key is associated with a list of corresponding values.
     */
    fun <K, V> groupBy(keySelector: (Entity) -> K, valueTransform: (Entity) -> V): Map<K, List<V>> =
        iterationEntities.groupBy(keySelector, valueTransform)

    /**
     * Groups [entities][Entity] by the key returned by the given [keySelector] function
     * applied to each [entity][Entity] and puts to the [destination] map each group key associated with
     * an [EntityBag] of corresponding elements.
     */
    fun <K, M : MutableMap<in K, MutableEntityBag>> groupByTo(destination: M, keySelector: (Entity) -> K): M =
        iterationEntities.groupByTo(destination, keySelector)

    /**
     * Groups values returned by the [valueTransform] function applied to each [entity][Entity] of the family
     * by the key returned by the given [keySelector] function applied to the [entity][Entity] and puts
     * to the [destination] map each group key associated with a list of corresponding values.
     */
    fun <K, V, M : MutableMap<in K, MutableList<V>>> groupByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M = iterationEntities.groupByTo(destination, keySelector, valueTransform)

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] of the family.
     */
    fun <R> map(transform: (Entity) -> R): List<R> = iterationEntities.map(transform)

    /**
     * Returns a [List] containing the results of applying the given [transform] function
     * to each [entity][Entity] and its index of the family.
     */
    fun <R> mapIndexed(transform: (index: Int, Entity) -> R): List<R> = iterationEntities.mapIndexed(transform)

    /**
     * Applies the given [transform] function to each [entity][Entity] of the family and appends
     * the results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapTo(destination: C, transform: (Entity) -> R): C =
        iterationEntities.mapTo(destination, transform)

    /**
     * Applies the given [transform] function to each [entity][Entity] and its index of the family and appends
     * the results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapIndexedTo(destination: C, transform: (index: Int, Entity) -> R): C =
        iterationEntities.mapIndexedTo(destination, transform)

    /**
     * Returns a list containing only the non-null results of applying the given [transform] function
     * to each [entity][Entity] of the family.
     */
    fun <R> mapNotNull(transform: (Entity) -> R?): List<R> = iterationEntities.mapNotNull(transform)

    /**
     * Applies the given [transform] function to each [entity][Entity] of the family and appends only
     * the non-null results to the given [destination].
     */
    fun <R, C : MutableCollection<in R>> mapNotNullTo(destination: C, transform: (Entity) -> R?): C =
        iterationEntities.mapNotNullTo(destination, transform)

    /**
     * Splits the original family into a pair of bags,
     * where the first bag contains elements for which predicate yielded true,
     * while the second bag contains elements for which predicate yielded false.
     */
    fun partition(predicate: (Entity) -> Boolean): Pair<EntityBag, EntityBag> = iterationEntities.partition(predicate)

    /**
     * Splits the original family into two bags,
     * where [first] contains elements for which predicate yielded true,
     * while [second] contains elements for which predicate yielded false.
     */
    fun partitionTo(first: MutableEntityBag, second: MutableEntityBag, predicate: (Entity) -> Boolean) =
        iterationEntities.partitionTo(first, second, predicate)

    /**
     * Returns a random [entity][Entity] of the family.
     *
     * @throws [NoSuchElementException] if the family is empty.
     */
    fun random(): Entity = iterationEntities.random()

    /**
     * Returns a random [entity][Entity] of the family, or null if the family is empty.
     */
    fun randomOrNull(): Entity? = iterationEntities.randomOrNull()

    /**
     * Returns the single [entity][Entity] of the family, or throws an exception
     * if the family is empty or has more than one [entity][Entity].
     */
    fun single(): Entity = iterationEntities.single()

    /**
     * Returns the single [entity][Entity] of the family matching the given [predicate],
     * or throws an exception if the family is empty or has more than one [entity][Entity].
     */
    fun single(predicate: (Entity) -> Boolean): Entity = iterationEntities.single(predicate)

    /**
     * Returns single [entity][Entity] of the family, or null
     * if the family is empty or has more than one [entity][Entity].
     */
    fun singleOrNull(): Entity? = iterationEntities.singleOrNull()

    /**
     * Returns the single [entity][Entity] of the family matching the given [predicate],
     * or null if the family is empty or has more than one [entity][Entity].
     */
    fun singleOrNull(predicate: (Entity) -> Boolean): Entity? = iterationEntities.singleOrNull(predicate)

    /**
     * Returns an [EntityBag] containing the first [n][] [entities][Entity].
     */
    fun take(n: Int): EntityBag = iterationEntities.take(n)

    /**
     * Adds the [entity] to the family if and only if the entity's [compMask] is matching
     * the family configuration.
     */
    @PublishedApi
    internal fun onEntityAdded(entity: Entity, compMask: BitArray) {
        if (compMask in this) {
            if (entity !in activeEntities) {
                activeEntities += entity
            }
            addHook?.invoke(world, entity)
        }
    }

    /**
     * Checks if the [entity] is part of the family by analyzing the entity's components.
     * The [compMask] is a [BitArray] that indicates which components the [entity] currently has.
     *
     * The [entity] gets either added to or removed of the [activeEntities] when needed.
     */
    @PublishedApi
    internal fun onEntityCfgChanged(entity: Entity, compMask: BitArray) {
        val entityInFamily = compMask in this
        if (entityInFamily && entity !in activeEntities) {
            // new entity gets added
            activeEntities += entity
            addHook?.invoke(world, entity)
        } else if (!entityInFamily && entity in activeEntities) {
            // existing entity gets removed
            activeEntities -= entity
            removeHook?.invoke(world, entity)
        }
    }

    /**
     * Removes the [entity] of the family if and only if the [entity] is already in the family.
     */
    internal fun onEntityRemoved(entity: Entity) {
        if (entity in activeEntities) {
            // existing entity gets removed
            activeEntities -= entity
            removeHook?.invoke(world, entity)
        }
    }

    override fun toString(): String {
        return "Family(allOf=$allOf, noneOf=$noneOf, anyOf=$anyOf, numEntities=$numEntities)"
    }
}
