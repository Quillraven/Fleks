package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.inject
import com.github.quillraven.fleks.World.Companion.mapper
import com.github.quillraven.fleks.collection.EntityComparator
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

private class SystemTestIteratingSystemMapper : IteratingSystem(interval = Fixed(0.25f)) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    override fun familyDefinition() = familyDefinition {
        allOf(SystemTestComponent)
    }

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

private class SystemTestEntityCreation : IteratingSystem() {
    var numTicks = 0

    init {
        world.entity { it += SystemTestComponent() }
    }

    override fun familyDefinition() = familyDefinition {
        anyOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) {
        ++numTicks
    }
}

private class SystemTestIteratingSystemSortAutomatic : IteratingSystem(
    comparator = object : EntityComparator {
        private val mapper = mapper(SystemTestComponent)
        override fun compare(entityA: Entity, entityB: Entity): Int {
            return mapper[entityB].x.compareTo(mapper[entityA].x)
        }
    },
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun familyDefinition() = familyDefinition {
        allOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestFixedSystemRemoval : IteratingSystem(interval = Fixed(1f)) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun familyDefinition() = familyDefinition {
        allOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
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
    comparator = compareEntityBy(SystemTestComponent),
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun familyDefinition() = familyDefinition {
        allOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemInjectable : IteratingSystem() {
    val injectable: String = inject()

    override fun familyDefinition() = familyDefinition {
        noneOf(SystemTestComponent)
        anyOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class SystemTestIteratingSystemQualifiedInjectable(
    val injectable: String = inject()
) : IteratingSystem() {
    val injectable2: String = world.inject("q1")

    override fun familyDefinition() = familyDefinition {
        noneOf(SystemTestComponent)
        anyOf(SystemTestComponent)
    }

    override fun onTickEntity(entity: Entity) = Unit
}

internal class SystemTest {
    @Test
    fun systemWithIntervalEachFrameGetsCalledEveryTime() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemEachFrame()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun systemWithIntervalEachFrameReturnsWorldDeltaTime() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemEachFrame()
        system.world.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun systemWithFixedIntervalOf025fGetsCalledFourTimesWhenDeltaTimeIs11f() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemFixed()
        system.world.update(1.1f)

        system.onUpdate()

        assertEquals(4, system.numCalls)
        assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun systemWithFixedIntervalReturnsStepRateAsDeltaTime() {
        val system = SystemTestIntervalSystemFixed()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun createIntervalSystemWithNoArgs() {
        val expectedWorld = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }

        val service = expectedWorld.systemService

        assertEquals(1, service.systems.size)
        assertNotNull(service.system<SystemTestIntervalSystemEachFrame>())
        assertSame(expectedWorld, service.system<SystemTestIntervalSystemEachFrame>().world)
    }

    @Test
    fun createIteratingSystemWithComponentMapperArg() {
        val expectedWorld = world {
            systems {
                add(SystemTestIteratingSystemMapper())
            }
        }

        val service = expectedWorld.systemService

        val actualSystem = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals(SystemTestComponent::class.simpleName, "SystemTestComponent")
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

        val service = expectedWorld.systemService

        val actualSystem = service.system<SystemTestIteratingSystemInjectable>()
        assertEquals(1, service.systems.size)
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

        val service = expectedWorld.systemService

        val actualSystem = service.system<SystemTestIteratingSystemQualifiedInjectable>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
        assertEquals("43", actualSystem.injectable2)
    }

    @Test
    fun iteratingSystemCallsOnTickAndOnAlphaForEachEntityOfTheSystem() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemMapper())
            }
        }
        val service = world.systemService
        world.entity { it += SystemTestComponent() }
        world.entity { it += SystemTestComponent() }

        world.update(0.3f)

        val system = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(2, system.numEntityCalls)
        assertEquals(2, system.numAlphaCalls)
        assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun configureEntityDuringIteration() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemMapper())
            }
        }
        val service = world.systemService
        val entity = world.entity { it += SystemTestComponent() }
        val system = service.system<SystemTestIteratingSystemMapper>()
        system.entityToConfigure = entity

        world.update(0.3f)

        assertFalse { entity in world[SystemTestComponent] }
    }

    @Test
    fun sortEntitiesAutomatically() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortAutomatic())
            }
        }
        val service = world.systemService
        world.entity { it += SystemTestComponent(x = 15f) }
        world.entity { it += SystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SystemTestComponent(x = 5f) }

        service.update()

        assertEquals(expectedEntity, service.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun sortEntitiesProgrammatically() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortManual())
            }
        }
        val service = world.systemService
        world.entity { it += SystemTestComponent(x = 15f) }
        world.entity { it += SystemTestComponent(x = 10f) }
        val expectedEntity = world.entity { it += SystemTestComponent(x = 5f) }
        val system = service.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        service.update()

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
        val service = world.systemService

        assertFailsWith<FleksNoSuchSystemException> {
            service.system<SystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun updateOnlyCallsEnabledSystems() {
        val world = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }
        val service = world.systemService
        val system = service.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        service.update()

        assertEquals(0, system.numCalls)
    }

    @Test
    fun removingAnEntityDuringUpdateIsDelayed() {
        val world = world {
            systems {
                add(SystemTestIteratingSystemSortAutomatic())
            }
        }
        val service = world.systemService
        world.entity { it += SystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SystemTestComponent(x = 10f) }
        world.entity { it += SystemTestComponent(x = 5f) }
        val system = service.system<SystemTestIteratingSystemSortAutomatic>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        service.update()
        service.update()

        assertEquals(5, system.numEntityCalls)
    }

    @Test
    fun removingAnEntityDuringAlphaIsDelayed() {
        val world = world {
            systems {
                add(SystemTestFixedSystemRemoval())
            }
        }
        val service = world.systemService
        // set delta time to 1f for the fixed interval
        world.update(1f)
        world.entity { it += SystemTestComponent(x = 15f) }
        val entityToRemove = world.entity { it += SystemTestComponent(x = 10f) }
        world.entity { it += SystemTestComponent(x = 5f) }
        val system = service.system<SystemTestFixedSystemRemoval>()
        system.entityToRemove = entityToRemove

        // call it twice - first call still iterates over all three entities
        // while the second call will only iterate over the remaining two entities
        service.update()
        service.update()

        assertEquals(4, system.numEntityCalls)
    }

    @Test
    fun disposeService() {
        val world = world {
            systems {
                add(SystemTestIntervalSystemEachFrame())
            }
        }
        val service = world.systemService

        service.dispose()

        assertEquals(1, service.system<SystemTestIntervalSystemEachFrame>().numDisposes)
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

        val service = world.systemService
        service.update()

        val system = service.system<SystemTestEntityCreation>()
        assertEquals(1, system.numTicks)
    }
}
