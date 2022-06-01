package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.EntityComparator
import kotlin.reflect.KClass

/**
 * An interval for an [IntervalSystem]. There are two kind of intervals:
 * - [EachFrame]
 * - [Fixed]
 *
 * [EachFrame] means that the [IntervalSystem] is updated every time the [world][World] gets updated.
 * [Fixed] means that the [IntervalSystem] is updated at a fixed rate given in seconds.
 */
sealed interface Interval
object EachFrame : Interval

/**
 * @param step the time in seconds when an [IntervalSystem] gets updated.
 */
data class Fixed(val step: Float) : Interval

/**
 * A basic system of a [world][World] without a context to [entities][Entity].
 * It is mandatory to implement [onTick] which gets called whenever the system gets updated
 * according to its [interval][Interval].
 *
 * If the system uses a [Fixed] interval then [onAlpha] can be overridden in case interpolation logic is needed.
 *
 * @param interval the [interval][Interval] in which the system gets updated. Default is [EachFrame].
 * @param enabled defines if the system gets updated when the [world][World] gets updated. Default is true.
 */
abstract class IntervalSystem(
    val interval: Interval = EachFrame,
    var enabled: Boolean = true,
) {
    /**
     * Returns the [world][World] to which this system belongs.
     */
    val world: World = World.CURRENT_WORLD

    private var accumulator: Float = 0.0f

    /**
     * Returns the time in seconds since the last time [onUpdate] was called.
     *
     * If the [interval] is [EachFrame] then the [world's][World] delta time is returned which is passed to [World.update].
     *
     * Otherwise, the [step][Fixed.step] value is returned.
     */
    val deltaTime: Float
        get() = if (interval is Fixed) interval.step else world.deltaTime

    /**
     * Updates the system according to its [interval]. This function gets called from [World.update] when
     * the system is [enabled].
     *
     * If the [interval] is [EachFrame] then [onTick] gets called.
     *
     * Otherwise, the world's [delta time][World.deltaTime] is analyzed and [onTick] is called at a fixed rate.
     * This could be multiple or zero times with a single call to [onUpdate]. At the end [onAlpha] is called.
     */
    open fun onUpdate() {
        when (interval) {
            is EachFrame -> onTick()
            is Fixed -> {
                accumulator += world.deltaTime
                val stepRate = interval.step
                while (accumulator >= stepRate) {
                    onTick()
                    accumulator -= stepRate
                }

                onAlpha(accumulator / stepRate)
            }
        }
    }

    /**
     * Function that contains the update logic of the system. Gets called whenever this system should get processed
     * according to its [interval].
     */
    abstract fun onTick()

    /**
     * Optional function for interpolation logic when using a [Fixed] interval. This function is not called for
     * an [EachFrame] interval.
     *
     * @param alpha a value between 0 (inclusive) and 1 (exclusive) that describes the progress between two ticks.
     */
    open fun onAlpha(alpha: Float) = Unit

    /**
     * Optional function to dispose any resources of the system if needed. Gets called when the world's [dispose][World.dispose]
     * function is called.
     */
    open fun onDispose() = Unit
}

/**
 * A sorting type for an [IteratingSystem]. There are two sorting options:
 * - [Automatic]
 * - [Manual]
 *
 * [Automatic] means that the sorting of [entities][Entity] is happening automatically each time
 * [IteratingSystem.onTick] gets called.
 *
 * [Manual] means that sorting must be called programmatically by setting [IteratingSystem.doSort] to true.
 * [Entities][Entity] are then sorted the next time [IteratingSystem.onTick] gets called.
 */
sealed interface SortingType
object Automatic : SortingType
object Manual : SortingType

/**
 * An [IntervalSystem] of a [world][World] with a context to [entities][Entity]. It must be linked to a
 * [family][Family] using at least one of the [AllOf], [AnyOf] or [NoneOf] annotations.
 *
 * @param comparator an optional [EntityComparator] that is used to sort [entities][Entity].
 * Default value is an empty comparator which means no sorting.
 * @param sortingType the [type][SortingType] of sorting for entities when using a [comparator].
 * @param interval the [interval][Interval] in which the system gets updated. Default is [EachFrame].
 * @param enabled defines if the system gets updated when the [world][World] gets updated. Default is true.
 */
