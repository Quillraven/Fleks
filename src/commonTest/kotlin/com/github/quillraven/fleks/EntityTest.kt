package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data class EntityTestComponent(var x: Float = 0f) : Component<EntityTestComponent> {
    override fun type(): ComponentType<EntityTestComponent> = EntityTestComponent

    companion object : ComponentType<EntityTestComponent>()
}

internal class EntityTest {
    private val testWorld = world { }
    private val testEntityService = EntityService(testWorld, 32)

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
    fun updateComponentOfEntityIfItAlreadyExists() {
        val expectedEntity = testEntityService.create { }
        val mapper = testWorld[EntityTestComponent]

        testEntityService.configure(expectedEntity) {
            mapper.addInternal(expectedEntity, EntityTestComponent(1f))
            mapper.addOrUpdateInternal(
                expectedEntity,
                add = { EntityTestComponent(1f) },
                update = { testCmp -> testCmp.x++ }
            )
        }

        assertTrue(expectedEntity in mapper)
        assertEquals(2f, mapper[expectedEntity].x)
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
        testEntityService.remove(recycled)

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
        testEntityService.remove(expectedEntity)

        val actualEntity = testEntityService.create { }

        assertEquals(expectedEntity, actualEntity)
    }

    @Test
    fun delayEntityRemoval() {
        val entity = testEntityService.create { }
        testEntityService.delayRemoval = true

        testEntityService.remove(entity)

        assertEquals(0, testEntityService.recycledEntities.size)
        assertEquals(1, testEntityService.numEntities)
    }

    @Test
    fun removeDelayedEntity() {
        val entity = testEntityService.create { }
        testEntityService.delayRemoval = true
        testEntityService.remove(entity)

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

        testEntityService.remove(entity)
        testEntityService.remove(entity)

        assertEquals(1, testEntityService.recycledEntities.size)
        assertEquals(0, testEntityService.numEntities)
    }

    @Test
    fun testContainsEntity() {
        val e1 = testEntityService.create { }
        val e2 = testEntityService.create { }
        testEntityService.remove(e2)
        val e3 = Entity(2)

        assertTrue(e1 in testEntityService)
        assertFalse(e2 in testEntityService)
        assertFalse(e3 in testEntityService)
        assertEquals(1, testEntityService.numEntities)
    }
}
