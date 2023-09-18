package com.github.quillraven.fleks.collection

import kotlin.math.max

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

    inline fun getOrNull(index: Int): T? {
        return values[index]
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

