import kotlin.jvm.JvmInline
import kotlin.time.Duration

/**
 * An interface representing an accumulator for clock intervals, supporting addition, subtraction,
 * division to calculate interval ratios, and comparison operations.
 *
 * @param T The type of the value being accumulated.
 */
interface ClockAccumulator<T> {
    /**
     * Adds the specified value to the current accumulator.
     *
     * @param value The value to be added to the current accumulator.
     */
    operator fun plusAssign(value: T)

    /**
     * Subtracts the specified value from the current accumulator.
     *
     * @param value The value to be subtracted from the current accumulator.
     */
    operator fun minusAssign(value: T)

    /**
     * Divides the current accumulator value by the specified value and returns the resulting interval ratio.
     *
     * @param value The value by which the current accumulator will be divided.
     * @return The resulting interval ratio as an [IntervalRatio].
     */
    operator fun div(value: T): IntervalRatio

    /**
     * Compares this object with the specified [other] object for order.
     * Returns a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified [other] object.
     *
     * @param other The object to be compared against.
     * @return A negative integer if this object is less than [other], zero if they are equal,
     *         or a positive integer if this object is greater than [other].
     */
    operator fun compareTo(other: T): Int
}

/**
 * Represents a clock interface capable of handling time progression and executing operations
 * at specified intervals. It allows tracking and updating of a time delta, providing mechanisms
 * to accumulate time and perform actions within defined intervals.
 *
 * @param T The type of the delta time value, typically a numeric type such as Float or Double.
 */
interface Clock<T> {
    /**
     * Represents the elapsed time since the last update or tick event.
     * This value is typically used for time progression calculations,
     * allowing for operations like animation updates or game loop processing
     * that depend on the passage of time.
     */
    val deltaTime: T

    /**
     * Creates and returns a new instance of a [ClockAccumulator] which facilitates
     * the accumulation of delta time values for processing time-based operations.
     *
     * @return A new [ClockAccumulator] instance.
     */
    fun createAccumulator(): ClockAccumulator<T>

    /**
     * Updates the clock with a given delta time value. This method is used to
     * incorporate the progression of time into the clock, enabling time-based
     * operations or updates like animations, simulations, or game states to
     * proceed based on the provided time increment.
     *
     * @param deltaTime The time increment to update the clock with. Typically
     * represents the elapsed time since the last update and is used for
     * time-based calculations.
     */
    fun update(deltaTime: T)
    /**
     * Progresses time in fixed intervals, performing actions on each tick, and provides
     * the ratio of remaining time as an interval.
     *
     * @param step The time interval representing the fixed step size for each tick.
     * @param accumulator The accumulator used to track the passage of time and ensure
     *        ticks are processed based on the accumulated delta time.
     * @param onTick A lambda function to execute on each tick. This is called every time
     *        the accumulated time exceeds or equals the given step interval.
     * @param remainingDelta A lambda function that receives the ratio of the remaining time
     *        (after all applicable ticks) to the step interval as an [IntervalRatio].
     */
    fun tickWithInterval(
        step: T,
        accumulator: ClockAccumulator<T>,
        onTick: () -> Unit,
        remainingDelta: (IntervalRatio) -> Unit
    ) {
        accumulator += deltaTime
        while (accumulator >= step) {
            onTick()
            accumulator -= step
        }

        remainingDelta(accumulator / step)
    }
}

/**
 * A no-op implementation of the [ClockAccumulator] interface for the [Unit] type.
 *
 * This implementation performs no operations for addition, subtraction, division,
 * or comparison, effectively acting as a placeholder or null object. It is designed
 * to have no side effects and return fixed values for all operations.
 */
object NoOpClockAccumulator : ClockAccumulator<Unit> {
    /**
     * Adds the specified value to the current accumulator.
     *
     * @param value The value to be added to the current accumulator.
     */
    override fun plusAssign(value: Unit) = Unit

