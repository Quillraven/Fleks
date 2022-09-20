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

        val actualComp: Position = emptyWorld[Position][entity]
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

        assertEquals(expectedComp1, emptyWorld[SpriteBackground][entity])
        assertEquals(expectedComp2, emptyWorld[SpriteForeground][entity])
    }

    @Test
    fun retrieveNonExistingComponent() {
        val entity = emptyWorld.entity()

        assertFailsWith<FleksNoSuchEntityComponentException> {
            emptyWorld[Position][entity]
        }
    }

    @Test
    fun configureEntityAfterCreation() {
        val entity = emptyWorld.entity { it += Position(0f, 0f) }

        emptyWorld.configure(entity) {
            it -= Position
            it += Sprite(true, "")
        }

        assertFalse(entity in emptyWorld[Position])
        assertTrue(entity in emptyWorld[SpriteBackground])
    }

    @Test
    fun configureEntityWithAddOrUpdate() {
        // this entity gets its position component updated
        val posEntity = emptyWorld.entity { it += Position(0f, 0f) }
        // this entity gets its position component added
        val emptyEntity = emptyWorld.entity()

        emptyWorld.configure(posEntity) {
            it.addOrUpdate(
                Position,
                add = { Position(1f, 1f) },
                update = { position -> position.set(2f, 2f) }
            )
        }
        emptyWorld.configure(emptyEntity) {
            it.addOrUpdate(
                Position,
                add = { Position(1f, 1f) },
                update = { position -> position.set(2f, 2f) }
            )
        }

        assertEquals(Position(2f, 2f), emptyWorld[Position][posEntity])
        assertEquals(Position(1f, 1f), emptyWorld[Position][emptyEntity])
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

        assertEquals(1f, world[Position][entity].x)
    }

    @Test
    fun testComponentHooks() {
        val addComponent = Position(0f, 0f)
        val removeComponent = Position(0f, 0f)
        lateinit var testWorld: World
        testWorld = world {
            components {
                onAdd(Position) { world, entity, component ->
                    component.x = 1f
                    assertEquals(testWorld, world)
                    assertTrue { entity.id in 0..1 }
                    assertTrue { component in listOf(addComponent, removeComponent) }
                }

                onRemove(Position) { world, entity, component ->
                    component.x = 2f
                    assertEquals(testWorld, world)
                    assertEquals(Entity(1), entity)
                    assertEquals(removeComponent, component)
                }
            }
        }

        // entity that triggers onAdd hook
        testWorld.entity { it += addComponent }
        // entity that triggers onRemove hook
        val removeEntity = testWorld.entity { it += removeComponent }
        testWorld.configure(removeEntity) { it -= Position }

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
                onAdd(testFamily) { world, entity ->
                    ++numAddCalls
                    assertEquals(testWorld, world)
                    assertTrue { entity.id in 0..1 }
                }

                onRemove(testFamily) { world, entity ->
                    ++numRemoveCalls
                    assertEquals(testWorld, world)
                    assertEquals(Entity(1), entity)
                }
            }
        }

        // entity that triggers onAdd hook
        testWorld.entity { it += Position(0f, 0f) }
        // entity that triggers onRemove hook
        val removeEntity = testWorld.entity { it += Position(0f, 0f) }
        testWorld.configure(removeEntity) { it -= Position }
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
    fun testEntityContextExtensions() {
        lateinit var testFamily: Family
        val expectedAddCmp = Position(0f, 0f)
        val expectedRemoveCmp = Sprite(true)
        var cmpAddCalled = false
        var cmpRemoveCalled = false
        var familyAddCalled = false
        var familyRemoveCalled = false
        /*
            component and family hooks run within an EntityHookContext which should
            allow easy access of components with get operator
         */
        val testWorld = world {
            components {
                onAdd(Position) { _, entity, _ ->
                    cmpAddCalled = true
                    assertSame(expectedAddCmp, entity[Position])
                }
                onRemove(Position) { _, entity, _ ->
                    cmpRemoveCalled = true
                    assertSame(expectedRemoveCmp, entity[SpriteBackground])
                }
            }

            families {
                testFamily = family { all(Position) }
                onAdd(testFamily) { _, entity ->
                    familyAddCalled = true
                    assertSame(expectedAddCmp, entity[Position])
                }
                onRemove(testFamily) { _, entity ->
                    familyRemoveCalled = true
                    assertSame(expectedRemoveCmp, entity[SpriteBackground])
                }
            }
        }

        // trigger component onAdd hook and verify EntityCreateContext extensions
        val testEntity = testWorld.entity {
            it += expectedAddCmp
            assertSame(expectedAddCmp, it[Position])
        }
        // trigger family onAdd hook
        testFamily.updateActiveEntities()
        // verify EntityUpdateContext extensions
        testWorld.configure(testEntity) {
            assertSame(expectedAddCmp, it[Position])
            it += expectedRemoveCmp
            // trigger component onRemove hook -> this also removes the entity of the family below
            it -= Position
            it.addOrUpdate(
                SpriteBackground,
                add = { Sprite(true, "add") },
                update = { sprite -> sprite.path = "update" }
            )
            assertEquals("update", it[SpriteBackground].path)
        }
        // trigger family onRemove hook
        testFamily.updateActiveEntities()
        assertTrue(cmpAddCalled)
        assertTrue(cmpRemoveCalled)
        assertTrue(familyAddCalled)
        assertTrue(familyRemoveCalled)
    }
}
