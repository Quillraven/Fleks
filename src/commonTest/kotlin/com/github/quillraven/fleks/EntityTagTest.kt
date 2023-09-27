package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.*

data object Visible : EntityTag()

class TestTagSystem(var ticks: Int = 0) : IteratingSystem(family { all(Visible) }) {
    override fun onTickEntity(entity: Entity) {
        ++ticks
    }
}

enum class TestTags : EntityTags by entityTagOf() {
    PLAYER, COLLISION
}

class EntityTagTest {

    @Test
    fun testTagAssignment() {
        val world = configureWorld {}
        val entity = world.entity { it += Visible }

        with(world) {
            assertTrue(Visible in entity)

            entity.configure { it -= Visible }

            assertFalse(Visible in entity)
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
            assertTrue(entity has TestTags.PLAYER)

            entity.configure { it -= TestTags.PLAYER }

            assertTrue(entity hasNo TestTags.PLAYER)
        }
    }
}
