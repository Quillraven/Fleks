package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.CURRENT_WORLD
import kotlin.native.concurrent.ThreadLocal

@DslMarker
annotation class WorldCfgMarker

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used in the [SystemService] to find out any unused injectables.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

/**
 * A DSL class to configure components and [ComponentListener] of a [WorldConfiguration].
 */
@WorldCfgMarker
class ComponentConfiguration(
    @PublishedApi
    internal val world: World
) {
    inline fun <reified T : Component<*>> onAdd(
        componentType: ComponentType<T>,
        noinline action: ComponentHook<T>
    ) {
        if (world.systems.isNotEmpty()) {
            throw FleksWrongConfigurationOrderException()
        }

        val mapper = world[componentType]
        if (mapper.addHook != null) {
            throw FleksHookAlreadyAddedException("addHook", "Component ${componentType::class.simpleName}")
        }
        mapper.addHook = action
    }

    inline fun <reified T : Component<*>> onRemove(
        componentType: ComponentType<T>,
        noinline action: ComponentHook<T>
    ) {
        if (world.systems.isNotEmpty()) {
            throw FleksWrongConfigurationOrderException()
        }

        val mapper = world[componentType]
        if (mapper.removeHook != null) {
            throw FleksHookAlreadyAddedException("removeHook", "Component ${componentType::class.simpleName}")
        }
        mapper.removeHook = action
    }
}

/**
 * A DSL class to configure [IntervalSystem] of a [WorldConfiguration].
 */
@WorldCfgMarker
class SystemConfiguration(private val world: World) {
    fun add(system: IntervalSystem) {
        if (world.systems.any { it::class == system::class }) {
            throw FleksSystemAlreadyAddedException(system::class)
        }
        world.systemService.systems += system
    }
}

/**
 * A DSL class to configure [Injectable] of a [WorldConfiguration].
 */
@WorldCfgMarker
class InjectableConfiguration(private val world: World) {
    /**
     * Adds the specified [dependency] under the given [name] which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
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
     * Adds the specified dependency which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
     * Refer to [add]: the name is the simpleName of the class of the [dependency].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     * @throws [FleksInjectableTypeHasNoNameException] if the simpleName of the [dependency] is null.
     */
    inline fun <reified T : Any> add(dependency: T) {
        val key = T::class.simpleName ?: T::class.toString()
        add(key, dependency)
    }
}

/**
 * A DSL class to configure [FamilyListener] of a [WorldConfiguration].
 */
@WorldCfgMarker
class FamilyConfiguration(
    @PublishedApi
    internal val world: World
) {
    fun onAdd(
        family: Family,
        action: FamilyHook
    ) {
        if (world.systems.isNotEmpty()) {
            throw FleksWrongConfigurationOrderException()
        }

        if (family.addHook != null) {
            throw FleksHookAlreadyAddedException("addHook", "Family $family")
        }
        family.addHook = action
    }

    fun onRemove(
        family: Family,
        action: FamilyHook
    ) {
        if (world.systems.isNotEmpty()) {
            throw FleksWrongConfigurationOrderException()
        }

        if (family.removeHook != null) {
            throw FleksHookAlreadyAddedException("removeHook", "Family $family")
        }
        family.removeHook = action
    }
}

/**
 * A configuration for an entity [world][World] to define the initial maximum entity capacity,
 * the systems of the [world][World] and the systems' dependencies to be injected.
 * Additionally, you can define [ComponentListener] to define custom logic when a specific component is
 * added or removed from an [entity][Entity].
 */
@WorldCfgMarker
class WorldConfiguration(internal val world: World) {

    private val compCfg = ComponentConfiguration(world)
    private val systemCfg = SystemConfiguration(world)
    private val injectableCfg = InjectableConfiguration(world)
    private val familyCfg = FamilyConfiguration(world)

    fun injectables(cfg: InjectableConfiguration.() -> Unit) = injectableCfg.run(cfg)

