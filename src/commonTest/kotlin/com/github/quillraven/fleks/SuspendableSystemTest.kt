package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.github.quillraven.fleks.collection.compareEntity
import com.github.quillraven.fleks.collection.compareEntityBy
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private class SuspendableSystemTestIntervalSystemEachFrame : SuspendableIntervalSystem(
    interval = EachFrame
) {
    var numInits = 0
    var numDisposes = 0
    var numCalls = 0

    override fun onInit() {
        numInits++
    }

    override suspend fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        numDisposes++
    }
}

private class SuspendableSystemTestIntervalSystemFixed : SuspendableIntervalSystem(
    interval = Fixed(0.25f)
) {
    var numCalls = 0
    var lastAlpha = 0f

    override suspend fun onTick() {
        ++numCalls
    }

    override fun onAlpha(alpha: Float) {
        lastAlpha = alpha
    }
}

private data class SuspendableSystemTestComponent(
    var x: Float = 0f
) : Component<SuspendableSystemTestComponent>, Comparable<SuspendableSystemTestComponent> {
    override fun type() = SuspendableSystemTestComponent

    override fun compareTo(other: SuspendableSystemTestComponent): Int {
        return other.x.compareTo(x)
    }

    companion object : ComponentType<SuspendableSystemTestComponent>()
}

private class SuspendableSystemTestIteratingSystem : SuspendableIteratingSystem(
    family = family { all(SuspendableSystemTestComponent) },
    interval = Fixed(0.25f)
) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    override suspend fun onTickEntity(entity: Entity) {
        entityToConfigure?.let { e ->
            e.configure { it -= SuspendableSystemTestComponent }
        }
        ++numEntityCalls
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        lastAlpha = alpha
        ++numAlphaCalls
    }
}

private class SuspendableSystemTestEntityCreation : SuspendableIteratingSystem(family { any(SuspendableSystemTestComponent) }) {
    var numTicks = 0

    override fun onInit() {
        super.onInit()
        world.entity { it += SuspendableSystemTestComponent() }
    }

    override suspend fun onTickEntity(entity: Entity) {
        ++numTicks
    }
}

