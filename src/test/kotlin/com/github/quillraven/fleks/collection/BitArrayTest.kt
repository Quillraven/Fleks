package com.github.quillraven.fleks.collection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BitArrayTest {
    @Test
    fun `create empty BitArray`() {
        val bits = BitArray(0)

        assertAll(
            { assertEquals(0, bits.length()) },
            { assertEquals(0, bits.capacity) }
        )
    }

    @Test
    fun `set bit at index 3 with sufficient capacity`() {
        val bits = BitArray(3)

        bits.set(2)

        assertAll(
            { assertEquals(3, bits.length()) },
            { assertEquals(64, bits.capacity) },
            { assertTrue { bits[2] } }
        )
    }

    @Test
    fun `set bit at index 3 with insufficient capacity`() {
        val bits = BitArray(0)

        bits.set(2)

        assertAll(
            { assertEquals(3, bits.length()) },
            { assertEquals(64, bits.capacity) },
            { assertTrue { bits[2] } }
        )
    }

    @Test
    fun `get bit of out of bounds index`() {
        val bits = BitArray(0)

        assertFalse(bits[64])
    }

    @Test
    fun `clear all set bits`() {
        val bits = BitArray()
        bits.set(2)
        bits.set(4)

        bits.clearAll()

        assertEquals(0, bits.length())
    }

    @Test
    fun `clear specific bit`() {
        val bits = BitArray()
        bits.set(2)

        bits.clear(2)

        assertEquals(0, bits.length())
    }

    @Test
    fun `two BitArrays intersect when they have at least one bit set at the same index`() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsA.set(6)
        bitsB.set(4)

        val actualA = bitsA.intersects(bitsB)
        val actualB = bitsB.intersects(bitsA)

        assertAll(
            { assertTrue(actualA) },
            { assertTrue(actualB) }
        )
    }

    @Test
    fun `two BitArrays do not intersect when they do not have at least one bit set at the same index`() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsA.set(6)
        bitsB.set(3)

        val actualA = bitsA.intersects(bitsB)
        val actualB = bitsB.intersects(bitsA)

        assertAll(
            { assertFalse(actualA) },
            { assertFalse(actualB) }
        )
    }

    @Test
    fun `BitArray contains BitArray if the same bits are set`() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsB.set(2)
        bitsB.set(4)

        val actualA = bitsA.contains(bitsB)
        val actualB = bitsB.contains(bitsA)

        assertAll(
            { assertTrue(actualA) },
            { assertTrue(actualB) }
        )
    }

    @Test
    fun `BitArray does not contain BitArray if different bits are set`() {
        val bitsA = BitArray(256)
        val bitsB = BitArray(1)
        bitsA.set(2)
        bitsA.set(4)
        bitsB.set(2)
        bitsB.set(3)

        val actualA = bitsA.contains(bitsB)
        val actualB = bitsB.contains(bitsA)

        assertAll(
            { assertFalse(actualA) },
            { assertFalse(actualB) }
        )
    }

    @Test
    fun `run action for each set bit`() {
        val bits = BitArray(128)
        bits.set(3)
        bits.set(5)
        bits.set(117)
        var numCalls = 0
        val bitsCalled = mutableListOf<Int>()

        bits.forEachSetBit {
            ++numCalls
            bitsCalled.add(it)
        }

        assertAll(
            { assertEquals(3, numCalls) },
            { assertEquals(listOf(117, 5, 3), bitsCalled) }
        )
    }

    @Test
    fun `test numBits`() {
        val bits = BitArray()

        bits.set(4)

        assertEquals(1, bits.numBits())
        assertEquals(5, bits.length())
    }

    @Test
    fun `test isEmpty`() {
        val bits = BitArray()
        assertTrue(bits.isEmpty)
        assertFalse(bits.isNotEmpty)

        bits.set(0)
        assertFalse(bits.isEmpty)
        assertTrue(bits.isNotEmpty)
    }

    @Test
    fun `test toString`() {
        val bits0 = BitArray()
        val bits1 = BitArray().apply { set(0) }
        val bits2 = BitArray().apply { set(3) }
        val bits3 = BitArray().apply { set(100) }
        val bits4 = BitArray().apply {
            set(3)
            set(100)
        }
        val expected0 = "0"
        val expected1 = "1"
        val expected2 = "0001"
        val expected3 = "0".repeat(100) + "1"
        val expected4 = "0".repeat(3) + "1" + "0".repeat(96) + "1"

        assertEquals(expected0, bits0.toString())
        assertEquals(expected1, bits1.toString())
        assertEquals(expected2, bits2.toString())
        assertEquals(expected3, bits3.toString())
        assertEquals(expected4, bits4.toString())
    }
}
