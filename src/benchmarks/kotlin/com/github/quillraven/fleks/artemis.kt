package com.github.quillraven.fleks

import com.artemis.Component
import com.artemis.World
import com.artemis.WorldConfigurationBuilder
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
}
