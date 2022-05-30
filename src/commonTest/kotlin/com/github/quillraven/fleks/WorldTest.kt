package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
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

private class WorldTestIteratingSystem : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    var numCalls = 0
    var numCallsEntity = 0
    val testInject: String = Inject.dependency()
    val mapper: ComponentMapper<WorldTestComponent> = Inject.componentMapper()

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        ++numCallsEntity
    }
}

private class WorldTestInitSystem : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    init {
        world.entity { add<WorldTestComponent>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestInitSystemExtraFamily : IteratingSystem(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    val extraFamily = world.family(
        anyOf = arrayOf(WorldTestComponent2::class),
        noneOf = arrayOf(WorldTestComponent::class)
    )

    init {
        world.entity { add<WorldTestComponent2>() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem : IntervalSystem() {
    val injName: String = Inject.dependency("name")
    val level: String = Inject.dependency("level")

    val name: String = injName

    override fun onTick() = Unit
}

private class WorldTestComponentListener : ComponentListener<WorldTestComponent> {
    val world = Inject.dependency<World>()
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) = Unit
    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) = Unit
}

internal class WorldTest {
    @Test
    fun createEmptyWorldFor32Entities() {
        val w = World { entityCapacity = 32 }

        assertEquals(0, w.numEntities)
        assertEquals(32, w.capacity)
    }

    @Test
    fun createEmptyWorldWith1NoArgsIntervalSystem() {
        val w = World { system(::WorldTestIntervalSystem) }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun getWorldSystems() {
        val w = World {
            system(::WorldTestIntervalSystem)
            component(::WorldTestComponent)
        }

        assertEquals(w.systemService.systems, w.systems)
    }

    @Test
    fun createEmptyWorldWith1InjectableArgsIteratingSystem() {
        val w = World {
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

            inject("42")
        }

        assertNotNull(w.system<WorldTestIteratingSystem>())
        assertEquals("42", w.system<WorldTestIteratingSystem>().testInject)
    }

    @Test
    fun createEmptyWorldWith2NamedInjectablesSystem() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = World {
            system(::WorldTestNamedDependencySystem)
            component(::WorldTestComponent)

            inject("name", expectedName)
            inject("level", "myLevel")
        }

        assertNotNull(w.system<WorldTestNamedDependencySystem>())
        assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name)
        assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level)
    }

    @Test
    fun cannotAddTheSameSystemTwice() {
        assertFailsWith<FleksSystemAlreadyAddedException> {
            World {
                system(::WorldTestIntervalSystem)
                system(::WorldTestIntervalSystem)
            }
        }
    }

    @Test
    fun cannotAccessSystemThatWasNotAdded() {
        val w = World {}

        assertFailsWith<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun cannotCreateSystemWhenInjectablesAreMissing() {
        assertFailsWith<FleksSystemDependencyInjectException> {
            World { system(::WorldTestIteratingSystem) }
        }
    }

    @Test
    fun cannotInjectTheSameTypeTwice() {
        assertFailsWith<FleksInjectableAlreadyAddedException> {
            World {
                inject("42")
                inject("42")
            }
        }
    }

    @Test
    fun createNewEntity() {
        val w = World {
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)
            inject("42")
        }

        val e = w.entity {
            add<WorldTestComponent> { x = 5f }
        }

        assertEquals(1, w.numEntities)
        assertEquals(0, e.id)
        assertEquals(5f, w.system<WorldTestIteratingSystem>().mapper[e].x)
    }

    @Test
    fun removeExistingEntity() {
        val w = World {}
        val e = w.entity()

        w.remove(e)

        assertEquals(0, w.numEntities)
    }

    @Test
    fun updateWorldWithDeltaTimeOf1() {
        val w = World {
            system(::WorldTestIntervalSystem)
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)

            inject("42")
        }
        w.system<WorldTestIteratingSystem>().enabled = false

        w.update(1f)

        assertEquals(1f, w.deltaTime)
        assertEquals(1, w.system<WorldTestIntervalSystem>().numCalls)
        assertEquals(0, w.system<WorldTestIteratingSystem>().numCalls)
    }

