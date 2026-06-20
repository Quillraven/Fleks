package com.github.quillraven.fleks.collection

import kotlin.math.max

/**
 * A lightweight, growable primitive integer array wrapper.
 * Avoids any boxing or standard library list overhead.
 */
class IntBag(initialCapacity: Int = 64) {
    var data: IntArray = IntArray(initialCapacity)
        private set
    var size: Int = 0
        private set

    val isEmpty: Boolean inline get() = size == 0

    fun add(value: Int) {
        if (size >= data.size) {
            grow()
        }
        data[size++] = value
    }

    operator fun get(index: Int): Int = data[index]

    operator fun set(index: Int, value: Int) {
        if (index >= data.size) {
            grow(index + 1)
        }
        data[index] = value
        size = max(size, index + 1)
    }

    fun removeAt(index: Int): Int {
        val value = data[index]
        data[index] = data[--size]
        return value
    }

    fun clear() {
        size = 0
    }

    private fun grow(minCapacity: Int = data.size * 2) {
        var newCapacity = max(64, data.size * 2)
        while (newCapacity < minCapacity) {
            newCapacity *= 2
        }
        data = data.copyOf(newCapacity)
    }
}

/**
 * A lightweight, growable primitive Long array wrapper.
 * Avoids any boxing or standard library list overhead.
 */
class LongBag(initialCapacity: Int = 64) {
    var data: LongArray = LongArray(initialCapacity)
        private set
    var size: Int = 0
        private set

    val isEmpty: Boolean inline get() = size == 0

    fun add(value: Long) {
        if (size >= data.size) {
            grow()
        }
        data[size++] = value
    }

    operator fun get(index: Int): Long = data[index]

    operator fun set(index: Int, value: Long) {
        if (index >= data.size) {
            grow(index + 1)
        }
        data[index] = value
        size = max(size, index + 1)
    }

    fun removeAt(index: Int): Long {
        val value = data[index]
        data[index] = data[--size]
        return value
    }

    fun clear() {
        size = 0
    }

    private fun grow(minCapacity: Int = data.size * 2) {
        var newCapacity = max(64, data.size * 2)
        while (newCapacity < minCapacity) {
            newCapacity *= 2
        }
        data = data.copyOf(newCapacity)
    }
}


/**
 * A lightweight, growable object array wrapper.
 * Avoids standard library ArrayList overhead.
 */
@Suppress("UNCHECKED_CAST")
class Bag<T>(initialCapacity: Int = 64) {
    var data: Array<Any?> = arrayOfNulls(initialCapacity)
        private set
    var size: Int = 0
        private set

    val isEmpty: Boolean inline get() = size == 0

    fun add(value: T) {
        if (size >= data.size) {
            grow()
        }
        data[size++] = value
    }

    operator fun get(index: Int): T = data[index] as T

    operator fun set(index: Int, value: T) {
        if (index >= data.size) {
            grow(index + 1)
        }
        data[index] = value
        size = max(size, index + 1)
    }

    fun removeAt(index: Int): T {
        val value = data[index]
        data[index] = data[--size]
        data[size] = null
        return value as T
    }

    fun clear() {
        data.fill(null, 0, size)
        size = 0
    }

    private fun grow(minCapacity: Int = data.size * 2) {
        var newCapacity = max(64, data.size * 2)
        while (newCapacity < minCapacity) {
            newCapacity *= 2
        }
        data = data.copyOf(newCapacity)
    }
}
