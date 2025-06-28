import kotlin.jvm.JvmInline
import kotlin.time.Duration

interface ClockAccumulator<T> {
    operator fun plusAssign(value: T)
    operator fun minusAssign(value: T)
    operator fun div(value: T): IntervalRatio
    operator fun compareTo(other: T): Int
}

interface Clock<T> {
    val deltaTime: T

    fun createAccumulator(): ClockAccumulator<T>
    fun update(deltaTime: T)
    fun tickWithInterval(
        step: T,
        accumulator: ClockAccumulator<T>,
        onTick: () -> Unit,
        remainingDelta: (IntervalRatio) -> Unit
    )
}

object NoOpClockAccumulator: ClockAccumulator<Unit> {
    override fun plusAssign(value: Unit) = Unit
    override fun minusAssign(value: Unit) = Unit
    override fun div(value: Unit): IntervalRatio = IntervalRatio.Zero
    override fun compareTo(other: Unit): Int = 0
}

object NoOpClock : Clock<Unit> {
    override val deltaTime = Unit
    override fun createAccumulator(): ClockAccumulator<Unit> = NoOpClockAccumulator

    override fun update(deltaTime: Unit) = Unit
    override fun tickWithInterval(step: Unit, accumulator: ClockAccumulator<Unit>, onTick: () -> Unit, remainingDelta: (IntervalRatio) -> Unit) = Unit
}

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

class FloatClock : Clock<Float> {
    private var _deltaTime = 0f

    override val deltaTime: Float
        get() = _deltaTime

    override fun createAccumulator(): ClockAccumulator<Float> = FloatClockAccumulator()

    override fun update(deltaTime: Float) {
        _deltaTime = deltaTime
    }

    override fun tickWithInterval(
        step: Float,
        accumulator: ClockAccumulator<Float>,
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

class DurationClock : Clock<Duration> {
    private var _deltaTime = Duration.ZERO

    override val deltaTime: Duration
        get() = _deltaTime

    override fun createAccumulator(): ClockAccumulator<Duration> = DurationClockAccumulator()

    override fun update(deltaTime: Duration) {
        _deltaTime = deltaTime
    }

    override fun tickWithInterval(
        step: Duration,
        accumulator: ClockAccumulator<Duration>,
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

@JvmInline
value class IntervalRatio private constructor(val value: Float) {
    companion object {
        fun create(value: Float): IntervalRatio {
            require(value in 0f..1f) { "Value must be between 0 and 1 included, current value: $value" }
            return IntervalRatio(value)
        }
        val Zero = IntervalRatio(0f)
    }
}
