package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

/**
 * An optional annotation for an [IntervalSystem] constructor parameter to
 * inject a dependency exactly by that qualifier's [name].
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Qualifier(val name: String)

@DslMarker
annotation class WorldCfgMarker

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
@WorldCfgMarker
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
    internal val injectables = mutableMapOf<String, Injectable>()

    @PublishedApi
    internal val cmpListenerTypes = mutableListOf<KClass<out ComponentListener<out Any>>>()

    @PublishedApi
    internal val famListenerType = mutableListOf<KClass<out FamilyListener>>()

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
     * Adds the specified [dependency] under the given [name] which can then be injected to any [IntervalSystem].
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
     * Adds the specified dependency which can then be injected to any [IntervalSystem].
     * Refer to [inject]: the name is the qualifiedName of the class of the [dependency].
     *
     * @throws [FleksInjectableAlreadyAddedException] if the dependency was already added before.
     * @throws [FleksInjectableWithoutNameException] if the qualifiedName of the [dependency] is null.
     */
    inline fun <reified T : Any> inject(dependency: T) {
        val key = T::class.qualifiedName ?: throw FleksInjectableWithoutNameException()
        inject(key, dependency)
    }

    /**
     * Adds the specified [ComponentListener] to the [world][World].
     *
     * @throws [FleksComponentListenerAlreadyAddedException] if the listener was already added before.
     */
    inline fun <reified T : ComponentListener<out Any>> componentListener() {
        val listenerType = T::class
        if (listenerType in cmpListenerTypes) {
            throw FleksComponentListenerAlreadyAddedException(listenerType)
        }
        cmpListenerTypes.add(listenerType)
    }

    /**
     * Adds the specified [FamilyListener] to the [world][World].
     *
     * @throws [FleksFamilyListenerAlreadyAddedException] if the listener was already added before.
     */
    inline fun <reified T : FamilyListener> familyListener() {
        val listenerType = T::class
        if (listenerType in famListenerType) {
            throw FleksFamilyListenerAlreadyAddedException(listenerType)
        }
        famListenerType.add(listenerType)
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
    internal val componentService = ComponentService()

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

        val injectables = worldCfg.injectables
        // add the world as a used dependency in case any system or ComponentListener needs it
        injectables[World::class.qualifiedName!!] = Injectable(this, true)

        // It is important to create the EntityService before the SystemService
        // since this reference is assigned to newly created systems that are
        // created inside the SystemService below.
        entityService = EntityService(worldCfg.entityCapacity, componentService)

        // create and register ComponentListener
        // it is important to do this BEFORE creating systems because if a system's init block
        // is creating entities then ComponentListener already need to be registered to get notified
        worldCfg.cmpListenerTypes.forEach { listenerType ->
            val listener = newInstance(listenerType, componentService, injectables)
            val genInter = listener.javaClass.genericInterfaces.first {
                it is ParameterizedType && it.rawType == ComponentListener::class.java
            }
            val cmpType = (genInter as ParameterizedType).actualTypeArguments[0]
            val mapper = componentService.mapper((cmpType as Class<*>).kotlin)
            mapper.addComponentListenerInternal(listener)
        }

        // create and register FamilyListener
        // like ComponentListener this must happen before systems are created
        worldCfg.famListenerType.forEach { listenerType ->
            val allOfAnn = listenerType.annotation<AllOf>()
            val noneOfAnn = listenerType.annotation<NoneOf>()
            val anyOfAnn = listenerType.annotation<AnyOf>()
            val family = try {
                familyOfAnnotations(allOfAnn, noneOfAnn, anyOfAnn)
            } catch (e: FleksFamilyException) {
                throw FleksFamilyListenerCreationException(
                    listenerType,
                    "FamilyListener that are created by a world must define at least one of AllOf, NoneOf or AnyOf"
                )
            }
            val listener = newInstance(listenerType, componentService, injectables)
            family.addFamilyListener(listener)
        }

        // set a Fleks internal global reference to the current world that
        // gets created. This is used to correctly initialize the world
        // reference of any created system in the SystemService below.
        CURRENT_WORLD = this
        systemService = SystemService(this, worldCfg.systemTypes, injectables)

        // verify that there are no unused injectables
        val unusedInjectables = injectables.filterValues { !it.used }.map { it.value.injObj::class }
        if (unusedInjectables.isNotEmpty()) {
            throw FleksUnusedInjectablesException(unusedInjectables)
        }
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
     * @throws [FleksMissingNoArgsComponentConstructorException] if the component of the given type does not have
     * a no argument constructor.
     */
    inline fun <reified T : Any> mapper(): ComponentMapper<T> = componentService.mapper(T::class)

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
     *
     * @throws [FleksMissingNoArgsComponentConstructorException] if the [allOf], [noneOf] or [anyOf]
     * have a component type that does not have a no argument constructor.
     */
    fun family(
        allOf: Array<KClass<*>>? = null,
        noneOf: Array<KClass<*>>? = null,
        anyOf: Array<KClass<*>>? = null,
    ): Family {
        val allOfCmps = if (allOf != null && allOf.isNotEmpty()) {
            allOf.map { componentService.mapper(it) }
        } else {
            null
        }

        val noneOfCmps = if (noneOf != null && noneOf.isNotEmpty()) {
            noneOf.map { componentService.mapper(it) }
        } else {
            null
        }

        val anyOfCmps = if (anyOf != null && anyOf.isNotEmpty()) {
            anyOf.map { componentService.mapper(it) }
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
                entityService.forEach { this.onEntityCfgChanged(it, entityService.cmpMasks[it.id]) }
            }
        }
        return family
    }

    /**
     * Creates or returns an already created [family][Family] for the given
     * [allOf][AllOf], [anyOf][AnyOf] and [noneOf][NoneOf] annotations.
     *
     * @throws [FleksFamilyException] if the components of [allOf], [noneOf] and [anyOf] are null or empty.
     *
     * @throws [FleksMissingNoArgsComponentConstructorException] if the [AllOf], [NoneOf] or [AnyOf] annotations
     * have a component type that does not have a no argument constructor.
     */
    internal fun familyOfAnnotations(
        allOf: AllOf?,
        noneOf: NoneOf?,
        anyOf: AnyOf?,
    ): Family {
        val allOfCmps = if (allOf != null && allOf.components.isNotEmpty()) {
            allOf.components.map { componentService.mapper(it) }
        } else {
            null
        }

        val noneOfCmps = if (noneOf != null && noneOf.components.isNotEmpty()) {
            noneOf.components.map { componentService.mapper(it) }
        } else {
            null
        }

        val anyOfCmps = if (anyOf != null && anyOf.components.isNotEmpty()) {
            anyOf.components.map { componentService.mapper(it) }
        } else {
            null
        }

        return familyOfMappers(allOfCmps, noneOfCmps, anyOfCmps)
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
        internal lateinit var CURRENT_WORLD: World
    }
}
