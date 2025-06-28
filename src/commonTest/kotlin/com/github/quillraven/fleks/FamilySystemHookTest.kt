package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.*

private class SimpleTestComponent : Component<SimpleTestComponent> {
    companion object : ComponentType<SimpleTestComponent>()
    override fun type() = SimpleTestComponent
}

private class OnAddHookSystem(world: GenericWorld? = null) : IteratingSystem<Unit>(
    world = world ?: World.CURRENT_WORLD!!, family = world?.family { all(SimpleTestComponent) } ?: family { all(SimpleTestComponent) }
), FamilyOnAdd {

    val addEntityHandled
        get() = addEntityHandledCount > 0
    var addEntityHandledCount = 0

    override fun onAddEntity(entity: Entity) {
        addEntityHandledCount++
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class OnAddHookSystem2(world: GenericWorld? = null) : IteratingSystem<Unit>(
    world = world ?: World.CURRENT_WORLD!!, family = world?.family { all(SimpleTestComponent) } ?: family { all(SimpleTestComponent) }
), FamilyOnAdd {

    var addEntityHandledCount = 0

    override fun onAddEntity(entity: Entity) {
        addEntityHandledCount++
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class OnAddHookSystem3(world: GenericWorld? = null) : IteratingSystem<Unit>(
    world = world ?: World.CURRENT_WORLD!!, family = world?.family { all(SimpleTestComponent) } ?: family { all(SimpleTestComponent) }
), FamilyOnAdd {

    var addEntityHandledCount = 0

    override fun onAddEntity(entity: Entity) {
        addEntityHandledCount++
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class OnRemoveHookSystem(world: GenericWorld? = null) : IteratingSystem<Unit>(
    world = world ?: World.CURRENT_WORLD!!, family = world?.family { all(SimpleTestComponent) } ?: family { all(SimpleTestComponent) }
), FamilyOnRemove {

    val removeEntityHandled
        get() = removeEntityHandledCount > 0
    var removeEntityHandledCount = 0

    override fun onRemoveEntity(entity: Entity) {
        removeEntityHandledCount++
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class IllegalOnAddHookSystem : IntervalSystem<Unit>(), FamilyOnAdd {
    override fun onTick() = Unit
    override fun onAddEntity(entity: Entity) = Unit
}

private class IllegalOnRemoveHookSystem : IntervalSystem<Unit>(), FamilyOnRemove {
    override fun onTick() = Unit
    override fun onRemoveEntity(entity: Entity) = Unit
}

internal class FamilySystemHookTest {

    @Test
    fun onAddHookSystem() {
        val world = configureWorld {
            systems {
                add(OnAddHookSystem())
            }
        }

        world.entity { it += SimpleTestComponent() }

        val system = world.system<OnAddHookSystem>()
        assertTrue { system.addEntityHandled }
    }

    @Test
    fun onAddHookSystemPostConfiguration() {
        val world = configureWorld {}.also {
            it.add(OnAddHookSystem(world = it))
        }
        val system = world.system<OnAddHookSystem>()

        // add an entity after the system is added, should trigger the onAddEntity hook
        world.entity { it += SimpleTestComponent() }
        assertEquals(1, system.addEntityHandledCount)

        // remove system
        world -= system

        // add an entity after the system is removed, should not trigger the onAddEntity hook
        world.entity { it += SimpleTestComponent() }
        assertEquals(1, system.addEntityHandledCount)

        // add the system back, existing entities don't trigger the hook
        world += system
        assertNotNull(system.family.addHook)
        assertEquals(1, system.addEntityHandledCount)

        // add an entity after the system is added back, should trigger the onAddEntity hook
        world.entity { it += SimpleTestComponent() }
        assertEquals(2, system.addEntityHandledCount)
    }

    @Test
    fun onRemoveHookSystem() {
        val world = configureWorld {
            systems {
                add(OnRemoveHookSystem())
            }
        }

        val entity = world.entity { it += SimpleTestComponent() }
        world -= entity

        val system = world.system<OnRemoveHookSystem>()
        assertTrue { system.removeEntityHandled }
    }

    @Test
    fun onRemoveHookSystemPostConfiguration() {
        val world = configureWorld {}.also {
            it.add(OnRemoveHookSystem(world = it))
        }
        val system = world.system<OnRemoveHookSystem>()

        val entity1 = world.entity { it += SimpleTestComponent() }
        val entity2 = world.entity { it += SimpleTestComponent() }
        val entity3 = world.entity { it += SimpleTestComponent() }

        // remove entity after the system is added, should trigger the onRemoveEntity hook
        world -= entity1
        assertEquals(1, system.removeEntityHandledCount)

        // remove system
        world -= system

        // remove an entity after the system is removed, should not trigger the onRemoveEntity hook
        world -= entity2
        assertEquals(1, system.removeEntityHandledCount)

        // add the system back, existing entities don't trigger the hook
        world += system
        assertNotNull(system.family.removeHook)
        assertEquals(1, system.removeEntityHandledCount)

        // add an entity after the system is added back, should trigger the onAddEntity hook
        world -= entity3
        assertEquals(2, system.removeEntityHandledCount)
    }


    @Test
    fun onHooksConfigureAndSystemHooksConfiguration() {
        var onAddCount = 0
        var onRemoveCount = 0
        val world = configureWorld {
            families {
                // hook that reacts on entities that have a MoveComponent and do not have a DeadComponent
                val family = family { all(SimpleTestComponent) }
                onAdd(family) { _ ->
                    onAddCount++
                }
                onRemove(family) { _ ->
                    onRemoveCount++
                }
            }
        }.also {
            it.add(OnAddHookSystem(world = it))
        }
        val systemAddedAfterConfigure = world.system<OnAddHookSystem>()

        // add an entity after the system is added, should trigger the onAddEntity hook
        world.entity { it += SimpleTestComponent() }
        assertEquals(1, systemAddedAfterConfigure.addEntityHandledCount)
        assertEquals(1, onAddCount)

        // remove system
        world -= systemAddedAfterConfigure

        // add an entity after the system is removed, should not trigger the onAddEntity hook
        val addedEntity = world.entity { it += SimpleTestComponent() }
        assertEquals(1, systemAddedAfterConfigure.addEntityHandledCount)
        assertEquals(2, onAddCount)

        // add the system back, existing entities don't trigger the hook
        world += systemAddedAfterConfigure
        assertEquals(1, systemAddedAfterConfigure.addEntityHandledCount)
        assertEquals(2, onAddCount)

        // add an entity after the system is added back, should trigger the onAddEntity hook
        world.entity { it += SimpleTestComponent() }
        assertEquals(2, systemAddedAfterConfigure.addEntityHandledCount)
        assertEquals(3, onAddCount)

        // let's test the onRemove hook
        world -= addedEntity
        assertEquals(1, onRemoveCount)
    }

    @Test
    fun testWorldAndSystemHooksScenario() {
        // add a family hook during world configuration
        var worldOnAddCount = 0
        val world = configureWorld {
            families {
                onAdd(family { all(SimpleTestComponent) }) { _ ->
                    worldOnAddCount++
                }
            }
            systems {
                add(OnAddHookSystem3())
            }
        }

        // add a FamilyOnAdd interface to system A that iterates over the same family of the previous step.
        val systemA = OnAddHookSystem(world).also {
            world += it
        }

        // do the same thing again for system B and call it systemBHook
        val systemB = OnAddHookSystem2(world).also {
            world += it
        }

        val systemC = world.system<OnAddHookSystem3>()

        // remove system A
        world -= systemA

        // trigger an update and confirm that the onAdd is called for all systems except system A
        world.entity { it += SimpleTestComponent() }
        assertEquals(1, worldOnAddCount)
        assertEquals(0, systemA.addEntityHandledCount)
        assertEquals(1, systemB.addEntityHandledCount)
        assertEquals(1, systemC.addEntityHandledCount)

        // put system A back in
        world += systemA

        // trigger an update and confirm that the onAdd is called for all systems
        world.entity { it += SimpleTestComponent() }
        assertEquals(2, worldOnAddCount)
        assertEquals(1, systemA.addEntityHandledCount)
        assertEquals(2, systemB.addEntityHandledCount)
        assertEquals(2, systemC.addEntityHandledCount)

        // remove system C
        world -= systemC

        // trigger an update and confirm that the onAdd is called for all systems except system C
        world.entity { it += SimpleTestComponent() }
        assertEquals(3, worldOnAddCount)
        assertEquals(2, systemA.addEntityHandledCount)
        assertEquals(3, systemB.addEntityHandledCount)
        assertEquals(2, systemC.addEntityHandledCount)

        // put system C back in
        world += systemC

        // trigger an update and confirm that the onAdd is called for all systems
        world.entity { it += SimpleTestComponent() }
        assertEquals(4, worldOnAddCount)
        assertEquals(3, systemA.addEntityHandledCount)
        assertEquals(4, systemB.addEntityHandledCount)
        assertEquals(3, systemC.addEntityHandledCount)
    }

    @Test
    fun illegalOnAddHookSystem() {
        assertFailsWith<FleksWrongSystemInterfaceException> {
            configureWorld {
                systems {
                    add(IllegalOnAddHookSystem())
                }
            }
        }
    }

    @Test
    fun illegalOnRemoveHookSystem() {
        assertFailsWith<FleksWrongSystemInterfaceException> {
            configureWorld {
                systems {
                    add(IllegalOnRemoveHookSystem())
                }
            }
        }
    }
}
