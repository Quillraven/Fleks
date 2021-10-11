package com.github.quillraven.fleks

class EventSystem(val number: Int = 42)

@AllOf([Position::class])
class TestSystem(
    private val eventSystem: EventSystem,
    private val number: Int = eventSystem.number,
    private val positions: ComponentMapper<Position>
) : IteratingSystem() {
    override fun onEntityAction(entity: Entity, deltaTime: Float) {
        println("$number $entity ${positions[entity]}")
    }
}

class TestSystem2(
    private val positions: ComponentMapper<Position>,
    private val lifes: ComponentMapper<Life>
) : EntitySystem(enabled = false) {
    override fun onTick(deltaTime: Float) {
        println("${positions[Entity(0)]} ${lifes[Entity(0)]}")
        println("${positions[Entity(1)]} ${lifes[Entity(1)]}")
    }
}

@AllOf(components = [Position::class])
class TestSystem3(
    private val lifes: ComponentMapper<Life>
) : IteratingSystem() {
    override fun onEntityAction(entity: Entity, deltaTime: Float) {
        println("$entity ${lifes[entity]}")
    }
}

data class Position(var x: Float = 0f, var y: Float = 0f)

data class Life(var points: Int = 150)

fun main() {
    val w = World {
        entityCapacity = 512

        system<TestSystem>()
        system<TestSystem2>()
        system<TestSystem3>()

        inject(EventSystem())
    }

    w.entity {
        add<Position> { x = 5f }
        add<Life>()
    }

    w.entity {
        add<Position> { y = 5f }
        add<Life> { points = 75 }
    }

    w.update(1f)
}
