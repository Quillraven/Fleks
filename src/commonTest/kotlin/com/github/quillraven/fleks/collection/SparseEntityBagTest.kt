package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.configureWorld
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SparseEntityBagTest {

    private val testEntity1 = Entity(0)
    private val testEntity2 = Entity(1)
    private val testEntity3 = Entity(2)

    @Test
    fun createEmptyBag() {
        val bag = SparseEntityBag()

        assertEquals(0, bag.size)
        assertFalse { testEntity1 in bag }
    }

    @Test
    fun addValueToBag() {
        val bag = SparseEntityBag()
        val versionBefore = bag.version

        bag += testEntity1

        assertEquals(1, bag.size)
        assertTrue { testEntity1 in bag }
        assertFalse { testEntity2 in bag }
        assertEquals(versionBefore + 1, bag.version)
    }

    @Test
    fun addValueToBagWithHighIdGrowsSparseArray() {
        val bag = SparseEntityBag(2)
        val highIdEntity = Entity(100)

        bag += highIdEntity

        assertTrue { highIdEntity in bag }
        assertEquals(1, bag.size)
    }

    @Test
    fun iterateOverDenselyPackedEntities() {
        val bag = SparseEntityBag()
        bag += testEntity3
        bag += testEntity1
        bag += testEntity2
        val valuesCalled = mutableListOf<Entity>()

        bag.forEach { valuesCalled += it }

        assertContentEquals(listOf(testEntity3, testEntity1, testEntity2), valuesCalled)
    }

    @Test
    fun removeValueFromMiddleOfDenseArray() {
        val bag = SparseEntityBag()
        bag += testEntity1
        bag += testEntity2
        bag += testEntity3
        val versionBefore = bag.version

        bag -= testEntity2

        assertEquals(2, bag.size)
        assertFalse { testEntity2 in bag }
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity3 in bag }
        assertEquals(versionBefore + 1, bag.version)
    }

    @Test
    fun removeValueFromLastDensePosition() {
        val bag = SparseEntityBag()
        bag += testEntity1
        bag += testEntity2

        bag -= testEntity2

        assertEquals(1, bag.size)
        assertFalse { testEntity2 in bag }
        assertTrue { testEntity1 in bag }
    }

    @Test
    fun removeNonExistingValueDoesNothing() {
        val bag = SparseEntityBag()
        bag += testEntity1

        bag -= testEntity2

        assertEquals(1, bag.size)
        assertTrue { testEntity1 in bag }
    }

    @Test
    fun readdValueAfterRemoval() {
        val bag = SparseEntityBag()
        bag += testEntity1
        bag += testEntity2
        bag -= testEntity1
        assertEquals(1, bag.size)

        bag += testEntity1

        assertEquals(2, bag.size)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }
        bag.forEach { assertTrue { it in bag } }
    }

    @Test
    fun sortEntitiesAndKeepSparseArrayConsistent() {
        val bag = SparseEntityBag()
        bag += testEntity3
        bag += testEntity1
        bag += testEntity2
        val comparator = compareEntity(configureWorld { }) { eA, eB -> eA.id.compareTo(eB.id) }

        bag.sort(comparator)

        val actual = mutableListOf<Entity>()
        bag.dense.forEach { actual += it }
        assertEquals(listOf(testEntity1, testEntity2, testEntity3), actual)
        assertTrue { testEntity1 in bag }
        assertTrue { testEntity2 in bag }
        assertTrue { testEntity3 in bag }
        bag.forEach { assertTrue { it in bag } }
    }

    @Test
    fun sortThenRemoveKeepsBagConsistent() {
        val bag = SparseEntityBag()
        bag += testEntity3
        bag += testEntity1
        bag += testEntity2
        val comparator = compareEntity(configureWorld { }) { eA, eB -> eA.id.compareTo(eB.id) }

        bag.sort(comparator)
        bag -= testEntity1

        assertEquals(2, bag.size)
        assertFalse { testEntity1 in bag }
        assertTrue { testEntity2 in bag }
        assertTrue { testEntity3 in bag }
        bag.forEach { assertTrue { it in bag } }
    }

    @Test
    fun clearAllValuesFromBag() {
        val bag = SparseEntityBag()
        bag += testEntity1
        bag += testEntity2

        bag.clear()

        assertEquals(0, bag.size)
        assertFalse { testEntity1 in bag }
        assertFalse { testEntity2 in bag }
    }

    @Test
    fun versionIncrementsOnStructuralChangesOnly() {
        val bag = SparseEntityBag()
        val versionBefore = bag.version

        bag.contains(testEntity1)

        assertEquals(versionBefore, bag.version)

        bag += testEntity1
        assertEquals(versionBefore + 1, bag.version)

        bag.sort(compareEntity(configureWorld { }) { eA, eB -> eA.id.compareTo(eB.id) })
        assertEquals(versionBefore + 2, bag.version)

        bag -= testEntity1
        assertEquals(versionBefore + 3, bag.version)
    }
}
