package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.CURRENT_WORLD
import com.github.quillraven.fleks.collection.EntityBag
import com.github.quillraven.fleks.collection.MutableEntityBag
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

/**
 * DSL marker for the [WorldConfiguration].
 */
@DslMarker
annotation class WorldCfgMarker

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used to identify unused injectables after [world][World] creation.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

/**
 * A DSL class to configure [Injectable] of a [WorldConfiguration].
 */
@WorldCfgMarker
class InjectableConfiguration(private val world: World) {

    /**
     * Adds the specified [dependency] under the given [name] which
     * can then be injected via [World.inject].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    fun <T : Any> add(name: String, dependency: T) {
        if (name in world.injectables) {
            throw FleksInjectableAlreadyAddedException(name)
        }

        world.injectables[name] = Injectable(dependency)
    }

    /**
     * Adds the specified [dependency] via its [simpleName][KClass.simpleName],
     * or via its [toString][KClass.toString] if it has no name.
     * It can then be injected via [World.inject].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    inline fun <reified T : Any> add(dependency: T) = add(T::class.simpleName ?: T::class.toString(), dependency)
}

/**
 * A DSL class to configure [IntervalSystem] of a [WorldConfiguration].
 */
@WorldCfgMarker
class SystemConfiguration(
    private val systems: MutableList<IntervalSystem>
) {
    /**
     * Adds the [system] to the [world][World].
     * The order in which systems are added is the order in which they will be executed when calling [World.update].
     *
     * @throws [FleksSystemAlreadyAddedException] if the system was already added before.
     */
    fun add(system: IntervalSystem) {
        if (systems.any { it::class == system::class }) {
            throw FleksSystemAlreadyAddedException(system::class)
        }
        systems += system
    }
}

/**
 * A DSL class to configure [FamilyHook] for specific [families][Family].
 */
@WorldCfgMarker
class FamilyConfiguration(
    @PublishedApi
    internal val world: World,
) {

    /**
     * Sets the add [hook][Family.addHook] for the given [family].
     * This hook gets called whenever an [entity][Entity] enters the [family].
     */
    fun onAdd(
        family: Family,
        hook: FamilyHook
    ) {
        if (family.addHook != null) {
            throw FleksHookAlreadyAddedException("addHook", "Family $family")
        }
        family.addHook = hook
    }

    /**
     * Sets the remove [hook][Family.removeHook] for the given [family].
     * This hook gets called whenever an [entity][Entity] leaves the [family].
     */
    fun onRemove(
        family: Family,
        hook: FamilyHook
    ) {
        if (family.removeHook != null) {
            throw FleksHookAlreadyAddedException("removeHook", "Family $family")
        }
        family.removeHook = hook
    }
}

/**
 * A configuration for an entity [world][World] to define the systems, dependencies to be injected,
 * [component][Component]- and [family][Family] hooks.
 *
 * @param world the [World] to be configured.
 */
@WorldCfgMarker
class WorldConfiguration(@PublishedApi internal val world: World) {

    private var injectableCfg: (InjectableConfiguration.() -> Unit)? = null
    private var familyCfg: (FamilyConfiguration.() -> Unit)? = null
    private var systemCfg: (SystemConfiguration.() -> Unit)? = null

    fun injectables(cfg: InjectableConfiguration.() -> Unit) {
        injectableCfg = cfg
    }

    fun families(cfg: FamilyConfiguration.() -> Unit) {
        familyCfg = cfg
    }

    fun systems(cfg: SystemConfiguration.() -> Unit) {
        systemCfg = cfg
    }

    /**
     * Sets the add entity [hook][EntityService.addHook].
     * This hook gets called whenever an [entity][Entity] gets created and
     * after its [components][Component] are assigned and [families][Family] are updated.
     */
    fun onAddEntity(hook: EntityHook) {
        world.setEntityAddHook(hook)
    }

    /**
     * Sets the remove entity [hook][EntityService.removeHook].
     * This hook gets called whenever an [entity][Entity] gets removed and
     * before its [components][Component] are removed and [families][Family] are updated.
     */
    fun onRemoveEntity(hook: EntityHook) {
        world.setEntityRemoveHook(hook)
    }

    /**
     * Sets the [EntityProvider] for the [EntityService] by calling the [factory] function
     * within the context of a [World]. Per default the [DefaultEntityProvider] is used.
     */
    fun entityProvider(factory: World.() -> EntityProvider) {
        world.entityService.entityProvider = world.run(factory)
    }

    /**
     * Configures the world in following sequence:
     * - injectables
     * - family
     * - system
     *
     * The order is important to correctly trigger [FamilyHook]s and [EntityHook]s.
     */
    fun configure() {
        injectableCfg?.invoke(InjectableConfiguration(world))
        familyCfg?.invoke(FamilyConfiguration(world))
        SystemConfiguration(world.mutableSystems).also {
            systemCfg?.invoke(it)
        }

        if (world.numEntities > 0) {
            throw FleksWorldModificationDuringConfigurationException()
        }

        setUpAggregatedFamilyHooks()

        world.systems.forEach { it.onInit() }
    }

