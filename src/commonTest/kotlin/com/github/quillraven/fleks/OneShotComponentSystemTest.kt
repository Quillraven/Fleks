package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class OneShotComp : Component<OneShotComp> {
    override fun type() = OneShotComp

    companion object : ComponentType<OneShotComp>()
}

private data object OneShotTag : EntityTag()

private class NormalComp : Component<NormalComp> {
    override fun type() = NormalComp

    companion object : ComponentType<NormalComp>()
}

internal class OneShotComponentSystemTest {
    @Test
    fun oneShotComponentShouldGetRemovedAfterOnUpdate() {
        val world = configureWorld {
            oneShotComponents(OneShotComp)
        }
        val entity = world.entity { it += OneShotComp() }

        with(world) {
            assertTrue { entity has OneShotComp }

            world.update(0f)

            assertFalse { entity has OneShotComp }
        }
    }

    @Test
    fun oneShotTagShouldGetRemovedAfterOnUpdate() {
        val world = configureWorld {
            oneShotComponents(OneShotTag)
        }
        val entity = world.entity { it += OneShotTag }

        with(world) {
            assertTrue { entity has OneShotTag }

            world.update(0f)

            assertFalse { entity has OneShotTag }
        }
    }

    @Test
    fun normalComponentShouldNotGetRemovedAfterOnUpdate() {
        val world = configureWorld {
            oneShotComponents(OneShotComp)
        }
        val entity = world.entity { it += NormalComp() }

        with(world) {
            assertTrue { entity has NormalComp }

            world.update(0f)

            assertTrue { entity has NormalComp }
        }
    }

    @Test
    fun listOfOneShotsShouldGetRemovedAfterOnUpdate() {
        val world = configureWorld {
            oneShotComponents(OneShotComp, OneShotTag)
        }
        val entity = world.entity {
            it += NormalComp()
            it += OneShotComp()
            it += OneShotTag
        }

        with(world) {
            assertTrue { entity has NormalComp }
            assertTrue { entity has OneShotComp }
            assertTrue { entity has OneShotTag }

            world.update(0f)

            assertTrue { entity has NormalComp }
            assertFalse { entity has OneShotComp }
            assertFalse { entity has OneShotTag }
        }
    }
}
