package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.EntityComparator
import com.github.quillraven.fleks.collection.IntBag
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
    fun all(vararg types: ComponentType<*>): FamilyDefinition {
        allOf = BitArray(types.size).also { bits ->
            types.forEach { bits.set(it.id) }
        }
        return this
    }

    /**
     * Any [entity][Entity] must not have any of the given [types] to be part of the [family][Family].
     */
    fun none(vararg types: ComponentType<*>): FamilyDefinition {
        noneOf = BitArray(types.size).also { bits ->
            types.forEach { bits.set(it.id) }
        }
        return this
    }

    /**
     * Any [entity][Entity] must have at least one of the given [types] to be part of the [family][Family].
     */
    fun any(vararg types: ComponentType<*>): FamilyDefinition {
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
    @PublishedApi
    internal val world: World,
    @PublishedApi
    internal val entityService: EntityService = world.entityService,
) : EntityGetComponentContext(world.componentService) {
    /**
     * An optional [FamilyHook] that gets called whenever an [entity][Entity] enters the family.
     */
    @PublishedApi
    internal var addHook: FamilyHook? = null

    /**
     * An optional [FamilyHook] that gets called whenever an [entity][Entity] leaves the family.
     */
    @PublishedApi
    internal var removeHook: FamilyHook? = null

    /**
     * Return the [entities] in form of an [IntBag] for better iteration performance.
     */
    @PublishedApi
    internal val entitiesBag = IntBag()

    /**
     * Returns the [entities][Entity] that belong to this family.
     */
    private val entities = BitArray(world.capacity)

    /**
     * Returns the number of [entities][Entity] that belong to this family.
     * This can be an expensive call if the amount of entities is very high because it
     * iterates through the entire underlying [BitArray].
     */
    val numEntities: Int
        get() = entities.numBits()

    /**
     * Returns true if and only if this [Family] does not contain any entity.
     */
    val isEmpty: Boolean
        get() = entities.isEmpty

    /**
     * Returns true if and only if this [Family] contains at least one entity.
     */
    val isNotEmpty: Boolean
        get() = entities.isNotEmpty

    /**
     * Flag to indicate if there are changes in the [entities]. If it is true then the [entitiesBag] should get
     * updated via a call to [updateActiveEntities].
     *
     * Refer to [IteratingSystem.onTick] for an example implementation.
     */
    @PublishedApi
    internal var isDirty = false
        private set

    /**
     * Returns true if the specified [compMask] matches the family's component configuration.
     *
     * @param compMask the component configuration of an [entity][Entity].
     */
    operator fun contains(compMask: BitArray): Boolean {
        return (allOf == null || compMask.contains(allOf))
            && (noneOf == null || !compMask.intersects(noneOf))
            && (anyOf == null || compMask.intersects(anyOf))
    }

    /**
     * Updates the [entitiesBag] and clears the [isDirty] flag if needed.
     */
    @PublishedApi
    internal fun updateActiveEntities() {
        if (isDirty) {
            isDirty = false
            entities.toIntBag(entitiesBag)
        }
    }

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
    inline fun forEach(action: Family.(Entity) -> Unit) {
        updateActiveEntities()
        if (!entityService.delayRemoval) {
            entityService.delayRemoval = true
            entitiesBag.forEach { this.action(Entity(it)) }
            entityService.cleanupDelays()
        } else {
            entitiesBag.forEach { this.action(Entity(it)) }
        }
    }

    /**
     * Updates this family if needed and returns its first [Entity].
     * @throws [NoSuchElementException] if the family has no entities.
     */
    fun first(): Entity {
        if (!entityService.delayRemoval || entitiesBag.isEmpty) {
            // no iteration in process -> update entities if necessary
            updateActiveEntities()
        }

        return Entity(entitiesBag.first)
    }

    /**
     * Updates this family if needed and returns its first [Entity] or null if the family has no entities.
     */
    fun firstOrNull(): Entity? {
        if (!entityService.delayRemoval || entitiesBag.isEmpty) {
            // no iteration in process -> update entities if necessary
            updateActiveEntities()
        }

        val id = entitiesBag.firstOrNull ?: return null
        return Entity(id)
    }

    /**
     * Updates the [entity][Entity] using the given [configuration] to add and remove [components][Component].
     */
    inline fun Entity.configure(configuration: EntityUpdateContext.(Entity) -> Unit) {
        entityService.configure(this, configuration)
    }

    /**
     * Sorts the [entities][Entity] of this family by the given [comparator].
     */
    fun sort(comparator: EntityComparator) {
        updateActiveEntities()
        entitiesBag.sort(comparator)
    }

    /**
     * Adds the [entity] to the family and sets the [isDirty] flag if and only
     * if the entity's [compMask] is matching the family configuration.
     */
    fun onEntityAdded(entity: Entity, compMask: BitArray) {
        if (compMask in this) {
            isDirty = true
            entities.set(entity.id)
            addHook?.invoke(world, entity)
        }
    }

    /**
     * Checks if the [entity] is part of the family by analyzing the entity's components.
     * The [compMask] is a [BitArray] that indicates which components the [entity] currently has.
     *
     * The [entity] gets either added to the [entities] or removed and [isDirty] is set when needed.
     */
    fun onEntityCfgChanged(entity: Entity, compMask: BitArray) {
        val entityInFamily = compMask in this
        if (entityInFamily && !entities[entity.id]) {
            // new entity gets added
            isDirty = true
            entities.set(entity.id)
            addHook?.invoke(world, entity)
        } else if (!entityInFamily && entities[entity.id]) {
            // existing entity gets removed
            isDirty = true
            entities.clear(entity.id)
            removeHook?.invoke(world, entity)
        }
    }

    /**
     * Removes the [entity] of the family and sets the [isDirty] flag if and only
     * if the [entity] is already in the family.
     */
    fun onEntityRemoved(entity: Entity) {
        if (entities[entity.id]) {
            // existing entity gets removed
            isDirty = true
            entities.clear(entity.id)
            removeHook?.invoke(world, entity)
        }
    }

    override fun toString(): String {
        return "Family(allOf=$allOf, noneOf=$noneOf, anyOf=$anyOf, numEntities=$numEntities)"
    }
}