    fun components(cfg: ComponentConfiguration.() -> Unit) = compCfg.run(cfg)

    fun families(cfg: FamilyConfiguration.() -> Unit) = familyCfg.run(cfg)

    fun systems(cfg: SystemConfiguration.() -> Unit) = systemCfg.run(cfg)
}

fun world(entityCapacity: Int = 512, cfg: WorldConfiguration.() -> Unit): World {
    val newWorld = World(entityCapacity)
    CURRENT_WORLD = newWorld

    try {
        WorldConfiguration(newWorld).run(cfg)
        // verify that there are no unused injectables
        val unusedInjectables = newWorld.injectables.filterValues { !it.used }.map { it.value.injObj::class }
        if (unusedInjectables.isNotEmpty()) {
            throw FleksUnusedInjectablesException(unusedInjectables)
        }
    } finally {
        CURRENT_WORLD = null
    }

    return newWorld
}

/**
 * A world to handle [entities][Entity] and [systems][IntervalSystem].
 *
 * @param entityCapacity the initial maximum capacity of entities.
 * @param injectables the injectables for any [system][IntervalSystem], [ComponentListener] or [FamilyListener].
 * @param componentFactory the factories to create components.
 * @param compListenerFactory the factories to create [ComponentListener].
 * @param famListenerFactory the factories to create [FamilyListener].
 * @param systemFactory the factories to create [systems][IntervalSystem].
 */
