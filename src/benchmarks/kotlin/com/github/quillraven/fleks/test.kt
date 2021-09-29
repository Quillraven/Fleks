package com.github.quillraven.fleks

import kotlin.system.measureTimeMillis

fun main() {
    /*
    val kClass = FleksPosition::class
    val constructor = FleksPosition::class.java.getConstructor()
    val lambda = { kClass.createInstance() }
    kClass.createInstance()
    println(measureNanoTime { kClass.createInstance() })
    println(measureNanoTime { constructor.newInstance() })
    println(measureNanoTime { lambda() })
    */


    val artemisTimes = mutableListOf<Long>()
    val artemisState = ArtemisStateComplex().apply { setup() }
    val artemisBm = ArtemisBenchmark()
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

/*
Artemis: max(31)    min(0)  avg(1.25)
Fleks:   max(31)    min(0)  avg(2.65)

Artemis: max(904)     min(728)   avg(795.6666666666666)
Fleks:   max(2501)    min(2109)  avg(2356.6666666666665)
 */
