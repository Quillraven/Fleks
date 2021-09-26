package com.github.quillraven.fleks

import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

data class FleksPosition(var x: Float = 0f, var y: Float = 0f)

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
            state.world.remove(it)
        }
    }
}
