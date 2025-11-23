package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.Family
import com.github.quillraven.fleks.configureWorld
import com.github.quillraven.fleks.entity
import kotlin.test.*

class EntityBagTest {

    private val testEntity1 = entity(0, version = 0u)
    private val testEntity2 = entity(1, version = 0u)
    private val testBag = mutableEntityBagOf(testEntity1, testEntity2)
    private val testBagSingle = mutableEntityBagOf(testEntity1)

    @Test
    fun createEmptyBagOfSize32() {
        val bag = mutableEntityBagOf(32)

        assertEquals(32, bag.capacity)
        assertEquals(0, bag.size)
    }

    @Test
    fun addValueToBag() {
        val bag = MutableEntityBag()

        bag += entity(42, version = 0u)

        assertTrue(bag.isNotEmpty())
        assertEquals(1, bag.size)
        assertTrue(entity(42, version = 0u) in bag)
    }

    @Test
    fun removeValueFromBag() {
        val bag = MutableEntityBag(2)
        val e1 = entity(0, version = 0u)
        val e2 = entity(1, version = 0u)
        bag += e1
        bag += e2

        bag -= e1
        assertEquals(1, bag.size)
        assertEquals(entityBagOf(e2), bag)

        bag -= e2
        assertTrue(bag.isEmpty())
    }

    @Test
    fun clearAllValuesFromBag() {
        val bag = MutableEntityBag()
        bag += entity(42, version = 0u)
        bag += entity(43, version = 0u)

        bag.clear()

        assertEquals(0, bag.size)
        assertFalse { entity(42, version = 0u) in bag }
        assertFalse { entity(43, version = 0u) in bag }
    }

    @Test
    fun addValueToBagWithInsufficientCapacity() {
        val bag = MutableEntityBag(0)

        bag += entity(42, version = 0u)

        assertEquals(1, bag.size)
        assertEquals(entity(42, version = 0u), bag[0])
        assertEquals(1, bag.capacity)
    }

    @Test
    fun doNotResizeWhenBagHasSufficientCapacity() {
        val bag = MutableEntityBag(8)

        bag.ensureCapacity(7)

        assertEquals(8, bag.capacity)

        bag.ensureCapacity(8)

        assertEquals(8, bag.capacity)
    }

    @Test
    fun resizeWhenBagHasInsufficientCapacity() {
        val bag = MutableEntityBag(8)

        bag.ensureCapacity(9)

        assertEquals(10, bag.capacity)
    }

    @Test
    fun executeActionForEachValueOfBag() {
        val bag = MutableEntityBag(4)
        bag += entity(42, version = 0u)
        bag += entity(43, version = 0u)
        var numCalls = 0
        val valuesCalled = mutableListOf<Entity>()

        bag.forEach {
            ++numCalls
            valuesCalled.add(it)
        }


        assertEquals(2, numCalls)
        assertEquals(listOf(entity(42, version = 0u), entity(43, version = 0u)), valuesCalled)
    }

