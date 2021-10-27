package com.github.quillraven.fleks.collection

import kotlin.math.min

class BitArray(
    nBits: Int = 0
) {
    private var bits = LongArray((nBits + 63) / 64)

    operator fun get(idx: Int): Boolean {
        val word = idx ushr 6
        return if (word >= bits.size) {
            false
        } else {
            (bits[word] and (1L shl (idx and 0x3F))) != 0L
        }
    }

    fun set(idx: Int) {
        val word = idx ushr 6
        ensureSize(word)
        bits[word] = bits[word] or (1L shl (idx and 0x3F))
    }

    fun clear() {
        bits.fill(0L)
    }

    fun clear(idx: Int) {
        val word = idx ushr 6
        if (word < bits.size) {
            bits[word] = bits[word] and (1L shl (idx and 0x3F)).inv()
        }
    }

    private fun ensureSize(newSize: Int) {
        if (newSize >= bits.size) {
            bits = bits.copyOf(newSize + 1)
        }
    }

    fun intersects(other: BitArray): Boolean {
        val otherBits = other.bits
        val start = min(bits.size, otherBits.size) - 1
        for (i in start downTo 0) {
            if ((bits[i] and otherBits[i]) != 0L) {
                return true
            }
        }
        return false
    }

    fun contains(other: BitArray): Boolean {
        val otherBits = other.bits

        // check if other BitArray is larger and if there is any of those bits set
        for (i in bits.size until otherBits.size) {
            if (otherBits[i] != 0L) {
                return false
            }
        }

        // check overlapping bits
        val start = min(bits.size, otherBits.size) - 1
        for (i in start downTo 0) {
            if ((bits[i] and otherBits[i]) != otherBits[i]) {
                return false
            }
        }

        return true
    }

    fun length(): Int {
        for (word in bits.size - 1 downTo 0) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                for (bit in 63 downTo 0) {
                    if ((bitsAtWord and (1L shl (bit and 0x3F))) != 0L) {
                        return (word shl 6) + bit + 1
                    }
                }
            }
        }
        return 0
    }

    fun toIntBag(bag: IntBag) {
        var checkSize = true
        bag.clear()

        for (word in bits.size - 1 downTo 0) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                for (bit in 63 downTo 0) {
                    if ((bitsAtWord and (1L shl (bit and 0x3F))) != 0L) {
                        val idx = (word shl 6) + bit
                        if (checkSize) {
                            checkSize = false
                            bag.ensureCapacity(idx + 1)
                        }
                        bag.unsafeAdd(idx)
                    }
                }
            }
        }
    }

    override fun hashCode(): Int {
        val word = length() ushr 6
        var hash = 0
        for (i in 0..word) {
            hash = 127 * hash + (bits[i] xor (bits[i] ushr 32)).toInt()
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BitArray
        val otherBits = other.bits

        val commonWords: Int = min(bits.size, otherBits.size)
        for (i in 0 until commonWords) {
            if (bits[i] != otherBits[i]) return false
        }

        return if (bits.size == otherBits.size) true else length() == other.length()
    }
}
