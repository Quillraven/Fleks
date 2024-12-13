package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.MutableEntityBag
import com.github.quillraven.fleks.collection.entityBagOf
import com.github.quillraven.fleks.collection.mutableEntityBagOf
import kotlin.test.*

class FamilyBagFunctionsTest {

    private val testEntity1 = Entity(0, version = 0u)
    private val testEntity2 = Entity(1, version = 0u)

    private val testWorld = configureWorld { }
    private val testFamily = Family(world = testWorld).apply {
        onEntityAdded(testEntity1, BitArray())
        onEntityAdded(testEntity2, BitArray())
    }
    private val testFamilySingle = Family(world = testWorld).apply {
        onEntityAdded(testEntity1, BitArray())
    }

    @Test
    fun testContainsAll() {
        assertTrue(testFamily.containsAll(listOf(testEntity2, testEntity1)))
        assertTrue(testFamily.containsAll(testFamily))
        assertTrue(testFamily.containsAll(entityBagOf(testEntity1, testEntity2)))
        assertFalse(testFamily.containsAll(listOf(Entity(2, version = 0u))))
        assertTrue(testFamily.containsAll(listOf(Entity(1, version = 0u))))
        assertTrue(testFamily.containsAll(emptyList()))
    }

    @Test
    fun testAll() {
        assertTrue(testFamily.all { it.id >= 0 })
        assertFalse(testFamily.all { it.id == 0 })
    }

    @Test
    fun testAny() {
        assertTrue(testFamily.any { it.id == 0 })
        assertFalse(testFamily.any { it.id == 2 })
    }

    @Test
    fun testNone() {
        assertTrue(testFamily.none { it.id == 2 })
        assertFalse(testFamily.none { it.id == 0 })
    }