    @Test
    fun executeActionForEachValueOfBagIndexed() {
        val bag = MutableEntityBag(4)
        bag += entity(42, version = 0u)
        bag += entity(43, version = 0u)
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
        assertEquals(listOf(entity(42, version = 0u), entity(43, version = 0u)), valuesCalled)
        assertContentEquals(expectedIndices, actualIndices)
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan7() {
        val bag = MutableEntityBag()
        repeat(6) { bag += entity(6 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(6) {
            assertEquals(entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeLessThan50ButGreater7() {
        val bag = MutableEntityBag()
        repeat(8) { bag += entity(8 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(8) {
            assertEquals(entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun sortValuesByNormalEntityComparisonWithSizeGreater50() {
        val bag = MutableEntityBag()
        repeat(51) { bag += entity(51 - it, version = 0u) }

        bag.sort(compareEntity(configureWorld { }) { e1, e2 -> e1.id.compareTo(e2.id) })

        repeat(51) {
            assertEquals(entity(it + 1, version = 0u), bag[it])
        }
    }

    @Test
    fun cannotGetValueOfOutOfBoundsIndex() {
        val bag = MutableEntityBag(2)

        assertFailsWith<IndexOutOfBoundsException> { bag[-1] }
        assertFailsWith<IndexOutOfBoundsException> { bag[2] }
    }

    @Test
    fun testContainsAll() {
        assertTrue(testBag.containsAll(listOf(testEntity2, testEntity1)))
        assertTrue(testBag.containsAll(testBag))
        assertFalse(testBag.containsAll(listOf(entity(2, version = 0u))))
        assertTrue(testBag.containsAll(listOf(entity(1, version = 0u))))
        assertTrue(testBag.containsAll(emptyList()))
    }

    @Test
    fun testIsEmpty() {
        val testBag = MutableEntityBag(1)
        assertTrue(testBag.isEmpty())
        assertFalse(testBag.isNotEmpty())

        testBag += entity(0, version = 0u)
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
        val expected = mapOf(entity(2, version = 0u) to 2, entity(3, version = 0u) to 3)

        val actual = testBag.associateBy(
            { entity(it.id + 2, version = 0u) },
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
        val expected = mapOf(testEntity1 to 0, testEntity2 to 1, entity(2, version = 0u) to 2)
        val destination = mutableMapOf(entity(2, version = 0u) to 2)

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
        val expected = mapOf(0 to testEntity1, 1 to testEntity2, 2 to entity(2, version = 0u))
        val destination = mutableMapOf(2 to entity(2, version = 0u))

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
        val expected = mapOf(entity(2, version = 0u) to 2, entity(3, version = 0u) to 3, entity(4, version = 0u) to 4)
        val destination = mutableMapOf(entity(4, version = 0u) to 4)


        val actual = testBag.associateByTo(
            destination,
            { entity(it.id + 2, version = 0u) },
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
        assertEquals(0, MutableEntityBag().count())
        assertEquals(2, testBag.count())
        assertEquals(1, testBag.count { it.id == 0 })
    }

    @Test
    fun testFilter() {
        val expected = entityBagOf(testEntity1)
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
        val expected = entityBagOf(testEntity2)

        val actual = testBag.filterNot { it == testEntity1 }

        assertEquals(expected, actual)
    }

    @Test
    fun testFilterTo() {
        val testEntity3 = entity(2, version = 0u)
        val expected = entityBagOf(testEntity3, testEntity1)
        val expectedIndices = listOf(0, 1)
        val destination1 = mutableEntityBagOf(testEntity3)
        val destination2 = mutableEntityBagOf(testEntity3)

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
        val testEntity3 = entity(2, version = 0u)
        val expected = entityBagOf(testEntity3, testEntity2)
        val destination = mutableEntityBagOf(testEntity3)

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
        assertFailsWith<NoSuchElementException> { MutableEntityBag().first() }
        assertFailsWith<NoSuchElementException> { testBag.first { it.id == 3 } }
    }

    @Test
    fun testFirstOrNull() {
        assertEquals(testEntity1, testBag.firstOrNull())
        assertEquals(testEntity2, testBag.firstOrNull { it == testEntity2 })
        assertNull(MutableEntityBag().firstOrNull())
        assertNull(testBag.firstOrNull { it.id == 3 })
    }

    @Test
    fun testFlatMap() {
        val expectedInts = listOf(0, 0, 1, 2)
        val expectedEntities = entityBagOf(testEntity1, testEntity1)

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
        val expectedEntities = entityBagOf(testEntity1)

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
        val expected1 = mapOf(0 to mutableEntityBagOf(testEntity1), 1 to mutableEntityBagOf(testEntity2))
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
        val expected1 = mapOf(
            0 to mutableEntityBagOf(testEntity1),
            1 to mutableEntityBagOf(testEntity2),
            2 to mutableEntityBagOf(entity(2, version = 0u))
        )
        val expected2 = mapOf(0 to listOf(3), 1 to listOf(3), 2 to listOf(3))

        val actual = testBag.groupByTo(mutableMapOf(2 to mutableEntityBagOf(entity(2, version = 0u)))) { it.id }
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
        val first = MutableEntityBag()
        val second = MutableEntityBag()

        testBag.partitionTo(first, second) { it.id <= 0 }

        assertEquals(1, first.size)
        assertEquals(1, second.size)
        assertTrue(testEntity1 in first)
        assertTrue(testEntity2 in second)
    }

    @Test
    fun testRandom() {
        val actual = testBag.random()

        assertFailsWith<NoSuchElementException> { MutableEntityBag().random() }
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testRandomOrNull() {
        val actual = testBag.randomOrNull()

        assertNull(MutableEntityBag().randomOrNull())
        assertTrue(actual == testEntity1 || actual == testEntity2)
    }

    @Test
    fun testSingle() {
        assertEquals(testEntity1, testBagSingle.single())
        assertEquals(testEntity1, testBagSingle.single { it == testEntity1 })
        assertFailsWith<IllegalArgumentException> { testBag.single() }
        assertFailsWith<IllegalArgumentException> { entityBagOf(testEntity1, testEntity1).single { it == testEntity1 } }
        assertFailsWith<NoSuchElementException> { MutableEntityBag().single() }
        assertFailsWith<NoSuchElementException> { testBag.single { it.id == 3 } }
    }

    @Test
    fun testSingleOrNull() {
        assertEquals(testEntity1, testBagSingle.singleOrNull())
        assertEquals(testEntity1, testBagSingle.singleOrNull { it == testEntity1 })
        assertNull(testBag.singleOrNull())
        assertNull(entityBagOf(testEntity1, testEntity1).singleOrNull { it == testEntity1 })
        assertNull(MutableEntityBag().singleOrNull())
        assertNull(testBag.singleOrNull { it.id == 3 })
    }

    @Test
    fun testTake() {
        assertTrue(testBag.take(-1).isEmpty())
        assertTrue(testBag.take(0).isEmpty())
        assertEquals(entityBagOf(testEntity1), testBag.take(1))
        assertEquals(entityBagOf(testEntity1, testEntity2), testBag.take(2))
        assertEquals(entityBagOf(testEntity1, testEntity2), testBag.take(3))
    }

    @Test
    fun testEntityBagOf() {
        var bag = entityBagOf(testEntity1, testEntity2)
        assertEquals(2, bag.size)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }

        bag = entityBagOf()
        assertEquals(0, bag.size)
    }

    @Test
    fun testMutableEntityBagOf() {
        var bag = mutableEntityBagOf(testEntity1, testEntity2)
        assertEquals(2, bag.size)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }

        bag = mutableEntityBagOf()
        assertEquals(0, bag.size)
    }

    @Test
    fun `test plusAssign of an EntityBag`() {
        val bag = mutableEntityBagOf()
        val toAdd = entityBagOf(testEntity1, testEntity2)

        bag += toAdd

        assertEquals(2, bag.size)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }
    }

    @Test
    fun `test plusAssign of a family`() {
        val bag = mutableEntityBagOf()
        val testWorld = configureWorld { }
        val toAdd = Family(world = testWorld).apply {
            onEntityAdded(testEntity1, BitArray())
            onEntityAdded(testEntity2, BitArray())
        }

        bag += toAdd

        assertEquals(2, bag.size)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }
    }

    @Test
    fun `test minusAssign of an EntityBag`() {
        val bag = mutableEntityBagOf()
        val toRemove = entityBagOf(testEntity1, testEntity2)

        // remove of empty bag does nothing
        bag -= toRemove
        assertEquals(0, bag.size)

        bag += testEntity2
        bag += testEntity1
        assertEquals(2, bag.size)
        bag -= toRemove
        assertEquals(0, bag.size)
        assertFalse { testEntity1 in bag }
        assertFalse { testEntity2 in bag }
    }
}
