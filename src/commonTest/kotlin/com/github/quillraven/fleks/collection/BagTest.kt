package com.github.quillraven.fleks.collection

import kotlin.test.*

class BagTest {
    @Test
    fun createEmptyBagOfStringOfSize32() {
        val bag = bag<String>(32)

        assertEquals(32, bag.capacity)
        assertEquals(0, bag.size)
    }

    @Test
    fun addStringToBag() {
        val bag = bag<String>()

        bag.add("42")

        assertEquals(1, bag.size)
        assertTrue("42" in bag)
    }

    @Test
    fun removeExistingStringFromBag() {
        val bag = bag<String>()
        bag.add("42")

        val expected = bag.removeValue("42")

        assertEquals(0, bag.size)
        assertFalse { "42" in bag }
        assertTrue(expected)
    }

    @Test
    fun removeNonExistingStringFromBag() {
        val bag = bag<String>()

        val expected = bag.removeValue("42")

        assertFalse(expected)
    }

    @Test
    fun setStringValueAtIndexWithSufficientCapacity() {
        val bag = bag<String>()

        bag[3] = "42"

        assertEquals(4, bag.size)
        assertEquals("42", bag[3])
    }

    @Test
    fun setStringValueAtIndexWithInsufficientCapacity() {
        val bag = bag<String>(2)

        bag[2] = "42"

        assertEquals(3, bag.size)
        assertEquals("42", bag[2])
        assertEquals(3, bag.capacity)
    }

    @Test
    fun addStringToBagWithInsufficientCapacity() {
        val bag = bag<String>(0)

        bag.add("42")

        assertEquals(1, bag.size)
        assertEquals("42", bag[0])
        assertEquals(1, bag.capacity)
    }

    @Test
    fun cannotGetStringValueOfInvalidInBoundsIndex() {
        val bag = bag<String>()

        assertFailsWith<IndexOutOfBoundsException> { bag[0] }
    }

    @Test
    fun executeActionForEachValueOfStringBag() {
        val bag = bag<String>(4)
        bag[1] = "42"
        bag[2] = "43"
        var numCalls = 0
        val valuesCalled = mutableListOf<String>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }

        assertEquals(2, numCalls)
        assertEquals(listOf("42", "43"), valuesCalled)
    }

    @Test
    fun cannotGetStringValueOfInvalidOutOfBoundsIndex() {
        val bag = bag<String>(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }

    @Test
    fun hasValueAtIndex() {
        val bag = bag<String>(3)
        bag[1] = "foo"

        assertFalse { bag.hasValueAtIndex(0) }
        assertTrue { bag.hasValueAtIndex(1) }
        assertFalse { bag.hasValueAtIndex(2) }
    }

    @Test
    fun hasNoValueAtIndex() {
        val bag = bag<String>(3)
        bag[1] = "foo"

        assertTrue { bag.hasNoValueAtIndex(0) }
        assertFalse { bag.hasNoValueAtIndex(1) }
        assertTrue { bag.hasNoValueAtIndex(2) }
    }

    @Test
    fun getOrNullReturnsValueOrNullForInBoundsIndex(){
        val bag = bag<String>(3)
        bag[1] = "foo"

        assertNull(bag.getOrNull(0))
        assertNotNull(bag.getOrNull(1))
        assertNull(bag.getOrNull(2))
    }

    @Test
    fun getOrNullReturnsNullForOutOfBoundsIndex(){
        val bag = bag<String>(1)
        bag[0] = "foo"

        assertNull(bag.getOrNull(-1))
        assertNull(bag.getOrNull(1))
    }
}
