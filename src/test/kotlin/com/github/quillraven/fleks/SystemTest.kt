package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import kotlin.reflect.KClass
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

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

private class SystemTestIntervalSystemMultipleCstrs(
    val arg: String = ""
) : IntervalSystem() {
    override fun onTick() = Unit
}

private data class SystemTestComponent(var x: Float = 0f)

private class SystemTestIteratingSystemNoFamily : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemMapper(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(interval = Fixed(0.25f)) {
    var numEntityCalls = 0
    var numAlphaCalls = 0
    var lastAlpha = 0f
    var entityToConfigure: Entity? = null

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

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemSortAutomatic(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(
    compareEntity { e1, e2 -> mapper[e2].x.compareTo(mapper[e1].x) }
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

@AllOf([SystemTestComponent::class])
private class SystemTestFixedSystemRemoval(
    private val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(interval = Fixed(1f)) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)
    var entityToRemove: Entity? = null

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

@AllOf([SystemTestComponent::class])
private class SystemTestIteratingSystemSortManual(
    val mapper: ComponentMapper<SystemTestComponent>
) : IteratingSystem(
    compareEntity { e1, e2 -> mapper[e2].x.compareTo(mapper[e1].x) },
    sortingType = Manual
) {
    var numEntityCalls = 0
    var lastEntityProcess = Entity(-1)

    override fun onTickEntity(entity: Entity) {
        lastEntityProcess = entity
        ++numEntityCalls
    }
}

@NoneOf([SystemTestComponent::class])
@AnyOf([SystemTestComponent::class])
private class SystemTestIteratingSystemInjectable(
    val injectable: String
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

@NoneOf([SystemTestComponent::class])
@AnyOf([SystemTestComponent::class])
private class SystemTestIteratingSystemQualifiedInjectable(
    val injectable: String,
    @Qualifier("q1") val injectable2: String
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) = Unit
}

@AnyOf([SystemTestComponent::class])
private class SystemTestEntityCreation : IteratingSystem() {
    var numTicks = 0

    init {
        world.entity { add<SystemTestComponent>() }
    }

    override fun onTickEntity(entity: Entity) {
        ++numTicks
    }
}

internal class SystemTest {
    private fun systemService(
        systemTypes: List<KClass<out IntervalSystem>>,
        injectables: Map<String, Injectable> = emptyMap(),
        world: World = world { }
    ) = SystemService(
        world,
        systemTypes,
        injectables
    )

    @Test
    fun `system with interval EachFrame gets called every time`() {
        val system = SystemTestIntervalSystemEachFrame()

        system.onUpdate()
        system.onUpdate()

        assertEquals(2, system.numCalls)
    }

    @Test
    fun `system with interval EachFrame returns world's delta time`() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemEachFrame()
        system.world.update(42f)

        assertEquals(42f, system.deltaTime)
    }

    @Test
    fun `system with fixed interval of 0,25f gets called four times when delta time is 1,1f`() {
        World.CURRENT_WORLD = world { }
        val system = SystemTestIntervalSystemFixed()
        system.world.update(1.1f)

        system.onUpdate()

        assertAll(
            { assertEquals(4, system.numCalls) },
            { assertEquals(0.1f / 0.25f, system.lastAlpha, 0.0001f) }
        )
    }

    @Test
    fun `system with fixed interval returns step rate as delta time`() {
        val system = SystemTestIntervalSystemFixed()

        assertEquals(0.25f, system.deltaTime, 0.0001f)
    }

    @Test
    fun `create IntervalSystem with no-args`() {
        val expectedWorld = world {}

        val service = systemService(listOf(SystemTestIntervalSystemEachFrame::class), world = expectedWorld)

        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertNotNull(service.system<SystemTestIntervalSystemEachFrame>()) },
            { assertSame(expectedWorld, service.system<SystemTestIntervalSystemEachFrame>().world) }
        )
    }

    @Test
    fun `create IteratingSystem with a ComponentMapper arg`() {
        val expectedWorld = world {}

        val service = systemService(listOf(SystemTestIteratingSystemMapper::class), world = expectedWorld)

        val actualSystem = service.system<SystemTestIteratingSystemMapper>()
        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertSame(expectedWorld, actualSystem.world) },
            { assertEquals(SystemTestComponent::class.java.name, actualSystem.mapper.cstr.name) },
        )
    }

    @Test
    fun `create IteratingSystem with an injectable arg`() {
        val expectedWorld = world {}

        val service = systemService(
            listOf(SystemTestIteratingSystemInjectable::class),
            mapOf(String::class.qualifiedName!! to Injectable("42")),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemInjectable>()
        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertSame(expectedWorld, actualSystem.world) },
            { assertEquals("42", actualSystem.injectable) },
        )
    }

    @Test
    fun `create IteratingSystem with qualified args`() {
        val expectedWorld = world {}

        val service = systemService(
            listOf(SystemTestIteratingSystemQualifiedInjectable::class),
            mapOf(String::class.qualifiedName!! to Injectable("42"), "q1" to Injectable("43")),
            expectedWorld
        )

        val actualSystem = service.system<SystemTestIteratingSystemQualifiedInjectable>()
        assertAll(
            { assertEquals(1, service.systems.size) },
            { assertSame(expectedWorld, actualSystem.world) },
            { assertEquals("42", actualSystem.injectable) },
            { assertEquals("43", actualSystem.injectable2) },
        )
    }

    @Test
    fun `cannot create IteratingSystem without family annotations`() {
        assertThrows<FleksSystemCreationException> { systemService(listOf(SystemTestIteratingSystemNoFamily::class)) }
    }

    @Test
    fun `cannot create IteratingSystem with missing injectables`() {
        assertThrows<FleksReflectionException> { systemService(listOf(SystemTestIteratingSystemInjectable::class)) }
    }

    @Test
    fun `cannot create system with multiple constructors`() {
        assertThrows<FleksReflectionException> { systemService(listOf(SystemTestIntervalSystemMultipleCstrs::class)) }
    }

    @Test
    fun `IteratingSystem calls onTick and onAlpha for each entity of the system`() {
        val world = world {}
        val service = systemService(listOf(SystemTestIteratingSystemMapper::class), world = world)
        world.entity { add<SystemTestComponent>() }
        world.entity { add<SystemTestComponent>() }
        world.update(0.3f)

        service.update()

        val system = service.system<SystemTestIteratingSystemMapper>()
        assertAll(
            { assertEquals(2, system.numEntityCalls) },
            { assertEquals(2, system.numAlphaCalls) },
            { assertEquals(0.05f / 0.25f, system.lastAlpha, 0.0001f) }
        )
    }

    @Test
    fun `configure entity during iteration`() {
        val world = world {}
        val service = systemService(listOf(SystemTestIteratingSystemMapper::class), world = world)
        world.update(0.3f)
        val entity = world.entity { add<SystemTestComponent>() }
        val system = service.system<SystemTestIteratingSystemMapper>()
        system.entityToConfigure = entity

        service.update()

        assertFalse { entity in system.mapper }
    }

    @Test
    fun `sort entities automatically`() {
        val world = world {}
        val service = systemService(listOf(SystemTestIteratingSystemSortAutomatic::class), world = world)
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }

        service.update()

        assertEquals(expectedEntity, service.system<SystemTestIteratingSystemSortAutomatic>().lastEntityProcess)
    }

    @Test
    fun `sort entities programmatically`() {
        val world = world {}
        val service = systemService(listOf(SystemTestIteratingSystemSortManual::class), world = world)
        world.entity { add<SystemTestComponent> { x = 15f } }
        world.entity { add<SystemTestComponent> { x = 10f } }
        val expectedEntity = world.entity { add<SystemTestComponent> { x = 5f } }
        val system = service.system<SystemTestIteratingSystemSortManual>()

        system.doSort = true
        service.update()

        assertAll(
            { assertEquals(expectedEntity, system.lastEntityProcess) },
            { assertFalse(system.doSort) }
        )
    }

    @Test
    fun `cannot get a non-existing system`() {
        val service = systemService(listOf(SystemTestIteratingSystemSortAutomatic::class))

        assertThrows<FleksNoSuchSystemException> {
            service.system<SystemTestIntervalSystemEachFrame>()
        }
    }

    @Test
    fun `update only calls enabled systems`() {
        val service = systemService(listOf(SystemTestIntervalSystemEachFrame::class))
        val system = service.system<SystemTestIntervalSystemEachFrame>()
        system.enabled = false

        service.update()

        assertEquals(0, system.numCalls)
    }

    @Test
    fun `removing an entity during update is delayed`() {
        val world = world {}
        val service = systemService(listOf(SystemTestIteratingSystemSortAutomatic::class), world = world)
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
    fun `removing an entity during alpha is delayed`() {
        val world = world {}
        val service = systemService(listOf(SystemTestFixedSystemRemoval::class), world = world)
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
    fun `dispose service`() {
        val service = systemService(listOf(SystemTestIntervalSystemEachFrame::class))

        service.dispose()

        assertEquals(1, service.system<SystemTestIntervalSystemEachFrame>().numDisposes)
    }

    @Test
    fun `create entity during system init`() {
        // this test verifies that entities that are created in a system's init block
        // are correctly added to families
        val world = world {}

        val service = systemService(listOf(SystemTestEntityCreation::class), world = world)
        service.update()

        val system = service.system<SystemTestEntityCreation>()
        assertEquals(1, system.numTicks)
    }
}
