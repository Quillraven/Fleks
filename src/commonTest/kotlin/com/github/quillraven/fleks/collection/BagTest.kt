package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.world
import kotlin.test.*

class GenericBagTest {
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
}

class EntityBagTest {
    @Test
    fun createEmptyBagOfSize32() {
        val bag = EntityBag(32)

        assertEquals(32, bag.capacity)
        assertEquals(0, bag.size)
    }

    @Test
    fun addValueToBag() {
        val bag = EntityBag()

        bag += Entity(42)

        assertTrue(bag.isNotEmpty())
        assertEquals(1, bag.size)
        assertTrue(Entity(42) in bag)
    }

    @Test
    fun clearAllValuesFromBag() {
        val bag = EntityBag()
        bag += Entity(42)
        bag += Entity(43)

        bag.clear()

        assertEquals(0, bag.size)
        assertFalse { Entity(42) in bag }
        assertFalse { Entity(43) in bag }
    }

    @Test
    fun addValueUnsafeWithSufficientCapacity() {
        val bag = EntityBag(1)

        bag.unsafeAdd(Entity(42))

        assertTrue(Entity(42) in bag)
    }

    @Test
    fun addValueToBagWithInsufficientCapacity() {
        val bag = EntityBag(0)

        bag += Entity(42)

        assertEquals(1, bag.size)
        assertEquals(Entity(42), bag[0])
        assertEquals(1, bag.capacity)
    }

    @Test
    fun doNotResizeWhenBagHasSufficientCapacity() {
        val bag = EntityBag(8)

        bag.ensureCapacity(7)

        assertEquals(8, bag.capacity)
    }

    @Test
    fun resizeWhenBagHasInsufficientCapacity() {
        val bag = EntityBag(8)

        bag.ensureCapacity(9)

        assertEquals(10, bag.capacity)
    }

    @Test
    fun executeActionForEachValueOfBag() {
        val bag = EntityBag(4)
        bag += Entity(42)
        bag += Entity(43)
        var numCalls = 0
        val valuesCalled = mutableListOf<Entity>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }


        assertEquals(2, numCalls)
        assertEquals(listOf(Entity(42), Entity(43)), valuesCalled)
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan7() {
        val bag = EntityBag()
        repeat(6) { bag += Entity(6 - it) }

        bag.sort(compareEntity(world { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(6) {
            assertEquals(Entity(it + 1), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan50ButGreater7() {
        val bag = EntityBag()
        repeat(8) { bag += Entity(8 - it) }

        bag.sort(compareEntity(world { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(8) {
            assertEquals(Entity(it + 1), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeGreater50() {
        val bag = EntityBag()
        repeat(51) { bag += Entity(51 - it) }

        bag.sort(compareEntity(world { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(51) {
            assertEquals(Entity(it + 1), bag[it])
        }
    }

    @Test
    fun addValueUnsafeWithInsufficientCapacity() {
        val bag = EntityBag(0)

        assertFailsWith<IndexOutOfBoundsException> { bag.unsafeAdd(Entity(42)) }
    }

    @Test
    fun cannotGetValueOfOutOfBoundsIndex() {
        val bag = EntityBag(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }
}