    /**
     * Extend [Family.addHook] and [Family.removeHook] with
     * other objects that needed to triggered by the hooks.
     */
    private fun setUpAggregatedFamilyHooks() = world.setUpAggregatedFamilyHooks(world.systems)
}

/**
 * Creates a new [world][World] with the given [cfg][WorldConfiguration].
 *
 * @param entityCapacity initial maximum entity capacity.
 * Will be used internally when a [world][World] is created to set the initial
 * size of some collections and to avoid slow resizing calls.
 *
 * @param cfg the [configuration][WorldConfiguration] of the world containing the [systems][IntervalSystem],
 * [injectables][Injectable] and [FamilyHook]s.
 */
fun configureWorld(entityCapacity: Int = 512, cfg: WorldConfiguration.() -> Unit): World {
    val newWorld = World(entityCapacity)
    CURRENT_WORLD = newWorld

    try {
        WorldConfiguration(newWorld).apply(cfg).configure()
    } finally {
        CURRENT_WORLD = null
    }

    return newWorld
}

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

    /**
     * Returns the world's systems.
     */
    internal val mutableSystems = mutableListOf<IntervalSystem>()
    val systems: List<IntervalSystem> = mutableSystems

    /**
     * Adds a new system to the world.
     *
     * @param system The system to be added to the world. This should be an instance of a class that extends IntervalSystem.
     * @param index The position at which the system should be inserted in the list of systems. If null, the system is added at the end of the list.
     * This parameter is optional and defaults to null.
     *
     * @throws FleksSystemAlreadyAddedException if the system was already added before.
     */
    fun add(system: IntervalSystem, index: Int? = null) {
        if (systems.any { it::class == system::class }) {
            throw FleksSystemAlreadyAddedException(system::class)
        }

        setUpAggregatedFamilyHooks(listOf(system))

        mutableSystems.add(index ?: mutableSystems.size, system)
    }

    /**
     * Removes the specified system from the world.
     *
     * @param system The system to be removed from the world. This should be an instance of a class that extends IntervalSystem.
     * @return True if the system was successfully removed, false otherwise.
     */
    fun remove(system: IntervalSystem) {
        mutableSystems.remove(system)

        if (system is IteratingSystem) {
            system.family.addHook = null
            system.family.removeHook = null
        }
    }

    /**
     * Adds a new system to the world using the '+=' operator.
     *
     * @param system The system to be added to the world. This should be an instance of a class that extends IntervalSystem.
     *
     * @throws FleksSystemAlreadyAddedException if the system was already added before.
     */
    operator fun plusAssign(system: IntervalSystem) = add(system)

    /**
     * Removes the specified system from the world using the '-=' operator.
     *
     * @param system The system to be removed from the world. This should be an instance of a class that extends IntervalSystem.
     */
    operator fun minusAssign(system: IntervalSystem) { remove(system) }

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
        val result = MutableEntityBag(numEntities)
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
     * using the given [deltaTime].
     */
    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        systems.forEach { system ->
            if (system.enabled) {
                system.onUpdate()
            }
        }
    }

    /**
     * Removes all [entities][Entity] of the world and calls the
     * [onDispose][IntervalSystem.onDispose] function of each system.
     */
    fun dispose() {
        entityService.removeAll()
        systems.reversed().forEach { it.onDispose() }
    }

    internal fun setUpAggregatedFamilyHooks(systems: List<IntervalSystem>) {
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

        // register family hooks for IteratingSystem.FamilyOnAdd containing systems
        systems
            .mapNotNull { if (it is IteratingSystem && it is FamilyOnAdd) it else null }
            .groupBy { it.family }
            .forEach { (family, systemList) ->
                val ownHook = family.addHook
                val systemArray = systemList.toTypedArray()
                family.addHook = { entity ->
                    ownHook?.invoke(this, entity)
                    systemArray.forEach { it.onAddEntity(entity) }
                }
            }

        // register family hooks for IteratingSystem.FamilyOnRemove containing systems
        systems
            .mapNotNull { if (it is IteratingSystem && it is FamilyOnRemove) it else null }
            .groupBy { it.family }
            .forEach { (family, systemList) ->
                val ownHook = family.removeHook
                val systemArray = systemList.toTypedArray()
                family.removeHook = { entity ->
                    systemArray.forEachReverse { it.onRemoveEntity(entity) }
                    ownHook?.invoke(this, entity)
                }
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

private inline fun <T> Array<T>.forEachReverse(action: (T) -> Unit) {
    val lastIndex = this.lastIndex
    for (i in lastIndex downTo 0) {
        action(this[i])
    }
}