    /**
     * Subtracts the specified value from the current accumulator.
     *
     * @param value The value to be subtracted from the current accumulator.
     */
    override fun minusAssign(value: Unit) = Unit

    /**
     * Divides the current accumulator value by the specified value and returns the resulting interval ratio.
     *
     * @param value The value by which the current accumulator will be divided.
     * @return The resulting interval ratio as an [IntervalRatio].
     */
    override fun div(value: Unit): IntervalRatio = IntervalRatio.Zero

    /**
     * Compares this object with the specified [other] object for order.
     * Returns a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified [other] object.
     *
     * @param other The object to be compared against.
     * @return A negative integer if this object is less than [other], zero if they are equal,
     *         or a positive integer if this object is greater than [other].
     */
    override fun compareTo(other: Unit): Int = 0
}

/**
 * A singleton implementation of the [Clock] interface that performs no operations.
 * This is useful when no actual time tracking is needed, or a neutral, non-functional
 * clock instance is required for certain use cases.
 *
 * This implementation provides fixed, no-op behaviors for all its methods:
 * - The `deltaTime` property always returns `Unit`.
 * - The `createAccumulator()` method returns a no-op accumulator.
 * - The `update()` and `tickWithInterval()` methods perform no actions.
 */
object NoOpClock : Clock<Unit> {
    /**
     * Represents the elapsed time since the last update or tick event.
     * This value is typically used for time progression calculations,
     * allowing for operations like animation updates or game loop processing
     * that depend on the passage of time.
     */
    override val deltaTime: Unit = Unit

    /**
     * Creates and returns a new instance of a [ClockAccumulator] which facilitates
     * the accumulation of delta time values for processing time-based operations.
     *
     * @return A new [ClockAccumulator] instance.
     */
    override fun createAccumulator(): ClockAccumulator<Unit> = NoOpClockAccumulator

    /**
     * Updates the clock with a given delta time value. This method is used to
     * incorporate the progression of time into the clock, enabling time-based
     * operations or updates like animations, simulations, or game states to
     * proceed based on the provided time increment.
     *
     * @param deltaTime The time increment to update the clock with. Typically
     * represents the elapsed time since the last update and is used for
     * time-based calculations.
     */
    override fun update(deltaTime: Unit) = Unit

    /**
     * Progresses time in fixed intervals, performing actions on each tick, and provides
     * the ratio of remaining time as an interval.
     *
     * @param step The time interval representing the fixed step size for each tick.
     * @param accumulator The accumulator used to track the passage of time and ensure
     *        ticks are processed based on the accumulated delta time.
     * @param onTick A lambda function to execute on each tick. This is called every time
     *        the accumulated time exceeds or equals the given step interval.
     * @param remainingDelta A lambda function that receives the ratio of the remaining time
     *        (after all applicable ticks) to the step interval as an [IntervalRatio].
     */
    override fun tickWithInterval(
        step: Unit,
        accumulator: ClockAccumulator<Unit>,
        onTick: () -> Unit,
        remainingDelta: (IntervalRatio) -> Unit
    ) = Unit
}

/**
 * A concrete implementation of [ClockAccumulator] for floating-point values.
 *
 * This class provides functionality to accumulate floating-point values, perform arithmetic
 * operations such as addition, subtraction, and division, and compare accumulator state
 * with other `Float` values. The division operation results in an [IntervalRatio] instance.
 */
internal class FloatClockAccumulator() : ClockAccumulator<Float> {

    private var accumulator = 0f

    override fun plusAssign(value: Float) {
        accumulator += value
    }

    override fun minusAssign(value: Float) {
        accumulator -= value
    }

    override fun div(value: Float): IntervalRatio = IntervalRatio.create(accumulator.div(value))
    override fun compareTo(other: Float): Int = accumulator.compareTo(other)
}

/**
 * A floating-point implementation of the [Clock] interface, handling time progression
 * and updates using `Float` values for delta time. Used to manage elapsed time in systems
 * such as animations, simulations, or game loops that require precise time tracking.
 */
class FloatClock : Clock<Float> {
    private var _deltaTime = 0f

