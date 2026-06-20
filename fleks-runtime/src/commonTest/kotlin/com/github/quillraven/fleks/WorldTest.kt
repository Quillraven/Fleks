package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class ComponentA(var value: Int) : Component {
    companion object : ComponentType<ComponentA>()
}

private class ComponentB(var name: String) : Component {
    companion object : ComponentType<ComponentB>()
}

private class ComponentC : Component {
    companion object : ComponentType<ComponentC>()
}

class WorldTest {

    @Test
    fun `create allocates unique entities`() {
        val world = World()
        val e1 = world.create()
        val e2 = world.create()

        assertTrue(world.entityRegistry.isAlive(e1))
        assertTrue(world.entityRegistry.isAlive(e2))
        assertEquals(0, e1.index)
        assertEquals(1, e2.index)
        assertEquals(0, e1.version)
        assertEquals(0, e2.version)
    }

    @Test
    fun `destroyImmediate recycles entity indices`() {
        val world = World()
        val e1 = world.create()
        world.destroyImmediate(e1)

        assertFalse(world.entityRegistry.isAlive(e1))

        val e2 = world.create()
        assertEquals(e1.index, e2.index)
        assertEquals(1, e2.version) // version incremented upon recycling
    }

    @Test
    fun `addImmediate adds components and transitions archetypes`() {
        val world = World()
        val entity = world.create()

        assertNull(world.get(entity, ComponentA))

        world.addImmediate(entity, ComponentA, ComponentA(42))
        val a = world.get(entity, ComponentA)
        assertNotNull(a)
        assertEquals(42, a.value)

        assertTrue(world.has(entity, ComponentA))
        assertFalse(world.has(entity, ComponentB))

        // Add another component
        world.addImmediate(entity, ComponentB, ComponentB("Fleks"))
        val b = world.get(entity, ComponentB)
        assertNotNull(b)
        assertEquals("Fleks", b.name)

        assertTrue(world.has(entity, ComponentA))
        assertTrue(world.has(entity, ComponentB))
    }

    @Test
    fun `removeImmediate transitions archetypes correctly`() {
        val world = World()
        val entity = world.create()

        world.addImmediate(entity, ComponentA, ComponentA(100))
        world.addImmediate(entity, ComponentB, ComponentB("Test"))

        assertTrue(world.has(entity, ComponentA))
        assertTrue(world.has(entity, ComponentB))

        world.removeImmediate(entity, ComponentA)
        assertFalse(world.has(entity, ComponentA))
        assertTrue(world.has(entity, ComponentB))

        world.removeImmediate(entity, ComponentB)
        assertFalse(world.has(entity, ComponentB))
    }

    @Test
    fun `deferred operations are applied on flush`() {
        val world = World()
        val entity = world.create()

        world.add(entity, ComponentA, ComponentA(10))
        world.add(entity, ComponentB, ComponentB("Deferred"))

        // Before flush, entity should not have the components
        assertFalse(world.has(entity, ComponentA))
        assertFalse(world.has(entity, ComponentB))

        world.flush()

        // After flush, components must be present
        assertTrue(world.has(entity, ComponentA))
        assertTrue(world.has(entity, ComponentB))
        assertEquals(10, world.get(entity, ComponentA)?.value)
        assertEquals("Deferred", world.get(entity, ComponentB)?.name)

        // Remove component and destroy entity deferred
        world.remove(entity, ComponentA)
        world.destroy(entity)

        assertTrue(world.has(entity, ComponentA)) // Still alive until flush
        assertTrue(world.entityRegistry.isAlive(entity))

        world.flush()

        assertFalse(world.entityRegistry.isAlive(entity))
    }

    @Test
    fun `swap and pop preserves row and archetype mapping for remaining entities`() {
        val world = World()
        val e1 = world.create()
        val e2 = world.create()
        val e3 = world.create()

        // Add component to all to place them in the same archetype
        world.addImmediate(e1, ComponentC, ComponentC())
        world.addImmediate(e2, ComponentC, ComponentC())
        world.addImmediate(e3, ComponentC, ComponentC())

        val archId = world.entityRegistry.getArchetypeId(e1)
        val arch = world.archetypeRegistry.archetypes[archId]

        assertEquals(archId, world.entityRegistry.getArchetypeId(e2))
        assertEquals(archId, world.entityRegistry.getArchetypeId(e3))

        // Rows should be 0, 1, 2
        assertEquals(0, world.entityRegistry.getRow(e1))
        assertEquals(1, world.entityRegistry.getRow(e2))
        assertEquals(2, world.entityRegistry.getRow(e3))
        assertEquals(3, arch.size)

        // Destroy e2 (row 1)
        world.destroyImmediate(e2)

        // e2 is dead
        assertFalse(world.entityRegistry.isAlive(e2))
        // arch size decremented to 2
        assertEquals(2, arch.size)
        // e1 is still at row 0
        assertEquals(0, world.entityRegistry.getRow(e1))
        // e3 should have been moved from row 2 to row 1 (swap-and-pop)
        assertEquals(1, world.entityRegistry.getRow(e3))
        assertEquals(e3.index, arch.entities[1])
    }
}
