package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private data class Position(
    var x: Float,
    var y: Float,
) : Component<Position> {
    override fun type(): ComponentType<Position> = Position

    companion object : ComponentType<Position>()
}

private class PositionSystem : IteratingSystem() {
    override fun familyDefinition(): FamilyDefinition = familyDefinition {
        allOf(Position)
    }

    override fun onTickEntity(entity: Entity) {
        entity[Position].x++
    }
}

// TODO
// 1) configureEntity with component adding/removal
// 2) configureEntity with addOrUpdate
// 3) ComponentListener / FamilyListener
// 4) injectables via "by inject" ?

class Fleks2TDD {
    private val emptyWorld = world { }

    private val positionWorld = world {
        systems {
            add(::PositionSystem)
        }
    }

    @Test
    fun createWorldWithEntityAndComponent() {
        val expectedComp = Position(1f, 1f)

        val entity = emptyWorld.entity {
            it += expectedComp
        }

        val actualComp: Position = emptyWorld[entity, Position]
        assertEquals(expectedComp, actualComp)
    }

    @Test
    fun retrieveNonExistingComponent() {
        val entity = emptyWorld.entity()

        assertFailsWith<FleksNoSuchEntityComponentException> {
            emptyWorld[entity, Position]
        }
    }

    @Test
    fun updatePositionSystem() {
        val entity = positionWorld.entity {
            it += Position(0f, 0f)
        }

        positionWorld.update(1f)

        assertEquals(1f, positionWorld[entity, Position].x)
    }
}
