package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

/**
 * Wrapper class for injectables of the [WorldConfiguration].
 * It is used in the [SystemService] to find out any unused injectables.
 */
data class Injectable(val injObj: Any, var used: Boolean = false)

/**
 * A configuration for an entity [world][World] to define the initial maximum entity capacity,
 * the systems of the [world][World] and the systems' dependencies to be injected.
 * Additionally, you can define [ComponentListener] to define custom logic when a specific component is
 * added or removed from an [entity][Entity].
 */
class WorldConfiguration {
    /**
     * Initial maximum entity capacity.
     * Will be used internally when a [world][World] is created to set the initial
     * size of some collections and to avoid slow resizing calls.
     */
    var entityCapacity = 512

    @PublishedApi
    internal val systemFactory = mutableMapOf<KClass<*>, () -> IntervalSystem>()

    @PublishedApi
    internal val injectables = mutableMapOf<String, Injectable>()

    @PublishedApi
    internal val compListenerFactory = mutableMapOf<String, () -> ComponentListener<*>>()

    @PublishedApi
    internal val componentFactory = mutableMapOf<String, () -> Any>()

    @PublishedApi
    internal val famListenerFactory = mutableMapOf<KClass<out FamilyListener>, () -> FamilyListener>()

    /**
     * Adds the specified [IntervalSystem] to the [world][World].
     * The order in which systems are added is the order in which they will be executed when calling [World.update].
     *
     * @param factory A function which creates an object of type [T].
     * @throws [FleksSystemAlreadyAddedException] if the system was already added before.
     */
    inline fun <reified T : IntervalSystem> system(noinline factory: () -> T) {
        val systemType = T::class
        if (systemType in systemFactory) {
            throw FleksSystemAlreadyAddedException(systemType)
        }
        systemFactory[systemType] = factory
    }

    /**
     * Adds the specified [dependency] under the given [name] which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    fun <T : Any> inject(name: String, dependency: T) {
        if (name in injectables) {
            throw FleksInjectableAlreadyAddedException(name)
        }

        injectables[name] = Injectable(dependency)
    }

    /**
     * Adds the specified dependency which can then be injected to any [IntervalSystem], [ComponentListener] or [FamilyListener].
     * Refer to [inject]: the name is the simpleName of the class of the [dependency].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     * @throws [FleksInjectableTypeHasNoName] if the simpleName of the [dependency] is null.
     */
    inline fun <reified T : Any> inject(dependency: T) {
        val key = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        inject(key, dependency)
    }

    /**
     * Adds the specified component and its [ComponentListener] to the [world][World]. If a component listener
     * is not needed than it can be omitted.
     *
     * @param compFactory the constructor method for creating the component.
     * @param listenerFactory the constructor method for creating the component listener.
     * @throws [FleksComponentAlreadyAddedException] if the component was already added before.
     * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
     */
    inline fun <reified T : Any> component(
        noinline compFactory: () -> T,
        noinline listenerFactory: (() -> ComponentListener<T>)? = null
    ) {
        val compType = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)

        if (compType in componentFactory) {
            throw FleksComponentAlreadyAddedException(compType)
        }
        componentFactory[compType] = compFactory
        if (listenerFactory != null) {
            // No need to check compType again in compListenerFactory - it is already guarded with check in componentFactory
            compListenerFactory[compType] = listenerFactory
        }
    }

    /**
     * Adds the specified [FamilyListener] to the [world][World].
     *
     * @param listenerFactory the constructor method for creating the FamilyListener.
     * @throws [FleksFamilyListenerAlreadyAddedException] if the listener was already added before.
     */
    inline fun <reified T : FamilyListener> familyListener(
        noinline listenerFactory: (() -> T)
    ) {
        val listenerType = T::class
        if (listenerType in famListenerFactory) {
            throw FleksFamilyListenerAlreadyAddedException(listenerType)
        }
        famListenerFactory[listenerType] = listenerFactory
    }
}

/**
 * A world to handle [entities][Entity] and [systems][IntervalSystem].
 *
 * @param cfg the [configuration][WorldConfiguration] of the world containing the initial maximum entity capacity
 * and the [systems][IntervalSystem] to be processed.
 */
