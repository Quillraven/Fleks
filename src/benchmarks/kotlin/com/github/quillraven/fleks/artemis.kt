package com.github.quillraven.fleks

import com.artemis.Component
import com.artemis.ComponentMapper
import com.artemis.World
import com.artemis.WorldConfigurationBuilder
import com.artemis.annotations.All
import com.artemis.annotations.Exclude
import com.artemis.annotations.One
import com.artemis.systems.IteratingSystem
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

data class ArtemisPosition(var x: Float = 0f, var y: Float = 0f) : Component()

data class ArtemisLife(var life: Float = 0f) : Component()

data class ArtemisSprite(var path: String = "", var animationTime: Float = 0f) : Component()

@All(ArtemisPosition::class)
class ArtemisSystemSimple : IteratingSystem() {
    private lateinit var mapper: ComponentMapper<ArtemisPosition>

    override fun process(entityId: Int) {
        mapper[entityId].x++
    }
}

@All(ArtemisPosition::class)
@Exclude(ArtemisLife::class)
@One(ArtemisSprite::class)
class ArtemisSystemComplex1 : IteratingSystem() {
    private var processCalls = 0
    private lateinit var mapper1: ComponentMapper<ArtemisPosition>
    private lateinit var mapper2: ComponentMapper<ArtemisLife>
    private lateinit var mapper3: ComponentMapper<ArtemisSprite>

    override fun process(entityId: Int) {
        if (processCalls % 2 == 0) {
            mapper1[entityId].x++
            mapper2.create(entityId)
        } else {
            mapper1.remove(entityId)
        }
        mapper3[entityId].animationTime++
        ++processCalls
    }
}

@One(ArtemisPosition::class, ArtemisLife::class, ArtemisSprite::class)
class ArtemisSystemComplex2 : IteratingSystem() {
    private lateinit var mapper1: ComponentMapper<ArtemisPosition>
    private lateinit var mapper2: ComponentMapper<ArtemisLife>

    override fun process(entityId: Int) {
        mapper2.remove(entityId)
        mapper1.create(entityId)
    }
}

@State(Scope.Benchmark)
open class ArtemisStateAddRemove {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World(WorldConfigurationBuilder().run {
            build()
        })
    }
}

@State(Scope.Benchmark)
open class ArtemisStateSimple {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World(WorldConfigurationBuilder().run {
            with(ArtemisSystemSimple())
            build()
        })

        repeat(NUM_ENTITIES) {
            world.createEntity().edit().create(ArtemisPosition::class.java)
        }
    }
}

@State(Scope.Benchmark)
open class ArtemisStateComplex {
    lateinit var world: World

    @Setup(value = Level.Iteration)
    fun setup() {
        world = World(WorldConfigurationBuilder().run {
            with(ArtemisSystemComplex1())
            with(ArtemisSystemComplex2())
            build()
        })

        repeat(NUM_ENTITIES) {
            val entityEdit = world.createEntity().edit()
            entityEdit.create(ArtemisPosition::class.java)
            entityEdit.create(ArtemisSprite::class.java)
        }
    }
}

@Fork(1)
@Warmup(iterations = WARMUPS)
@Measurement(iterations = ITERATIONS, time = TIME, timeUnit = TimeUnit.SECONDS)
open class ArtemisBenchmark {
    @Benchmark
    fun addRemove(state: ArtemisStateAddRemove) {
        repeat(NUM_ENTITIES) {
            state.world.createEntity().edit().create(ArtemisPosition::class.java)
        }
        repeat(NUM_ENTITIES) {
            state.world.delete(it)
        }
    }

    @Benchmark
    fun simple(state: ArtemisStateSimple) {
        repeat(WORLD_UPDATES) {
            state.world.delta = 1f
            state.world.process()
        }
    }

    @Benchmark
    fun complex(state: ArtemisStateComplex) {
        repeat(WORLD_UPDATES) {
            state.world.delta = 1f
            state.world.process()
        }
    }
}
