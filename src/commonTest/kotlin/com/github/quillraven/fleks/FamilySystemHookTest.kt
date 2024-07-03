package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.*

private class SimpleTestComponent : Component<SimpleTestComponent> {
    companion object : ComponentType<SimpleTestComponent>()
    override fun type() = SimpleTestComponent
}

private class OnAddHookSystem(world: World? = null) : IteratingSystem(
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

private class OnRemoveHookSystem(world: World? = null) : IteratingSystem(
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

private class IllegalOnAddHookSystem : IntervalSystem(), FamilyOnAdd {
    override fun onTick() = Unit
    override fun onAddEntity(entity: Entity) = Unit
}

private class IllegalOnRemoveHookSystem : IntervalSystem(), FamilyOnRemove {
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

        // remove system, there should be no hooks
        world -= system
        assertNull(system.family.addHook)

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
        assertTrue { system.removeEntityHandled }

        // remove system, there should be no hooks
        world -= system
        assertNull(system.family.removeHook)

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

        // remove system, there should be no hooks
        world -= systemAddedAfterConfigure
        assertNull(systemAddedAfterConfigure.family.addHook)

        // add an entity after the system is removed, should not trigger the onAddEntity hook
        val addedEntity = world.entity { it += SimpleTestComponent() }
        assertEquals(1, systemAddedAfterConfigure.addEntityHandledCount)
        assertEquals(2, onAddCount)

        // add the system back, existing entities don't trigger the hook
        world += systemAddedAfterConfigure
        assertNotNull(systemAddedAfterConfigure.family.addHook)
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
