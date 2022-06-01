package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContentEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private data class WorldTestComponent(var x: Float = 0f)

private class WorldTestComponent2

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

@AllOf([WorldTestComponent::class])
private class WorldTestIteratingSystem(
    val testInject: String,
    val mapper: ComponentMapper<WorldTestComponent>
) : IteratingSystem() {
    var numCalls = 0
    var numCallsEntity = 0

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        ++numCallsEntity
    }
}

@AllOf([WorldTestComponent::class])
private class WorldTestInitSystem : IteratingSystem() {
    init {
        world.entity { add<WorldTestComponent>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

@AllOf([WorldTestComponent::class])
private class WorldTestInitSystemExtraFamily : IteratingSystem() {
    val extraFamily = world.family(
        anyOf = arrayOf(WorldTestComponent2::class),
        noneOf = arrayOf(WorldTestComponent::class)
    )

    init {
        world.entity { add<WorldTestComponent2>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem(
    @Qualifier("name") _name: String,
    @Qualifier("level") val level: String
) : IntervalSystem() {
    val name: String = _name

    override fun onTick() = Unit
}

private class WorldTestComponentListener(
    val world: World
) : ComponentListener<WorldTestComponent> {
    var numAdd = 0
    var numRemove = 0
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) {
        ++numAdd
    }

    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) {
        ++numRemove
    }
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
    fun `get world systems`() {
        val w = World { system<WorldTestIntervalSystem>() }

        assertEquals(w.systemService.systems, w.systems)
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
    fun `create empty world with 2 named injectables system`() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = World {
            system<WorldTestNamedDependencySystem>()

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
        assertThrows<FleksReflectionException> {
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
            system<WorldTestIntervalSystem>()
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
            componentListener<WorldTestComponentListener>()
        }
        val actualListeners = w.componentService.mapper<WorldTestComponent>().listeners

        assertAll(
            { assertEquals(1, actualListeners.size) },
            { assertEquals(w, (actualListeners[0] as WorldTestComponentListener).world) }
        )
    }

    @Test
    fun `cannot add same ComponentListener twice`() {
        assertThrows<FleksComponentListenerAlreadyAddedException> {
            World {
                componentListener<WorldTestComponentListener>()
                componentListener<WorldTestComponentListener>()
            }
        }
    }

    @Test
    fun `get mapper`() {
        val w = World {}

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
    fun `cannot use local classes as dependencies`() {
        class Local

        assertThrows<FleksInjectableWithoutNameException> {
            World {
                inject(Local())
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

    @Test
    fun `create two worlds with systems`() {
        val w1 = World {
            system<WorldTestInitSystem>()
        }
        val w2 = World {
            system<WorldTestInitSystem>()
        }

        assertAll(
            { assertEquals(w1, w1.system<WorldTestInitSystem>().world) },
            { assertEquals(1, w1.numEntities) },
            { assertEquals(w2, w2.system<WorldTestInitSystem>().world) },
            { assertEquals(1, w2.numEntities) },
        )
    }

    @Test
    fun `configure entity after creation`() {
        val w = World {
            inject("test")
            system<WorldTestIteratingSystem>()
        }
        val e = w.entity()
        val mapper: ComponentMapper<WorldTestComponent> = w.mapper()

        w.configureEntity(e) { mapper.add(it) }
        w.update(0f)

        assertEquals(1, w.system<WorldTestIteratingSystem>().numCallsEntity)
    }

    @Test
    fun `get Family after world creation`() {
        // WorldTestInitSystem creates an entity in its init block
        // -> family must be dirty and has a size of 1
        val w = World {
            system<WorldTestInitSystem>()
        }

        val wFamily = w.family(allOf = arrayOf(WorldTestComponent::class))

        assertAll(
            { assertTrue(wFamily.isDirty) },
            { assertEquals(1, wFamily.numEntities) }
        )
    }

    @Test
    fun `get Family within system's constructor`() {
        // WorldTestInitSystemExtraFamily creates an entity in its init block and
        // also a family with a different configuration that the system itself
        // -> system family is empty and extra family contains 1 entity
        val w = World {
            system<WorldTestInitSystemExtraFamily>()
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertAll(
            { assertEquals(1, s.extraFamily.numEntities) },
            { assertEquals(0, s.family.numEntities) }
        )
    }

    @Test
    fun `iterate over Family`() {
        val w = World {}
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun `sorted iteration over Family`() {
        val w = World {}
        val e1 = w.entity { add<WorldTestComponent> { x = 15f } }
        val e2 = w.entity { add<WorldTestComponent> { x = 10f } }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()
        val mapper = w.mapper<WorldTestComponent>()

        f.sort(compareEntity { entity1, entity2 -> mapper[entity1].x.compareTo(mapper[entity2].x) })
        f.forEach { actualEntities.add(it) }

        assertEquals(arrayListOf(e2, e1), actualEntities)
    }

    @Test
    fun `configure entity during Family iteration`() {
        // family excludes entities of Component2 which gets added during iteration
        // -> family must be empty after iteration
        val w = World {}
        w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class), noneOf = arrayOf(WorldTestComponent2::class))
        val mapper = w.mapper<WorldTestComponent2>()

        f.forEach { entity ->
            this.configureEntity(entity) {
                mapper.add(it)
            }
        }

        assertEquals(0, f.numEntities)
    }

    @Test
    fun `cannot create family without any configuration`() {
        val w = World {}

        assertThrows<FleksFamilyException> { w.family() }
        assertThrows<FleksFamilyException> { w.family(arrayOf(), arrayOf(), arrayOf()) }
    }

    @Test
    fun `notify ComponentListener during system creation`() {
        val w = World {
            system<WorldTestInitSystem>()

            componentListener<WorldTestComponentListener>()
        }
        val listener = w.mapper<WorldTestComponent>().listeners[0] as WorldTestComponentListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
    }
}
