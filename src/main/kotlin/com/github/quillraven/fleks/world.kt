package com.github.quillraven.fleks

import kotlin.reflect.KClass

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
    internal val systemTypes = mutableListOf<KClass<out IntervalSystem>>()

    @PublishedApi
    internal val injectables = mutableMapOf<KClass<*>, Any>()

    @PublishedApi
    internal val cmpListeners = mutableMapOf<KClass<*>, MutableList<ComponentListener<out Any>>>()

    /**
     * Adds the specified [IntervalSystem] to the [world][World].
     * The order in which systems are added is the order in which they will be executed when calling [World.update].
     *
     * @throws [FleksSystemAlreadyAddedException] if the system was already added before.
     */
    inline fun <reified T : IntervalSystem> system() {
        val systemType = T::class
        if (systemType in systemTypes) {
            throw FleksSystemAlreadyAddedException(systemType)
        }
        systemTypes.add(systemType)
    }

    /**
     * Adds the specified dependency which can then be injected to any [IntervalSystem].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     */
    inline fun <reified T : Any> inject(dependency: T) {
        val injectType = T::class
        if (injectType in injectables) {
            throw FleksInjectableAlreadyAddedException(injectType)
        }
        injectables[injectType] = dependency
    }

    /**
     * Adds the specified [listener] as a [ComponentListener] of the [world][World].
     */
    inline fun <reified T : Any> componentListener(listener: ComponentListener<T>) {
        val listeners = cmpListeners.getOrPut(T::class) { mutableListOf() }
        if (listener in listeners) {
            throw FleksComponentListenerAlreadyAddedException(listener)
        }
        listeners.add(listener)
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

    internal val componentService = ComponentService()

    @PublishedApi
    internal val entityService: EntityService

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

    init {
        val worldCfg = WorldConfiguration().apply(cfg)
        entityService = EntityService(worldCfg.entityCapacity, componentService)
        systemService = SystemService(this, worldCfg.systemTypes, worldCfg.injectables)
        worldCfg.cmpListeners.forEach { (type, listeners) ->
            val mapper = componentService.mapper(type)
            listeners.forEach { mapper.addComponentListenerInternal(it) }
        }
    }

    /**
     * Adds a new [entity][Entity] to the world using the given [configuration][EntityCreateCfg].
     */
    inline fun entity(configuration: EntityCreateCfg.() -> Unit = {}): Entity {
        return entityService.create(configuration)
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
     * Returns the specified [system][IntervalSystem] of the world.
     *
     * @throws [FleksNoSuchSystemException] if there is no such [system][IntervalSystem].
     */
    inline fun <reified T : IntervalSystem> system(): T {
        return systemService.system()
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

    companion object {
        internal val EMPTY_WORLD = World { entityCapacity = 0 }
    }
}
