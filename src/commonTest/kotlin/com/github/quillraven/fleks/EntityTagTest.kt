package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data object Visible : EntityTag()

class TestTagSystem(var ticks: Int = 0) : IteratingSystem(family { all(Visible) }) {
    override fun onTickEntity(entity: Entity) {
        ++ticks
    }
}

class EntityTagTest {

    @Test
    fun testTagAssignment() {
        val world = configureWorld {}
        val entity = world.entity { it[Visible] = true }

        with(world) {
            assertTrue(entity[Visible])
            // entity.getOrNull(Visible) // <- compile error because Visible does not extend Component
            // val cmp : Visible = entity[Visible] // <- compile error because Visible does not extend Component and therefore 'cmp' must be of type Boolean
            // assertTrue(Visible in entity) // <- compile error because Visible does not extend Component
            // assertTrue(entity has Visible) // <- compile error because Visible does not extend Component
            // assertTrue(entity hasNo  Visible) // <- compile error because Visible does not extend Component

            entity.configure { it[Visible] = false }

            assertFalse(entity[Visible])
        }
    }

    @Test
    fun testTagSystem() {
        lateinit var testSystem: TestTagSystem
        val world = configureWorld {
            systems {
                add(TestTagSystem().also { testSystem = it })
            }
        }
        val entity = world.entity { it[Visible] = true }

        world.update(1f)
        assertEquals(1, testSystem.ticks)

        testSystem.ticks = 0
        with(world) { entity.configure { it[Visible] = false } }
        world.update(1f)
        assertEquals(0, testSystem.ticks)
    }
}
