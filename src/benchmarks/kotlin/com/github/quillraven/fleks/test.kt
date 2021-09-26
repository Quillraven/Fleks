package com.github.quillraven.fleks

import com.artemis.WorldConfigurationBuilder
import kotlin.system.measureTimeMillis

fun main() {
    val artemisTimes = mutableListOf<Long>()
    repeat(100) {
        val world = com.artemis.World(WorldConfigurationBuilder().run {
            build()
        })
        artemisTimes.add(measureTimeMillis {
            repeat(NUM_ENTITIES) {
                world.createEntity().edit().create(ArtemisPosition::class.java)
            }
        })
    }

    val fleksTimes = mutableListOf<Long>()
    repeat(100) {
        val world = World {
            entityCapacity = NUM_ENTITIES
        }
        fleksTimes.add(measureTimeMillis {
            repeat(NUM_ENTITIES) {
                world.entity { add<FleksPosition>() }
            }
        })
    }

    println(
        """
        Artemis: max(${artemisTimes.maxOrNull()})    min(${artemisTimes.minOrNull()})  avg(${artemisTimes.average()})
        Fleks:   max(${fleksTimes.maxOrNull()})    min(${fleksTimes.minOrNull()})  avg(${fleksTimes.average()})
    """.trimIndent()
    )
}

/*
    Fleks:      3033010
    Artemis:    1419843
 */
