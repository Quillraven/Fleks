package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityBagIteratorTest {

    @Test
    fun `test iterator on empty bag`() {
        val emptyBag = MutableEntityBag()

        val iterator = emptyBag.iterator()

        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(Entity.NONE, iterator.next())
        assertEquals(Entity.NONE, iterator.previous())
        assertEquals(Entity.NONE, iterator.next(loop = true))
        assertEquals(Entity.NONE, iterator.previous(loop = true))
    }

    @Test
    fun `test iterator on bag with single entity`() {
        val entity = Entity(1, 0u)
        val emptyBag = mutableEntityBagOf(entity)

        val iterator = emptyBag.iterator()

        assertTrue(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(entity, iterator.next())
        assertEquals(entity, iterator.previous())
        assertEquals(Entity.NONE, iterator.previous())
        assertEquals(entity, iterator.next())
        assertEquals(Entity.NONE, iterator.next())

        // verify loop version
        iterator.reset()
        assertEquals(entity, iterator.next(loop = true))
        assertEquals(entity, iterator.next(loop = true))
        assertEquals(entity, iterator.previous(loop = true))
        assertEquals(entity, iterator.previous(loop = true))
    }

    @Test
    fun `test iterator on bag with two entities`() {
        val entity1 = Entity(1, 0u)
        val entity2 = Entity(2, 0u)
        val emptyBag = mutableEntityBagOf(entity1, entity2)

        val iterator = emptyBag.iterator()

        assertTrue(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(entity1, iterator.next())
        assertEquals(entity2, iterator.next())
        assertEquals(Entity.NONE, iterator.next())
        assertEquals(entity2, iterator.previous())
        assertEquals(entity1, iterator.previous())
        assertEquals(Entity.NONE, iterator.previous())

        // verify loop version
        iterator.reset()
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity2, iterator.next(loop = true))
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity2, iterator.previous(loop = true))
        assertEquals(entity1, iterator.previous(loop = true))
        assertEquals(entity2, iterator.previous(loop = true))
    }

}
