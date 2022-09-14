package com.github.quillraven.fleks.benchmark

import kotlin.system.measureTimeMillis

fun main() {
    compareArtemisFleksAddRemove()
    compareArtemisFleksSimple()
    compareArtemisFleksComplex()
    // profileComplex()
}

/*
ADD_REMOVE:
Artemis: max(17)    min(6)  avg(11.666666666666666)
Fleks:   max(18)    min(7)  avg(11.666666666666666)
 */
private fun compareArtemisFleksAddRemove() {
    val artemisTimes = mutableListOf<Long>()
    val artemisState = ArtemisStateAddRemove().apply { setup() }
    val artemisBm = ArtemisBenchmark()
    artemisBm.addRemove(artemisState)
    repeat(3) {
        artemisTimes.add(measureTimeMillis { artemisBm.addRemove(artemisState) })
    }

    val fleksTimes = mutableListOf<Long>()
    val fleksState = FleksStateAddRemove().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.addRemove(fleksState)
    repeat(3) {
        fleksTimes.add(measureTimeMillis { fleksBm.addRemove(fleksState) })
    }

    println(
        """
            ADD_REMOVE:
          Artemis: max(${artemisTimes.maxOrNull()})    min(${artemisTimes.minOrNull()})  avg(${artemisTimes.average()})
          Fleks:   max(${fleksTimes.maxOrNull()})    min(${fleksTimes.minOrNull()})  avg(${fleksTimes.average()})
      """.trimIndent()
    )
}

/*
SIMPLE:
Artemis: max(32)    min(30)  avg(31.0)
Fleks:   max(21)    min(15)  avg(17.333333333333332)
 */
private fun compareArtemisFleksSimple() {
    val artemisTimes = mutableListOf<Long>()
    val artemisState = ArtemisStateSimple().apply { setup() }
    val artemisBm = ArtemisBenchmark()
    artemisBm.simple(artemisState)
    repeat(3) {
        artemisTimes.add(measureTimeMillis { artemisBm.simple(artemisState) })
    }

    val fleksTimes = mutableListOf<Long>()
    val fleksState = FleksStateSimple().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.simple(fleksState)

    // verify benchmark
    assert(fleksState.world.numEntities == NUM_ENTITIES)
    fleksState.world.forEach { entity ->
        assert(fleksState.world[FleksPosition][entity].x == WORLD_UPDATES.toFloat())
    }

    repeat(3) {
        fleksTimes.add(measureTimeMillis { fleksBm.simple(fleksState) })
    }

    println(
        """
            SIMPLE:
          Artemis: max(${artemisTimes.maxOrNull()})    min(${artemisTimes.minOrNull()})  avg(${artemisTimes.average()})
          Fleks:   max(${fleksTimes.maxOrNull()})    min(${fleksTimes.minOrNull()})  avg(${fleksTimes.average()})
      """.trimIndent()
    )
}

/*
COMPLEX:
Artemis: max(744)    min(731)  avg(737.0)
Fleks:   max(924)    min(888)  avg(901.0)
 */
private fun compareArtemisFleksComplex() {
    val artemisTimes = mutableListOf<Long>()
    val artemisState = ArtemisStateComplex().apply { setup() }
    val artemisBm = ArtemisBenchmark()
    artemisBm.complex(artemisState)
    repeat(3) {
        artemisTimes.add(measureTimeMillis { artemisBm.complex(artemisState) })
    }

    val fleksTimes = mutableListOf<Long>()
    val fleksState = FleksStateComplex().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.complex(fleksState)
    repeat(3) {
        fleksTimes.add(measureTimeMillis { fleksBm.complex(fleksState) })
    }

    println(
        """
            COMPLEX:
          Artemis: max(${artemisTimes.maxOrNull()})    min(${artemisTimes.minOrNull()})  avg(${artemisTimes.average()})
          Fleks:   max(${fleksTimes.maxOrNull()})    min(${fleksTimes.minOrNull()})  avg(${fleksTimes.average()})
      """.trimIndent()
    )
}

fun profileComplex() {
    val fleksState = FleksStateComplex().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.complex(fleksState)
}
