package com.github.quillraven.fleks

import kotlin.test.*

internal class ComponentTest {

    private data class ComponentTestComponent(var x: Int = 0) : Component<ComponentTestComponent> {
        override fun type() = ComponentTestComponent

        companion object : ComponentType<ComponentTestComponent>()
    }

    private data class ComponentTestComponent2(
        val realType: ComponentType<ComponentTestComponent2>
    ) : Component<ComponentTestComponent2> {
        override fun type(): ComponentType<ComponentTestComponent2> = realType

        companion object {
            val type1 = componentTypeOf<ComponentTestComponent2>()
            val type2 = componentTypeOf<ComponentTestComponent2>()
        }
    }

    private data class ComponentTestWithLifecycleComponent(
        val expectedWorld: World,
        val expectedEntity: Entity,
        var numAddCalls: Int = 0,
        var numRemoveCalls: Int = 0
    ) : Component<ComponentTestWithLifecycleComponent> {
        override fun type() = ComponentTestWithLifecycleComponent

        override fun World.onAddComponent(entity: Entity) {
            assertEquals(expectedWorld, this)
            assertEquals(expectedEntity, entity)
            numAddCalls++
        }

        override fun World.onRemoveComponent(entity: Entity) {
            assertEquals(expectedWorld, this)
            assertEquals(expectedEntity, entity)
            numRemoveCalls++
        }

        companion object : ComponentType<ComponentTestWithLifecycleComponent>()
    }

    private val testWorld = configureWorld { }
    private val testService = ComponentService().also { it.world = testWorld }
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
    fun addAndRemoveComponentWithLifecycleMethod() {
        val expectedEntity = Entity(1)
        val expectedComp = ComponentTestWithLifecycleComponent(testWorld, expectedEntity)

        val testHolderForLifecycleComponent = testService.holder(ComponentTestWithLifecycleComponent)

        assertEquals(0, expectedComp.numAddCalls)
        assertEquals(0, expectedComp.numRemoveCalls)
        testHolderForLifecycleComponent[expectedEntity] = expectedComp
        assertEquals(1, expectedComp.numAddCalls)
        assertEquals(0, expectedComp.numRemoveCalls)
        testHolderForLifecycleComponent -= expectedEntity
        assertEquals(1, expectedComp.numAddCalls)
        assertEquals(1, expectedComp.numRemoveCalls)
    }

    @Test
    fun addAndReplaceComponentWithLifecycleMethod() {
        val expectedEntity = Entity(1)
        val expectedComp1 = ComponentTestWithLifecycleComponent(testWorld, expectedEntity)
        val expectedComp2 = ComponentTestWithLifecycleComponent(testWorld, expectedEntity)

        val testHolderForLifecycleComponent = testService.holder(ComponentTestWithLifecycleComponent)

        testHolderForLifecycleComponent[expectedEntity] = expectedComp1
        assertEquals(1, expectedComp1.numAddCalls)
        assertEquals(0, expectedComp1.numRemoveCalls)

        // Should trigger onRemoveComponent on expectedComp1
        testHolderForLifecycleComponent[expectedEntity] = expectedComp2
        assertEquals(1, expectedComp1.numAddCalls)
        assertEquals(1, expectedComp1.numRemoveCalls)
        assertEquals(1, expectedComp2.numAddCalls)
        assertEquals(0, expectedComp2.numRemoveCalls)
    }

    @Test
    fun cannotRemoveNonExistingEntityFromHolderWithInsufficientCapacity() {
        val holder = testService.holder(ComponentTestComponent)
        val entity = Entity(10_000)

        assertFailsWith<IndexOutOfBoundsException> { holder -= entity }
    }

    @Test
    fun testComponentTypeOf() {
        assertFalse { ComponentTestComponent2.type1.id == ComponentTestComponent2.type2.id }
    }
}
