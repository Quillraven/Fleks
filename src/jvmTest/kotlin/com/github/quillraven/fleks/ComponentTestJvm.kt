package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class ComponentTestJvm {

    private data class ComponentTestComponent(var x: Int = 0) : Component<ComponentTestComponent> {
        override fun type() = ComponentTestComponent

        companion object : ComponentType<ComponentTestComponent>()
    }

    @Test
    fun cannotRemoveNonExistingEntityFromHolderWithInsufficientCapacity() {
        val world = world { }
        val cmpService = ComponentService(world)
        val holder = cmpService.holder(ComponentTestComponent)
        val entity = Entity(10_000)

        assertFailsWith<IndexOutOfBoundsException> { holder -= entity }
    }
}