    @Test
    fun removeAllEntities() {
        val w = World {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun disposeWorld() {
        val w = World {
            system(::WorldTestIntervalSystem)
        }
        w.entity()
        w.entity()

        w.dispose()

        assertTrue(w.system<WorldTestIntervalSystem>().disposed)
        assertEquals(0, w.numEntities)
    }

    @Test
    fun createWorldWithComponentListener() {
        val w = World {
            component(::WorldTestComponent, ::WorldTestComponentListener)
        }
        val actualListeners = w.componentService.mapper<WorldTestComponent>().listeners

        assertEquals(1, actualListeners.size)
        assertEquals(w, (actualListeners[0] as WorldTestComponentListener).world)
    }

    @Test
    fun cannotAddSameComponentTwice() {
        assertFailsWith<FleksComponentAlreadyAddedException> {
            World {
                component(::WorldTestComponent)
                component(::WorldTestComponent)
            }
        }
    }

    @Test
    fun getMapper() {
        val w = World {
            component(::WorldTestComponent)
        }

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun throwExceptionWhenThereAreUnusedInjectables() {
        assertFailsWith<FleksUnusedInjectablesException> {
            World {
                inject("42")
            }
        }
    }

    @Test
    fun iterateOverAllActiveEntities() {
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
    fun createTwoWorldsWithSystems() {
        val w1 = World {
            system(::WorldTestInitSystem)
            component(::WorldTestComponent)
        }
        val w2 = World {
            system(::WorldTestInitSystem)
            component(::WorldTestComponent)
        }

        assertEquals(w1, w1.system<WorldTestInitSystem>().world)
        assertEquals(1, w1.numEntities)
        assertEquals(w2, w2.system<WorldTestInitSystem>().world)
        assertEquals(1, w2.numEntities)
    }

    @Test
    fun configureEntityAfterCreation() {
        val w = World {
            inject("test")
            system(::WorldTestIteratingSystem)
            component(::WorldTestComponent)
        }
        val e = w.entity()
        val mapper: ComponentMapper<WorldTestComponent> = w.mapper()

        w.configureEntity(e) { mapper.add(it) }
        w.update(0f)

        assertEquals(1, w.system<WorldTestIteratingSystem>().numCallsEntity)
    }

    @Test
    fun getWorldFamilyAfterWorldCreation() {
        // WorldTestInitSystem creates an entity in its init block
        // -> family must be dirty and has a size of 1
        val w = World {
            system(::WorldTestInitSystem)
            component(::WorldTestComponent)
        }

        val wFamily = w.family(allOf = arrayOf(WorldTestComponent::class))

        assertTrue(wFamily.family.isDirty)
        assertEquals(1, wFamily.family.numEntities)
    }

    @Test
    fun getWorldFamilyWithinSystemsConstructor() {
        // WorldTestInitSystemExtraFamily creates an entity in its init block and
        // also a family with a different configuration that the system itself
        // -> system family is empty and extra family contains 1 entity
        val w = World {
            system(::WorldTestInitSystemExtraFamily)
            component(::WorldTestComponent)
            component(::WorldTestComponent2)
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertEquals(1, s.extraFamily.family.numEntities)
        assertEquals(0, s.family.numEntities)
    }

    @Test
    fun iterateOverWorldFamily() {
        val w = World {
            component(::WorldTestComponent)
        }
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun sortedIterationOverWorldFamily() {
        val w = World {
            component(::WorldTestComponent)
        }
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
    fun cannotCreateFamilyWithoutAnyConfiguration() {
        val w = World {}

        assertFailsWith<FleksFamilyException> { w.family() }
        assertFailsWith<FleksFamilyException> { w.family(arrayOf(), arrayOf(), arrayOf()) }
    }
}