abstract class IteratingSystem(
    private val comparator: EntityComparator = EMPTY_COMPARATOR,
    private val sortingType: SortingType = Automatic,
    interval: Interval = EachFrame,
    enabled: Boolean = true
) : IntervalSystem(interval, enabled) {
    /**
     * Returns the [family][Family] of this system.
     */
    val family: Family = Family.CURRENT_FAMILY

    /**
     * Returns the [entityService][EntityService] of this system.
     */
    @PublishedApi
    internal val entityService: EntityService = world.entityService

    /**
     * Flag that defines if sorting of [entities][Entity] will be performed the next time [onTick] is called.
     *
     * If a [comparator] is defined and [sortingType] is [Automatic] then this flag is always true.
     *
     * Otherwise, it must be set programmatically to perform sorting. The flag gets cleared after sorting.
     */
    var doSort = sortingType == Automatic && comparator != EMPTY_COMPARATOR

    /**
     * Updates an [entity] using the given [configuration] to add and remove components.
     */
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        entityService.configureEntity(entity, configuration)
    }

    /**
     * Updates the [family] if needed and calls [onTickEntity] for each [entity][Entity] of the [family].
     * If [doSort] is true then [entities][Entity] are sorted using the [comparator] before calling [onTickEntity].
     */
    override fun onTick() {
        if (doSort) {
            doSort = sortingType == Automatic
            family.sort(comparator)
        }
        family.forEach { onTickEntity(it) }
    }

    /**
     * Function that contains the update logic for each [entity][Entity] of the system.
     */
    abstract fun onTickEntity(entity: Entity)

    /**
     * Optional function for interpolation logic when using a [Fixed] interval. This function is not called for
     * an [EachFrame] interval. Calls [onAlphaEntity] for each [entity][Entity] of the system.
     *
     * @param alpha a value between 0 (inclusive) and 1 (exclusive) that describes the progress between two ticks.
     */
    override fun onAlpha(alpha: Float) {
        family.forEach { onAlphaEntity(it, alpha) }
    }

    /**
     * Optional function for interpolation logic for each [entity][Entity] of the system.
     *
     * @param alpha a value between 0 (inclusive) and 1 (exclusive) that describes the progress between two ticks.
     */
    open fun onAlphaEntity(entity: Entity, alpha: Float) = Unit

    companion object {
        private val EMPTY_COMPARATOR = object : EntityComparator {
            override fun compare(entityA: Entity, entityB: Entity): Int = 0
        }
    }
}

/**
 * A service class for any [IntervalSystem] of a [world][World]. It is responsible to create systems using
 * constructor dependency injection. It also stores [systems] and updates [enabled][IntervalSystem.enabled] systems
 * each time [update] is called.
 *
 * @param world the [world][World] the service belongs to.
 * @param systemTypes the [systems][IntervalSystem] to be created.
 * @param injectables the required dependencies to create the [systems][IntervalSystem].
 */
class SystemService(
    world: World,
    systemTypes: List<KClass<out IntervalSystem>>,
    injectables: Map<String, Injectable>
) {
    @PublishedApi
    internal val systems: Array<IntervalSystem>

    init {
        // create systems
        val cmpService = world.componentService
        systems = Array(systemTypes.size) { sysIdx ->
            val sysType = systemTypes[sysIdx]
            val allOfAnn = sysType.annotation<AllOf>()
            val noneOfAnn = sysType.annotation<NoneOf>()
            val anyOfAnn = sysType.annotation<AnyOf>()
            val hasFamilyAnnotations = allOfAnn != null || noneOfAnn != null || anyOfAnn != null
            if (hasFamilyAnnotations) {
                // create family for IteratingSystem that gets created below.
                // This is needed because if the user wants to access the family in the system's constructor
                // then it must be already set. Therefore, it must happen during creation and not afterwards.
                // We do this like we do it for the world via its CURRENT_WORLD global.
                Family.CURRENT_FAMILY = familyOfSystem(allOfAnn, noneOfAnn, anyOfAnn, world, cmpService)
            }

            val system = newInstance(sysType, cmpService, injectables)
            if (system is IteratingSystem && !hasFamilyAnnotations) {
                throw FleksSystemCreationException(
                    sysType,
                    "IteratingSystem must define at least one of AllOf, NoneOf or AnyOf"
                )
            }
            system
        }
    }

    /**
     * Returns [Annotation] of the specific type if the class has that annotation. Otherwise, returns null.
     */
    private inline fun <reified T : Annotation> KClass<*>.annotation(): T? {
        return this.java.getAnnotation(T::class.java)
    }

    /**
     * Creates or returns an already created [family][Family] for the given [IteratingSystem]
     * by analyzing the system's [AllOf], [AnyOf] and [NoneOf] annotations.
     *
     * @throws [FleksSystemCreationException] if the [IteratingSystem] does not contain at least one
     * [AllOf], [AnyOf] or [NoneOf] annotation.
     *
     * @throws [FleksMissingNoArgsComponentConstructorException] if the [AllOf], [NoneOf] or [AnyOf] annotations
     * of the system have a component type that does not have a no argument constructor.
     */
    private fun familyOfSystem(
        allOfAnn: AllOf?,
        noneOfAnn: NoneOf?,
        anyOfAnn: AnyOf?,
        world: World,
        cmpService: ComponentService
    ): Family {
        val allOfCmps = if (allOfAnn != null && allOfAnn.components.isNotEmpty()) {
            allOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        val noneOfCmps = if (noneOfAnn != null && noneOfAnn.components.isNotEmpty()) {
            noneOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        val anyOfCmps = if (anyOfAnn != null && anyOfAnn.components.isNotEmpty()) {
            anyOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        return world.familyOfMappers(allOfCmps, noneOfCmps, anyOfCmps)
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
     * Updates all [enabled][IntervalSystem.enabled] [systems][IntervalSystem] by calling
     * their [IntervalSystem.onUpdate] function.
     */
    fun update() {
        systems.forEach { system ->
            if (system.enabled) {
                system.onUpdate()
            }
        }
    }

    /**
     * Calls the [onDispose][IntervalSystem.onDispose] function of all [systems].
     */
    fun dispose() {
        systems.forEach { it.onDispose() }
    }
}
