package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private class CtxPosition(var x: Float) : Component<CtxPosition> {
    override fun type() = CtxPosition

    companion object : ComponentType<CtxPosition>()
}

private class CtxVelocity(var x: Float) : Component<CtxVelocity> {
    override fun type() = CtxVelocity

    companion object : ComponentType<CtxVelocity>()
}

private data object CtxTag : EntityTag()

context(_: EntityComponentContext)
private fun Entity.moveBy(dx: Float) {
    this[CtxPosition].x += dx
}

context(_: EntityComponentContext)
private fun EntityRef.moveBy(dx: Float) {
    this[CtxPosition].x += dx
}

private class CtxMoveSystem : IteratingSystem(family { all(CtxPosition) }) {
    override fun onTickEntity(entity: Entity) = entity.moveBy(1f)
}

internal class EntityContextExtensionsTest {
    @Test
    fun `context functions can be called from a system`() {
        val world = configureWorld { systems { add(CtxMoveSystem()) } }
        val entity = world.entity { it += CtxPosition(0f) }

        world.update(1f)

        assertEquals(1f, with(world) { entity[CtxPosition].x })
    }

    @Test
    fun `context functions can be called from a world and entity creation`() {
        val world = configureWorld { }
        val entity = world.entity {
            it += CtxPosition(0f)
            it.moveBy(1f)
        }

        with(world) { entity.moveBy(2f) }

        assertEquals(3f, with(world) { entity[CtxPosition].x })
    }

    @Test
    fun `entity extensions work with a context parameter`() {
        val world = configureWorld { }
        val entity = world.entity { it += CtxPosition(0f) }

        with(world) { checkEntity(entity) }

        assertTrue { with(world) { entity.wasRemoved() } }
    }

    context(_: EntityComponentContext)
    private fun checkEntity(entity: Entity) {
        assertTrue { entity has CtxPosition }
        assertTrue { CtxPosition in entity }
        assertTrue { entity hasNo CtxVelocity }
        assertNull(entity.getOrNull(CtxVelocity))
        assertSame(entity.getRef(), entity.getRef())

        entity.configure {
            it += CtxVelocity(2f)
            it += CtxTag
        }
        assertEquals(2f, entity[CtxVelocity].x)
        assertTrue { entity has CtxTag }

        assertFalse { entity.isMarkedForRemoval() }
        entity.remove()
    }

    @Test
    fun `entity ref extensions work with a context parameter`() {
        val world = configureWorld { }
        val entity = world.entity { it += CtxPosition(0f) }
        val ref = with(world) { entity.getRef() }

        with(world) { checkEntityRef(ref) }

        assertFalse { ref.valid }
    }

    context(_: EntityComponentContext)
    private fun checkEntityRef(ref: EntityRef) {
        ref.moveBy(1f)
        assertEquals(1f, ref[CtxPosition].x)
        assertTrue { ref has CtxPosition }
        assertTrue { CtxPosition in ref }
        assertTrue { ref hasNo CtxVelocity }
        assertNull(ref.getOrNull(CtxVelocity))

        ref.configure { it += CtxVelocity(2f) }
        assertEquals(2f, ref[CtxVelocity].x)

        ref.remove()
    }
}
