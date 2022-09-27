package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.github.quillraven.fleks.collection.compareEntity
import com.github.quillraven.fleks.collection.compareEntityBy
import kotlin.test.*

private class SystemTestIntervalSystemEachFrame : IntervalSystem(
    interval = EachFrame
) {
    var numDisposes = 0
    var numCalls = 0

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        numDisposes++
    }
}

private class SystemTestIntervalSystemFixed : IntervalSystem(
    interval = Fixed(0.25f)
) {
    var numCalls = 0
    var lastAlpha = 0f

    override fun onTick() {
        ++numCalls
    }

    override fun onAlpha(alpha: Float) {
        lastAlpha = alpha
    }
}

private data class SystemTestComponent(
    var x: Float = 0f
) : Component<SystemTestComponent>, Comparable<SystemTestComponent> {
    override fun type() = SystemTestComponent

    override fun compareTo(other: SystemTestComponent): Int {
        return other.x.compareTo(x)
    }

    companion object : ComponentType<SystemTestComponent>()
}

private class SystemTestIteratingSystem : IteratingSystem(
    family = family { all(SystemTestComponent) },
    interval = Fixed(0.25f)
) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToConfigure?.let { e ->
            e.configure { it -= SystemTestComponent }
        }
        ++numEntityCalls
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        lastAlpha = alpha
        ++numAlphaCalls
    }
}

private class SystemTestEntityCreation : IteratingSystem(family { any(SystemTestComponent) }) {
    var numTicks = 0

    init {
        world.entity { it += SystemTestComponent() }
    }

    override fun onTickEntity(entity: Entity) {
        ++numTicks
    }
}

