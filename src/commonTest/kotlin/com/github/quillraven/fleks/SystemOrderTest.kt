package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals

private class TestComponent : Component<TestComponent> {
    companion object : ComponentType<TestComponent>()
    override fun type() = TestComponent
}

private abstract class BaseSystem(
    val onInitOrder: MutableList<BaseSystem> = mutableListOf(),
    val onUpdateOrder: MutableList<BaseSystem> = mutableListOf(),
    val onDisposeOrder: MutableList<BaseSystem> = mutableListOf(),
    val onAddEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    val onTickEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    val onRemoveEntityOrder: MutableList<BaseSystem> = mutableListOf(),
) : IteratingSystem<Unit>(
    family { all (TestComponent) }
), FamilyOnAdd, FamilyOnRemove {

    override fun onInit() {
        super.onInit()
        onInitOrder += this
    }

    override fun onUpdate() {
        super.onUpdate()
        onUpdateOrder += this
    }

    override fun onDispose() {
        super.onDispose()
        onDisposeOrder += this
    }

    override fun onAddEntity(entity: Entity) {
        onAddEntityOrder += this
    }

    override fun onTickEntity(entity: Entity) {
        onTickEntityOrder += this
    }

    override fun onRemoveEntity(entity: Entity) {
        onRemoveEntityOrder += this
    }
}

private class FirstSystem(
    onInitOrder: MutableList<BaseSystem> = mutableListOf(),
    onUpdateOrder: MutableList<BaseSystem> = mutableListOf(),
    onDisposeOrder: MutableList<BaseSystem> = mutableListOf(),
    onAddEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    onTickEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    onRemoveEntityOrder: MutableList<BaseSystem> = mutableListOf(),
) : BaseSystem(onInitOrder, onUpdateOrder, onDisposeOrder, onAddEntityOrder, onTickEntityOrder, onRemoveEntityOrder)

private class SecondSystem(
    onInitOrder: MutableList<BaseSystem> = mutableListOf(),
    onUpdateOrder: MutableList<BaseSystem> = mutableListOf(),
    onDisposeOrder: MutableList<BaseSystem> = mutableListOf(),
    onAddEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    onTickEntityOrder: MutableList<BaseSystem> = mutableListOf(),
    onRemoveEntityOrder: MutableList<BaseSystem> = mutableListOf(),
) : BaseSystem(onInitOrder, onUpdateOrder, onDisposeOrder, onAddEntityOrder, onTickEntityOrder, onRemoveEntityOrder)

internal class SystemOrderTest {

    @Test
    fun onInit() {
        val systemOrder = mutableListOf<BaseSystem>()

        configureWorld {
            systems {
                add(FirstSystem(onInitOrder = systemOrder))
                add(SecondSystem(onInitOrder = systemOrder))
            }
        }

        assertEquals(FirstSystem::class, systemOrder[0]::class)
        assertEquals(SecondSystem::class, systemOrder[1]::class)
    }

    @Test
    fun onUpdate() {
        val systemOrder = mutableListOf<BaseSystem>()

        val world = configureWorld {
            systems {
                add(FirstSystem(onUpdateOrder = systemOrder))
                add(SecondSystem(onUpdateOrder = systemOrder))
            }
        }

        world.update()

        assertEquals(FirstSystem::class, systemOrder[0]::class)
        assertEquals(SecondSystem::class, systemOrder[1]::class)
    }

    @Test
    fun onDispose() {
        val systemOrder = mutableListOf<BaseSystem>()

        val world = configureWorld {
            systems {
                add(FirstSystem(onDisposeOrder = systemOrder))
                add(SecondSystem(onDisposeOrder = systemOrder))
            }
        }

        world.dispose()

        assertEquals(SecondSystem::class, systemOrder[0]::class)
        assertEquals(FirstSystem::class, systemOrder[1]::class)
    }

    @Test
    fun onAddEntity() {
        val systemOrder = mutableListOf<BaseSystem>()

        val world = configureWorld {
            systems {
                add(FirstSystem(onAddEntityOrder = systemOrder))
                add(SecondSystem(onAddEntityOrder = systemOrder))
            }
        }

        world.entity { it += TestComponent() }

        assertEquals(FirstSystem::class, systemOrder[0]::class)
        assertEquals(SecondSystem::class, systemOrder[1]::class)
    }

    @Test
    fun onTickEntity() {
        val systemOrder = mutableListOf<BaseSystem>()

        val world = configureWorld {
            systems {
                add(FirstSystem(onTickEntityOrder = systemOrder))
                add(SecondSystem(onTickEntityOrder = systemOrder))
            }
        }

        world.entity { it += TestComponent() }
        world.update()

        assertEquals(FirstSystem::class, systemOrder[0]::class)
        assertEquals(SecondSystem::class, systemOrder[1]::class)
    }

    @Test
    fun onRemoveEntity() {
        val systemOrder = mutableListOf<BaseSystem>()

        val world = configureWorld {
            systems {
                add(FirstSystem(onRemoveEntityOrder = systemOrder))
                add(SecondSystem(onRemoveEntityOrder = systemOrder))
            }
        }

        val entity = world.entity { it += TestComponent() }
        world -= entity

        assertEquals(SecondSystem::class, systemOrder[0]::class)
        assertEquals(FirstSystem::class, systemOrder[1]::class)
    }
}
