package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.ArrayEntityBag
import com.github.quillraven.fleks.collection.EntityBag
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.native.concurrent.ThreadLocal
import kotlin.time.Duration

/**
 * Snapshot for an [entity][Entity] that contains its [components][Component] and [tags][EntityTag].
 */
@Serializable
data class Snapshot(
    val components: List<Component<out @Contextual Any>>,
    val tags: List<UniqueId<out @Contextual Any>>,
)

/**
 * Utility function to manually create a [Snapshot].
 */
@Suppress("UNCHECKED_CAST")
fun wildcardSnapshotOf(components: List<Component<*>>, tags: List<UniqueId<*>>): Snapshot {
    return Snapshot(components as List<Component<out Any>>, tags as List<UniqueId<out Any>>)
}

/**
 * A world to handle [entities][Entity] and [systems][IntervalSystem].
 *
 * @param entityCapacity the initial maximum capacity of entities.
 */
class World internal constructor(
    entityCapacity: Int,
) : EntityComponentContext(ComponentService()) {
    @PublishedApi
    internal val injectables = mutableMapOf<String, Injectable>()

    /**
     * Returns the time that is passed to [update][World.update].
     * It represents the time in seconds between two frames.
     */
    var deltaTime = 0f
        private set

    @PublishedApi
    internal val entityService = EntityService(this, entityCapacity)

    /**
     * List of all [families][Family] of the world that are created either via
     * an [IteratingSystem] or via the world's [family] function to
     * avoid creating duplicates.
     */
    @PublishedApi
    internal var allFamilies = emptyArray<Family>()

    /**
     * Returns the amount of active entities.
     */
    val numEntities: Int
        get() = entityService.numEntities

    /**
     * Returns the maximum capacity of active entities.
     */
    val capacity: Int
        get() = entityService.capacity

    // internal mutable list of systems
    // can be replaced in a later version of Kotlin with "backing field" syntax
    internal val mutableSystems = arrayListOf<IntervalSystem>()

    /**
     * Returns the world's systems.
     */
    val systems: List<IntervalSystem>
        get() = mutableSystems

    /**
     * Map of add [FamilyHook] out of the [WorldConfiguration].
     * Only used if there are also aggregated system hooks for the family to remember
     * its original world configuration hook (see [initAggregatedFamilyHooks] and [updateAggregatedFamilyHooks]).
     */
    private val worldCfgFamilyAddHooks = mutableMapOf<Family, FamilyHook?>()

    /**
     * Map of remove [FamilyHook] out of the [WorldConfiguration].
     * Only used if there are also aggregated system hooks for the family to remember
     * its original world configuration hook (see [initAggregatedFamilyHooks] and [updateAggregatedFamilyHooks]).
     */
    private val worldCfgFamilyRemoveHooks = mutableMapOf<Family, FamilyHook?>()

    /**
     * Cache of used [EntityTag] instances. Needed for snapshot functionality.
     */
    @PublishedApi
    internal val tagCache = mutableMapOf<Int, UniqueId<*>>()

    init {
        /**
         * Maybe because of design flaws, the world reference of the ComponentService must be
         * set in the world's constructor because the parent class (=EntityComponentContext) already
         * requires a ComponentService, and it is not possible to pass "this" reference directly.
         *
         * That's why it is happening here to set it as soon as possible.
         */
        componentService.world = this
    }

    /**
     * Returns an already registered injectable of the given [name] and marks it as used.
     *
     * @throws FleksNoSuchInjectableException if there is no injectable registered for [name].
     */
    inline fun <reified T> inject(name: String = T::class.simpleName ?: T::class.toString()): T {
        val injectable = injectables[name] ?: throw FleksNoSuchInjectableException(name)
        injectable.used = true
        return injectable.injObj as T
    }

    /**
     * Returns a new map of unused [injectables][Injectable]. An injectable gets set to 'used'
     * when it gets injected at least once via a call to [inject].
     */
    fun unusedInjectables(): Map<String, Any> =
        injectables.filterValues { !it.used }.mapValues { it.value.injObj }

    /**
     * Returns a new [EntityBag] instance containing all [entities][Entity] of the world.
     *
     * Do not call this operation each frame, as it can be expensive depending on the amount
     * of entities in your world.
     *
     * For frequent entity operations on specific entities, use [families][Family].
     */
    fun asEntityBag(): EntityBag {
        val result = ArrayEntityBag(numEntities)
        entityService.forEach {
            result += it
        }
        return result
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateContext].
     *
     * **Attention** Make sure that you only modify the entity of the current scope.
     * Otherwise, you will get wrong behavior for families. E.g. don't do this:
     *
     * ```
     * entity {
     *     // modifying the current entity is allowed ✅
     *     it += Position()
     *     // don't modify other entities ❌
     *     someOtherEntity += Position()
     * }
     * ```
     */
    inline fun entity(configuration: EntityCreateContext.(Entity) -> Unit = {}): Entity {
        return entityService.create(configuration)
    }

    /**
     * Returns true if and only if the [entity] is not removed and is part of the [World].
     */
    operator fun contains(entity: Entity) = entityService.contains(entity)

    /**
     * Removes the given [entity] from the world. The [entity] will be recycled and reused for
     * future calls to [World.entity].
     */
    operator fun minusAssign(entity: Entity) {
        entityService -= entity
    }

    /**
     * Removes all [entities][Entity] from the world. The entities will be recycled and reused for
     * future calls to [World.entity].
     * If [clearRecycled] is true then the recycled entities are cleared and the ids for newly
     * created entities start at 0 again.
     */
    fun removeAll(clearRecycled: Boolean = false) {
        entityService.removeAll(clearRecycled)
    }

    /**
     * Performs the given [action] on each active [entity][Entity].
     */
    fun forEach(action: World.(Entity) -> Unit) {
        entityService.forEach(action)
    }

    /**
     * Returns the specified [system][IntervalSystem].
     *
     * @throws [FleksNoSuchSystemException] if there is no such system.
     */
    inline fun <reified T : IntervalSystem> system(): T {
        systems.forEach { system ->
            if (system is T) {
                return system
            }
        }
        throw FleksNoSuchSystemException(T::class)
    }

    /**
     * Returns true if and only if the given [system][IntervalSystem] is part of the world.
     */
    inline fun <reified T : IntervalSystem> contains(): Boolean {
        return systems.any { it is T }
    }

    /**
     * Returns the specified [system][IntervalSystem] or null if there is no such system.
     */
    inline fun <reified T : IntervalSystem> systemOrNull(): T? {
        return systems.firstOrNull { it is T } as T?
    }

    /**
     * Adds the [system] to the world's [systems] at the given [index].
     *
     * @throws FleksSystemAlreadyAddedException if the system was already added before.
     */
    fun add(index: Int, system: IntervalSystem) {
        if (systems.any { it::class == system::class }) {
            throw FleksSystemAlreadyAddedException(system::class)
        }

        mutableSystems.add(index, system)
        if (system is IteratingSystem && (system is FamilyOnAdd || system is FamilyOnRemove)) {
            updateAggregatedFamilyHooks(system.family)
        }
        system.onInit()
    }

    /**
     * Adds the [system] to the world's [systems].
     *
     * @throws FleksSystemAlreadyAddedException if the system was already added before.
     */
    fun add(system: IntervalSystem) = add(systems.size, system)

    /**
     * Adds the [system] to the world's [systems].
     *
     * @throws FleksSystemAlreadyAddedException if the system was already added before.
     */
    operator fun plusAssign(system: IntervalSystem) = add(system)

    /**
     * Removes the [system] of the world's [systems].
     */
    fun remove(system: IntervalSystem) {
        mutableSystems.remove(system)
        if (system is IteratingSystem && (system is FamilyOnAdd || system is FamilyOnRemove)) {
            updateAggregatedFamilyHooks(system.family)
        }
        system.onDispose()
    }

    /**
     * Removes the [system] of the world's [systems].
     */
    operator fun minusAssign(system: IntervalSystem) = remove(system)

    /**
     * Sets the [hook] as an [EntityService.addHook].
     *
     * @throws FleksHookAlreadyAddedException if the [EntityService] already has an add hook set.
     */
    @PublishedApi
    internal fun setEntityAddHook(hook: EntityHook) {
        if (entityService.addHook != null) {
            throw FleksHookAlreadyAddedException("addHook", "Entity")
        }
        entityService.addHook = hook
    }

    /**
     * Sets the [hook] as an [EntityService.removeHook].
     *
     * @throws FleksHookAlreadyAddedException if the [EntityService] already has a remove hook set.
     */
    @PublishedApi
    internal fun setEntityRemoveHook(hook: EntityHook) {
        if (entityService.removeHook != null) {
            throw FleksHookAlreadyAddedException("removeHook", "Entity")
        }
        entityService.removeHook = hook
    }

    /**
     * Creates a new [Family] for the given [cfg][FamilyDefinition].
     *
     * This function internally either creates or reuses an already existing [family][Family].
     * In case a new [family][Family] gets created it will be initialized with any already existing [entity][Entity]
     * that matches its configuration.
     * Therefore, this might have a performance impact on the first call if there are a lot of entities in the world.
     *
     * As a best practice families should be created as early as possible, ideally during world creation.
     * Also, store the result of this function instead of calling this function multiple times with the same arguments.
     *
     * @throws [FleksFamilyException] if the [FamilyDefinition] is null or empty.
     */
    fun family(cfg: FamilyDefinition.() -> Unit): Family = family(FamilyDefinition().apply(cfg))

    /**
     * Creates a new [Family] for the given [definition][FamilyDefinition].
     *
     * This function internally either creates or reuses an already existing [family][Family].
     * In case a new [family][Family] gets created it will be initialized with any already existing [entity][Entity]
     * that matches its configuration.
     * Therefore, this might have a performance impact on the first call if there are a lot of entities in the world.
     *
     * As a best practice families should be created as early as possible, ideally during world creation.
     * Also, store the result of this function instead of calling this function multiple times with the same arguments.
     *
     * @throws [FleksFamilyException] if the [FamilyDefinition] is null or empty.
     */
    @PublishedApi
    internal fun family(definition: FamilyDefinition): Family {
        if (definition.isEmpty()) {
            throw FleksFamilyException(definition)
        }

        val (defAll, defNone, defAny) = definition

        var family = allFamilies.find { it.allOf == defAll && it.noneOf == defNone && it.anyOf == defAny }
        if (family == null) {
            family = Family(defAll, defNone, defAny, this)
            allFamilies += family
            // initialize a newly created family by notifying it for any already existing entity
            // world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
            entityService.forEach { family.onEntityCfgChanged(it, entityService.compMasks[it.id]) }
        }
        return family
    }

    /**
     * Returns a map that contains all [entities][Entity] and their components of this world.
     * The keys of the map are the entities.
     * The values are a list of components that a specific entity has. If the entity
     * does not have any components then the value is an empty list.
     */
    fun snapshot(): Map<Entity, Snapshot> {
        val result = mutableMapOf<Entity, Snapshot>()

        entityService.forEach { result[it] = snapshotOf(it) }

        return result
    }

    /**
     * Returns a list that contains all components of the given [entity] of this world.
     * If the entity does not have any components then an empty list is returned.
     */
    @Suppress("UNCHECKED_CAST")
    fun snapshotOf(entity: Entity): Snapshot {
        val comps = mutableListOf<Component<*>>()
        val tags = mutableListOf<UniqueId<*>>()

        if (entity in entityService) {
            entityService.compMasks[entity.id].forEachSetBit { cmpId ->
                val holder = componentService.holderByIndexOrNull(cmpId)
                if (holder == null) {
                    // tag instead of component
                    tags += tagCache[cmpId] ?: throw FleksSnapshotException("Tag with id $cmpId was never assigned")
                } else {
                    comps += holder[entity]
                }
            }
        }

        return Snapshot(comps as List<Component<out Any>>, tags as List<UniqueId<out Any>>)
    }

    /**
     * Loads the given [snapshot] of the world. This will first clear any existing
     * entity of the world. After that it will load all provided entities and components.
     * This will also execute [FamilyHook].
     *
     * @throws FleksSnapshotException if a family iteration is currently in process.
     */
    fun loadSnapshot(snapshot: Map<Entity, Snapshot>) {
        if (entityService.delayRemoval) {
            throw FleksSnapshotException("Snapshots cannot be loaded while a family iteration is in process")
        }

        // remove any existing entity and clean up recycled ids
        removeAll(true)
        if (snapshot.isEmpty()) {
            // snapshot is empty -> nothing to load
            return
        }

        val versionLookup = snapshot.keys.associateBy { it.id }

        // Set next entity id to the maximum provided id + 1.
        // All ids before that will be either created or added to the recycled
        // ids to guarantee that the provided snapshot entity ids match the newly created ones.
        with(entityService) {
            val maxId = snapshot.keys.maxOf { it.id }
            repeat(maxId + 1) {
                val entity = Entity(it, version = (versionLookup[it]?.version ?: 0u) - 1u)
                this.recycle(entity)
                val entitySnapshot = snapshot[versionLookup[it]]
                if (entitySnapshot != null) {
                    // snapshot for entity is provided -> create it
                    // note that the id for the entity will be the recycled id from above
                    this.configure(this.create { }, entitySnapshot)
                }
            }
        }
    }

    /**
     * Loads the given [entity] and its [snapshot][Snapshot].
     * If the entity does not exist yet, it will be created.
     * If the entity already exists it will be updated with the given components.
     *
     * @throws FleksSnapshotException if a family iteration is currently in process.
     */
    fun loadSnapshotOf(entity: Entity, snapshot: Snapshot) {
        if (entityService.delayRemoval) {
            throw FleksSnapshotException("Snapshots cannot be loaded while a family iteration is in process")
        }

        if (entity !in entityService) {
            // entity not part of service yet -> create it
            entityService.create(entity.id) { }
        }

        // load components for entity
        entityService.configure(entity, snapshot)
    }

    /**
     * Updates all [enabled][IntervalSystem.enabled] [systems][IntervalSystem] of the world
     * using the given [deltaTime] in seconds.
     */
    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        for (i in systems.indices) {
            val system = systems[i]
            if (system.enabled) {
                system.onUpdate()
            }
        }
    }

    /**
     * Updates all [enabled][IntervalSystem.enabled] [systems][IntervalSystem] of the world
     * using the given [duration]. The duration is converted to seconds.
     */
    fun update(duration: Duration) {
        update(duration.inWholeNanoseconds * 0.000000001f)
    }

    /**
     * Removes all [entities][Entity] of the world and calls the
     * [onDispose][IntervalSystem.onDispose] function of each system.
     */
    fun dispose() {
        entityService.removeAll()
        systems.forEachReverse { it.onDispose() }
    }

    /**
     * Extend [Family.addHook] and [Family.removeHook] for all
     * [systems][IteratingSystem] that implement [FamilyOnAdd] and/or [FamilyOnRemove].
     */
    internal fun initAggregatedFamilyHooks() {
        // validate systems against illegal interfaces
        systems.forEach { system ->
            // FamilyOnAdd and FamilyOnRemove interfaces are only meant to be used by IteratingSystem
            if (system !is IteratingSystem) {
                if (system is FamilyOnAdd) {
                    throw FleksWrongSystemInterfaceException(system::class, FamilyOnAdd::class)
                }
                if (system is FamilyOnRemove) {
                    throw FleksWrongSystemInterfaceException(system::class, FamilyOnRemove::class)
                }
            }
        }

        // add all the configured family hooks to the cache
        allFamilies.forEach {
            worldCfgFamilyAddHooks[it] = it.addHook
            worldCfgFamilyRemoveHooks[it] = it.removeHook
        }
        allFamilies.forEach { updateAggregatedFamilyHooks(it) }
    }

    /**
     * Update [Family.addHook] and [Family.removeHook] for all
     * [systems][IteratingSystem] that implement [FamilyOnAdd] and/or [FamilyOnRemove]
     * and iterate over the given [family].
     */
    private fun updateAggregatedFamilyHooks(family: Family) {
        // system validation like in initAggregatedFamilyHooks is not necessary
        // because it is already validated before (in initAggregatedFamilyHooks and in add/remove system)

        // update family add hook by adding systems' onAddEntity calls after its original world cfg hook
        val addSystems = systems.filter { it is IteratingSystem && it is FamilyOnAdd && it.family == family }
        val ownAddHook = worldCfgFamilyAddHooks[family]
        family.addHook = if (ownAddHook != null) { entity ->
            ownAddHook(this, entity)
            addSystems.forEach { (it as FamilyOnAdd).onAddEntity(entity) }
        } else { entity ->
            addSystems.forEach { (it as FamilyOnAdd).onAddEntity(entity) }
        }

        // update family remove hook by adding systems' onRemoveEntity calls before its original world cfg hook
        val removeSystems = systems.filter { it is IteratingSystem && it is FamilyOnRemove && it.family == family }
        val ownRemoveHook = worldCfgFamilyRemoveHooks[family]
        family.removeHook = if (ownRemoveHook != null) { entity ->
            removeSystems.forEachReverse { (it as FamilyOnRemove).onRemoveEntity(entity) }
            ownRemoveHook(this, entity)
        } else { entity ->
            removeSystems.forEachReverse { (it as FamilyOnRemove).onRemoveEntity(entity) }
        }
    }

    @ThreadLocal
    companion object {
        @PublishedApi
        internal var CURRENT_WORLD: World? = null

        /**
         * Returns an already registered injectable of the given [name] and marks it as used.
         *
         * @throws FleksNoSuchInjectableException if there is no injectable registered for [name].
         * @throws FleksWrongConfigurationUsageException if called outside a [WorldConfiguration] scope.
         */
        inline fun <reified T> inject(name: String = T::class.simpleName ?: T::class.toString()): T =
            CURRENT_WORLD?.inject(name) ?: throw FleksWrongConfigurationUsageException()

        /**
         * Creates a new [Family] for the given [cfg][FamilyDefinition].
         *
         * This function internally either creates or reuses an already existing [family][Family].
         * In case a new [family][Family] gets created it will be initialized with any already existing [entity][Entity]
         * that matches its configuration.
         * Therefore, this might have a performance impact on the first call if there are a lot of entities in the world.
         *
         * As a best practice families should be created as early as possible, ideally during world creation.
         * Also, store the result of this function instead of calling this function multiple times with the same arguments.
         *
         * @throws [FleksFamilyException] if the [FamilyDefinition] is null or empty.
         * @throws FleksWrongConfigurationUsageException if called outside a [WorldConfiguration] scope.
         */
        fun family(cfg: FamilyDefinition.() -> Unit): Family =
            CURRENT_WORLD?.family(cfg) ?: throw FleksWrongConfigurationUsageException()
    }
}

private inline fun <T> List<T>.forEachReverse(action: (T) -> Unit) {
    val lastIndex = this.lastIndex
    for (i in lastIndex downTo 0) {
        action(this[i])
    }
}
