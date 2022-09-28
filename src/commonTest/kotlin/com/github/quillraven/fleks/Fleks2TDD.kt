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

private class PositionSystem : IteratingSystem(family { all(Position) }) {
    override fun onTickEntity(entity: Entity) {
        entity[Position].x++
    }
}

private class SpriteSystem(
    val cstrInjectable: String = inject()
) : IteratingSystem(family { any(SpriteBackground, SpriteForeground) }) {
    val propInjectable: String = world.inject("qualifiedString")

    override fun onTickEntity(entity: Entity) = Unit
}

class Fleks2TDD {
    private val emptyWorld = world { }

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
        val world = world {
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
    fun testComponentHooks() {
        val addComponent = Position(0f, 0f)
        val removeComponent = Position(0f, 0f)
        lateinit var testWorld: World
        testWorld = world {
            components {
                onAdd(Position) { entity, component ->
                    component.x = 1f
                    assertEquals(testWorld, this)
                    assertTrue { entity.id in 0..1 }
                    assertTrue { component in listOf(addComponent, removeComponent) }
                }

                onRemove(Position) { entity, component ->
                    component.x = 2f
                    assertEquals(testWorld, this)
                    assertEquals(Entity(1), entity)
                    assertEquals(removeComponent, component)
                }
            }
        }

        // entity that triggers onAdd hook
        testWorld.entity { it += addComponent }
        // entity that triggers onRemove hook
        val removeEntity = testWorld.entity { it += removeComponent }
        with(testWorld) { removeEntity.configure { it -= Position } }

        assertEquals(1f, addComponent.x)
        assertEquals(2f, removeComponent.x)
    }

    @Test
    fun testFamilyHooks() {
        lateinit var testWorld: World
        lateinit var testFamily: Family
        var numAddCalls = 0
        var numRemoveCalls = 0
        testWorld = world {
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
                    assertEquals(Entity(1), entity)
                }
            }
        }

        // entity that triggers onAdd hook
        testWorld.entity { it += Position(0f, 0f) }
        // entity that triggers onRemove hook
        val removeEntity = testWorld.entity { it += Position(0f, 0f) }
        with(testWorld) { removeEntity.configure { it -= Position } }
        // trigger family update to call the hooks
        testFamily.updateActiveEntities()

        assertEquals(2, numAddCalls)
        assertEquals(1, numRemoveCalls)
    }

    @Test
    fun testSystemCreationWithInjectables() {
        val expectedCstrStr = "42"
        val expectedPropStr = "1337"
        val world = world(64) {
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
        val world = world { }
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
