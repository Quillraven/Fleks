package com.github.quillraven.fleks

class EventSystem(val number: Int = 42)

class TestSystem(
    val eventSystem: EventSystem,
    val number: Int = eventSystem.number,
    val positions: ComponentMapper<Position>
) : EntitySystem() {
    override fun update() {
        println("$number ${positions[0]}")
        println("$number ${positions[1]}")
    }
}

class TestSystem2(
    val positions: ComponentMapper<Position>,
    val lifes: ComponentMapper<Life>
) : EntitySystem() {
    override fun update() {
        println("${positions[0]} ${lifes[0]}")
        println("${positions[1]} ${lifes[1]}")
    }
}

data class Position(var x: Float = 0f, var y: Float = 0f)

data class Life(var points: Int = 150)

fun main() {

    val w = World {
        entityCapacity = 512

        system<TestSystem>()
        system<TestSystem2>()

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

    w.update()
}
