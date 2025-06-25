package com.github.quillraven.fleks.benchmark

import com.github.quillraven.fleks.Component
import com.github.quillraven.fleks.ComponentType
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.IteratingSystem
import com.github.quillraven.fleks.World
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.configureWorld
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

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
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = configureWorld(NUM_ENTITIES) { }
    }
}

@State(Scope.Benchmark)
open class FleksStateSimple {
    lateinit var world: World

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
    lateinit var world: World

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
            state.world.update(1.seconds)
        }
    }

    @Benchmark
    fun complex(state: FleksStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.update(1.seconds)
        }
    }
}

