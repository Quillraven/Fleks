package com.github.quillraven.fleks

data class Position(var x: Float = 0f, var y: Float = 0f) : Component<Position> {
    companion object : ComponentType<Position>()

    override fun type() = Position
}

class PositionComponentListener : ComponentListener<Position> {
    override fun onComponentAdded(entity: Entity, component: Position) {
        println("added")
    }

    override fun onComponentRemoved(entity: Entity, component: Position) = Unit
}

@AllOf([Position::class])
class PositionSystem : IteratingSystem() {
    override fun onTickEntity(entity: Entity) {
        println(entity[Position])
    }
}

fun main() {
    val w = world {
        components {
            add<PositionComponentListener>()
        }

        systems {
            add<PositionSystem>()
        }
    }

    w.entity {
        it += Position(2f, 3f)
    }

    w.update(1f)
}
