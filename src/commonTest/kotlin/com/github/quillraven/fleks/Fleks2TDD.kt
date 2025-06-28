package com.github.quillraven.fleks

import com.github.quillraven.fleks.Sprite.Companion.SpriteBackground
import com.github.quillraven.fleks.Sprite.Companion.SpriteForeground
import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import kotlin.test.*

private data class Position(
    var x: Float,
    var y: Float,
) : Component<Position> {
    override fun type(): ComponentType<Position> = Position

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    companion object : ComponentType<Position>()
}

abstract class ColliderService {
    abstract fun getId(): String
}

private data class Collider(
    val size: Float
) : Component<Collider> {
    override fun type(): ComponentType<Collider> = Collider

    var colliderId: String? = null

    override fun GenericWorld.onAdd(entity: Entity) {
        val provider = inject<ColliderService>()
        colliderId = provider.getId()
    }

    override fun GenericWorld.onRemove(entity: Entity) {
        colliderId = null
    }

    companion object : ComponentType<Collider>()
}

private data class Sprite(
    val background: Boolean,
    var path: String = "",
) : Component<Sprite> {
    override fun type(): ComponentType<Sprite> {
        return when (background) {
            true -> SpriteBackground
            else -> SpriteForeground
        }
    }

    companion object {
        val SpriteForeground = object : ComponentType<Sprite>() {}
        val SpriteBackground = object : ComponentType<Sprite>() {}
    }
}

private class PositionSystem : IteratingSystem<Unit>(family { all(Position) }) {
    override fun onTickEntity(entity: Entity) {
        entity[Position].x++
    }
}

private class SpriteSystem(
    val cstrInjectable: String = inject()
) : IteratingSystem<Unit>(family { any(SpriteBackground, SpriteForeground) }) {
    val propInjectable: String = world.inject("qualifiedString")

    override fun onTickEntity(entity: Entity) = Unit
}

class Fleks2TDD {
    private val emptyWorld = configureWorld { }

    @Test
    fun createWorldWithEntityAndComponent() {
        val expectedComp = Position(1f, 1f)

        val entity = emptyWorld.entity {
            it += expectedComp
        }

        val actualComp: Position = with(emptyWorld) { entity[Position] }
        assertEquals(expectedComp, actualComp)
    }

    @Test
    fun createEntityWithTwoEqualComponents() {
        val expectedComp1 = Sprite(true, "background")
        val expectedComp2 = Sprite(false, "foreground")

        val entity = emptyWorld.entity {
            it += expectedComp1
            it += expectedComp2
        }

        assertEquals(expectedComp1, with(emptyWorld) { entity[SpriteBackground] })
        assertEquals(expectedComp2, with(emptyWorld) { entity[SpriteForeground] })
    }

    @Test
    fun retrieveNonExistingComponent() {
        val entity = emptyWorld.entity()

        assertFailsWith<FleksNoSuchEntityComponentException> {
            with(emptyWorld) { entity[Position] }
        }
    }

    @Test
    fun configureEntityAfterCreation() {
        val entity = emptyWorld.entity { it += Position(0f, 0f) }

        with(emptyWorld) {
            entity.configure {
                it -= Position
                it += Sprite(true, "")
            }
        }

        assertFalse(with(emptyWorld) { Position in entity })
        assertTrue(with(emptyWorld) { entity has SpriteBackground })
    }

    @Test
    fun configureEntityWithGetOrAdd() {
        // this entity gets its position component updated
        val posEntity = emptyWorld.entity { it += Position(0f, 0f) }
        // this entity gets its position component added
        val emptyEntity = emptyWorld.entity()

        with(emptyWorld) {
            posEntity.configure {
                it.getOrAdd(Position) { Position(1f, 1f) }.x = 2f
            }

            emptyEntity.configure {
                it.getOrAdd(Position) { Position(1f, 1f) }.x = 3f
            }
        }

        assertEquals(Position(2f, 0f), with(emptyWorld) { posEntity[Position] })
        assertEquals(Position(3f, 1f), with(emptyWorld) { emptyEntity[Position] })
    }

    @Test
    fun updatePositionSystem() {
        val world = configureWorld {
            systems {
                add(PositionSystem())
            }
        }
        val entity = world.entity {
            it += Position(0f, 0f)
        }

        world.update(1f)

        assertEquals(1f, with(world) { entity[Position] }.x)
    }

    @Test
    fun testComponentLifecycleMethods() {
        val addComponent = Collider(0f)
        val removeComponent = Collider(2f)
        val testWorld = configureWorld {
            injectables {
                add<ColliderService>(object : ColliderService() {
                    var nextId = 0
                    override fun getId() = (nextId++).toString()
                })
            }
        }

        // entity that triggers onAdd lifecycle method
        assertEquals(null, addComponent.colliderId)
        testWorld.entity { it += addComponent }
        assertEquals("0", addComponent.colliderId)

        // entity that triggers onRemove lifecycle method
        val removeEntity = testWorld.entity { it += removeComponent }
        assertEquals("1", removeComponent.colliderId)
        with(testWorld) { removeEntity.configure { it -= Collider } }
        assertEquals(null, removeComponent.colliderId)
    }

    @Test
    fun testFamilyHooks() {
        lateinit var testWorld: GenericWorld
        lateinit var testFamily: Family
        var numAddCalls = 0
        var numRemoveCalls = 0
        testWorld = configureWorld {
            testFamily = family { all(Position) }
            families {
                onAdd(testFamily) { entity ->
                    ++numAddCalls
                    assertEquals(testWorld, this)
                    assertTrue { entity.id in 0..1 }
                }

                onRemove(testFamily) { entity ->
                    ++numRemoveCalls
                    assertEquals(testWorld, this)
                    assertEquals(Entity(1, version = 0u), entity)
                }
            }
        }

        // entity that triggers onAdd hook
        testWorld.entity { it += Position(0f, 0f) }
        // entity that triggers onRemove hook
        val removeEntity = testWorld.entity { it += Position(0f, 0f) }
        with(testWorld) { removeEntity.configure { it -= Position } }

        assertEquals(2, numAddCalls)
        assertEquals(1, numRemoveCalls)
    }

    @Test
    fun testSystemCreationWithInjectables() {
        val expectedCstrStr = "42"
        val expectedPropStr = "1337"
        val world = configureWorld(entityCapacity = 64) {
            injectables {
                add(expectedCstrStr)
                add("qualifiedString", expectedPropStr)
            }

            systems {
                add(SpriteSystem())
            }
        }

        val system = world.system<SpriteSystem>()

        assertEquals(expectedCstrStr, system.cstrInjectable)
        assertEquals(expectedPropStr, system.propInjectable)
    }

    @Test
    fun testEntityComponentContextExtensions() {
        val world = configureWorld { }
        val expectedCmp = Position(0f, 0f)
        val entity = world.entity { it += expectedCmp }

        with(world) {
            // get extensions
            assertSame(expectedCmp, entity[Position])
            assertSame(expectedCmp, entity.getOrNull(Position))
            assertNull(entity.getOrNull(SpriteForeground))

            // contains extensions
            assertTrue(Position in entity)
            assertTrue(entity has Position)
            assertFalse(entity hasNo Position)
            assertFalse(SpriteForeground in entity)
            assertFalse(entity has SpriteForeground)
            assertTrue(entity hasNo SpriteForeground)

            // configure extension
            entity.configure {
                it += Sprite(background = false)
            }
            assertTrue(entity has SpriteForeground)

            // remove extension
            assertTrue(entity in world)
            entity.remove()
            assertFalse(entity in world)
        }
    }
}
