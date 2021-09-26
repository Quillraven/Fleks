package com.github.quillraven.fleks

class EventSystem(val number: Int = 42)

class TestSystem(val eventSystem: EventSystem, val number: Int) : EntitySystem() {
    override fun update() {
        println(number)
    }
}

fun main() {
    val w = World {
        entityCapacity = 512

        system<TestSystem>()

        inject(EventSystem())
        inject(1337)
    }

    w.update()
}
