package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data object Visible : EntityTag()

class TestTagSystem(var ticks: Int = 0) : IteratingSystem(family { all(Visible, TestTags.PLAYER) }) {
    override fun onTickEntity(entity: Entity) {
        ++ticks
    }
}

enum class TestTags : EntityTags by entityTagOf() {
    PLAYER, COLLISION
}

class TestTagComponent : Component<TestTagComponent> {
    override fun type() = TestTagComponent

    companion object : ComponentType<TestTagComponent>()
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
        val entity = world.entity {
            it += Visible
            it += TestTags.PLAYER
        }

        world.update(1000.toDuration(DurationUnit.SECONDS))
        assertEquals(1, testSystem.ticks)

        testSystem.ticks = 0
        with(world) { entity.configure { it -= Visible } }
        world.update(1000.toDuration(DurationUnit.SECONDS))
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

    @Test
    fun testRemoveEntityWithTag() {
        val world = configureWorld {}
        val entity = world.entity { it += TestTags.PLAYER }

        with(world) {
            entity.remove()

            assertFalse(entity in world)
        }
    }

    @Test
    fun testSnapshotWithTag() {
        val world = configureWorld {}
        val entity = world.entity { it += TestTags.PLAYER }

        val snapshot = world.snapshotOf(entity)
        assertEquals(TestTags.PLAYER, snapshot.tags[0])

        world.removeAll(true)
        with(world) {
            assertFalse(entity has TestTags.PLAYER)
            world.loadSnapshot(mapOf(entity to snapshot))
            assertTrue(entity has TestTags.PLAYER)
        }
    }

    @Test
    fun testSnapshotWithTagAndComponent() {
        val world = configureWorld {}
        val component = TestTagComponent()
        val entity = world.entity {
            it += TestTags.PLAYER
            it += component
        }

        val snapshot = world.snapshotOf(entity)
        assertEquals(TestTags.PLAYER, snapshot.tags[0])
        assertEquals(component, snapshot.components[0])

        world.removeAll(true)
        with(world) {
            assertFalse(entity has TestTags.PLAYER)
            assertFalse(entity has TestTagComponent)

            world.loadSnapshot(mapOf(entity to snapshot))

            assertTrue(entity has TestTags.PLAYER)
            assertTrue(entity has TestTagComponent)
        }
    }

    @Test
    fun testLoadSnapshotOfTag() {
        val world = configureWorld {}
        val entity = world.entity { }

        world.loadSnapshot(mapOf(entity to Snapshot(emptyList(), listOf(TestTags.PLAYER))))

        with(world) {
            assertTrue(entity has TestTags.PLAYER)
            assertTrue(TestTags.PLAYER.id in world.tagCache)
        }
    }

    @Test
    fun testSetListOfTags() {
        val world = configureWorld {}
        val entity = world.entity { it += TestTags.entries }

        with(world) {
            TestTags.entries.forEach {
                assertTrue(it in entity)
            }
        }
    }

    /*
    fun syntaxCheck() {
        val world = configureWorld {}
        world.entity {
            it += TestTags.PLAYER
            it += Visible
            it += listOf(Visible)
            it += TestTags.entries
            it += listOf(Visible, TestTags.PLAYER)

            it += TestTagComponent()
            it += listOf(TestTagComponent())

            it += TestTagComponent // <-- should not compile
        }
    }
     */
}
