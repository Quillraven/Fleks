package com.github.quillraven.fleks

import Clock
import ClockAccumulator
import IntervalRatio
import com.github.quillraven.fleks.collection.EntityComparator

/**
 * An interval for an [IntervalSystem]. There are two kinds of intervals:
 * - [EachFrame]
 * - [Fixed]
 *
 * [EachFrame] means that the [IntervalSystem] is updated every time the [world][World] gets updated.
 * [Fixed] means that the [IntervalSystem] is updated at a fixed rate given in seconds.
 */
sealed interface Interval<T>

/**
 * @param step the time in seconds when an [IntervalSystem] gets updated.
 */
data class Fixed<T>(val step: T) : Interval<T>

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
abstract class IntervalSystem<T>(
    val interval: Interval<T>? = null,
    enabled: Boolean = true,
    /**
     * Returns the [world][World] to which this system belongs.
     */
    val world: GenericWorld = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException()
) : EntityComponentContext(world.componentService) {

    private lateinit var _clock: Clock<T>
    private lateinit var accumulator: ClockAccumulator<T>

    internal fun injectClock(clock: Clock<T>) {
        _clock = clock
        accumulator = clock.createAccumulator()
    }

    val clock: Clock<T>
        get() = _clock

    var enabled: Boolean = enabled
        set(value) {
            if (value == field) {
                return
            }

            field = value
            if (value) {
                onEnable()
            } else {
                onDisable()
            }
        }

    /**
     * This function gets called when the [world configuration][WorldConfiguration.configure] is completed.
     */
    open fun onInit() = Unit

    /**
     * This function gets called whenever the system gets [enabled].
     */
    open fun onEnable() = Unit

    /**
     * This function gets called whenever the system gets [disabled][enabled].
     */
    open fun onDisable() = Unit

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
            null -> onTick()
            is Fixed<T> -> {
                clock.tickWithInterval(
                    interval.step,
                    accumulator,
                    onTick = {
                        onTick()
                    },
                    remainingDelta = { deltaTime ->
                        onAlpha(deltaTime)
                    }
                )
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
    open fun onAlpha(alpha: IntervalRatio) = Unit

    /**
     * Optional function to dispose of any resources of the system if needed.
     * Gets called in reversed world's [systems][World.systems] order when the
     * world's [dispose][World.dispose] function is called.
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
data object Automatic : SortingType
data object Manual : SortingType

/**
 * An [IntervalSystem] of a [world][World] with a context to [entities][Entity].
 *
 * An IteratingSystem is always linked to exactly one [family][Family] which defines the [entities][Entity]
 * over which it will iterate.
 *
 * Refer to [FamilyDefinition] for more details.
 *
 * @param family the [Family] that is used for iteration.
 * @param comparator an optional [EntityComparator] that is used to sort [entities][Entity].
 * Default value is an empty comparator that means no sorting.
 * @param sortingType the [type][SortingType] of sorting for entities when using a [comparator].
 * @param interval the [interval][Interval] in which the system gets updated. Default is [EachFrame].
 * @param enabled defines if the system gets updated when the [world][World] gets updated. Default is true.
 */
abstract class IteratingSystem<T>(
    val family: Family,
    protected val comparator: EntityComparator = EMPTY_COMPARATOR,
    protected val sortingType: SortingType = Automatic,
    interval: Interval<T>? = null,
    enabled: Boolean = true,
    world: GenericWorld
) : IntervalSystem<T>(interval, enabled, world) {

    constructor(
        family: Family,
        comparator: EntityComparator = EMPTY_COMPARATOR,
        sortingType: SortingType = Automatic,
        interval: Interval<T>? = null,
        enabled: Boolean = true,
    ) : this(
        family,
        comparator,
        sortingType,
        interval,
        enabled,
        World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException()
    )

    /**
     * Flag that defines if sorting of [entities][Entity] will be performed the next time [onTick] is called.
     *
     * If a [comparator] is defined and [sortingType] is [Automatic] then this flag is always true.
     *
     * Otherwise, it must be set programmatically to perform sorting. The flag gets cleared after sorting.
     */
    var doSort = sortingType == Automatic && comparator != EMPTY_COMPARATOR

    /**
     * If [doSort] is true then [entities][Entity] are sorted using the [comparator] before calling [onTickEntity].
     */
    open fun onSort() {
        if (doSort) {
            doSort = sortingType == Automatic
            family.sort(comparator)
        }
    }

    /**
     * Updates the [family] if needed and calls [onTickEntity] for each [entity][Entity] of the [family].
     * Does entity sorting using [onSort] before calling [onTickEntity].
     */
    override fun onTick() {
        onSort()

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
    override fun onAlpha(alpha: IntervalRatio) {
        family.forEach { onAlphaEntity(it, alpha) }
    }

    /**
     * Optional function for interpolation logic for each [entity][Entity] of the system.
     *
     * @param alpha a value between 0 (inclusive) and 1 (exclusive) that describes the progress between two ticks.
     */
    open fun onAlphaEntity(entity: Entity, alpha: IntervalRatio) = Unit

    companion object {
        private val EMPTY_COMPARATOR = EntityComparator { _, _ -> 0 }
    }
}

/**
 * Any [IteratingSystem] having this interface will be triggered
 * by own [Family] similarly to [Family.addHook].
 */
interface FamilyOnAdd {
    /**
     * Gets called whenever an [entity][Entity] enters the family.
     */
    fun onAddEntity(entity: Entity)
}

/**
 * Any [IteratingSystem] having this interface will be triggered
 * by own [Family] similarly to [Family.removeHook].
 */
interface FamilyOnRemove {
    /**
     * Gets called whenever an [entity][Entity] leaves the family.
     * This function gets called in reversed world's [systems][World.systems] order.
     */
    fun onRemoveEntity(entity: Entity)
}
