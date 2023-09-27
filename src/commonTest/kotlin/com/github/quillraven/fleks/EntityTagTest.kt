package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.*

data object Visible : EntityTag()

class TestTagSystem(var ticks: Int = 0) : IteratingSystem(family { all(Visible) }) {
    override fun onTickEntity(entity: Entity) {
        ++ticks
    }
}

enum class TestTags : EntityTags by entityTagsOf() {
    PLAYER, COLLISION
}

class EntityTagTest {

    @Test
    fun testTagAssignment() {
        val world = configureWorld {}
        val entity = world.entity { it += Visible }

        with(world) {
            assertTrue(entity[Visible])
//             entity.getOrNull(Visible) // <- compile error because Visible does not extend Component
//             val cmp : Visible = entity[Visible] // <- compile error because Visible does not extend Component and therefore 'cmp' must be of type Boolean
//             assertTrue(Visible in entity) // <- compile error because Visible does not extend Component
//             assertTrue(entity has Visible) // <- compile error because Visible does not extend Component
//             assertTrue(entity hasNo  Visible) // <- compile error because Visible does not extend Component

            entity.configure {
                it -= Visible
            }

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
        val entity = world.entity { it += Visible }

        world.update(1f)
        assertEquals(1, testSystem.ticks)

        testSystem.ticks = 0
        with(world) { entity.configure { it -= Visible } }
        world.update(1f)
        assertEquals(0, testSystem.ticks)
    }

    @Test
    fun testEnumTags() {
        assertNotEquals(TestTags.PLAYER.id, TestTags.COLLISION.id)
        assertNotEquals(Visible.id, TestTags.PLAYER.id)
        assertNotEquals(Visible.id, TestTags.COLLISION.id)

        val world = configureWorld {}
        val entity = world.entity { it += TestTags.PLAYER }

        with(world) {
            assertTrue(entity[TestTags.PLAYER])

            entity.configure { it -= TestTags.PLAYER }

            assertFalse(entity[TestTags.PLAYER])
        }
    }
}
