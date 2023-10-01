package com.github.quillraven.fleks.collection

import kotlin.math.min

/**
 * A BitArray implementation in Kotlin containing only the necessary functions for Fleks.
 *
 * Boolean[] gives a better performance when iterating over a BitArray, but uses more memory and
 * also the amount of array resizing is increased when enlarging the array which makes it then slower in the end.
 *
 * For that reason I used a Long[] implementation which is similar to the one of java.util with inspirations also from
 * https://github.com/lemire/javaewah/blob/master/src/main/java/com/googlecode/javaewah/datastructure/BitSet.java.
 * It is more memory efficient and requires less resizing calls since one Long can store up to 64 bits.
 */
class BitArray(
    nBits: Int = 0
) {
    @PublishedApi
    internal var bits = LongArray((nBits + 63) ushr 6)

    val capacity: Int
        get() = bits.size shl 6

    val isNotEmpty: Boolean
        get() {
            for (word in bits.size - 1 downTo 0) {
                if (bits[word] != 0L) {
                    return true
                }
            }
            return false
        }

    val isEmpty: Boolean
        get() = !isNotEmpty

    operator fun get(idx: Int): Boolean {
        val word = idx ushr 6
        return word < bits.size && (bits[word] and (1L shl idx)) != 0L
    }

    fun set(idx: Int) {
        val word = idx ushr 6
        if (word >= bits.size) {
            // ((word or -word) shr 63) is an idiom that returns 0 if word is 0, or -1 otherwise.
            // Calling inv() on that makes it -1 if word is 0, or 0 otherwise.
            // the rest of the line resizes the bits Array to be twice as large, unless word is
            // 0 and bits.size is 0; then it uses one Long item.
            bits = bits.copyOf(word + word - ((word or -word) shr 63).inv())
        }
        bits[word] = bits[word] or (1L shl idx)
    }

    fun clearAll() {
        bits.fill(0L)
    }

    fun clear(idx: Int) {
        val word = idx ushr 6
        if (word < bits.size) {
            bits[word] = bits[word] and (1L shl idx).inv()
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

    /**
     * Returns the logical size of the [BitArray] which is equal to the highest index of the
     * bit that is set.
     *
     * Returns zero if the [BitArray] is empty.
     */
    fun length(): Int {
        for (word in bits.size - 1 downTo 0) {
            val bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                val w = word shl 6
                return w + (64 - bitsAtWord.countLeadingZeroBits())
            }
        }
        return 0
    }

    /**
     * Returns number of bits that are set in the [BitArray].
     */
    fun numBits(): Int {
        var sum = 0
        for (word in bits.size - 1 downTo 0) {
            sum += bits[word].countOneBits()
        }
        return sum
    }

    inline fun forEachSetBit(action: (Int) -> Unit) {
        for (word in bits.size - 1 downTo 0) {
            var bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                val w = word shl 6
                while (bitsAtWord != 0L) {
                    // gets the distance from the start of the word to the highest (leftmost) bit
                    val bit = 63 - bitsAtWord.countLeadingZeroBits()
                    action(w + bit)
                    bitsAtWord = (bitsAtWord xor (1L shl bit)) // removes highest bit
                }
            }
        }
    }

    inline fun clearAndForEachSetBit(action: (Int) -> Unit) {
        for (word in bits.size - 1 downTo 0) {
            var bitsAtWord = bits[word]
            if (bitsAtWord != 0L) {
                val w = word shl 6
                while (bitsAtWord != 0L) {
                    bits[word] = 0L // clear
                    // gets the distance from the start of the word to the highest (leftmost) bit
                    val bit = 63 - bitsAtWord.countLeadingZeroBits()
                    action(w + bit)
                    bitsAtWord = (bitsAtWord xor (1L shl bit)) // removes highest bit
                }
            }
        }
    }

    override fun hashCode(): Int {
        if (bits.isEmpty()) {
            return 0
        }

        val word = length() ushr 6
        var hash = 0
        for (i in 0..word) {
            hash = 127 * hash + (bits[i] xor (bits[i] ushr 32)).toInt()
        }
        return hash
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BitArray) return false

        val otherBits = other.bits

        val commonWords: Int = min(bits.size, otherBits.size)
        for (i in 0 until commonWords) {
            if (bits[i] != otherBits[i]) return false
        }

        return if (bits.size == otherBits.size) true else length() == other.length()
    }

    override fun toString(): String {
        return buildString {
            for (bitsAtWord in bits) {
                if (bitsAtWord != 0L) {
                    for (bit in 0 until 64) {
                        if ((bitsAtWord and (1L shl bit)) != 0L) {
                            append("1")
                        } else {
                            append("0")
                        }
                    }
                } else {
                    repeat(64) { append("0") }
                }
            }
        }.trimEnd('0').ifBlank { "0" }
    }
}

fun BitArray?.isNullOrEmpty(): Boolean =
    this == null || this.isEmpty
