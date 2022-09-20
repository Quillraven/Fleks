package com.github.quillraven.fleks

import kotlin.test.*

internal class ComponentTest {

    private data class ComponentTestComponent(var x: Int = 0) : Component<ComponentTestComponent> {
        override fun type() = ComponentTestComponent

        companion object : ComponentType<ComponentTestComponent>()
    }

    private val testWorld = world { }
    private val testService = ComponentService(testWorld)
    private val testHolder = testService.holder(ComponentTestComponent)

    @Test
    fun addEntityToHolderWithSufficientCapacity() {
        val entity = Entity(0)
        val expectedComp = ComponentTestComponent()

        testHolder[entity] = expectedComp

        assertTrue(entity in testHolder)
        assertEquals(expectedComp, testHolder[entity])
    }

    @Test
    fun addEntityToHolderWithInsufficientCapacity() {
        val entity = Entity(10_000)
        val expectedComp = ComponentTestComponent()

        testHolder[entity] = expectedComp

        assertTrue(entity in testHolder)
        assertEquals(expectedComp, testHolder[entity])
    }

    @Test
    fun returnsFalseWhenEntityIsNotPartOfHolder() {
        // within capacity
        assertFalse(Entity(0) in testHolder)
        // outside capacity
        assertFalse(Entity(10_000) in testHolder)
    }

    @Test
    fun removeExistingEntityFromHolder() {
        val entity = Entity(0)
        testHolder[entity] = ComponentTestComponent()

        testHolder -= entity

        assertFalse(entity in testHolder)
    }

    @Test
    fun getComponentOfExistingEntity() {
        val entity = Entity(0)
        testHolder[entity] = ComponentTestComponent(2)

        val cmp = testHolder[entity]

        assertEquals(2, cmp.x)
    }

    @Test
    fun cannotGetComponentOfNonExistingEntity() {
        val entity = Entity(0)

        assertFailsWith<FleksNoSuchEntityComponentException> { testHolder[entity] }
    }

    @Test
    fun getComponentOfNonExistingEntityWithSufficientCapacity() {
        val entity = Entity(0)

        val cmp = testHolder.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfNonExistingEntityWithoutSufficientCapacity() {
        val entity = Entity(2048)

        val cmp = testHolder.getOrNull(entity)

        assertNull(cmp)
    }

    @Test
    fun getComponentOfExistingEntityViaGetOrNull() {
        val entity = Entity(0)
        testHolder[entity] = ComponentTestComponent(2)

        val cmp = testHolder.getOrNull(entity)

        assertEquals(2, cmp?.x)
    }

    @Test
    fun doNotCreateTheSameHolderTwice() {
        val expected = testService.holder(ComponentTestComponent)

        val actual = testService.holder(ComponentTestComponent)

        assertSame(expected, actual)
    }

    @Test
    fun getHolderByComponentId() {
        val actual = testService.holderByIndex(0)

        assertSame(testHolder, actual)
    }

    @Test
    fun addComponentWithHook() {
        var numAddCalls = 0
        var numRemoveCalls = 0
        val expectedEntity = Entity(1)
        val expectedComp = ComponentTestComponent()

        val onAdd: ComponentHook<ComponentTestComponent> = { world, entity, component ->
            assertEquals(testWorld, world)
            assertEquals(expectedEntity, entity)
            assertEquals(expectedComp, component)
            numAddCalls++
        }

        val onRemove: ComponentHook<ComponentTestComponent> = { world, entity, component ->
            assertEquals(testWorld, world)
            assertEquals(expectedEntity, entity)
            assertEquals(expectedComp, component)
            numRemoveCalls++
        }

        testHolder.addHook = onAdd
        testHolder.removeHook = onRemove

        testHolder[expectedEntity] = expectedComp

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

        val onAdd: ComponentHook<ComponentTestComponent> = { world, entity, component ->
            assertSame(testWorld, world)
            assertSame(expectedEntity, entity)
            assertTrue { expectedComp1 === component || expectedComp2 === component }
            numAddCalls++
        }

        val onRemove: ComponentHook<ComponentTestComponent> = { world, entity, component ->
            assertSame(testWorld, world)
            assertSame(expectedEntity, entity)
            assertSame(expectedComp1, component)
            numRemoveCalls++
        }

        testHolder.addHook = onAdd
        testHolder.removeHook = onRemove

        testHolder[expectedEntity] = expectedComp1
        // this should trigger onRemove of the first component
        testHolder[expectedEntity] = expectedComp2

        assertEquals(2, numAddCalls)
        assertEquals(1, numRemoveCalls)
    }

    @Test
    fun addComponentIfItDoesNotExistYet() {
        val entity = Entity(1)

        testHolder.setOrUpdate(
            entity,
            factory = { ComponentTestComponent(1) },
            update = { cmp -> cmp.x = 2 }
        )

        assertTrue(entity in testHolder)
        assertEquals(1, testHolder[entity].x)
    }

    @Test
    fun updateComponentIfItAlreadyExists() {
        val entity = Entity(1)
        testHolder[entity] = ComponentTestComponent(0)

        testHolder.setOrUpdate(
            entity,
            factory = { ComponentTestComponent(1) },
            update = { cmp -> cmp.x = 2 }
        )

        assertTrue(entity in testHolder)
        assertEquals(2, testHolder[entity].x)
    }
}