    @Test
    fun testFilter() {
        val expected = entityBagOf(testEntity1)
        val expectedIndices = listOf(0, 1)

        val actual1 = testFamily.filter { it == testEntity1 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testFamily.filterIndexed { index, entity ->
            actualIndices += index
            entity == testEntity1
        }

        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
        assertEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testFilterNot() {
        val expected = entityBagOf(testEntity2)

        val actual = testFamily.filterNot { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFilterTo() {
        val testEntity3 = Entity(2, version = 0u)
        val expected = entityBagOf(testEntity3, testEntity1)
        val expectedIndices = listOf(0, 1)
        val destination1 = mutableEntityBagOf(testEntity3)
        val destination2 = mutableEntityBagOf(testEntity3)

        val actual1 = testFamily.filterTo(destination1) { it == testEntity1 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testFamily.filterIndexedTo(destination2) { index, entity ->
            actualIndices += index
            entity == testEntity1
        }

        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
        assertEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testFilterNotTo() {
        val testEntity3 = Entity(2, version = 0u)
        val expected = entityBagOf(testEntity3, testEntity2)
        val destination = mutableEntityBagOf(testEntity3)

        val actual = testFamily.filterNotTo(destination) { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFind() {
        assertEquals(testEntity1, testFamily.find { it == testEntity1 })
        assertNull(testFamily.find { it.id == 3 })
    }

    @Test
    fun testFirst() {
        assertEquals(testEntity1, testFamily.first())
        assertEquals(testEntity2, testFamily.first { it == testEntity2 })
        assertFailsWith<NoSuchElementException> { MutableEntityBag().first() }
        assertFailsWith<NoSuchElementException> { testFamily.first { it.id == 3 } }
    }

    @Test
    fun testFirstOrNull() {
        assertEquals(testEntity1, testFamily.firstOrNull())
        assertEquals(testEntity2, testFamily.firstOrNull { it == testEntity2 })
        assertNull(MutableEntityBag().firstOrNull())
        assertNull(testFamily.firstOrNull { it.id == 3 })
    }

    @Test
    fun testGroupBy() {
        val expected1 = mapOf(0 to mutableEntityBagOf(testEntity1), 1 to mutableEntityBagOf(testEntity2))
        val expected2 = mapOf(0 to listOf(3), 1 to listOf(3))

        val actual1 = testFamily.groupBy { it.id }
        val actual2 = testFamily.groupBy(
            { it.id },
            { 3 }
        )

        assertTrue(expected1.keys.containsAll(actual1.keys))
        assertTrue(actual1.keys.containsAll(expected1.keys))
        expected1.forEach { (id, bag) ->
            assertEquals(bag, actual1[id])
        }
        assertTrue(expected2.keys.containsAll(actual2.keys))
        assertTrue(actual2.keys.containsAll(expected2.keys))
        expected2.forEach { (id, intList) ->
            assertContentEquals(intList, actual2[id])
        }
    }

    @Test
    fun testGroupByTo() {
        val expected1 = mapOf(
            0 to mutableEntityBagOf(testEntity1),
            1 to mutableEntityBagOf(testEntity2),
            2 to mutableEntityBagOf(Entity(2, version = 0u))
        )
        val expected2 = mapOf(0 to listOf(3), 1 to listOf(3), 2 to listOf(3))

        val actual = testFamily.groupByTo(mutableMapOf(2 to mutableEntityBagOf(Entity(2, version = 0u)))) { it.id }
        val actual2 = testFamily.groupByTo(
            mutableMapOf(2 to mutableListOf(3)),
            { it.id },
            { 3 }
        )

        assertTrue(expected1.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected1.keys))
        expected1.forEach { (id, bag) ->
            assertEquals(bag, actual[id])
        }
        assertTrue(expected2.keys.containsAll(actual2.keys))
        assertTrue(actual2.keys.containsAll(expected2.keys))
        expected2.forEach { (id, intList) ->
            assertContentEquals(intList, actual2[id])
        }
    }

    @Test
    fun testMap() {
        val expected = listOf(2, 3)
        val expectedIndices = listOf(0, 1)

        val actual1 = testFamily.map { it.id + 2 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testFamily.mapIndexed { index, entity ->
            actualIndices += index
            entity.id + 2
        }

        assertContentEquals(expected, actual1)
        assertContentEquals(expected, actual2)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testMapTo() {
        val expected = listOf(5, 2, 3)
        val expectedIndices = listOf(0, 1)

        val actual1 = testFamily.mapTo(mutableListOf(5)) { it.id + 2 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testFamily.mapIndexedTo(mutableListOf(5)) { index, entity ->
            actualIndices += index
            entity.id + 2
        }

        assertContentEquals(expected, actual1)
        assertContentEquals(expected, actual2)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testMapNotNull() {
        val expected = listOf(2)

        val actual = testFamily.mapNotNull { if (it.id == 0) 2 else null }

        assertContentEquals(expected, actual)
    }

    @Test
    fun testMapNotNullTo() {
        val expected = listOf(5, 2)

        val actual = testFamily.mapNotNullTo(mutableListOf(5)) { if (it.id == 0) 2 else null }

        assertContentEquals(expected, actual)
    }

    @Test
    fun testPartition() {
        val (first, second) = testFamily.partition { it.id <= 0 }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(testEntity1 in first)
        assertTrue(testEntity2 in second)
    }

    @Test
    fun testPartitionTo() {
        val first = MutableEntityBag()
        val second = MutableEntityBag()

        testFamily.partitionTo(first, second) { it.id <= 0 }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(testEntity1 in first)
        assertTrue(testEntity2 in second)
    }

    @Test
    fun testRandom() {
        val actual = testFamily.random()

        assertFailsWith<NoSuchElementException> { MutableEntityBag().random() }
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testRandomOrNull() {
        val actual = testFamily.randomOrNull()

        assertNull(MutableEntityBag().randomOrNull())
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testSingle() {
        assertEquals(testEntity1, testFamilySingle.single())
        assertEquals(testEntity1, testFamilySingle.single { it == testEntity1 })
        assertFailsWith<IllegalArgumentException> { testFamily.single() }
        assertFailsWith<IllegalArgumentException> { entityBagOf(testEntity1, testEntity1).single { it == testEntity1 } }
        assertFailsWith<NoSuchElementException> { MutableEntityBag().single() }
        assertFailsWith<NoSuchElementException> { testFamily.single { it.id == 3 } }
    }

    @Test
    fun testSingleOrNull() {
        assertEquals(testEntity1, testFamilySingle.singleOrNull())
        assertEquals(testEntity1, testFamilySingle.singleOrNull { it == testEntity1 })
        assertNull(testFamily.singleOrNull())
        assertNull(entityBagOf(testEntity1, testEntity1).singleOrNull { it == testEntity1 })
        assertNull(MutableEntityBag().singleOrNull())
        assertNull(testFamily.singleOrNull { it.id == 3 })
    }

    @Test
    fun testTake() {
        assertTrue(testFamily.take(-1).isEmpty())
        assertTrue(testFamily.take(0).isEmpty())
        assertEquals(entityBagOf(testEntity1), testFamily.take(1))
        assertEquals(entityBagOf(testEntity1, testEntity2), testFamily.take(2))
        assertEquals(entityBagOf(testEntity1, testEntity2), testFamily.take(3))
    }
}
