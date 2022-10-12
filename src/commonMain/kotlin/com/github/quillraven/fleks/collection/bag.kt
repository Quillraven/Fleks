package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.*
import kotlin.math.max
import kotlin.math.min

/**
 * Creates a new [Bag] of the given [capacity] and type. Default capacity is 64.
 */
inline fun <reified T> bag(capacity: Int = 64): Bag<T> {
    return Bag(arrayOfNulls(capacity))
}

/**
 * A bag implementation in Kotlin containing only the necessary functions for Fleks.
 */
class Bag<T>(
    @PublishedApi
    internal var values: Array<T?>
) {
    var size: Int = 0
        private set

    val capacity: Int
        get() = values.size

    fun add(value: T) {
        if (size == values.size) {
            values = values.copyOf(max(1, size * 2))
        }
        values[size++] = value
    }

    operator fun set(index: Int, value: T) {
        if (index >= values.size) {
            values = values.copyOf(max(size * 2, index + 1))
        }
        size = max(size, index + 1)
        values[index] = value
    }

    operator fun get(index: Int): T {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index] ?: throw NoSuchElementException("Bag has no value at index $index")
    }

    fun hasNoValueAtIndex(index: Int): Boolean {
        return index >= size || values[index] == null
    }

    fun removeValue(value: T): Boolean {
        for (i in 0 until size) {
            if (values[i] == value) {
                values[i] = values[--size]
                values[size] = null
                return true
            }
        }
        return false
    }

    fun clear() {
        values.fill(null)
        size = 0
    }

    operator fun contains(value: T): Boolean {
        for (i in 0 until size) {
            if (values[i] == value) {
                return true
            }
        }
        return false
    }

    inline fun forEach(action: (T) -> Unit) {
        for (i in 0 until size) {
            values[i]?.let(action)
        }
    }
}

/**
 * A bag implementation for [entities][Entity](=integer) values in Kotlin to avoid autoboxing.
 * It contains only the necessary functions for Fleks.
 */
class EntityBag(
    size: Int = 64
) {
    @PublishedApi
    internal var values: Array<Entity> = Array(size) { Entity(-1) }

    var size: Int = 0
        private set

    val capacity: Int
        get() = values.size

    val isEmpty: Boolean
        get() = size == 0

    val isNotEmpty: Boolean
        get() = size > 0

    val first: Entity
        get() {
            if (isEmpty) {
                throw NoSuchElementException("Bag is empty!")
            }
            return values[0]
        }

    val firstOrNull: Entity?
        get() {
            if (isEmpty) {
                return null
            }
            return values[0]
        }

    fun add(value: Entity) {
        if (size == values.size) {
            values = values.copyInto(Array(max(1, size * 2)) { Entity(-1) })
        }
        values[size++] = value
    }

    internal fun unsafeAdd(value: Entity) {
        if (size >= values.size) throw IndexOutOfBoundsException("Cannot add value because of insufficient size")
        values[size++] = value
    }

    operator fun get(index: Int): Entity {
        if (index < 0 || index >= size) throw IndexOutOfBoundsException("$index is not valid for bag of size $size")
        return values[index]
    }

    fun clear() {
        values.fill(Entity(-1))
        size = 0
    }

    fun ensureCapacity(capacity: Int) {
        if (capacity >= values.size) {
            values = values.copyInto(Array(capacity + 1) { Entity(-1) })
        }
    }

    operator fun contains(value: Entity): Boolean {
        for (i in 0 until size) {
            if (values[i] == value) {
                return true
            }
        }
        return false
    }

    inline fun forEach(action: (Entity) -> Unit) {
        for (i in 0 until size) {
            action(values[i])
        }
    }

    fun sort(comparator: EntityComparator) {
        values.quickSort(0, size, comparator)
    }
}

/**
 * Sorting of int[] logic taken from: https://github.com/karussell/fastutil/blob/master/src/it/unimi/dsi/fastutil/ints/IntArrays.java
 */
interface EntityComparator {
    fun compare(entityA: Entity, entityB: Entity): Int
}

fun compareEntity(
    world: World = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException(),
    compareFun: World.(Entity, Entity) -> Int,
): EntityComparator {
    return object : EntityComparator {
        override fun compare(entityA: Entity, entityB: Entity): Int {
            return compareFun(world, entityA, entityB)
        }
    }
}

inline fun <reified T> compareEntityBy(
    componentType: ComponentType<T>,
    world: World = World.CURRENT_WORLD ?: throw FleksWrongConfigurationUsageException(),
): EntityComparator where T : Component<*>, T : Comparable<*> {
    return object : EntityComparator {
        private val holder = world.componentService.holder(componentType)

        @Suppress("UNCHECKED_CAST")
        override fun compare(entityA: Entity, entityB: Entity): Int {
            val valA: Comparable<T> = holder[entityA] as Comparable<T>
            val valB = holder[entityB]
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

private fun Array<Entity>.quickSort(fromIdx: Int, toIdx: Int, comparator: EntityComparator) {
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
