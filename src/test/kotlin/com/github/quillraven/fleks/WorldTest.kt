package com.github.quillraven.fleks

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertNotNull

private data class WorldTestComponent(var x: Float = 0f)

private class WorldTestIntervalSystem : IntervalSystem() {
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }
}

@AllOf([WorldTestComponent::class])
private class WorldTestIteratingSystem(
    val testInject: String,
    val mapper: ComponentMapper<WorldTestComponent>
) : IteratingSystem() {
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
        val w = World { system<WorldTestIntervalSystem>() }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun `create empty world with 1 injectable args IteratingSystem`() {
        val w = World {
            system<WorldTestIteratingSystem>()

            inject("42")
        }

        assertAll(
            { assertNotNull(w.system<WorldTestIteratingSystem>()) },
            { assertEquals("42", w.system<WorldTestIteratingSystem>().testInject) }
        )
    }

    @Test
    fun `cannot add the same system twice`() {
        assertThrows<FleksSystemAlreadyAddedException> {
            World {
                system<WorldTestIntervalSystem>()
                system<WorldTestIntervalSystem>()
            }
        }
    }

    @Test
    fun `cannot access a system that was not added`() {
        val w = World {}

        assertThrows<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun `cannot create a system when injectables are missing`() {
        assertThrows<FleksSystemCreationException> {
            World { system<WorldTestIteratingSystem>() }
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
        val w = World {
            system<WorldTestIteratingSystem>()
            inject("42")
        }

        val e = w.entity {
            add<WorldTestComponent> { x = 5f }
        }

        assertAll(
            { assertEquals(1, w.numEntities) },
            { assertEquals(0, e.id) },
            { assertEquals(5f, w.system<WorldTestIteratingSystem>().mapper[e].x) }
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
            system<WorldTestIntervalSystem>()
            system<WorldTestIteratingSystem>()
            inject("42")
        }
        w.system<WorldTestIteratingSystem>().enabled = false

        w.update(1f)

        assertAll(
            { assertEquals(1f, w.deltaTime) },
            { assertEquals(1, w.system<WorldTestIntervalSystem>().numCalls) },
            { assertEquals(0, w.system<WorldTestIteratingSystem>().numCalls) }
        )
    }
}
