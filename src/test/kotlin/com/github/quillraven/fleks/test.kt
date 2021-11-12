package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.compareEntity

class EventSystem(val number: Int = 42)

@AllOf([Position::class])
class TestSystem(
    private val eventSystem: EventSystem,
    private val number: Int = eventSystem.number,
    private val positions: ComponentMapper<Position>
) : IteratingSystem(compareEntity { entA, entB -> positions[entA].y.compareTo(positions[entB].y) }) {
    private var numIterations = 0

    override fun onTick() {
        ++numIterations
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        if (numIterations == 4) {
            world.remove(Entity(1))
        }
        println("$number $entity ${positions[entity]} ${world.deltaTime}")
    }
}

class TestSystem2(
    private val positions: ComponentMapper<Position>,
    private val lifes: ComponentMapper<Life>
) : IntervalSystem(enabled = false, interval = Fixed(1f)) {
    override fun onTick() {
        if (Entity(0) in positions && Entity(0) in lifes) {
            println("${positions[Entity(0)]} ${lifes[Entity(0)]} $deltaTime")
        }
        if (Entity(1) in positions && Entity(1) in lifes) {
            println("${positions[Entity(1)]} ${lifes[Entity(1)]}")
        }
    }
}

@AllOf(components = [Position::class])
class TestSystem3(
    private val lifes: ComponentMapper<Life>
) : IteratingSystem() {
    override fun onTickEntity(entity: Entity) {
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

    val e = w.entity {
        add<Position> { x = 5f }
        add<Life>()
    }

    w.entity {
        add<Position> { y = 5f }
        add<Life> { points = 75 }
    }

    w.entity {
        add<Position> { y = 3f }
        add<Life> { points = 125 }
    }

    w.update(1f)
    println()

    w.system<TestSystem2>().enabled = true
    w.update(1f)
    println()

    w.remove(e)
    w.update(1f)
    println()

    w.update(1f)
}
