package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.*
import kotlin.math.min

/**
 * Sorting of `int[]` logic taken from: https://github.com/karussell/fastutil/blob/master/src/it/unimi/dsi/fastutil/ints/IntArrays.java
 */
typealias EntityComparator = Comparator<Entity>

fun compareEntity(
    world: World = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException(),
    compareFun: World.(Entity, Entity) -> Int,
): EntityComparator {
    return EntityComparator { entityA, entityB -> compareFun(world, entityA, entityB) }
}

inline fun <reified T> compareEntityBy(
    componentType: ComponentType<T>,
    world: World = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException(),
): EntityComparator where T : Component<T>, T : Comparable<T> {
    return object : EntityComparator {
        private val holder = world.componentService.holder(componentType)

        override fun compare(a: Entity, b: Entity): Int {
            val valA = holder[a]
            val valB = holder[b]
            return valA.compareTo(valB)
        }
    }
}

private const val SMALL = 7
private const val MEDIUM = 50
private fun Array<Entity>.swap(idxA: Int, idxB: Int) {
    val tmp = this[idxA]
    this[idxA] = this[idxB]
    this[idxB] = tmp
}

private fun Array<Entity>.vecSwap(idxA: Int, idxB: Int, n: Int) {
    var a = idxA
    var b = idxB
    for (i in 0 until n) {
        this.swap(a++, b++)
    }
}

private fun Array<Entity>.med3(idxA: Int, idxB: Int, idxC: Int, comparator: EntityComparator): Int {
    val ab = comparator.compare(this[idxA], this[idxB])
    val ac = comparator.compare(this[idxA], this[idxC])
    val bc = comparator.compare(this[idxB], this[idxC])

    return when {
        ab < 0 -> {
            when {
                bc < 0 -> idxB
                ac < 0 -> idxC
                else -> idxA
            }
        }

        bc > 0 -> idxB
        ac > 0 -> idxC
        else -> idxA
    }
}

private fun Array<Entity>.selectionSort(fromIdx: Int, toIdx: Int, comparator: EntityComparator) {
    for (i in fromIdx until toIdx - 1) {
        var m = i
        for (j in i + 1 until toIdx) {
            if (comparator.compare(this[j], this[m]) < 0) {
                m = j
            }
        }
        if (m != i) {
            this.swap(i, m)
        }
    }
}

internal fun Array<Entity>.quickSort(fromIdx: Int, toIdx: Int, comparator: EntityComparator) {
    val len = toIdx - fromIdx

    // Selection sort on smallest arrays
    if (len < SMALL) {
        this.selectionSort(fromIdx, toIdx, comparator)
        return
    }

    // Choose a partition element, v
    var m = fromIdx + len / 2 // Small arrays, middle element
    if (len > SMALL) {
        var l = fromIdx
        var n = toIdx - 1
        if (len > MEDIUM) {
            // Big arrays, pseudo median of 9
            val s = len / 8
            l = this.med3(l, l + s, l + 2 * s, comparator)
            m = this.med3(m - s, m, m + s, comparator)
            n = this.med3(n - 2 * s, n - s, n, comparator)
        }
        // Mid-size, med of 3
        m = this.med3(l, m, n, comparator)
    }

    val v = this[m]
    // Establish Invariant: v* (<v)* (>v)* v*
    var a = fromIdx
    var b = a
    var c = toIdx - 1
    var d = c
    while (true) {
        var comparison = 0

        while (b <= c && comparator.compare(this[b], v).also { comparison = it } <= 0) {
            if (comparison == 0) {
                this.swap(a++, b)
            }
            b++
        }

        while (c >= b && comparator.compare(this[c], v).also { comparison = it } >= 0) {
            if (comparison == 0) {
                this.swap(c, d--)
            }
            c--
        }

        if (b > c) {
            break
        }

        this.swap(b++, c--)
    }

    // Swap partition elements back to middle
    var s = min(a - fromIdx, b - a)
    this.vecSwap(fromIdx, b - s, s)
    s = min(d - c, toIdx - d - 1)
    this.vecSwap(b, toIdx - s, s)
    // Recursively sort non-partition-elements
    if ((b - a).also { s = it } > 1) {
        this.quickSort(fromIdx, fromIdx + s, comparator)
    }
    if ((d - c).also { s = it } > 1) {
        this.quickSort(toIdx - s, toIdx, comparator)
    }
}