class World(
    cfg: WorldConfiguration.() -> Unit
) {
    /**
     * Returns the time that is passed to [update][World.update].
     * It represents the time in seconds between two frames.
     */
    var deltaTime = 0f
        private set

    @PublishedApi
    internal val systemService: SystemService

    @PublishedApi
    internal val componentService: ComponentService

    @PublishedApi
    internal val entityService: EntityService

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
    val systems: Array<IntervalSystem>
        get() = systemService.systems

    init {
        val worldCfg = WorldConfiguration().apply(cfg)
        componentService = ComponentService(worldCfg.componentFactory)
        entityService = EntityService(worldCfg.entityCapacity, componentService)
        val injectables = worldCfg.injectables
        // add the world as a used dependency in case any system or ComponentListener needs it
        injectables["World"] = Injectable(this, true)

        // set a Fleks internal global reference to the current world that
        // gets created. This is used to correctly initialize the world
        // reference of any created system and to correctly register
        // any created FamilyListener below.
        CURRENT_WORLD = this

        Inject.injectObjects = injectables
        Inject.mapperObjects = componentService.mappers

        // create and register ComponentListener
        // it is important to do this BEFORE creating systems because if a system's init block
        // is creating entities then ComponentListener already need to be registered to get notified
        worldCfg.compListenerFactory.forEach {
            val compType = it.key
            val listener = it.value.invoke()
            val mapper = componentService.mapper(compType)
            mapper.addComponentListenerInternal(listener)
        }

        // create and register FamilyListener
        // like ComponentListener this must happen before systems are created
        worldCfg.famListenerFactory.forEach {
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
        }

        // create systems
        systemService = SystemService(worldCfg.systemFactory)

        // verify that there are no unused injectables
        val unusedInjectables = injectables.filterValues { !it.used }.map { it.value.injObj::class }
        if (unusedInjectables.isNotEmpty()) {
            throw FleksUnusedInjectablesException(unusedInjectables)
        }

        // clear dependencies at the end because they are no longer necessary,
        // and we don't want to keep a reference to them
        Inject.injectObjects = EMPTY_INJECTIONS
        Inject.mapperObjects = EMPTY_MAPPERS
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
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        entityService.configureEntity(entity, configuration)
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
     */
    fun removeAll() {
        entityService.removeAll()
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

    /**
     * Returns a [ComponentMapper] for the given type. If the mapper does not exist then it will be created.
     *
     * @throws [FleksNoSuchComponentException] if the component of the given type does not exist in the
     * world configuration.
     * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> mapper(): ComponentMapper<T> {
        val type = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        return componentService.mapper(type) as ComponentMapper<T>
    }

    /**
     * Creates a new [Family] for the given [allOf], [noneOf] and [anyOf] component configuration.
     *
     * This function internally either creates or reuses an already existing [family][Family].
     * In case a new [family][Family] gets created it will be initialized with any already existing [entity][Entity]
     * that matches its configuration.
     * Therefore, this might have a performance impact on the first call if there are a lot of entities in the world.
     *
     * As a best practice families should be created as early as possible, ideally during world creation.
     * Also, store the result of this function instead of calling this function multiple times with the same arguments.
     *
     * @throws [FleksFamilyException] if [allOf], [noneOf] and [anyOf] are null or empty.
     */
    fun family(
        allOf: Array<KClass<*>>? = null,
        noneOf: Array<KClass<*>>? = null,
        anyOf: Array<KClass<*>>? = null,
    ): Family {
        val allOfCmps = if (allOf != null && allOf.isNotEmpty()) {
            allOf.map {
                val type = it.simpleName ?: throw FleksInjectableTypeHasNoName(it)
                componentService.mapper(type)
            }
        } else {
            null
        }

        val noneOfCmps = if (noneOf != null && noneOf.isNotEmpty()) {
            noneOf.map {
                val type = it.simpleName ?: throw FleksInjectableTypeHasNoName(it)
                componentService.mapper(type)
            }
        } else {
            null
        }

        val anyOfCmps = if (anyOf != null && anyOf.isNotEmpty()) {
            anyOf.map {
                val type = it.simpleName ?: throw FleksInjectableTypeHasNoName(it)
                componentService.mapper(type)
            }
        } else {
            null
        }

        return familyOfMappers(allOfCmps, noneOfCmps, anyOfCmps)
    }

    /**
     * Creates or returns an already created [family][Family] for the given
     * [allOf], [noneOf] and [anyOf] component configuration.
     *
     * Also, adds a newly created [family][Family] as [EntityListener] and
     * initializes it by notifying it with any already existing [entity][Entity]
     * that matches its configuration.
     *
     * @throws [FleksFamilyException] if [allOf], [noneOf] and [anyOf] are null or empty.
     */
    private fun familyOfMappers(
        allOf: List<ComponentMapper<*>>?,
        noneOf: List<ComponentMapper<*>>?,
        anyOf: List<ComponentMapper<*>>?,
    ): Family {
        if ((allOf == null || allOf.isEmpty())
            && (noneOf == null || noneOf.isEmpty())
            && (anyOf == null || anyOf.isEmpty())
        ) {
            throw FleksFamilyException(allOf, noneOf, anyOf)
        }

        val allBs = if (allOf == null) null else BitArray().apply { allOf.forEach { this.set(it.id) } }
        val noneBs = if (noneOf == null) null else BitArray().apply { noneOf.forEach { this.set(it.id) } }
        val anyBs = if (anyOf == null) null else BitArray().apply { anyOf.forEach { this.set(it.id) } }

        var family = allFamilies.find { it.allOf == allBs && it.noneOf == noneBs && it.anyOf == anyBs }
        if (family == null) {
            family = Family(allBs, noneBs, anyBs, entityService).apply {
                entityService.addEntityListener(this)
                allFamilies.add(this)
                // initialize a newly created family by notifying it for any already existing entity
                entityService.forEach { this.onEntityCfgChanged(it, entityService.compMasks[it.id]) }
            }
        }
        return family
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
        private val EMPTY_INJECTIONS: Map<String, Injectable> = emptyMap()
        private val EMPTY_MAPPERS: Map<String, ComponentMapper<*>> = emptyMap()
        internal lateinit var CURRENT_WORLD: World
    }
}
