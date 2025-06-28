
interface ClockAccumulator<T> {
    operator fun plusAssign(value: T)
    operator fun minusAssign(value: T)
    operator fun div(value: T): T
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
        remainingDelta: (T) -> Unit
    )
}

object NoOpClockAccumulator: ClockAccumulator<Unit> {
    override fun plusAssign(value: Unit) = Unit
    override fun minusAssign(value: Unit) = Unit
    override fun div(value: Unit) = Unit
    override fun compareTo(other: Unit): Int = 0
}

object NoOpClock : Clock<Unit> {
    override val deltaTime = Unit
    override fun createAccumulator(): ClockAccumulator<Unit> = NoOpClockAccumulator

    override fun update(deltaTime: Unit) = Unit
    override fun tickWithInterval(step: Unit, accumulator: ClockAccumulator<Unit>, onTick: () -> Unit, remainingDelta: (Unit) -> Unit) = Unit
}

internal class FloatClockAccumulator() : ClockAccumulator<Float> {

    private var accumulator = 0f

    override fun plusAssign(value: Float) {
        accumulator += value
    }

    override fun minusAssign(value: Float) {
        accumulator -= value
    }

    override fun div(value: Float): Float = accumulator.div(value)
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
        remainingDelta: (Float) -> Unit
    ) {
        accumulator += deltaTime
        while (accumulator >= step) {
            onTick()
            accumulator -= step
        }

        remainingDelta(accumulator / step)
    }
}
