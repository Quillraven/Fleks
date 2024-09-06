package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import kotlin.test.*

class ListMutableEntityBagTest {

    private val testEntity1 = Entity(0, version = 0u)
    private val testEntity2 = Entity(1, version = 0u)
    private val testBag = ListMutableEntityBag(ArrayList(2)).apply {
        this += testEntity1
        this += testEntity2
    }

    private fun bagOf(entity: Entity) = ListMutableEntityBag(ArrayList(1)).apply {
        this += entity
    }

    private fun bagOf(entity1: Entity, entity2: Entity) = ListMutableEntityBag(ArrayList(2)).apply {
        this += entity1
        this += entity2
    }

    private fun arrayBagOf(entity: Entity) = ArrayMutableEntityBag(1).apply {
        this += entity
    }

    private fun arrayBagOf(entity1: Entity, entity2: Entity) = ArrayMutableEntityBag(2).apply {
        this += entity1
        this += entity2
    }

    @Test
    fun addValueToBag() {
        val bag = ListMutableEntityBag(ArrayList())

        bag += Entity(42, version = 0u)

        assertTrue(bag.isNotEmpty())
        assertEquals(1, bag.size)
        assertTrue(Entity(42, version = 0u) in bag)
    }

    @Test
    fun removeValueFromBag() {
        val bag = ListMutableEntityBag(ArrayList(2))
        val e1 = Entity(0, version = 0u)
        val e2 = Entity(1, version = 0u)
        bag += e1
        bag += e2

        bag -= e1
        assertEquals(1, bag.size)
        assertEquals(bagOf(e2), bag)

        bag -= e2
        assertTrue(bag.isEmpty())
    }

    @Test
    fun clearAllValuesFromBag() {
        val bag = ListMutableEntityBag(ArrayList())
        bag += Entity(42, version = 0u)
        bag += Entity(43, version = 0u)

        bag.clear()

        assertEquals(0, bag.size)
        assertFalse { Entity(42, version = 0u) in bag }
        assertFalse { Entity(43, version = 0u) in bag }
    }

    @Test
    fun executeActionForEachValueOfBag() {
        val bag = ListMutableEntityBag(ArrayList(4))
        bag += Entity(42, version = 0u)
        bag += Entity(43, version = 0u)
        var numCalls = 0
        val valuesCalled = mutableListOf<Entity>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }


