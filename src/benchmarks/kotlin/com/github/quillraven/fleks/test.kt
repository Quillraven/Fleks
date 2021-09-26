package com.github.quillraven.fleks

import com.artemis.WorldConfigurationBuilder
import kotlin.reflect.full.createInstance
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main() {
    val kClass = FleksPosition::class
    val constructor = FleksPosition::class.java.getConstructor()
    val lambda = { kClass.createInstance() }
    kClass.createInstance()
    println(measureNanoTime { kClass.createInstance() })
    println(measureNanoTime { constructor.newInstance() })
    println(measureNanoTime { lambda() })


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
Artemis: max(31)    min(0)  avg(1.25)
Fleks:   max(31)    min(0)  avg(2.65)
 */
