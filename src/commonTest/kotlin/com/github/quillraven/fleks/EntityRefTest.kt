package com.github.quillraven.fleks

import kotlin.test.*

data class EntityRefComponent(val ref: EntityRef) : Component<EntityRefComponent> {
    override fun type() = EntityRefComponent

    companion object : ComponentType<EntityRefComponent>()
}

class EntityRefTest {

    @Test
    fun testEntityRefValid() {
        lateinit var entityRefCmp: EntityRefComponent
        val world = configureWorld { }
        val entity1 = world.entity()
        world.entity { e ->
            e += EntityRefComponent(entity1.ref()).also { entityRefCmp = it }
        }

        assertTrue(entityRefCmp.ref.valid)
        assertEquals(entity1, entityRefCmp.ref.entity)

        world -= entity1
        assertFalse(entityRefCmp.ref.valid)
        assertEquals(entity1, entityRefCmp.ref.entity)
    }

    @Test
    fun testEntityRefInstance() {
        val world = configureWorld { }
        val entity1 = world.entity()

        with(world) {
            val ref1 = entity1.ref()
            val ref2 = entity1.ref()
            assertSame(ref1, ref2)
        }
    }

}