private class SuspendableSystemTestIteratingSystemSortAutomatic : SuspendableIteratingSystem(
    family = family { all(SuspendableSystemTestComponent) },
    comparator = compareEntity { entityA, entityB ->
        entityB[SuspendableSystemTestComponent].x.compareTo(entityA[SuspendableSystemTestComponent].x)
    },
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity.NONE
    var entityToRemove: Entity? = null

    override suspend fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world -= it
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SuspendableSystemTestFixedSystemRemoval : SuspendableIteratingSystem(
    family = family { all(SuspendableSystemTestComponent) },
    interval = Fixed(1f)
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity.NONE
    var entityToRemove: Entity? = null

    override suspend fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world -= it
            entityToRemove = null
        }
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        // the next line would cause an exception if we don't update the family properly in alpha
        // because component removal is instantly
        entity[SuspendableSystemTestComponent].x++
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SuspendableSystemTestIteratingSystemSortManual : SuspendableIteratingSystem(
    family = family { all(SuspendableSystemTestComponent) },
    comparator = compareEntityBy(SuspendableSystemTestComponent),
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity.NONE

    override suspend fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SuspendableSystemTestIteratingSystemInjectable :
    SuspendableIteratingSystem(family { none(SuspendableSystemTestComponent).any(SuspendableSystemTestComponent) }) {
    val injectable: String = inject()

    override suspend fun onTickEntity(entity: Entity) = Unit
}

private class SuspendableSystemTestIteratingSystemQualifiedInjectable(
    val injectable: String = inject()
) : SuspendableIteratingSystem(family { none(SuspendableSystemTestComponent).any(SuspendableSystemTestComponent) }) {
    val injectable2: String = world.inject("q1")

    override suspend fun onTickEntity(entity: Entity) = Unit
}

private class SuspendableSystemTestEnable(enabled: Boolean) : SuspendableIntervalSystem(enabled = enabled) {
    var enabledCall = false
    var disabledCall = false

    override fun onEnable() {
        enabledCall = true
    }

    override fun onDisable() {
        disabledCall = true
    }

    override suspend fun onTick() = Unit
}

private class SuspendableAddEntityInConstructorSystem() : SuspendableIntervalSystem() {

    init {
        world.entity { }
    }

    override suspend fun onTick() = Unit
}

internal class SuspendableSystemTest {
    @Test
    fun systemWithIntervalEachFrameGetsCalledEveryTime() = runTest {
        val w = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }
        val system = w.system<SuspendableSystemTestIntervalSystemEachFrame>()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun systemWithIntervalEachFrameReturnsWorldDeltaTime() {
        val w = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }
        val system = w.system<SuspendableSystemTestIntervalSystemEachFrame>()
        w.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun systemWithFixedIntervalOf025fGetsCalledFourTimesWhenDeltaTimeIs11f() {
        val w = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemFixed())
            }
        }
        val system = w.system<SuspendableSystemTestIntervalSystemFixed>()

        system.world.update(1.1f)

        assertEquals(4, system.numCalls)
        assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun systemWithFixedIntervalReturnsStepRateAsDeltaTime() {
        val w = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemFixed())
            }
        }
        val system = w.system<SuspendableSystemTestIntervalSystemFixed>()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun createIntervalSystemWithNoArgs() {
        val expectedWorld = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }

        assertEquals(1, expectedWorld.systems.size)
        assertNotNull(expectedWorld.system<SuspendableSystemTestIntervalSystemEachFrame>())
        assertSame(expectedWorld, expectedWorld.system<SuspendableSystemTestIntervalSystemEachFrame>().world)
    }

    @Test
    fun createIteratingSystemWithAnInjectableArg() {
        val expectedWorld = configureWorld {
            injectables {
                add("42")
            }

            systems {
                add(SuspendableSystemTestIteratingSystemInjectable())
            }
        }

        val actualSystem = expectedWorld.system<SuspendableSystemTestIteratingSystemInjectable>()
        assertEquals(1, expectedWorld.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
    }

    @Test
    fun createIteratingSystemWithQualifiedArgs() {
        val expectedWorld = configureWorld {
            injectables {
                add("42")
                add("q1", "43")
            }

            systems {
                add(SuspendableSystemTestIteratingSystemQualifiedInjectable())
            }
        }

        val actualSystem = expectedWorld.system<SuspendableSystemTestIteratingSystemQualifiedInjectable>()
        assertEquals(1, expectedWorld.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
        assertEquals("43", actualSystem.injectable2)
    }

    @Test
    fun iteratingSystemCallsOnTickAndOnAlphaForEachEntityOfTheSystem() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystem())
            }
        }
        world.entity { it += SuspendableSystemTestComponent() }
        world.entity { it += SuspendableSystemTestComponent() }

        world.update(0.3f)

        val system = world.system<SuspendableSystemTestIteratingSystem>()
        assertEquals(2, system.numEntityCalls)
        assertEquals(2, system.numAlphaCalls)
        assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun configureEntityDuringIteration() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystem())
            }
        }
        val entity = world.entity { it += SuspendableSystemTestComponent() }
        val system = world.system<SuspendableSystemTestIteratingSystem>()
        system.entityToConfigure = entity

        world.update(0.3f)

        assertFalse(with(world) { entity has SuspendableSystemTestComponent })
    }

    @Test
    fun sortEntitiesAutomatically() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystemSortAutomatic())
            }
        }
        world.entity { it += SuspendableSystemTestComponent(x = 15f) }
        world.entity { it += SuspendableSystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SuspendableSystemTestComponent(x = 5f) }

        world.update(0f)

        assertEquals(expectedEntity, world.system<SuspendableSystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun sortEntitiesProgrammatically() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystemSortManual())
            }
        }
        world.entity { it += SuspendableSystemTestComponent(x = 15f) }
        world.entity { it += SuspendableSystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SuspendableSystemTestComponent(x = 5f) }
        val system = world.system<SuspendableSystemTestIteratingSystemSortManual>()

        system.doSort = true
        world.update(0f)

        assertEquals(expectedEntity, system.lastEntityProcess)
        assertFalse(system.doSort)
    }

    @Test
    fun cannotGetNonExistingSystem() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystemSortAutomatic())
            }
        }

        assertFailsWith<FleksNoSuchSystemException> {
            world.system<SuspendableSystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun updateOnlyCallsEnabledSystems() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }
        val system = world.system<SuspendableSystemTestIntervalSystemEachFrame>()
        system.enabled = false

        world.update(0f)

        assertEquals(0, system.numCalls)
    }

    @Test
    fun removingAnEntityDuringUpdateIsDelayed() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIteratingSystemSortAutomatic())
            }
        }
        world.entity { it += SuspendableSystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SuspendableSystemTestComponent(x = 10f) }
        world.entity { it += SuspendableSystemTestComponent(x = 5f) }
        val system = world.system<SuspendableSystemTestIteratingSystemSortAutomatic>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        world.update(0f)
        world.update(0f)

        assertEquals(5, system.numEntityCalls)
    }

    @Test
    fun removingAnEntityDuringAlphaIsDelayed() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestFixedSystemRemoval())
            }
        }
        // set delta time to 1f for the fixed interval
        world.entity { it += SuspendableSystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SuspendableSystemTestComponent(x = 10f) }
        world.entity { it += SuspendableSystemTestComponent(x = 5f) }
        val system = world.system<SuspendableSystemTestFixedSystemRemoval>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        world.update(1f)
        world.update(1f)

        assertEquals(4, system.numEntityCalls)
    }

    @Test
    fun initService() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }

        assertEquals(1, world.system<SuspendableSystemTestIntervalSystemEachFrame>().numInits)
    }

    @Test
    fun disposeService() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestIntervalSystemEachFrame())
            }
        }

        world.dispose()

        assertEquals(1, world.system<SuspendableSystemTestIntervalSystemEachFrame>().numDisposes)
    }

    @Test
    fun createEntityDuringSystemInit() {
        // this test verifies that entities that are created in a system's init block
        // are correctly added to families
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestEntityCreation())
            }
        }

        world.update(0f)

        val system = world.system<SuspendableSystemTestEntityCreation>()
        assertEquals(1, system.numTicks)
    }

    @Test
    fun testOnEnabledDisabled() {
        val world = configureWorld {
            systems {
                add(SuspendableSystemTestEnable(false))
            }
        }
        val system = world.system<SuspendableSystemTestEnable>()

        assertFalse(system.enabledCall)
        assertFalse(system.disabledCall)

        system.enabled = true
        assertTrue(system.enabled)
        assertTrue(system.enabledCall)
        assertFalse(system.disabledCall)
        system.enabledCall = false

        system.enabled = false
        assertFalse(system.enabled)
        assertFalse(system.enabledCall)
        assertTrue(system.disabledCall)
        system.disabledCall = false

        // should not call methods when value does not change
        system.enabled = false
        assertFalse(system.enabledCall)
        assertFalse(system.disabledCall)

        system.enabled = true
        system.enabledCall = false
        system.disabledCall = false
        system.enabled = true
        assertFalse(system.enabledCall)
        assertFalse(system.disabledCall)
    }

    @Test
    fun testWorldModificationDuringConfiguration() {
        assertFailsWith<FleksWorldModificationDuringConfigurationException> {
            configureWorld {
                systems {
                    add(SuspendableAddEntityInConstructorSystem())
                }
            }
        }
    }
}
