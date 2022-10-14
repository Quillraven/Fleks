package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.world
import kotlin.test.*

class EntityBagTest {

    private val testEntity1 = Entity(0)
    private val testEntity2 = Entity(1)
    private val testBag = EntityBag(2).apply {
        this += testEntity1
        this += testEntity2
    }

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
    fun executeActionForEachValueOfBagIndexed() {
        val bag = EntityBag(4)
        bag += Entity(42)
        bag += Entity(43)
        var numCalls = 0
        val valuesCalled = mutableListOf<Entity>()
        val expectedIndices = listOf(0, 1)

        val actualIndices = mutableListOf<Int>()
        bag.forEachIndexed { index, entity ->
            actualIndices += index
            ++numCalls
            valuesCalled.add(entity)
        }


        assertEquals(2, numCalls)
        assertEquals(listOf(Entity(42), Entity(43)), valuesCalled)
        assertContentEquals(expectedIndices, actualIndices)
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
    fun cannotGetValueOfOutOfBoundsIndex() {
        val bag = EntityBag(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }

    @Test
    fun testContainsAll() {
        assertTrue(testBag.containsAll(listOf(testEntity2, testEntity1)))
        assertTrue(testBag.containsAll(testBag))
        assertFalse(testBag.containsAll(listOf(Entity(2))))
        assertTrue(testBag.containsAll(listOf(Entity(1))))
        assertTrue(testBag.containsAll(emptyList()))
    }

    @Test
    fun testIsEmpty() {
        val testBag = EntityBag(1)
        assertTrue(testBag.isEmpty())
        assertFalse(testBag.isNotEmpty())

        testBag += Entity(0)
        assertFalse(testBag.isEmpty())
        assertTrue(testBag.isNotEmpty())
    }

    @Test
    fun testAll() {
        assertTrue(testBag.all { it.id >= 0 })
        assertFalse(testBag.all { it.id == 0 })
    }

    @Test
    fun testAny() {
        assertTrue(testBag.any { it.id == 0 })
        assertFalse(testBag.any { it.id == 2 })
    }

    @Test
    fun testNone() {
        assertTrue(testBag.none { it.id == 2 })
        assertFalse(testBag.none { it.id == 0 })
    }

    @Test
    fun testAssociate() {
        val expected = mapOf(testEntity1 to 0, testEntity2 to 1)

        val actual = testBag.associate { it to it.id }

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (entity, id) ->
            assertEquals(id, actual[entity])
        }
    }

    @Test
    fun testAssociateByKeySelector() {
        val expected = mapOf(0 to testEntity1, 1 to testEntity2)

        val actual = testBag.associateBy { it.id }

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (id, entity) ->
            assertEquals(entity, actual[id])
        }
    }

