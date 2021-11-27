package com.github.quillraven.fleks.benchmark

import com.github.quillraven.fleks.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

data class FleksPosition(var x: Float = 0f, var y: Float = 0f)

data class FleksLife(var life: Float = 0f)

data class FleksSprite(var path: String = "", var animationTime: Float = 0f)

@AllOf([FleksPosition::class])
class FleksSystemSimple(
    private val positions: ComponentMapper<FleksPosition>
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) {
        positions[entity].x++
    }
}

@AllOf([FleksPosition::class])
@NoneOf([FleksLife::class])
@AnyOf([FleksSprite::class])
class FleksSystemComplex1(
    private val positions: ComponentMapper<FleksPosition>,
    private val lifes: ComponentMapper<FleksLife>,
    private val sprites: ComponentMapper<FleksSprite>
) : IteratingSystem() {
    private var actionCalls = 0

    override fun onTickEntity(entity: Entity) {
        if (actionCalls % 2 == 0) {
            positions[entity].x++
            configureEntity(entity) { lifes.add(it) }
        } else {
            configureEntity(entity) { positions.remove(it) }
        }
        sprites[entity].animationTime++
        ++actionCalls
    }
}

@AnyOf([FleksPosition::class, FleksLife::class, FleksSprite::class])
class FleksSystemComplex2(
    private val positions: ComponentMapper<FleksPosition>,
    private val lifes: ComponentMapper<FleksLife>,
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) {
        configureEntity(entity) {
            lifes.remove(it)
            positions.add(it)
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateAddRemove {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateSimple {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES
            system<FleksSystemSimple>()
        }

        repeat(NUM_ENTITIES) {
            world.entity { add<FleksPosition>() }
        }
    }
}

@State(Scope.Benchmark)
open class FleksStateComplex {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World {
            entityCapacity = NUM_ENTITIES
            system<FleksSystemComplex1>()
            system<FleksSystemComplex2>()
        }

        repeat(NUM_ENTITIES) {
            world.entity {
                add<FleksPosition>()
                add<FleksSprite>()
            }
        }
    }
}

@Fork(1)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class FleksBenchmark {
    @Benchmark
    fun addRemove(state: FleksStateAddRemove) {
        repeat(NUM_ENTITIES) {
            state.world.entity { add<FleksPosition>() }
        }
        repeat(NUM_ENTITIES) {
            state.world.remove(Entity(it))
        }
    }

    @Benchmark
    fun simple(state: FleksStateSimple) {
        repeat(WORLD_UPDATES) {
            state.world.update(1f)
        }
    }

    @Benchmark
    fun complex(state: FleksStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.update(1f)
        }
    }
}