class World internal constructor(
    entityCapacity: Int,
) {
    @PublishedApi
    internal val injectables = mutableMapOf<String, Injectable>()

    /**
     * Returns the time that is passed to [update][World.update].
     * It represents the time in seconds between two frames.
     */
    var deltaTime = 0f
        private set

    @PublishedApi
    internal val systemService = SystemService()

    @PublishedApi
    internal val componentService = ComponentService(this)

    @PublishedApi
    internal val hookCtx = EntityHookContext(componentService)

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
    val systems: Array<IntervalSystem>
        get() = systemService.systems

    inline fun <reified T> inject(name: String = T::class.simpleName ?: T::class.toString()): T {
        val injectable = injectables[name] ?: throw FleksNoSuchInjectableException(name)
        injectable.used = true
        return injectable.injObj as T
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateContext].
     */
    inline fun entity(configuration: EntityCreateContext.(Entity) -> Unit = {}): Entity {
        return entityService.create(configuration)
    }

    /**
     * Updates an [entity] using the given [configuration] to add and remove components.
     */
    inline fun configure(entity: Entity, configuration: EntityUpdateContext.(Entity) -> Unit) {
        entityService.configure(entity, configuration)
    }

    /**
     * Removes the given [entity] from the world. The [entity] will be recycled and reused for
     * future calls to [World.entity].
     */
    fun remove(entity: Entity) {
        entityService.remove(entity)
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
    inline fun forEach(action: (Entity) -> Unit) {
        entityService.forEach(action)
    }

    /**
     * Returns the specified [system][IntervalSystem] of the world.
     *
     * @throws [FleksNoSuchSystemException] if there is no such [system][IntervalSystem].
     */
    inline fun <reified T : IntervalSystem> system(): T {
        return systemService.system()
    }

    inline operator fun <reified T : Component<*>> get(componentType: ComponentType<T>): ComponentsHolder<T> {
        return componentService.holder(componentType)
    }

    fun family(cfg: FamilyDefinition.() -> Unit): Family {
        return family(FamilyDefinition().apply(cfg))
    }

    @PublishedApi
    internal fun family(definition: FamilyDefinition): Family {
        if (definition.isEmpty()) {
            throw FleksFamilyException(definition)
        }

        val (defAll, defNone, defAny) = definition

        var family = allFamilies.find { it.allOf == defAll && it.noneOf == defNone && it.anyOf == defAny }
        if (family == null) {
            family = Family(defAll, defNone, defAny, this).apply {
                allFamilies += this
                // initialize a newly created family by notifying it for any already existing entity
                entityService.forEach { this.onEntityCfgChanged(it, entityService.compMasks[it.id]) }
            }
        }
        return family
    }

    /**
     * Returns a map that contains all [entities][Entity] and their components of this world.
     * The keys of the map are the entities.
     * The values are a list of components that a specific entity has. If the entity
     * does not have any components then the value is an empty list.
     */
    fun snapshot(): Map<Entity, List<Component<*>>> {
        val entityComps = mutableMapOf<Entity, List<Component<*>>>()

        entityService.forEach { entity ->
            val components = mutableListOf<Component<*>>()
            val compMask = entityService.compMasks[entity.id]
            compMask.forEachSetBit { cmpId ->
                components += componentService.holderByIndex(cmpId)[entity]
            }
            entityComps[entity] = components
        }

        return entityComps
    }

    /**
     * Returns a list that contains all components of the given [entity] of this world.
     * If the entity does not have any components then an empty list is returned.
     */
    fun snapshotOf(entity: Entity): List<Component<*>> {
        val comps = mutableListOf<Component<*>>()

        if (entity in entityService) {
            entityService.compMasks[entity.id].forEachSetBit { cmpId ->
                comps += componentService.holderByIndex(cmpId)[entity]
            }
        }

        return comps
    }

    /**
     * Loads the given [snapshot] of the world. This will first clear any existing
     * entity of the world. After that it will load all provided entities and components.
     * This will also notify [ComponentListener] and [FamilyListener].
     *
     * @throws FleksSnapshotException if a family iteration is currently in process.
     *
     * @throws [FleksNoSuchComponentException] if any of the components does not exist in the
     * world configuration.
     */
    fun loadSnapshot(snapshot: Map<Entity, List<Component<*>>>) {
        if (entityService.delayRemoval) {
            throw FleksSnapshotException("Snapshots cannot be loaded while a family iteration is in process")
        }

        // remove any existing entity and clean up recycled ids
        removeAll(true)
        if (snapshot.isEmpty()) {
            // snapshot is empty -> nothing to load
            return
        }

        // Set next entity id to the maximum provided id + 1.
        // All ids before that will be either created or added to the recycled
        // ids to guarantee that the provided snapshot entity ids match the newly created ones.
        with(entityService) {
            val maxId = snapshot.keys.maxOf { it.id }
            this.nextId = maxId + 1
            repeat(maxId + 1) {
                val entity = Entity(it)
                this.recycle(entity)
                val components = snapshot[entity]
                if (components != null) {
                    // components for entity are provided -> create it
                    // note that the id for the entity will be the recycled id from above
                    this.configure(this.create { }, components)
                }
            }
        }
    }

    /**
     * Updates all [enabled][IntervalSystem.enabled] [systems][IntervalSystem] of the world
     * using the given [deltaTime].
     */
    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime
        systemService.update()
    }

    /**
     * Removes all [entities][Entity] of the world and calls the [onDispose][IntervalSystem.onDispose] function of each system.
     */
    fun dispose() {
        entityService.removeAll()
        systemService.dispose()
    }


    @ThreadLocal
    companion object {
        @PublishedApi
        internal var CURRENT_WORLD: World? = null

        inline fun <reified T> inject(name: String = T::class.simpleName ?: "anonymous"): T =
            CURRENT_WORLD?.inject(name) ?: throw FleksWrongConfigurationUsageException()

        inline fun <reified T : Component<*>> mapper(componentType: ComponentType<T>): ComponentsHolder<T> =
            CURRENT_WORLD?.get(componentType) ?: throw FleksWrongConfigurationUsageException()

        fun family(cfg: FamilyDefinition.() -> Unit): Family =
            CURRENT_WORLD?.family(cfg) ?: throw FleksWrongConfigurationUsageException()
    }
}
