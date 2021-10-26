package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.IntBag
import kotlin.reflect.full.createInstance
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun main() {
    measureReflectionCreation()
    measureBits()
    validateFleksBenchmark()
    compareArtemisFleksSimple()
    compareArtemisFleksComplex()
}

private fun measureReflectionCreation() {
    val kClass = FleksPosition::class
    val cstr1 = FleksPosition::class.java.getConstructor()
    val cstr2 = FleksPosition::class.java.getDeclaredConstructor()
    val lambda = { FleksPosition() }

    println("Kotlin createInstance  " + measureNanoTime { kClass.createInstance() })
    println("Java cstr              " + measureNanoTime { cstr1.newInstance() })
    println("Java declared cstr     " + measureNanoTime { cstr2.newInstance() })
    println("Kotlin lambda          " + measureNanoTime { lambda() })
}

/*
Complex:
Artemis: max(754)    min(688)    avg(727.6666666666666)
Fleks:   max(949)    min(840)    avg(893.0)
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

/*
Simple:
Artemis: max(31)    min(30)  avg(30.666666666666668)
Fleks:   max(51)    min(50)  avg(50.333333333333336)
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

private fun measureBits() {
    val bitArrayTimes = mutableListOf<Long>()
    repeat(3) {
        bitArrayTimes.add(measureTimeMillis {
            val bitArray = BitArray(NUM_ENTITIES)
            val bag = IntBag()
            repeat(NUM_ENTITIES) {
                bitArray.set(it)
            }
            bitArray.toIntBag(bag)
        })
    }

    val booleanTimes = mutableListOf<Long>()
    repeat(3) {
        booleanTimes.add(measureTimeMillis {
            val bits = BooleanArray(NUM_ENTITIES)
            val bag = IntBag()
            repeat(NUM_ENTITIES) {
                bits[it] = true
            }
            bits.forEachIndexed { idx, value ->
                if (value) {
                    bag.add(idx)
                }
            }
        })
    }

    println(
        """
            BITS:
          BitArray: max(${bitArrayTimes.maxOrNull()})    min(${bitArrayTimes.minOrNull()})  avg(${bitArrayTimes.average()})
          Boolean[]:   max(${booleanTimes.maxOrNull()})    min(${booleanTimes.minOrNull()})  avg(${booleanTimes.average()})
      """.trimIndent()
    )
}

private fun validateFleksBenchmark() {
    val fleksState = FleksStateComplex().apply { setup() }
    val fleksBm = FleksBenchmark()
    fleksBm.complex(fleksState)
    println("PAUSE FOR DEBUGGER")
}
