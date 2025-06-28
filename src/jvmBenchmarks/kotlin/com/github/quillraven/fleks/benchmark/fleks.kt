package com.github.quillraven.fleks.benchmark

import com.github.quillraven.fleks.*
import com.github.quillraven.fleks.World<*>.Companion.family
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

data class FleksPosition(var x: Float = 0f, var y: Float = 0f) : Component<FleksPosition> {
    override fun type() = FleksPosition

    companion object : ComponentType<FleksPosition>()
}

data class FleksLife(var life: Float = 0f) : Component<FleksLife> {
    override fun type() = FleksLife

    companion object : ComponentType<FleksLife>()
}

data class FleksSprite(var path: String = "", var animationTime: Float = 0f) : Component<FleksSprite> {
    override fun type() = FleksSprite

    companion object : ComponentType<FleksSprite>()
}

class FleksSystemSimple : IteratingSystem(family { all(FleksPosition) }) {

    override fun onTickEntity(entity: Entity) {
        entity[FleksPosition].x++
    }
}

class FleksSystemComplex1 : IteratingSystem(family { all(FleksPosition).none(FleksLife).any(FleksSprite) }) {
    private var actionCalls = 0

    override fun onTickEntity(entity: Entity) {
        if (actionCalls % 2 == 0) {
            entity[FleksPosition].x++
            entity.configure { it += FleksLife() }
        } else {
            entity.configure { it -= FleksPosition }
        }
        entity[FleksSprite].animationTime++
        ++actionCalls
    }
}

class FleksSystemComplex2 : IteratingSystem(family { any(FleksPosition, FleksLife, FleksSprite) }) {

    override fun onTickEntity(entity: Entity) {
        entity.configure {
            it -= FleksLife
            it += FleksPosition()
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateAddRemove {
    lateinit var world: World<*>

    @Setup(value = Level.Iteration)
    fun setup() {
        world = configureWorld(NUM_ENTITIES) { }
    }
}

@State(Scope.Benchmark)
open class FleksStateSimple {
    lateinit var world: World<*>

    @Setup(value = Level.Iteration)
    fun setup() {
        world = configureWorld(NUM_ENTITIES) {
            systems {
                add(FleksSystemSimple())
            }
        }

        repeat(NUM_ENTITIES) {
            world.entity {
                it += FleksPosition()
            }
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateComplex {
    lateinit var world: World<*>

    @Setup(value = Level.Iteration)
    fun setup() {
        world = configureWorld(NUM_ENTITIES) {
            systems {
                add(FleksSystemComplex1())
                add(FleksSystemComplex2())
            }
        }

        repeat(NUM_ENTITIES) {
            world.entity {
                it += FleksPosition()
                it += FleksSprite()
            }
        }
    }
}

@Fork(value = WARMUPS)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class FleksBenchmark {
    @Benchmark
    fun addRemove(state: FleksStateAddRemove) {
        repeat(NUM_ENTITIES) {
            state.world.entity {
                it += FleksPosition()
            }
        }
        
        state.world.removeAll()
    }

    @Benchmark
    fun simple(state: FleksStateSimple) {
        repeat(WORLD_UPDATES) {
            state.world.update()
        }
    }

    @Benchmark
    fun complex(state: FleksStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.update()
        }
    }
}
