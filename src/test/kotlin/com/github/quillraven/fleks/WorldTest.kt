package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*

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

@AllOf([WorldTestComponent::class])
private class WorldTestFamilyListener(
    val world: World
) : FamilyListener {
    var numAdd = 0
    var numRemove = 0

    override fun onEntityAdded(entity: Entity) {
        ++numAdd
    }

    override fun onEntityRemoved(entity: Entity) {
        ++numRemove
    }
}

private class WorldTestFamilyListenerMissingAnnotations : FamilyListener

internal class WorldTest {
    @Test
    fun `create empty world for 32 entities`() {
        val w = world { entityCapacity = 32 }

        assertAll(
            { assertEquals(0, w.numEntities) },
            { assertEquals(32, w.capacity) }
        )
    }

    @Test
    fun `create empty world with 1 no-args IntervalSystem`() {
        val w = world {
            systems {
                add<WorldTestIntervalSystem>()
            }
        }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun `get world systems`() {
        val w = world {
            systems {
                add<WorldTestIntervalSystem>()
            }
        }

        assertEquals(w.systemService.systems, w.systems)
    }

    @Test
    fun `create empty world with 1 injectable args IteratingSystem`() {
        val w = world {
            systems {
                add<WorldTestIteratingSystem>()
            }

            injectables {
                add("42")
            }
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
        val w = world {
            systems {
                add<WorldTestNamedDependencySystem>()
            }

            injectables {
                add("name", expectedName)
                add("level", "myLevel")
            }
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
            world {
                systems {
                    add<WorldTestIntervalSystem>()
                    add<WorldTestIntervalSystem>()
                }
            }
        }
    }

    @Test
    fun `cannot access a system that was not added`() {
        val w = world {}

        assertThrows<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun `cannot create a system when injectables are missing`() {
        assertThrows<FleksReflectionException> {
            world {
                systems {
                    add<WorldTestIteratingSystem>()
                }
            }
        }
    }

    @Test
    fun `cannot inject the same type twice`() {
        assertThrows<FleksInjectableAlreadyAddedException> {
            world {
                injectables {
                    add("42")
                    add("42")
                }
            }
        }
    }

    @Test
    fun `create new entity`() {
        val w = world {
            systems {
                add<WorldTestIteratingSystem>()
            }

            injectables {
                add("42")
            }
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
        val w = world {}
        val e = w.entity()

        w.remove(e)

        assertEquals(0, w.numEntities)
    }

    @Test
    fun `update world with deltaTime of 1`() {
        val w = world {
            systems {
                add<WorldTestIntervalSystem>()
                add<WorldTestIteratingSystem>()
            }

            injectables {
                add("42")
            }
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
        val w = world {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun `dispose world`() {
        val w = world {
            systems {
                add<WorldTestIntervalSystem>()
            }
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
    fun `get mapper`() {
        val w = world {}

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun `throw exception when there are unused injectables`() {
        assertThrows<FleksUnusedInjectablesException> {
            world {
                injectables {
                    add("42")
                }
            }
        }
    }

    @Test
    fun `cannot use local classes as dependencies`() {
        class Local

        assertThrows<FleksInjectableWithoutNameException> {
            world {
                injectables {
                    add(Local())
                }
            }
        }
    }

    @Test
    fun `iterate over all active entities`() {
        val w = world {}
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
        val w1 = world {
            systems {
                add<WorldTestInitSystem>()
            }
        }
        val w2 = world {
            systems {
                add<WorldTestInitSystem>()
            }
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
        val w = world {
            injectables {
                add("test")
            }

            systems {
                add<WorldTestIteratingSystem>()
            }
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
        val w = world {
            systems {
                add<WorldTestInitSystem>()
            }
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
        val w = world {
            systems {
                add<WorldTestInitSystemExtraFamily>()
            }
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertAll(
            { assertEquals(1, s.extraFamily.numEntities) },
            { assertEquals(0, s.family.numEntities) }
        )
    }

    @Test
    fun `iterate over Family`() {
        val w = world {}
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun `sorted iteration over Family`() {
        val w = world {}
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
        val w = world {}
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
        val w = world {}

        assertThrows<FleksFamilyException> { w.family() }
        assertThrows<FleksFamilyException> { w.family(arrayOf(), arrayOf(), arrayOf()) }
    }

    @Test
    fun `create world with ComponentListener`() {
        val w = world {
            components {
                add<WorldTestComponentListener>()
            }
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
            world {
                components {
                    add<WorldTestComponentListener>()
                    add<WorldTestComponentListener>()
                }
            }
        }
    }

    @Test
    fun `notify ComponentListener during system creation`() {
        val w = world {
            systems {
                add<WorldTestInitSystem>()
            }

            components {
                add<WorldTestComponentListener>()
            }
        }
        val listener = w.mapper<WorldTestComponent>().listeners[0] as WorldTestComponentListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
    }

    @Test
    fun `create world with FamilyListener`() {
        val w = world {
            families {
                add<WorldTestFamilyListener>()
            }
        }
        val actualListeners = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners

        assertAll(
            { assertEquals(1, actualListeners.size) },
            { assertEquals(w, (actualListeners[0] as WorldTestFamilyListener).world) }
        )
    }

    @Test
    fun `cannot add same FamilyListener twice`() {
        assertThrows<FleksFamilyListenerAlreadyAddedException> {
            world {
                families {
                    add<WorldTestFamilyListener>()
                    add<WorldTestFamilyListener>()
                }
            }
        }
    }

    @Test
    fun `cannot create FamilyListener without annotations`() {
        assertThrows<FleksFamilyListenerCreationException> {
            world {
                families {
                    add<WorldTestFamilyListenerMissingAnnotations>()
                }
            }
        }
    }

    @Test
    fun `notify FamilyListener during system creation`() {
        val w = world {
            systems {
                add<WorldTestInitSystem>()
            }

            families {
                add<WorldTestFamilyListener>()
            }
        }
        val listener = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners[0] as WorldTestFamilyListener

        assertAll(
            { assertEquals(1, listener.numAdd) },
            { assertEquals(0, listener.numRemove) },
            // verify that listener and system are not creating the same family twice
            { assertEquals(1, w.allFamilies.size) }
        )
    }

    @Test
    fun `test family first and empty functions`() {
        val w = world { }

        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        assertTrue(f.isEmpty)
        assertFalse(f.isNotEmpty)
        assertThrows<NoSuchElementException> { f.first() }
        assertNull(f.firstOrNull())

        val e = w.entity { add<WorldTestComponent>() }
        assertFalse(f.isEmpty)
        assertTrue(f.isNotEmpty)
        assertEquals(e, f.first())
        assertEquals(e, f.firstOrNull())
    }

    @Test
    fun `test family 'first' during iteration`() {
        val w = world { }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        w.entity { add<WorldTestComponent>() }

        val iteratedEntities = mutableListOf<Entity>()
        f.forEach {
            if (iteratedEntities.isEmpty()) {
                // add second entity to family which should not be considered during this iteration
                w.entity { add<WorldTestComponent>() }
            }
            // make a call to 'first' which updates the family's entity bag internally.
            // This must NOT affect the current forEach iteration.
            f.first()
            iteratedEntities.add(it)
        }

        // verify that only a single iteration happened
        assertContentEquals(listOf(Entity(0)), iteratedEntities)
        assertEquals(2, f.numEntities)
    }

    @Test
    fun `test entity removal with noneOf family`() {
        // entity that gets removed has no components and is therefore
        // part of any family that only has a noneOf configuration.
        // However, such entities still need to be removed of those families.
        val w = world { }
        val family = w.family(noneOf = arrayOf(WorldTestComponent::class))
        val e = w.entity { }

        family.updateActiveEntities()
        assertTrue(e.id in family.entitiesBag)

        w.remove(e)
        family.updateActiveEntities()
        assertFalse(e.id in family.entitiesBag)
    }
}
