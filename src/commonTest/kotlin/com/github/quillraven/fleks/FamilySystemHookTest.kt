package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class SimpleTestComponent : Component<SimpleTestComponent> {
    companion object : ComponentType<SimpleTestComponent>()
    override fun type() = SimpleTestComponent
}

private class OnAddHookSystem : IteratingSystem(
    family = family { all(SimpleTestComponent) }
), FamilyOnAdd {

    var addEntityHandled = false

    override fun onAddEntity(entity: Entity) {
        addEntityHandled = true
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class OnRemoveHookSystem : IteratingSystem(
    family = family { all(SimpleTestComponent) }
), FamilyOnRemove {

    var removeEntityHandled = false

    override fun onRemoveEntity(entity: Entity) {
        removeEntityHandled = true
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
