package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.EntityComparator

/**
 * Coroutines-friendly version of [IntervalSystem]
 *
 * A basic system of a [world][World] without a context to [entities][Entity].
 * It is mandatory to implement [onTick] which gets called whenever the system gets updated
 * according to its [interval][Interval].
 *
 * If the system uses a [Fixed] interval then [onAlpha] can be overridden in case interpolation logic is needed.
 *
 * @param interval the [interval][Interval] in which the system gets updated. Default is [EachFrame].
 * @param enabled defines if the system gets updated when the [world][World] gets updated. Default is true.
 */
abstract class SuspendableIntervalSystem(
    val interval: Interval = EachFrame,
    enabled: Boolean = true,
    /**
     * Returns the [world][World] to which this system belongs.
     */
    val world: World = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException()
) : EntityComponentContext(world.componentService), System {

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
    open suspend fun onUpdate() {
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
    abstract suspend fun onTick()

    /**
     * Optional function for interpolation logic when using a [Fixed] interval. This function is not called for
     * an [EachFrame] interval.
     *
     * @param alpha a value between 0 (inclusive) and 1 (exclusive) that describes the progress between two ticks.
     */
    open fun onAlpha(alpha: Float) = Unit

    /**
     * Optional function to dispose of any resources of the system if needed.
     * Gets called in reversed world's [systems][World.systems] order when the
     * world's [dispose][World.dispose] function is called.
     */
    open fun onDispose() = Unit
}


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
abstract class SuspendableIteratingSystem(
    val family: Family,
    protected val comparator: EntityComparator = EMPTY_COMPARATOR,
    protected val sortingType: SortingType = Automatic,
    interval: Interval = EachFrame,
    enabled: Boolean = true,
    world: World
) : SuspendableIntervalSystem(interval, enabled, world) {

    constructor(
        family: Family,
        comparator: EntityComparator = EMPTY_COMPARATOR,
        sortingType: SortingType = Automatic,
        interval: Interval = EachFrame,
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
    override suspend fun onTick() {
        onSort()

        family.suspendForEach { onTickEntity(it) }
    }

    /**
     * Function that contains the update logic for each [entity][Entity] of the system.
     */
    abstract suspend fun onTickEntity(entity: Entity)

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
        private val EMPTY_COMPARATOR = EntityComparator { _, _ -> 0 }
    }
}
