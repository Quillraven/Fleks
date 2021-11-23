package com.github.quillraven.fleks

import io.mockk.spyk
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private data class TestComponent(val x: Float = 0f)

private class TestIntervalSystem : IntervalSystem() {
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }
}

@AllOf([TestComponent::class])
private class TestIteratingSystem(val testInject: String) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

internal class WorldTest {
    @Test
    fun `create empty world for 32 entities`() {
        val w = World { entityCapacity = 32 }

        assertAll(
            { assertEquals(0, w.numEntities) },
            { assertEquals(32, w.entityService.cmpMasks.capacity) },
            { assertTrue(w.systemService.systems.isEmpty()) },
            { assertTrue(w.componentService.mappers.isEmpty()) }
        )
    }

    @Test
    fun `create empty world with 1 no-args IntervalSystem`() {
        val w = World { system<TestIntervalSystem>() }

        assertAll(
            { assertEquals(1, w.systemService.systems.size) },
            { assertNotNull(w.system<TestIntervalSystem>()) }
        )
    }

    @Test
    fun `create empty world with 1 injectable args IteratingSystem`() {
        val w = World {
            system<TestIteratingSystem>()

            inject("42")
        }

        // should also create a component mapper for TestComponent which is used by TestIteratingSystem
        assertAll(
            { assertEquals(1, w.systemService.systems.size) },
            { assertNotNull(w.system<TestIteratingSystem>()) },
            { assertEquals("42", w.system<TestIteratingSystem>().testInject) },
            { assertEquals(1, w.componentService.mappers.size) },
            { assertEquals(TestComponent::class.java, w.componentService.mapper(0).cstr.declaringClass) }
        )
    }

    @Test
    fun `throw FleksSystemAlreadyAddedException`() {
        assertThrows<FleksSystemAlreadyAddedException> {
            World {
                system<TestIntervalSystem>()
                system<TestIntervalSystem>()
            }
        }
    }

    @Test
    fun `throw FleksInjectableAlreadyAddedException`() {
        assertThrows<FleksInjectableAlreadyAddedException> {
            World {
                inject("42")
                inject("42")
            }
        }
    }

    @Test
    fun `create new entity`() {
        val w = spyk(World {})

        val e = w.entity()

        assertAll(
            { assertEquals(1, w.numEntities) },
            { assertEquals(0, e.id) }
        )
    }

    @Test
    fun `remove existing entity`() {
        val w = World {}
        val e = w.entity()

        w.remove(e)

        assertAll(
            { assertEquals(0, w.numEntities) },
            { assertEquals(e, w.entityService.recycledEntities.last()) }
        )
    }

    @Test
    fun `update world with deltaTime of 1`() {
        val w = World {
            system<TestIntervalSystem>()
        }

        w.update(1f)

        assertAll(
            { assertEquals(1f, w.deltaTime) },
            { assertEquals(1, w.system<TestIntervalSystem>().numCalls) }
        )
    }
}
