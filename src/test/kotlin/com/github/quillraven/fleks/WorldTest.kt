package com.github.quillraven.fleks

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private data class WorldTestComponent(var x: Float = 0f)

private class WorldTestIntervalSystem : IntervalSystem() {
    var numCalls = 0
    var disposed = false

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        disposed = true
    }
}

private class WorldTestIteratingSystem : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    var numCalls = 0

    val testInject: String = Inject.dependency()
    val mapper: ComponentMapper<WorldTestComponent> = Inject.componentMapper()

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem : IntervalSystem() {
    val _name: String = Inject.dependency("name")
    val level: String = Inject.dependency("level")

    val name: String = _name

    override fun onTick() = Unit
}

private class WorldTestComponentListener : ComponentListener<WorldTestComponent> {
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) = Unit
    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) = Unit
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
        val w = World { system(::WorldTestIntervalSystem) }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun `create empty world with 1 injectable args IteratingSystem`() {
        val w = World {
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

            inject("42")
        }

        assertAll(
            { assertNotNull(w.system<WorldTestIteratingSystem>()) },
            { assertEquals("42", w.system<WorldTestIteratingSystem>().testInject) }
        )
    }

    @Test
    fun `create empty world with 2 named injectables system`() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = World {
            system(::WorldTestNamedDependencySystem)
            component(::WorldTestComponent)

            inject("name", expectedName)
            inject("level", "myLevel")
        }

        assertAll(
            { assertNotNull(w.system<WorldTestNamedDependencySystem>()) },
            { assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name) },
            { assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level) }
        )
    }

    @Test
    fun `cannot add the same system twice`() {
        assertThrows<FleksSystemAlreadyAddedException> {
            World {
                system(::WorldTestIntervalSystem)
                system(::WorldTestIntervalSystem)
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
        assertThrows<FleksSystemDependencyInjectException> {
            World { system(::WorldTestIteratingSystem) }
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
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)
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
            system(::WorldTestIntervalSystem)
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

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

    @Test
    fun `remove all entities`() {
        val w = World {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun `dispose world`() {
        val w = World {
            system(::WorldTestIntervalSystem)
        }
        w.entity()
        w.entity()

        w.dispose()

        assertAll(
            { assertTrue(w.system<WorldTestIntervalSystem>().disposed) },
            { assertEquals(0, w.numEntities) }
        )
    }

    @Test
    fun `create world with ComponentListener`() {
        val w = World {
            component(::WorldTestComponent, ::WorldTestComponentListener)
        }

        assertEquals(1, w.componentService.mapper<WorldTestComponent>().listeners.size)
    }

    @Test
    fun `cannot add same Component twice`() {
        assertThrows<FleksComponentAlreadyAddedException> {
            World {
                component(::WorldTestComponent)
                component(::WorldTestComponent)
            }
        }
    }

    @Test
    fun `get mapper`() {
        val w = World {
            component(::WorldTestComponent)
        }

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun `throw exception when there are unused injectables`() {
        assertThrows<FleksUnusedInjectablesException> {
            World {
                inject("42")
            }
        }
    }

    @Test
    fun `iterate over all active entities`() {
        val w = World {}
        val e1 = w.entity()
        val e2 = w.entity()
        val e3 = w.entity()
        w.remove(e2)
        val actualEntities = mutableListOf<Entity>()

        w.forEach { actualEntities.add(it) }

        assertContentEquals(listOf(e1, e3), actualEntities)
    }
}
