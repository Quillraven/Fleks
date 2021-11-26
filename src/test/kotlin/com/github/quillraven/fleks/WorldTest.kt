package com.github.quillraven.fleks

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull

private data class TestComponent(val x: Float = 0f)

private class TestIntervalSystem : IntervalSystem() {
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }
}

@AllOf([TestComponent::class])
private class TestIteratingSystem(val testInject: String) : IteratingSystem() {
    var numCalls = 0

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) = Unit
}

internal class WorldTest {
    @Test
    fun `create empty world for 32 entities`() {
        val w = World { entityCapacity = 32 }

        assertAll(
            { assertEquals(0, w.numEntities) },
            { assertEquals(32, w.capacity) }
        )
    }

    @Test
    fun `create empty world with 1 no-args IntervalSystem`() {
        val w = World { system<TestIntervalSystem>() }

        assertNotNull(w.system<TestIntervalSystem>())
    }

    @Test
    fun `create empty world with 1 injectable args IteratingSystem`() {
        val w = World {
            system<TestIteratingSystem>()

            inject("42")
        }

        assertAll(
            { assertNotNull(w.system<TestIteratingSystem>()) },
            { assertEquals("42", w.system<TestIteratingSystem>().testInject) }
        )
    }

    @Test
    fun `cannot add the same system twice`() {
        assertThrows<FleksSystemAlreadyAddedException> {
            World {
                system<TestIntervalSystem>()
                system<TestIntervalSystem>()
            }
        }
    }

    @Test
    fun `cannot inject the same type twice`() {
        assertThrows<FleksInjectableAlreadyAddedException> {
            World {
                inject("42")
                inject("42")
            }
        }
    }

    @Test
    fun `create new entity`() {
        val w = World {}

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

        assertEquals(0, w.numEntities)
    }

    @Test
    fun `update world with deltaTime of 1`() {
        val w = World {
            system<TestIntervalSystem>()
            system<TestIteratingSystem>()
            inject("42")
        }
        w.system<TestIteratingSystem>().enabled = false

        w.update(1f)

        assertAll(
            { assertEquals(1f, w.deltaTime) },
            { assertEquals(1, w.system<TestIntervalSystem>().numCalls) },
            { assertEquals(0, w.system<TestIteratingSystem>().numCalls) }
        )
    }
}
