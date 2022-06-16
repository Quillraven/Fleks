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
    val world: World = Inject.dependency()
    var numAdd = 0
    var numRemove = 0
    override fun onComponentAdded(entity: Entity, component: WorldTestComponent) {
        ++numAdd
    }

    override fun onComponentRemoved(entity: Entity, component: WorldTestComponent) {
        ++numRemove
    }
}

private class WorldTestFamilyListener : FamilyListener(
    allOfComponents = arrayOf(WorldTestComponent::class)
) {
    val world: World = Inject.dependency()
    var numAdd = 0
    var numRemove = 0

    override fun onEntityAdded(entity: Entity) {
        ++numAdd
    }

    override fun onEntityRemoved(entity: Entity) {
        ++numRemove
    }
}

private class WorldTestFamilyListenerMissingCfg : FamilyListener()

internal class WorldTest {
    @Test
    fun createEmptyWorldFor32Entities() {
        val w = World { entityCapacity = 32 }

        assertEquals(0, w.numEntities)
        assertEquals(32, w.capacity)
    }

    @Test
    fun createEmptyWorldWith1NoArgsIntervalSystem() {
        val w = World {
            systems {
                add(::WorldTestIntervalSystem)
            }
        }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun getWorldSystems() {
        val w = World {
            systems {
                add(::WorldTestIntervalSystem)
            }
        }

        assertEquals(w.systemService.systems, w.systems)
    }

    @Test
    fun createEmptyWorldWith1InjectableArgsIteratingSystem() {
        val w = World {
            injectables {
                add("42")
            }

            systems {
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

        }

        assertNotNull(w.system<WorldTestIteratingSystem>())
        assertEquals("42", w.system<WorldTestIteratingSystem>().testInject)
    }

    @Test
    fun createEmptyWorldWith2NamedInjectablesSystem() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = World {
            injectables {
                add("name", expectedName)
                add("level", "myLevel")
            }

            systems {
                add(::WorldTestNamedDependencySystem)
            }

            components {
                add(::WorldTestComponent)
            }
        }

        assertNotNull(w.system<WorldTestNamedDependencySystem>())
        assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name)
        assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level)
    }

    @Test
    fun cannotAddTheSameSystemTwice() {
        assertFailsWith<FleksSystemAlreadyAddedException> {
            World {
                systems {
                    add(::WorldTestIntervalSystem)
                    add(::WorldTestIntervalSystem)
                }
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
            World {
                components {
                    add(::WorldTestComponent)
                }

                systems {
                    add(::WorldTestIteratingSystem)
                }
            }
        }
    }

    @Test
    fun cannotInjectTheSameTypeTwice() {
        assertFailsWith<FleksInjectableAlreadyAddedException> {
            World {
                injectables {
                    add("42")
                    add("42")
                }
            }
        }
    }

    @Test
    fun createNewEntity() {
        val w = World {
            systems {
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

            injectables {
                add("42")
            }
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
            systems {
                add(::WorldTestIntervalSystem)
                add(::WorldTestIteratingSystem)
            }

            components {
                add(::WorldTestComponent)
            }

            injectables {
                add("42")
            }
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
            systems {
                add(::WorldTestIntervalSystem)
            }
        }
        w.entity()
        w.entity()

        w.dispose()

        assertTrue(w.system<WorldTestIntervalSystem>().disposed)
        assertEquals(0, w.numEntities)
    }

    @Test
    fun getMapper() {
        val w = World {
            components {
                add(::WorldTestComponent)
            }
        }

        val mapper = w.mapper<WorldTestComponent>()

        assertEquals(0, mapper.id)
    }

    @Test
    fun throwExceptionWhenThereAreUnusedInjectables() {
        assertFailsWith<FleksUnusedInjectablesException> {
            World {
                injectables {
                    add("42")
                }
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
    fun createTwoWorldsWithDifferentDependencies() {
        val w1 = World {
            systems {
                add(::WorldTestNamedDependencySystem)
            }

            injectables {
                add("name", "name1")
                add("level", "level1")
            }

        }
        val w2 = World {
            systems {
                add(::WorldTestNamedDependencySystem)
            }

            injectables {
                add("name", "name2")
                add("level", "level2")
            }

        }
        val s1 = w1.system<WorldTestNamedDependencySystem>()
        val s2 = w2.system<WorldTestNamedDependencySystem>()

        assertEquals("name1", s1.injName)
        assertEquals("level1", s1.level)
        assertEquals("name2", s2.injName)
        assertEquals("level2", s2.level)
    }

    @Test
    fun configureEntityAfterCreation() {
        val w = World {
            injectables {
                add("test")
            }

            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestIteratingSystem)
            }
        }
        val e = w.entity()
        val mapper: ComponentMapper<WorldTestComponent> = w.mapper()

        w.configureEntity(e) { mapper.add(it) }
        w.update(0f)

        assertEquals(1, w.system<WorldTestIteratingSystem>().numCallsEntity)
    }

    @Test
    fun getFamilyAfterWorldCreation() {
        // WorldTestInitSystem creates an entity in its init block
        // -> family must be dirty and has a size of 1
        val w = World {
            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystem)
            }
        }

        val wFamily = w.family(allOf = arrayOf(WorldTestComponent::class))

        assertTrue(wFamily.isDirty)
        assertEquals(1, wFamily.numEntities)
    }

    @Test
    fun getFamilyWithinSystemConstructor() {
        // WorldTestInitSystemExtraFamily creates an entity in its init block and
        // also a family with a different configuration that the system itself
        // -> system family is empty and extra family contains 1 entity
        val w = World {
            components {
                add(::WorldTestComponent2)
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystemExtraFamily)
            }
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertEquals(1, s.extraFamily.numEntities)
        assertEquals(0, s.family.numEntities)
    }

    @Test
    fun iterateOverFamily() {
        val w = World {
            components {
                add(::WorldTestComponent)
            }
        }
        val e1 = w.entity { add<WorldTestComponent>() }
        val e2 = w.entity { add<WorldTestComponent>() }
        val f = w.family(allOf = arrayOf(WorldTestComponent::class))
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun sortedIterationOverFamily() {
        val w = World {
            components {
                add(::WorldTestComponent)
            }
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

    @Test
    fun createWorldWithComponentListener() {
        val w = World {
            components {
                add(::WorldTestComponent, ::WorldTestComponentListener)
            }
        }
        val actualListeners = w.componentService.mapper<WorldTestComponent>().listeners

        assertEquals(1, actualListeners.size)
        assertEquals(w, (actualListeners[0] as WorldTestComponentListener).world)
    }

    @Test
    fun cannotAddSameComponentTwice() {
        assertFailsWith<FleksComponentAlreadyAddedException> {
            World {
                components {
                    add(::WorldTestComponent)
                    add(::WorldTestComponent)
                }
            }
        }
    }

    @Test
    fun notifyComponentListenerDuringSystemCreation() {
        val w = World {
            systems {
                add(::WorldTestInitSystem)
            }

            components {
                add(::WorldTestComponent, ::WorldTestComponentListener)
            }
        }
        val listener = w.mapper<WorldTestComponent>().listeners[0] as WorldTestComponentListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
    }

    @Test
    fun createWorldWithFamilyListener() {
        val w = World {
            components {
                add(::WorldTestComponent)
            }

            families {
                add(::WorldTestFamilyListener)
            }
        }
        val actualListeners = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners

        assertEquals(1, actualListeners.size)
        assertEquals(w, (actualListeners[0] as WorldTestFamilyListener).world)
    }

    @Test
    fun cannotAddSameFamilyListenerTwice() {
        assertFailsWith<FleksFamilyListenerAlreadyAddedException> {
            World {
                families {
                    add(::WorldTestFamilyListener)
                    add(::WorldTestFamilyListener)
                }
            }
        }
    }

    @Test
    fun cannotCreateFamilyListenerWithoutComponentConfiguration() {
        assertFailsWith<FleksFamilyListenerCreationException> {
            World {
                families {
                    add(::WorldTestFamilyListenerMissingCfg)
                }
            }
        }
    }

    @Test
    fun notifyFamilyListenerDuringSystemCreation() {
        val w = World {
            components {
                add(::WorldTestComponent)
            }

            systems {
                add(::WorldTestInitSystem)
            }

            families {
                add(::WorldTestFamilyListener)
            }
        }
        val listener = w.family(allOf = arrayOf(WorldTestComponent::class)).listeners[0] as WorldTestFamilyListener

        assertEquals(1, listener.numAdd)
        assertEquals(0, listener.numRemove)
        // verify that listener and system are not creating the same family twice
        assertEquals(1, w.allFamilies.size)
    }
}