private class SystemTestIteratingSystemSortAutomatic : IteratingSystem(
    family = family { all(SystemTestComponent) },
    comparator = compareEntity { entityA, entityB ->
        entityB[SystemTestComponent].x.compareTo(entityA[SystemTestComponent].x)
    },
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world -= it
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestFixedSystemRemoval : IteratingSystem(
    family = family { all(SystemTestComponent) },
    interval = Fixed(1f)
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world -= it
            entityToRemove = null
        }
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        // the next line would cause an exception if we don't update the family properly in alpha
        // because component removal is instantly
        entity[SystemTestComponent].x++
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemSortManual : IteratingSystem(
    family = family { all(SystemTestComponent) },
    comparator = compareEntityBy(SystemTestComponent),
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemInjectable :
    IteratingSystem(family { none(SystemTestComponent).any(SystemTestComponent) }) {
    val injectable: String = inject()

    override fun onTickEntity(entity: Entity) = Unit
}

private class SystemTestIteratingSystemQualifiedInjectable(
    val injectable: String = inject()
) : IteratingSystem(family { none(SystemTestComponent).any(SystemTestComponent) }) {
    val injectable2: String = world.inject("q1")

    override fun onTickEntity(entity: Entity) = Unit
}

internal class SystemTest {
    @Test
    fun systemWithIntervalEachFrameGetsCalledEveryTime() {
        val w = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }
        val system = w.system<SystemTestIntervalSystemEachFrame>()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun systemWithIntervalEachFrameReturnsWorldDeltaTime() {
        val w = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }
        val system = w.system<SystemTestIntervalSystemEachFrame>()
        w.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun systemWithFixedIntervalOf025fGetsCalledFourTimesWhenDeltaTimeIs11f() {
        val w = world {
            systems {
                add(SystemTestIntervalSystemFixed())
            }
        }
        val system = w.system<SystemTestIntervalSystemFixed>()

        system.world.update(1.1f)

        assertEquals(4, system.numCalls)
        assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun systemWithFixedIntervalReturnsStepRateAsDeltaTime() {
        val w = world {
            systems {
                add(SystemTestIntervalSystemFixed())
            }
        }
        val system = w.system<SystemTestIntervalSystemFixed>()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun createIntervalSystemWithNoArgs() {
        val expectedWorld = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }

        assertEquals(1, expectedWorld.systems.size)
        assertNotNull(expectedWorld.system<SystemTestIntervalSystemEachFrame>())
        assertSame(expectedWorld, expectedWorld.system<SystemTestIntervalSystemEachFrame>().world)
    }

    @Test
    fun createIteratingSystemWithAnInjectableArg() {
        val expectedWorld = world {
            injectables {
                add("42")
            }

            systems {
                add(SystemTestIteratingSystemInjectable())
            }
        }

        val actualSystem = expectedWorld.system<SystemTestIteratingSystemInjectable>()
        assertEquals(1, expectedWorld.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
    }

    @Test
    fun createIteratingSystemWithQualifiedArgs() {
        val expectedWorld = world {
            injectables {
                add("42")
                add("q1", "43")
            }

            systems {
                add(SystemTestIteratingSystemQualifiedInjectable())
            }
        }

        val actualSystem = expectedWorld.system<SystemTestIteratingSystemQualifiedInjectable>()
        assertEquals(1, expectedWorld.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
        assertEquals("43", actualSystem.injectable2)
    }

    @Test
    fun iteratingSystemCallsOnTickAndOnAlphaForEachEntityOfTheSystem() {
        val world = world {
            systems {
                add(SystemTestIteratingSystem())
            }
        }
        world.entity { it += SystemTestComponent() }
        world.entity { it += SystemTestComponent() }

        world.update(0.3f)

        val system = world.system<SystemTestIteratingSystem>()
        assertEquals(2, system.numEntityCalls)
        assertEquals(2, system.numAlphaCalls)
        assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun configureEntityDuringIteration() {
        val world = world {
            systems {
                add(SystemTestIteratingSystem())
            }
        }
        val entity = world.entity { it += SystemTestComponent() }
        val system = world.system<SystemTestIteratingSystem>()
        system.entityToConfigure = entity

        world.update(0.3f)

        assertFalse(with(world) { entity has SystemTestComponent })
    }

    @Test
    fun sortEntitiesAutomatically() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortAutomatic())
            }
        }
        world.entity { it += SystemTestComponent(x = 15f) }
        world.entity { it += SystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SystemTestComponent(x = 5f) }

        world.update(0f)

        assertEquals(expectedEntity, world.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun sortEntitiesProgrammatically() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortManual())
            }
        }
        world.entity { it += SystemTestComponent(x = 15f) }
        world.entity { it += SystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SystemTestComponent(x = 5f) }
        val system = world.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        world.update(0f)

        assertEquals(expectedEntity, system.lastEntityProcess)
        assertFalse(system.doSort)
    }

    @Test
    fun cannotGetNonExistingSystem() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortAutomatic())
            }
        }

        assertFailsWith<FleksNoSuchSystemException> {
            world.system<SystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun updateOnlyCallsEnabledSystems() {
        val world = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }
        val system = world.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        world.update(0f)

        assertEquals(0, system.numCalls)
    }

    @Test
    fun removingAnEntityDuringUpdateIsDelayed() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortAutomatic())
            }
        }
        world.entity { it += SystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SystemTestComponent(x = 10f) }
        world.entity { it += SystemTestComponent(x = 5f) }
        val system = world.system<SystemTestIteratingSystemSortAutomatic>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        world.update(0f)
        world.update(0f)

        assertEquals(5, system.numEntityCalls)
    }

    @Test
    fun removingAnEntityDuringAlphaIsDelayed() {
        val world = world {
            systems {
                add(SystemTestFixedSystemRemoval())
            }
        }
        // set delta time to 1f for the fixed interval
        world.entity { it += SystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SystemTestComponent(x = 10f) }
        world.entity { it += SystemTestComponent(x = 5f) }
        val system = world.system<SystemTestFixedSystemRemoval>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        world.update(1f)
        world.update(1f)

        assertEquals(4, system.numEntityCalls)
    }

    @Test
    fun disposeService() {
        val world = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }

        world.dispose()

        assertEquals(1, world.system<SystemTestIntervalSystemEachFrame>().numDisposes)
    }

    @Test
    fun createEntityDuringSystemInit() {
        // this test verifies that entities that are created in a system's init block
        // are correctly added to families
        val world = world {
            systems {
                add(SystemTestEntityCreation())
            }
        }

        world.update(0f)

        val system = world.system<SystemTestEntityCreation>()
        assertEquals(1, system.numTicks)
    }
}
