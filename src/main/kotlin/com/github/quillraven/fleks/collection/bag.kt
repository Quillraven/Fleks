package com.github.quillraven.fleks.collection

import kotlin.math.max

inline fun <reified T> bag(capacity: Int = 64): Bag<T> {
    return Bag(arrayOfNulls(capacity))
}

class Bag<T>(
    @PublishedApi
    internal var values: Array<T?>
) {
    var size: Int = 0
        private set

    fun add(value: T) {
        if (size == values.size) {
            values = values.copyOf(size * 2)
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
        return values[index] ?: throw NoSuchElementException("Bag has no value at index $index")
    }

    fun remove(index: Int): T {
        val value = values[index] ?: throw NoSuchElementException("Bag has no value at index $index")
        values[index] = values[--size]
        values[size] = null
        return value
    }

    fun remove(value: T): Boolean {
        for (i in values.indices) {
            if (values[i] == value) {
                values[i] = values[--size]
                values[size] = null
                return true
            }
        }
        return false
    }

    inline fun forEach(action: (T) -> Unit) {
        values.forEach {
            if (it != null) {
                action(it)
            }
        }
    }
}
