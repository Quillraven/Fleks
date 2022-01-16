package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private class EntityTestListener : EntityListener {
    var numCalls = 0
    var entityReceived = Entity(-1)
    lateinit var cmpMaskReceived: BitArray

    override fun onEntityCfgChanged(entity: Entity, cmpMask: BitArray) {
        ++numCalls
        entityReceived = entity
        cmpMaskReceived = cmpMask
    }
}

private data class EntityTestComponent(var x: Float = 0f)

internal class EntityTest {
    @Test
    fun `create empty service for 32 entities`() {
        val cmpService = ComponentService()

        val entityService = EntityService(32, cmpService)

        assertAll(
            { assertEquals(0, entityService.numEntities) },
            { assertEquals(32, entityService.capacity) }
        )
    }

    @Test
    fun `create entity without configuration and sufficient capacity`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)

        val entity = entityService.create {}

        assertAll(
            { assertEquals(0, entity.id) },
            { assertEquals(1, entityService.numEntities) }
        )
    }

    @Test
    fun `create entity without configuration and insufficient capacity`() {
        val cmpService = ComponentService()
        val entityService = EntityService(0, cmpService)

        val entity = entityService.create {}

        assertAll(
            { assertEquals(0, entity.id) },
            { assertEquals(1, entityService.numEntities) },
            { assertEquals(1, entityService.capacity) },
        )
    }

    @Test
    fun `create entity with configuration and custom listener`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        var processedEntity = Entity(-1)

        val expectedEntity = entityService.create { entity ->
            add<EntityTestComponent>()
            processedEntity = entity
        }
        val mapper = cmpService.mapper<EntityTestComponent>()

        assertAll(
            { assertEquals(1, listener.numCalls) },
            { assertEquals(expectedEntity, listener.entityReceived) },
            { assertTrue(listener.cmpMaskReceived[0]) },
            { assertEquals(0f, mapper[listener.entityReceived].x) },
            { assertEquals(expectedEntity, processedEntity) }
        )
    }

    @Test
    fun `remove component from entity with custom listener`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { add<EntityTestComponent>() }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.configureEntity(expectedEntity) { mapper.remove(expectedEntity) }

        assertAll(
            { assertEquals(1, listener.numCalls) },
            { assertEquals(expectedEntity, listener.entityReceived) },
            { assertFalse(listener.cmpMaskReceived[0]) },
            { assertFalse(expectedEntity in mapper) }
        )
    }

    @Test
    fun `add component to entity with custom listener`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.configureEntity(expectedEntity) { mapper.add(expectedEntity) }

        assertAll(
            { assertEquals(1, listener.numCalls) },
            { assertEquals(expectedEntity, listener.entityReceived) },
            { assertTrue(listener.cmpMaskReceived[0]) },
            { assertTrue(expectedEntity in mapper) }
        )
    }

    @Test
    fun `remove entity with a component immediately with custom listener`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        val expectedEntity = entityService.create { add<EntityTestComponent>() }
        val mapper = cmpService.mapper<EntityTestComponent>()
        entityService.addEntityListener(listener)

        entityService.remove(expectedEntity)

        assertAll(
            { assertEquals(1, listener.numCalls) },
            { assertEquals(expectedEntity, listener.entityReceived) },
            { assertFalse(listener.cmpMaskReceived[0]) },
            { assertFalse(expectedEntity in mapper) }
        )
    }

    @Test
    fun `remove all entities`() {
        val entityService = EntityService(32, ComponentService())
        entityService.create {}
        entityService.create {}

        entityService.removeAll()

        assertAll(
            { assertEquals(2, entityService.recycledEntities.size) },
            { assertEquals(0, entityService.numEntities) }
        )
    }

    @Test
    fun `remove all entities with already recycled entities`() {
        val entityService = EntityService(32, ComponentService())
        val recycled = entityService.create {}
        entityService.create {}
        entityService.remove(recycled)

        entityService.removeAll()

        assertAll(
            { assertEquals(2, entityService.recycledEntities.size) },
            { assertEquals(0, entityService.numEntities) }
        )
    }

    @Test
    fun `remove all entities when removal is delayed`() {
        val entityService = EntityService(32, ComponentService())
        entityService.create {}
        entityService.create {}
        entityService.delayRemoval = true
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.removeAll()

        assertAll(
            { assertEquals(0, listener.numCalls) },
            { assertTrue(entityService.delayRemoval) }
        )
    }

    @Test
    fun `create recycled entity`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val expectedEntity = entityService.create { }
        entityService.remove(expectedEntity)

        val actualEntity = entityService.create { }

        assertEquals(expectedEntity, actualEntity)
    }

    @Test
    fun `delay entity removal`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        entityService.delayRemoval = true

        entityService.remove(entity)

        assertEquals(0, listener.numCalls)
    }

    @Test
    fun `remove delayed entity`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)
        entityService.delayRemoval = true
        entityService.remove(entity)

        // call two times to make sure that removals are only processed once
        entityService.cleanupDelays()
        entityService.cleanupDelays()

        assertAll(
            { assertFalse(entityService.delayRemoval) },
            { assertEquals(1, listener.numCalls) }
        )
    }

    @Test
    fun `remove existing listener`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.removeEntityListener(listener)

        assertFalse(listener in entityService)
    }

    @Test
    fun `remove entity twice`() {
        val cmpService = ComponentService()
        val entityService = EntityService(32, cmpService)
        val entity = entityService.create { }
        val listener = EntityTestListener()
        entityService.addEntityListener(listener)

        entityService.remove(entity)
        entityService.remove(entity)

        assertAll(
            { assertEquals(1, entityService.recycledEntities.size) },
            { assertEquals(1, listener.numCalls) }
        )
    }
}
