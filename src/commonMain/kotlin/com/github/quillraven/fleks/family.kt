package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.*

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
 * It is not mandatory to specify all three parts but **at least one** part must be provided.
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
 * A family gets notified when an [entity][Entity] is added, updated or removed of the [world][World].
 *
 * Every [IteratingSystem] is linked to exactly one family but a family can also exist outside of systems.
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
     * Returns the [entities][Entity] that belong to this family.
     */
    private val activeEntities = bag<Entity>(world.capacity)
    private var countEntities = 0

    /**
     * Returns true if an iteration of this family is currently in process.
     */
    @PublishedApi
    internal var isIterating = false

    // This bag is added for better iteration performance.
    @PublishedApi
    internal val mutableEntities = ArrayEntityBag()
        get() {
            if (isDirty && !isIterating) {
                // no iteration in process -> update entities if necessary
                isDirty = false
                field.clearEnsuringCapacity(activeEntities.size)
                activeEntities.forEach { field += it }
            }
            return field
        }

    /**
     * Returns the [entities][Entity] that belong to this family.
     * Be aware that the underlying [EntityBag] collection is not always up to date.
     * The collection is not updated while a family iteration is in progress. It
     * gets automatically updated whenever it is accessed and no iteration is currently
     * in progress.
     */
    val entities: EntityBag
        get() = mutableEntities

    /**
     * Returns the number of [entities][Entity] that belong to this family.
     */
    val numEntities: Int
        get() = countEntities

    /**
     * Returns true if and only if this [Family] does not contain any entity.
     */
    val isEmpty: Boolean
        get() = countEntities == 0

    /**
     * Returns true if and only if this [Family] contains at least one entity.
     */
    val isNotEmpty: Boolean
        get() = countEntities > 0

    /**
     * Flag to indicate if there are changes in the [activeEntities].
     * If it is true then the [mutableEntities] will get updated the next time it is accessed.
     */
    private var isDirty = false

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
    operator fun contains(entity: Entity): Boolean = activeEntities.hasValueAtIndex(entity.id)

    /**
     * Updates this family if needed and runs the given [action] for all [entities][Entity].
     *
     * **Important note**: There is a potential risk when iterating over entities and one of those entities
     * gets removed. Removing the entity immediately and cleaning up its components could
     * cause problems because if you access a component which is mandatory for the family, you will get
     * a FleksNoSuchComponentException. To avoid that you could check if an entity really has the component
     * before accessing it but that is redundant in context of a family.
     *
     * To avoid these kinds of issues, entity removals are delayed until the end of the iteration. This also means
     * that a removed entity of this family will still be part of the [action] for the current iteration.
     */
    inline fun forEach(crossinline action: Family.(Entity) -> Unit) {
        // Access entities before 'forEach' call to properly update them.
        // Check mutableEntities getter for more details.
        val entitiesForIteration = mutableEntities

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
    fun first(): Entity = mutableEntities.first()

    /**
     * Updates this family if needed and returns its first [Entity] matching the given [predicate].
     * @throws [NoSuchElementException] if the family has no such entity.
     */
    fun first(predicate: (Entity) -> Boolean): Entity = mutableEntities.first(predicate)

    /**
     * Updates this family if needed and returns its first [Entity] or null if the family has no entities.
     */
    fun firstOrNull(): Entity? = mutableEntities.firstOrNull()

    /**
     * Updates this family if needed and returns its first [Entity] matching the given [predicate],
     * or null if the family has no such entity.
     */
    fun firstOrNull(predicate: (Entity) -> Boolean): Entity? = mutableEntities.firstOrNull(predicate)

    /**
     * Sorts the [entities][Entity] of this family by the given [comparator].
     */
    fun sort(comparator: EntityComparator) = mutableEntities.sort(comparator)

    /**
     * Adds the [entity] to the family and sets the [isDirty] flag if and only
     * if the entity's [compMask] is matching the family configuration.
     */
    @PublishedApi
    internal fun onEntityAdded(entity: Entity, compMask: BitArray) {
        if (compMask in this) {
            isDirty = true
            if (activeEntities.hasNoValueAtIndex(entity.id)) countEntities++
            activeEntities[entity.id] = entity
            addHook?.invoke(world, entity)
        }
    }

    /**
     * Checks if the [entity] is part of the family by analyzing the entity's components.
     * The [compMask] is a [BitArray] that indicates which components the [entity] currently has.
     *
     * The [entity] gets either added to the [activeEntities] or removed and [isDirty] is set when needed.
     */
    @PublishedApi
    internal fun onEntityCfgChanged(entity: Entity, compMask: BitArray) {
        val entityInFamily = compMask in this
        val currentEntity = activeEntities.getOrNull(entity.id)
        if (entityInFamily && currentEntity == null) {
            // new entity gets added
            isDirty = true
            countEntities++
            activeEntities[entity.id] = entity
            addHook?.invoke(world, entity)
        } else if (!entityInFamily && currentEntity != null) {
            // existing entity gets removed
            isDirty = true
            countEntities--
            activeEntities.removeAt(entity.id)
            removeHook?.invoke(world, entity)
        }
    }

    /**
     * Removes the [entity] of the family and sets the [isDirty] flag if and only
     * if the [entity] is already in the family.
     */
    internal fun onEntityRemoved(entity: Entity) {
        if (activeEntities.hasValueAtIndex(entity.id)) {
            // existing entity gets removed
            isDirty = true
            activeEntities.removeAt(entity.id)
            countEntities--
            removeHook?.invoke(world, entity)
        }
    }

    override fun toString(): String {
        return "Family(allOf=$allOf, noneOf=$noneOf, anyOf=$anyOf, numEntities=$numEntities)"
    }
}