    override val deltaTime: Float
        get() = _deltaTime

    override fun createAccumulator(): ClockAccumulator<Float> = FloatClockAccumulator()

    override fun update(deltaTime: Float) {
        _deltaTime = deltaTime
    }

}

/**
 * A concrete implementation of [ClockAccumulator] that accumulates and manipulates time durations using [Duration].
 *
 * This class maintains an internal accumulator initialized to [Duration.ZERO] and provides operations
 * for adding, subtracting, dividing, and comparing durations.
 *
 * ## Key Operations
 * - Add durations to the accumulator using the `plusAssign` operator.
 * - Subtract durations from the accumulator using the `minusAssign` operator.
 * - Divide the accumulated duration by another duration to compute an [IntervalRatio].
 * - Compare the accumulated duration with another [Duration] using `compareTo`.
 */
internal class DurationClockAccumulator() : ClockAccumulator<Duration> {

    private var accumulator = Duration.ZERO

    override fun plusAssign(value: Duration) {
        accumulator += value
    }

    override fun minusAssign(value: Duration) {
        accumulator -= value
    }

    override fun div(value: Duration): IntervalRatio = IntervalRatio.create(accumulator.div(value).toFloat())
    override fun compareTo(other: Duration): Int = accumulator.compareTo(other)
}

/**
 * A clock implementation for managing and updating time using [Duration].
 *
 * This class provides functionality for computing and tracking the progression
 * of time through a delta time value, which represents the elapsed time
 * since the last update. It utilizes a [DurationClockAccumulator] to accumulate
 * durations to facilitate time-based operations.
 *
 * ## Features
 * - Tracks a delta time as a [Duration], which can be accessed through the [deltaTime] property.
 * - Supports creating a [DurationClockAccumulator] for accumulating time intervals.
 * - Allows updating the clock with a new delta time through the [update] method.
 */
class DurationClock : Clock<Duration> {
    private var _deltaTime = Duration.ZERO

    override val deltaTime: Duration
        get() = _deltaTime

    override fun createAccumulator(): ClockAccumulator<Duration> = DurationClockAccumulator()

    override fun update(deltaTime: Duration) {
        _deltaTime = deltaTime
    }

}

/**
 * Represents a ratio within a specific interval [0, 1].
 *
 * This is a value class that ensures the encapsulated value is always
 * between 0 (inclusive) and 1 (inclusive). It provides a method to
 * create an instance of `IntervalRatio` using the `create` function, which
 * validates the input value.
 *
 * The class is designed to enforce type safety and ensure that operations
 * involving this ratio remain within the bounds of the defined interval.
 *
 * The `Zero` property represents the lower limit (0) of the permissible range.
 */
@JvmInline
value class IntervalRatio private constructor(val value: Float) {
    companion object {
        /**
         * Creates an instance of [IntervalRatio] ensuring that the provided value is within the interval [0, 1].
         *
         * @param value A floating-point number that must be between 0 (inclusive) and 1 (inclusive).
         * @return An [IntervalRatio] instance encapsulating the validated value.
         * @throws IllegalArgumentException If the value is not within the range [0, 1].
         */
        fun create(value: Float): IntervalRatio {
            require(value in 0f..1f) { "Value must be between 0 and 1 included, current value: $value" }
            return IntervalRatio(value)
        }

        /**
         * Represents the lower bound of the [IntervalRatio] interval (0.0).
         *
         * This constant defines the minimum valid value for an [IntervalRatio], which is 0.0.
         * It is a predefined instance of [IntervalRatio] initialized at the lower bound of the interval.
         */
        val Zero = IntervalRatio(0f)

        /**
         * Represents the upper bound of the [IntervalRatio] interval (1.0).
         *
         * This constant defines the maximum valid value for an [IntervalRatio], which is 1.0.
         * It is a predefined instance of [IntervalRatio] initialized at the upper bound of the interval.
         */
        val One = IntervalRatio(1f)
    }
}
