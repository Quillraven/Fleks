package com.github.quillraven.fleks

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

        world.initAggregatedFamilyHooks()
        world.systems.forEach { it.onInit() }
    }
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
    World.CURRENT_WORLD = newWorld

    try {
        WorldConfiguration(newWorld).apply(cfg).configure()
    } finally {
        World.CURRENT_WORLD = null
    }

    return newWorld
}