    @Test
    fun testAssociateByKeyAndValueSelector() {
        val expected = mapOf(Entity(2) to 2, Entity(3) to 3)

        val actual = testBag.associateBy(
            { Entity(it.id + 2) },
            { it.id + 2 }
        )

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (id, entity) ->
            assertEquals(entity, actual[id])
        }
    }

    @Test
    fun testAssociateTo() {
        val expected = mapOf(testEntity1 to 0, testEntity2 to 1, Entity(2) to 2)
        val destination = mutableMapOf(Entity(2) to 2)

        val actual = testBag.associateTo(destination) { it to it.id }

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (entity, id) ->
            assertEquals(id, actual[entity])
        }
    }

    @Test
    fun testAssociateByToKeySelector() {
        val expected = mapOf(0 to testEntity1, 1 to testEntity2, 2 to Entity(2))
        val destination = mutableMapOf(2 to Entity(2))

        val actual = testBag.associateByTo(destination) { it.id }

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (id, entity) ->
            assertEquals(entity, actual[id])
        }
    }

    @Test
    fun testAssociateByToKeyAndValueSelector() {
        val expected = mapOf(Entity(2) to 2, Entity(3) to 3, Entity(4) to 4)
        val destination = mutableMapOf(Entity(4) to 4)


        val actual = testBag.associateByTo(
            destination,
            { Entity(it.id + 2) },
            { it.id + 2 }
        )

        assertEquals(expected.keys.size, actual.keys.size)
        assertTrue(expected.keys.containsAll(actual.keys))
        assertTrue(actual.keys.containsAll(expected.keys))
        expected.forEach { (id, entity) ->
            assertEquals(entity, actual[id])
        }
    }

    @Test
    fun testCount() {
        assertEquals(0, EntityBag().count())
        assertEquals(2, testBag.count())
        assertEquals(1, testBag.count { it.id == 0 })
    }

    @Test
    fun testFilter() {
        val expected = listOf(testEntity1)
        val expectedIndices = listOf(0, 1)

        val actual1 = testBag.filter { it == testEntity1 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testBag.filterIndexed { index, entity ->
            actualIndices += index
            entity == testEntity1
        }

        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
        assertEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testFilterNot() {
        val expected = listOf(testEntity2)

        val actual = testBag.filterNot { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFilterTo() {
        val testEntity3 = Entity(2)
        val expected = listOf(testEntity3, testEntity1)
        val expectedIndices = listOf(0, 1)
        val destination1 = mutableListOf(testEntity3)
        val destination2 = mutableListOf(testEntity3)

        val actual1 = testBag.filterTo(destination1) { it == testEntity1 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testBag.filterIndexedTo(destination2) { index, entity ->
            actualIndices += index
            entity == testEntity1
        }

        assertEquals(expected, actual1)
        assertEquals(expected, actual2)
        assertEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testFilterNotTo() {
        val testEntity3 = Entity(2)
        val expected = listOf(testEntity3, testEntity2)
        val destination = mutableListOf(testEntity3)

        val actual = testBag.filterNotTo(destination) { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFind() {
        assertEquals(testEntity1, testBag.find { it == testEntity1 })
        assertNull(testBag.find { it.id == 3 })
    }

    @Test
    fun testFirst() {
        assertEquals(testEntity1, testBag.first())
        assertEquals(testEntity2, testBag.first { it == testEntity2 })
        assertFailsWith<NoSuchElementException> { EntityBag().first() }
        assertFailsWith<NoSuchElementException> { testBag.first { it.id == 3 } }
    }

    @Test
    fun testFirstOrNull() {
        assertEquals(testEntity1, testBag.firstOrNull())
        assertEquals(testEntity2, testBag.firstOrNull { it == testEntity2 })
        assertNull(EntityBag().firstOrNull())
        assertNull(testBag.firstOrNull { it.id == 3 })
    }

    @Test
    fun testFold() {
        val expectedIndices = listOf(0, 1)

        val actual1 = testBag.fold(3) { acc, entity ->
            acc + entity.id
        }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testBag.foldIndexed(3) { index, acc, entity ->
            actualIndices += index
            acc + entity.id
        }

        // initial 3 + id 0 + id 1 = 1
        assertEquals(4, actual1)
        assertEquals(4, actual2)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testMap() {
        val expected = listOf(2, 3)
        val expectedIndices = listOf(0, 1)

        val actual1 = testBag.map { it.id + 2 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testBag.mapIndexed { index, entity ->
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

        val actual1 = testBag.mapTo(mutableListOf(5)) { it.id + 2 }
        val actualIndices = mutableListOf<Int>()
        val actual2 = testBag.mapIndexedTo(mutableListOf(5)) { index, entity ->
            actualIndices += index
            entity.id + 2
        }

        assertContentEquals(expected, actual1)
        assertContentEquals(expected, actual2)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun testRandom() {
        val actual = testBag.random()

        assertFailsWith<NoSuchElementException> { EntityBag().random() }
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testRandomOrNull() {
        val actual = testBag.randomOrNull()

        assertNull(EntityBag().randomOrNull())
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testTake() {
        assertTrue(testBag.take(-1).isEmpty())
        assertTrue(testBag.take(0).isEmpty())
        assertContentEquals(listOf(testEntity1), testBag.take(1))
        assertContentEquals(listOf(testEntity1, testEntity2), testBag.take(2))
        assertContentEquals(listOf(testEntity1, testEntity2), testBag.take(3))
    }
}
