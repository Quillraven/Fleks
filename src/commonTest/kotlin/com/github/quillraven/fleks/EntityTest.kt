package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class EntityTestComponent1 : Component<EntityTestComponent1> {
    override fun type() = EntityTestComponent1

    companion object : ComponentType<EntityTestComponent1>()
}

private class EntityTestComponent2 : Component<EntityTestComponent2> {
    override fun type() = EntityTestComponent2

    companion object : ComponentType<EntityTestComponent2>()
}

internal class EntityTest {
    private val testWorld = world(32) { }
    private val testEntityService = testWorld.entityService

    @Test
    fun createEmptyServiceFor32Entities() {
        val service = EntityService(testWorld, 32)

        assertEquals(0, service.numEntities)
        assertEquals(32, service.capacity)
    }

    @Test
    fun createEntityWithoutConfigurationAndSufficientCapacity() {
        val service = EntityService(testWorld, 32)

        val entity = service.create {}

        assertEquals(0, entity.id)
        assertEquals(1, service.numEntities)
    }

    @Test
    fun createEntityWithoutConfigurationAndInsufficientCapacity() {
        val service = EntityService(testWorld, 0)

        val entity = service.create {}

        assertEquals(0, entity.id)
        assertEquals(1, service.numEntities)
        assertEquals(1, service.capacity)
    }

    @Test
    fun removeAllEntities() {
        testEntityService.create {}
        testEntityService.create {}

        testEntityService.removeAll()

        assertEquals(2, testEntityService.recycledEntities.size)
        assertEquals(0, testEntityService.numEntities)
    }

    @Test
    fun removeAllEntitiesWithAlreadyRecycledEntities() {
        val recycled = testEntityService.create {}
        testEntityService.create {}
        testEntityService -= recycled

        testEntityService.removeAll()

        assertEquals(2, testEntityService.recycledEntities.size)
        assertEquals(0, testEntityService.numEntities)
    }

    @Test
    fun removeAllEntitiesWhenRemovalIsDelayed() {
        testEntityService.create {}
        testEntityService.create {}
        testEntityService.delayRemoval = true

        testEntityService.removeAll()

        assertTrue(testEntityService.delayRemoval)
        assertEquals(0, testEntityService.recycledEntities.size)
        assertEquals(2, testEntityService.numEntities)
    }

    @Test
    fun createRecycledEntity() {
        val expectedEntity = testEntityService.create { }
        testEntityService -= expectedEntity

        val actualEntity = testEntityService.create { }

        assertEquals(expectedEntity, actualEntity)
    }

    @Test
    fun delayEntityRemoval() {
        val entity = testEntityService.create { }
        testEntityService.delayRemoval = true

        testEntityService -= entity

        assertEquals(0, testEntityService.recycledEntities.size)
        assertEquals(1, testEntityService.numEntities)
    }

    @Test
    fun removeDelayedEntity() {
        val entity = testEntityService.create { }
        testEntityService.delayRemoval = true
        testEntityService -= entity

        // call two times to make sure that removals are only processed once
        testEntityService.cleanupDelays()
        testEntityService.cleanupDelays()

        assertFalse(testEntityService.delayRemoval)
        assertEquals(1, testEntityService.recycledEntities.size)
        assertEquals(0, testEntityService.numEntities)
    }

    @Test
    fun removeEntityTwice() {
        val entity = testEntityService.create { }

        testEntityService -= entity
        testEntityService -= entity

        assertEquals(1, testEntityService.recycledEntities.size)
        assertEquals(0, testEntityService.numEntities)
    }

    @Test
    fun testContainsEntity() {
        val e1 = testEntityService.create { }
        val e2 = testEntityService.create { }
        testEntityService -= e2
        val e3 = Entity(2)

        assertTrue(e1 in testEntityService)
        assertFalse(e2 in testEntityService)
        assertFalse(e3 in testEntityService)
        assertEquals(1, testEntityService.numEntities)
    }

    @Test
    fun testNestedEntityCreation() {
        var entity1 = Entity(-1)
        var entity2 = Entity(-1)

        testEntityService.create { e1 ->
            entity1 = e1
            e1 += EntityTestComponent1()

            testEntityService.create { e2 ->
                entity2 = e2
                e2 += EntityTestComponent1()
            }

            e1 += EntityTestComponent2()
        }

        assertEquals(0, entity1.id)
        assertEquals(1, entity2.id)
        assertTrue(testEntityService.compMasks[entity1.id][EntityTestComponent1.id])
        assertTrue(testEntityService.compMasks[entity1.id][EntityTestComponent2.id])
        assertTrue(testEntityService.compMasks[entity2.id][EntityTestComponent1.id])
        assertFalse(testEntityService.compMasks[entity2.id][EntityTestComponent2.id])
    }

    @Test
    fun testNestedEntityConfiguration() {
        val entity1 = testEntityService.create { }
        val entity2 = testEntityService.create { }

        testEntityService.configure(entity1) {
            entity1 += EntityTestComponent1()

            testEntityService.configure(entity2) {
                entity2 += EntityTestComponent1()
            }

            entity1 += EntityTestComponent2()
        }

        assertEquals(0, entity1.id)
        assertEquals(1, entity2.id)
        assertTrue(testEntityService.compMasks[entity1.id][EntityTestComponent1.id])
        assertTrue(testEntityService.compMasks[entity1.id][EntityTestComponent2.id])
        assertTrue(testEntityService.compMasks[entity2.id][EntityTestComponent1.id])
        assertFalse(testEntityService.compMasks[entity2.id][EntityTestComponent2.id])
    }
}
