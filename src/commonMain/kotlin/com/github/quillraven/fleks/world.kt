package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.CURRENT_WORLD
import com.github.quillraven.fleks.collection.BitArray
import kotlin.native.concurrent.ThreadLocal

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used in the [SystemService] to find out any unused injectables.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

@DslMarker
annotation class ComponentCfgMarker

/**
 * A DSL class to configure components and [ComponentListener] of a [WorldConfiguration].
 */
@ComponentCfgMarker
class ComponentConfiguration(
    @PublishedApi
    internal val world: World
) {
    inline fun <reified T : Any> onAdd(
        componentType: ComponentType<T>,
        noinline action: (World, Entity, T) -> Unit
    ) {
        world[componentType].addHook = action
    }

    inline fun <reified T : Any> onRemove(
        componentType: ComponentType<T>,
        noinline action: (World, Entity, T) -> Unit
    ) {
        world[componentType].removeHook = action
    }
}

@DslMarker
annotation class SystemCfgMarker

/**
 * A DSL class to configure [IntervalSystem] of a [WorldConfiguration].
 */
@SystemCfgMarker
class SystemConfiguration(private val world: World) {
    fun add(system: IntervalSystem) {
        if (system in world.systems) {
            throw FleksSystemAlreadyAddedException(system::class)
        }
        world.systemService.systems += system
    }
}

@DslMarker
annotation class InjectableCfgMarker

/**
 * A DSL class to configure [Injectable] of a [WorldConfiguration].
 */
@InjectableCfgMarker
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
     * @throws [FleksInjectableTypeHasNoName] if the simpleName of the [dependency] is null.
     */
    inline fun <reified T : Any> add(dependency: T) {
        val key = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        add(key, dependency)
    }
}

@DslMarker
annotation class FamilyCfgMarker

/**
 * A DSL class to configure [FamilyListener] of a [WorldConfiguration].
 */
@FamilyCfgMarker
class FamilyConfiguration(
    @PublishedApi
    internal val world: World
) {
    inline fun onAdd(
        familyDefinition: FamilyDefinition,
        noinline action: (World, Entity) -> Unit
    ) {
        world.familyOfDefinition(familyDefinition).addHook = action
    }

    inline fun onRemove(
        familyDefinition: FamilyDefinition,
        noinline action: (World, Entity) -> Unit
    ) {
        world.familyOfDefinition(familyDefinition).removeHook = action
    }
}

@DslMarker
annotation class WorldCfgMarker

/**
 * A configuration for an entity [world][World] to define the initial maximum entity capacity,
 * the systems of the [world][World] and the systems' dependencies to be injected.
 * Additionally, you can define [ComponentListener] to define custom logic when a specific component is
 * added or removed from an [entity][Entity].
 */
@WorldCfgMarker
class WorldConfiguration(internal val world: World) {

    /**
     * Initial maximum entity capacity.
     * Will be used internally when a [world][World] is created to set the initial
     * size of some collections and to avoid slow resizing calls.
     */
    var entityCapacity = 512

    internal val compCfg = ComponentConfiguration(world)

    internal val systemCfg = SystemConfiguration(world)

    internal val injectableCfg = InjectableConfiguration(world)

    internal val familyCfg = FamilyConfiguration(world)

    fun components(cfg: ComponentConfiguration.() -> Unit) = compCfg.run(cfg)

    fun systems(cfg: SystemConfiguration.() -> Unit) = systemCfg.run(cfg)

    fun injectables(cfg: InjectableConfiguration.() -> Unit) = injectableCfg.run(cfg)

    fun families(cfg: FamilyConfiguration.() -> Unit) = familyCfg.run(cfg)
}

fun world(entityCapacity: Int = 512, cfg: WorldConfiguration.() -> Unit): World {
    CURRENT_WORLD = World(entityCapacity)
    WorldConfiguration(CURRENT_WORLD).run(cfg)

    // verify that there are no unused injectables
    val unusedInjectables = CURRENT_WORLD.injectables.filterValues { !it.used }.map { it.value.injObj::class }
    if (unusedInjectables.isNotEmpty()) {
        throw FleksUnusedInjectablesException(unusedInjectables)
    }
    return CURRENT_WORLD
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
    internal val entityService = EntityService(entityCapacity, componentService)

    /**
     * List of all [families][Family] of the world that are created either via
     * an [IteratingSystem] or via the world's [family] function to
     * avoid creating duplicates.
     */
    internal val allFamilies = mutableListOf<Family>()

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
    val systems: List<IntervalSystem>
        get() = systemService.systems

    init {
        // create and register FamilyListener
        // like ComponentListener this must happen before systems are created
        /*famListenerFactory.forEach {
            val (listenerType, factory) = it
            try {
                val listener = factory.invoke()
                FamilyListener.CURRENT_FAMILY.addFamilyListener(listener)
            } catch (e: Exception) {
                if (e is FleksFamilyException) {
                    throw FleksFamilyListenerCreationException(
                        listenerType,
                        "FamilyListener must define at least one of AllOf, NoneOf or AnyOf"
                    )
                }
                throw e
            }
        }*/
    }

    inline fun <reified T> inject(name: String = T::class.simpleName ?: "anonymous"): T {
        val injectable = injectables[name] ?: throw FleksNoSuchInjectable(name)
        injectable.used = true
        return injectable.injObj as T
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateCfg].
     */
    inline fun entity(configuration: EntityCreateCfg.(Entity) -> Unit = {}): Entity {
        return entityService.create(configuration)
    }

    /**
     * Updates an [entity] using the given [configuration] to add and remove components.
     */
    inline fun configure(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
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
    fun forEach(action: (Entity) -> Unit) {
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

    inline operator fun <reified T : Any> get(componentType: ComponentType<T>): ComponentMapper<T> {
        return componentService.mapper(componentType)
    }

    fun familyOfDefinition(definition: FamilyDefinition): Family {
        val allOf = definition.allOfComponents
        val noneOf = definition.noneOfComponents
        val anyOf = definition.anyOfComponents

        val allBs = if (allOf.isNullOrEmpty()) null else BitArray().apply { allOf.forEach { this.set(it.id) } }
        val noneBs = if (noneOf.isNullOrEmpty()) null else BitArray().apply { noneOf.forEach { this.set(it.id) } }
        val anyBs = if (anyOf.isNullOrEmpty()) null else BitArray().apply { anyOf.forEach { this.set(it.id) } }

        var family = allFamilies.find { it.allOf == allBs && it.noneOf == noneBs && it.anyOf == anyBs }
        if (family == null) {
            family = Family(allBs, noneBs, anyBs, this).apply {
                entityService.addEntityListener(this)
                allFamilies.add(this)
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
                components += componentService.mapperByIndex(cmpId)[entity] as Component<*>
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
                comps += componentService.mapperByIndex(cmpId)[entity] as Component<*>
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
        internal lateinit var CURRENT_WORLD: World

        inline fun <reified T> inject(name: String = T::class.simpleName ?: "anonymous"): T {
            return CURRENT_WORLD.inject(name)
        }
    }
}
