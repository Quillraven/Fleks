package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.EntityComparator
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

private class SystemTestIntervalSystemEachFrame(
    injections: Injections
) : IntervalSystem(
    injections,
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

private class SystemTestIntervalSystemFixed(
    injections: Injections
) : IntervalSystem(
    injections,
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

private data class SystemTestComponent(var x: Float = 0f)

private class SystemTestInitBlock(
    injections: Injections
) : IntervalSystem(injections) {
    private val someValue: Float = world.deltaTime

    override fun onTick() = Unit
}

private class SystemTestOnInitBlock(
    injections: Injections
) : IntervalSystem(injections) {
    var someValue: Float = 42f

    override fun onInit() {
        someValue = world.deltaTime
    }

    override fun onTick() = Unit
}

private class SystemTestIteratingSystemMapper(
    injections: Injections
) : IteratingSystem(
    injections,
    allOfComponents = arrayOf(SystemTestComponent::class),
    interval = Fixed(0.25f)
) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

    val mapper = injections.componentMapper<SystemTestComponent>()

    override fun onTickEntity(entity: Entity) {
        entityToConfigure?.let { e ->
            configureEntity(e) {
                mapper.remove(it)
            }
        }
        ++numEntityCalls
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        lastAlpha = alpha
        ++numAlphaCalls
    }
}

private class SystemTestIteratingSystemSortAutomatic(
    injections: Injections
) : IteratingSystem(
    injections,
    allOfComponents = arrayOf(SystemTestComponent::class),
    createComparatorFn = {
        object : EntityComparator {
            override val injections: Injections = injections
            private val mapper: ComponentMapper<SystemTestComponent> = injections.componentMapper()
            override fun compare(entityA: Entity, entityB: Entity): Int {
                return mapper[entityB].x.compareTo(mapper[entityA].x)
            }
        }
    },
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }

        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestFixedSystemRemoval(
    injections: Injections
) : IteratingSystem(
    injections,
    allOfComponents = arrayOf(SystemTestComponent::class),
    interval = Fixed(1f)
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

    private val mapper = injections.componentMapper<SystemTestComponent>()

    override fun onTickEntity(entity: Entity) {
        entityToRemove?.let {
            world.remove(it)
            entityToRemove = null
        }
    }

    override fun onAlphaEntity(entity: Entity, alpha: Float) {
        // the next line would cause an exception if we don't update the family properly in alpha
        // because component removal is instantly
        mapper[entity].x++
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemSortManual(
    injections: Injections
) : IteratingSystem(
    injections,
    allOfComponents = arrayOf(SystemTestComponent::class),
    createComparatorFn = system@{
        object : EntityComparator {
            override val injections: Injections = this@system.injections
            private val mapper: ComponentMapper<SystemTestComponent> = injections.componentMapper()
            override fun compare(entityA: Entity, entityB: Entity): Int {
                return mapper[entityB].x.compareTo(mapper[entityA].x)
            }
        }
    },
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

private class SystemTestIteratingSystemInjectable(
    injections: Injections
) : IteratingSystem(
    injections,
    noneOfComponents = arrayOf(SystemTestComponent::class),
    anyOfComponents = arrayOf(SystemTestComponent::class)
) {
    val injectable: String = injections.dependency()

    override fun onTickEntity(entity: Entity) = Unit
}

private class SystemTestIteratingSystemQualifiedInjectable(
    injections: Injections
) : IteratingSystem(
    injections,
    noneOfComponents = arrayOf(SystemTestComponent::class),
    anyOfComponents = arrayOf(SystemTestComponent::class)
) {
    val injectable: String = injections.dependency()
    val injectable2: String = injections.dependency("q1")

    override fun onTickEntity(entity: Entity) = Unit
}

internal class SystemTest {
    private fun systemService(
        systemFactory: MutableMap<KClass<*>, (injections: Injections) -> IntervalSystem> = mutableMapOf(),
        injections: Injections = Injections.EMPTY,
        world: World = World {}
    ) = SystemService(
        world,
        systemFactory,
        injections
    )

    @Test
    fun systemWithIntervalEachFrameGetsCalledEveryTime() {
        val system = SystemTestIntervalSystemEachFrame(Injections.EMPTY)

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun systemWithIntervalEachFrameReturnsWorldsDeltaTime() {
        val system =
            SystemTestIntervalSystemEachFrame(Injections.EMPTY).apply { this.world = World {} }
        system.world.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun systemWithFixedIntervalOf0_25fGetsCalledFourTimesWhenDeltaTimeIs1_1f() {
        val system = SystemTestIntervalSystemFixed(Injections.EMPTY).apply { this.world = World {} }
        system.world.update(1.1f)

        system.onUpdate()

        assertEquals(4, system.numCalls)
        assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun systemWithFixedIntervalReturnsStepRateAsDeltaTime() {
        val system = SystemTestIntervalSystemFixed(Injections.EMPTY)

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun createIntervalSystemWithNoArgs() {
        val expectedWorld = World {
            component(::SystemTestComponent)
        }

        val service =
            systemService(
                mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame),
                world = expectedWorld
            )

        assertEquals(1, service.systems.size)
        assertNotNull(service.system<SystemTestIntervalSystemEachFrame>())
        assertSame(expectedWorld, service.system<SystemTestIntervalSystemEachFrame>().world)
    }

    @Test
    fun createIteratingSystemWithComponentMapperArg() {
        val expectedWorld = World {
            component(::SystemTestComponent)
        }

        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
                world = expectedWorld
            )

        val actualSystem = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals(SystemTestComponent::class.simpleName, "SystemTestComponent")
    }

    @Test
    fun createIteratingSystemWithAnInjectableArg() {
        val expectedWorld = World {
            component(::SystemTestComponent)
        }

        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemInjectable::class to ::SystemTestIteratingSystemInjectable),
            Injections(
                injectObjects = mutableMapOf(String::class.simpleName!! to Injectable("42"))
            ),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemInjectable>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
    }

    @Test
    fun createIteratingSystemWithQualifiedArgs() {
        val expectedWorld = World {
            component(::SystemTestComponent)
        }

        val service = systemService(
            mutableMapOf(SystemTestIteratingSystemQualifiedInjectable::class to ::SystemTestIteratingSystemQualifiedInjectable),
            Injections(
                injectObjects = mutableMapOf(
                    String::class.simpleName!! to Injectable("42"),
                    "q1" to Injectable("43")
                )
            ),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemQualifiedInjectable>()
        assertEquals(1, service.systems.size)
        assertSame(expectedWorld, actualSystem.world)
        assertEquals("42", actualSystem.injectable)
        assertEquals("43", actualSystem.injectable2)
    }

    @Test
    fun cannotCreateIteratingSystemWithMissingInjectables() {
        assertFailsWith<FleksSystemDependencyInjectException> {
            systemService(
                mutableMapOf(
                    SystemTestIteratingSystemInjectable::class to ::SystemTestIteratingSystemInjectable
                )
            )
        }
    }

    @Test
    fun iteratingSystemCallsOnTickAndOnAlphaForEachEntityOfTheSystem() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
                world = world
            )
        world.entity { add<SystemTestComponent>() }
        world.entity { add<SystemTestComponent>() }
        world.update(0.3f)

        service.update()

        val system = service.system<SystemTestIteratingSystemMapper>()
        assertEquals(2, system.numEntityCalls)
        assertEquals(2, system.numAlphaCalls)
        assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f)
    }

    @Test
    fun configureEntityDuringIteration() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemMapper::class to ::SystemTestIteratingSystemMapper),
                world = world
            )
        world.update(0.3f)
        val entity = world.entity { add<SystemTestComponent>() }
        val system = service.system<SystemTestIteratingSystemMapper>()
        system.entityToConfigure = entity

        service.update()

        assertFalse { entity in system.mapper }
    }

    @Test
    fun sortEntitiesAutomatically() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
                world = world
            )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }

        service.update()

        assertEquals(
            expectedEntity,
            service.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess
        )
    }

    @Test
    fun sortEntitiesProgrammatically() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemSortManual::class to ::SystemTestIteratingSystemSortManual),
                world = world
            )
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        service.update()

        assertEquals(expectedEntity, system.lastEntityProcess)
        assertFalse(system.doSort)
    }

    @Test
    fun cannotGetNonExistingSystem() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
                world = world
            )

        assertFailsWith<FleksNoSuchSystemException> { service.system<SystemTestIntervalSystemEachFrame>() }
    }

    @Test
    fun updateOnlyCallsEnabledSystems() {
        val service =
            systemService(mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame))
        val system = service.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        service.update()

        assertEquals(0, system.numCalls)
    }

    @Test
    fun removingAnEntityDuringUpdateIsDelayed() {
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestIteratingSystemSortAutomatic::class to ::SystemTestIteratingSystemSortAutomatic),
                world = world
            )
        world.entity { add<SystemTestComponent> { x = 15f } }
        val entityToRemove = world.entity { add<SystemTestComponent> { x = 10f } }
        world.entity { add<SystemTestComponent> { x = 5f } }
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
        val world = World {
            component(::SystemTestComponent)
        }
        val service =
            systemService(
                mutableMapOf(SystemTestFixedSystemRemoval::class to ::SystemTestFixedSystemRemoval),
                world = world
            )
        // set delta time to 1f for the fixed interval
        world.update(1f)
        world.entity { add<SystemTestComponent> { x = 15f } }
        val entityToRemove = world.entity { add<SystemTestComponent> { x = 10f } }
        world.entity { add<SystemTestComponent> { x = 5f } }
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
        val service =
            systemService(mutableMapOf(SystemTestIntervalSystemEachFrame::class to ::SystemTestIntervalSystemEachFrame))

        service.dispose()

        assertEquals(1, service.system<SystemTestIntervalSystemEachFrame>().numDisposes)
    }

    @Test
    fun initBlockOfSystemConstructorHasNoAccessToTheWorld() {
        assertFailsWith<RuntimeException> { systemService(mutableMapOf(SystemTestInitBlock::class to ::SystemTestInitBlock)) }
    }

    @Test
    fun onInitBlockIsCalledForAnyNewlyCreatedSystem() {
        val expected = 0f

        val service =
            systemService(mutableMapOf(SystemTestOnInitBlock::class to ::SystemTestOnInitBlock))

        assertEquals(expected, service.system<SystemTestOnInitBlock>().someValue)
    }
}
