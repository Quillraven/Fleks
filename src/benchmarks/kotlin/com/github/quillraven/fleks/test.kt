package com.github.quillraven.fleks

import kotlin.reflect.full.createInstance
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main() {
    measureReflectionCreation()
    validateFleksBenchmark()
    compareArtemisFleks()
}

private fun measureReflectionCreation() {
    val kClass = FleksPosition::class
    val constructor = FleksPosition::class.java.getConstructor()
    val lambda = { kClass.createInstance() }
    kClass.createInstance()
    println(measureNanoTime { kClass.createInstance() })
    println(measureNanoTime { constructor.newInstance() })
    println(measureNanoTime { lambda() })
}

/*
Complex:
Artemis: max(987)    min(865)  avg(906.3333333333334)
Fleks:   max(1221)    min(1123)  avg(1162.6666666666667)
 */
private fun compareArtemisFleks() {
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
    repeat(3) {
        fleksTimes.add(measureTimeMillis { fleksBm.complex(fleksState) })
    }

    println(
        """
          Artemis: max(${artemisTimes.maxOrNull()})    min(${artemisTimes.minOrNull()})  avg(${artemisTimes.average()})
          Fleks:   max(${fleksTimes.maxOrNull()})    min(${fleksTimes.minOrNull()})  avg(${fleksTimes.average()})
      """.trimIndent()
    )
}

private fun validateFleksBenchmark() {
    val fleksState = FleksStateComplex().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.complex(fleksState)
    println("PAUSE FOR DEBUGGER")
}
