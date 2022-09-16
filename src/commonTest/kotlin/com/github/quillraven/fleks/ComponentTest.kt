package com.github.quillraven.fleks

import kotlin.test.*

internal class ComponentTest {

    private data class ComponentTestComponent(var x: Int = 0) : Component<ComponentTestComponent> {
        override fun type() = ComponentTestComponent

        companion object : ComponentType<ComponentTestComponent>()
    }

    private val testWorld = world { }
    private val testService = ComponentService(testWorld)
    private val testMapper = testService.mapper(ComponentTestComponent)

    @Test
    fun addEntityToMapperWithSufficientCapacity() {
        val entity = Entity(0)
        val expectedComp = ComponentTestComponent()

        testMapper.addInternal(entity, expectedComp)

        assertTrue(entity in testMapper)
        assertEquals(expectedComp, testMapper[entity])
    }

    @Test
    fun addEntityToMapperWithInsufficientCapacity() {
        val entity = Entity(10_000)
        val expectedComp = ComponentTestComponent()

        testMapper.addInternal(entity, expectedComp)

        assertTrue(entity in testMapper)
        assertEquals(expectedComp, testMapper[entity])
    }

    @Test
    fun returnsFalseWhenEntityIsNotPartOfMapper() {
        // within capacity
        assertFalse(Entity(0) in testMapper)
        // outside capacity
        assertFalse(Entity(10_000) in testMapper)
    }

    @Test
    fun removeExistingEntityFromMapper() {
        val entity = Entity(0)
        testMapper.addInternal(entity, ComponentTestComponent())

        testMapper.removeInternal(entity)

        assertFalse(entity in testMapper)
    }

    @Test
    fun getComponentOfExistingEntity() {
        val entity = Entity(0)
        testMapper.addInternal(entity, ComponentTestComponent(2))

        val cmp = testMapper[entity]

        assertEquals(2, cmp.x)
    }

    @Test
    fun cannotGetComponentOfNonExistingEntity() {
        val entity = Entity(0)

        assertFailsWith<FleksNoSuchEntityComponentException> { testMapper[entity] }
    }

    @Test
    fun getComponentOfNonExistingEntityWithSufficientCapacity() {
        val entity = Entity(0)

        val cmp = testMapper.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfNonExistingEntityWithoutSufficientCapacity() {
        val entity = Entity(2048)

        val cmp = testMapper.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfExistingEntityViaGetOrNull() {
        val entity = Entity(0)
        testMapper.addInternal(entity, ComponentTestComponent(2))

        val cmp = testMapper.getOrNull(entity)

        assertEquals(2, cmp?.x)
    }

    @Test
    fun doNotCreateTheSameMapperTwice() {
        val expected = testService.mapper(ComponentTestComponent)

        val actual = testService.mapper(ComponentTestComponent)

        assertSame(expected, actual)
    }

    @Test
    fun getMapperByComponentId() {
        val actual = testService.mapperByIndex(0)

        assertSame(testMapper, actual)
    }

    @Test
    fun addComponentWithHook() {
        var numAddCalls = 0
        var numRemoveCalls = 0
        val expectedEntity = Entity(1)
        val expectedComp = ComponentTestComponent()

        fun onAdd(world: World, entity: Entity, component: ComponentTestComponent) {
            assertEquals(testWorld, world)
            assertEquals(expectedEntity, entity)
            assertEquals(expectedComp, component)
            numAddCalls++
        }

        fun onRemove(world: World, entity: Entity, component: ComponentTestComponent) {
            assertEquals(testWorld, world)
            assertEquals(expectedEntity, entity)
            assertEquals(expectedComp, component)
            numRemoveCalls++
        }

        testMapper.addHook = ::onAdd
        testMapper.removeHook = ::onRemove

        testMapper.addInternal(expectedEntity, expectedComp)

        assertEquals(1, numAddCalls)
        assertEquals(0, numRemoveCalls)
    }

    @Test
    fun addComponentWithComponentListenerWhenComponentAlreadyPresent() {
        var numAddCalls = 0
        var numRemoveCalls = 0
        val expectedEntity = Entity(1)
        val expectedComp1 = ComponentTestComponent()
        val expectedComp2 = ComponentTestComponent()

        fun onAdd(world: World, entity: Entity, component: ComponentTestComponent) {
            assertSame(testWorld, world)
            assertSame(expectedEntity, entity)
            assertTrue { expectedComp1 === component || expectedComp2 === component }
            numAddCalls++
        }

        fun onRemove(world: World, entity: Entity, component: ComponentTestComponent) {
            assertSame(testWorld, world)
            assertSame(expectedEntity, entity)
            assertSame(expectedComp1, component)
            numRemoveCalls++
        }

        testMapper.addHook = ::onAdd
        testMapper.removeHook = ::onRemove

        testMapper.addInternal(expectedEntity, expectedComp1)
        // this should trigger onRemove of the first component
        testMapper.addInternal(expectedEntity, expectedComp2)

        assertEquals(2, numAddCalls)
        assertEquals(1, numRemoveCalls)
    }

    @Test
    fun addComponentIfItDoesNotExistYet() {
        val entity = Entity(1)

        testMapper.addOrUpdateInternal(
            entity,
            add = { ComponentTestComponent(1) },
            update = { cmp -> cmp.x = 2 }
        )

        assertTrue(entity in testMapper)
        assertEquals(1, testMapper[entity].x)
    }

    @Test
    fun updateComponentIfItAlreadyExists() {
        val entity = Entity(1)
        testMapper.addInternal(entity, ComponentTestComponent(0))

        testMapper.addOrUpdateInternal(
            entity,
            add = { ComponentTestComponent(1) },
            update = { cmp -> cmp.x = 2 }
        )

        assertTrue(entity in testMapper)
        assertEquals(2, testMapper[entity].x)
    }
}