        assertEquals(2, numCalls)
        assertEquals(listOf(Entity(42, version = 0u), Entity(43, version = 0u)), valuesCalled)
    }

    @Test
    fun executeActionForEachValueOfBagIndexed() {
        val bag = ListMutableEntityBag(ArrayList(4))
        bag += Entity(42, version = 0u)
        bag += Entity(43, version = 0u)
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
        assertEquals(listOf(Entity(42, version = 0u), Entity(43, version = 0u)), valuesCalled)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan7() {
        val bag = ListMutableEntityBag(ArrayList())
        repeat(6) { bag += Entity(6 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(6) {
            assertEquals(Entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan50ButGreater7() {
        val bag = ListMutableEntityBag(ArrayList())
        repeat(8) { bag += Entity(8 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(8) {
            assertEquals(Entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeGreater50() {
        val bag = ListMutableEntityBag(ArrayList())
        repeat(51) { bag += Entity(51 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(51) {
            assertEquals(Entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun cannotGetValueOfOutOfBoundsIndex() {
        val bag = ListMutableEntityBag(ArrayList(2))

        assertFailsWith<IndexOutOfBoundsException> { bag[-1] }
        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }

    @Test
    fun testContainsAll() {
        assertTrue(testBag.containsAll(listOf(testEntity2, testEntity1)))
        assertTrue(testBag.containsAll(testBag))
        assertFalse(testBag.containsAll(listOf(Entity(2, version = 0u))))
        assertTrue(testBag.containsAll(listOf(Entity(1, version = 0u))))
        assertTrue(testBag.containsAll(emptyList()))
    }

    @Test
    fun testIsEmpty() {
        val testBag = ListMutableEntityBag(ArrayList(1))
        assertTrue(testBag.isEmpty())
        assertFalse(testBag.isNotEmpty())

        testBag += Entity(0, version = 0u)
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
        val expected = mapOf(Entity(2, version = 0u) to 2, Entity(3, version = 0u) to 3)

        val actual = testBag.associateBy(
            { Entity(it.id + 2, version = 0u) },
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
        val expected = mapOf(testEntity1 to 0, testEntity2 to 1, Entity(2, version = 0u) to 2)
        val destination = mutableMapOf(Entity(2, version = 0u) to 2)

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
        val expected = mapOf(0 to testEntity1, 1 to testEntity2, 2 to Entity(2, version = 0u))
        val destination = mutableMapOf(2 to Entity(2, version = 0u))

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
        val expected = mapOf(Entity(2, version = 0u) to 2, Entity(3, version = 0u) to 3, Entity(4, version = 0u) to 4)
        val destination = mutableMapOf(Entity(4, version = 0u) to 4)


        val actual = testBag.associateByTo(
            destination,
            { Entity(it.id + 2, version = 0u) },
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
        assertEquals(0, ListMutableEntityBag(ArrayList()).count())
        assertEquals(2, testBag.count())
        assertEquals(1, testBag.count { it.id == 0 })
    }

    @Test
    fun testFilter() {
        val expected = arrayBagOf(testEntity1)
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
        val expected = arrayBagOf(testEntity2)

        val actual = testBag.filterNot { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFilterTo() {
        val testEntity3 = Entity(2, version = 0u)
        val expected = bagOf(testEntity3, testEntity1)
        val expectedIndices = listOf(0, 1)
        val destination1 = bagOf(testEntity3)
        val destination2 = bagOf(testEntity3)

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
        val testEntity3 = Entity(2, version = 0u)
        val expected = bagOf(testEntity3, testEntity2)
        val destination = bagOf(testEntity3)

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
        assertFailsWith<NoSuchElementException> { ListMutableEntityBag(ArrayList()).first() }
        assertFailsWith<NoSuchElementException> { testBag.first { it.id == 3 } }
    }

    @Test
    fun testFirstOrNull() {
        assertEquals(testEntity1, testBag.firstOrNull())
        assertEquals(testEntity2, testBag.firstOrNull { it == testEntity2 })
        assertNull(ListMutableEntityBag(ArrayList()).firstOrNull())
        assertNull(testBag.firstOrNull { it.id == 3 })
    }

    @Test
    fun testFlatMap() {
        val expectedInts = listOf(0, 0, 1, 2)
        val expectedEntities = arrayBagOf(testEntity1, testEntity1)

        val actualIntsIter = testBag.flatMap { listOf(it.id, it.id * 2) }
        val actualIntsSeq = testBag.flatMapSequence { listOf(it.id, it.id * 2).asSequence() }
        val actualEntities = testBag.flatMapBag { testBag.filter { it.id == 0 } }

        assertContentEquals(expectedInts, actualIntsIter)
        assertContentEquals(expectedInts, actualIntsSeq)
        assertEquals(expectedEntities, actualEntities)
    }

    @Test
    fun testFlatMapNotNull() {
        val expectedInts = listOf(0, 2)
        val expectedEntities = arrayBagOf(testEntity1)

        val actualIntsIter = testBag.flatMapNotNull { listOf(null, it.id * 2) }
        val actualIntsSeq = testBag.flatMapSequenceNotNull { listOf(null, it.id * 2).asSequence() }
        val actualEntities = testBag.flatMapBagNotNull { e -> if (e.id == 0) testBag.filter { it.id == 0 } else null }
        val actual1: List<String> = testBag.flatMapNotNull { null }
        val actual2: List<String> = testBag.flatMapSequenceNotNull { null }

        assertContentEquals(expectedInts, actualIntsIter)
        assertContentEquals(expectedInts, actualIntsSeq)
        assertEquals(expectedEntities, actualEntities)
        assertTrue(actual1.isEmpty())
        assertTrue(actual2.isEmpty())
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
    fun testGroupBy() {
        val expected1 = mapOf(0 to arrayBagOf(testEntity1), 1 to arrayBagOf(testEntity2))
        val expected2 = mapOf(0 to listOf(3), 1 to listOf(3))

        val actual1 = testBag.groupBy { it.id }
        val actual2 = testBag.groupBy(
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
        val expected1 = mapOf(0 to arrayBagOf(testEntity1), 1 to arrayBagOf(testEntity2), 2 to arrayBagOf(Entity(2, version = 0u)))
        val expected2 = mapOf(0 to listOf(3), 1 to listOf(3), 2 to listOf(3))

        val actual = testBag.groupByTo(mutableMapOf(2 to arrayBagOf(Entity(2, version = 0u)))) { it.id }
        val actual2 = testBag.groupByTo(
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
    fun testMapNotNull() {
        val expected = listOf(2)

        val actual = testBag.mapNotNull { if (it.id == 0) 2 else null }

        assertContentEquals(expected, actual)
    }

    @Test
    fun testMapNotNullTo() {
        val expected = listOf(5, 2)

        val actual = testBag.mapNotNullTo(mutableListOf(5)) { if (it.id == 0) 2 else null }

        assertContentEquals(expected, actual)
    }

    @Test
    fun testPartition() {
        val (first, second) = testBag.partition { it.id <= 0 }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(testEntity1 in first)
        assertTrue(testEntity2 in second)
    }

    @Test
    fun testPartitionTo() {
        val first = ListMutableEntityBag(ArrayList())
        val second = ListMutableEntityBag(ArrayList())

        testBag.partitionTo(first, second) { it.id <= 0 }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(testEntity1 in first)
        assertTrue(testEntity2 in second)
    }

    @Test
    fun testRandom() {
        val actual = testBag.random()

        assertFailsWith<NoSuchElementException> { ListMutableEntityBag(ArrayList()).random() }
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testRandomOrNull() {
        val actual = testBag.randomOrNull()

        assertNull(ListMutableEntityBag(ArrayList()).randomOrNull())
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testTake() {
        assertTrue(testBag.take(-1).isEmpty())
        assertTrue(testBag.take(0).isEmpty())
        assertEquals(arrayBagOf(testEntity1), testBag.take(1))
        assertEquals(arrayBagOf(testEntity1, testEntity2), testBag.take(2))
        assertEquals(arrayBagOf(testEntity1, testEntity2), testBag.take(3))
    }
}
