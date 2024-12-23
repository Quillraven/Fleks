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
        val testBag = mutableEntityBagOf(entity)

        val iterator = testBag.iterator()

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
        val testBag = mutableEntityBagOf(entity1, entity2)

        val iterator = testBag.iterator()

        assertTrue(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(entity1, iterator.next())
        assertEquals(entity2, iterator.next())
        assertEquals(Entity.NONE, iterator.next())
        assertEquals(entity1, iterator.previous())
        assertEquals(Entity.NONE, iterator.previous())
        assertEquals(entity1, iterator.next())
        assertEquals(entity2, iterator.next())
        assertEquals(entity1, iterator.previous())

        // verify loop version
        iterator.reset()
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity2, iterator.next(loop = true))
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity2, iterator.next(loop = true))
        assertEquals(entity1, iterator.previous(loop = true))
        assertEquals(entity2, iterator.previous(loop = true))
        assertEquals(entity1, iterator.previous(loop = true))
        assertEquals(entity2, iterator.previous(loop = true))
    }

    @Test
    fun `test goToFirst`() {
        val entity1 = Entity(1, 0u)
        val entity2 = Entity(2, 0u)
        val entity3 = Entity(2, 1u) // same id on purpose to check that goTo stops at first entity
        val testBag = mutableEntityBagOf(entity1, entity2, entity3)

        val iterator = testBag.iterator()

        assertEquals(entity2, iterator.goToFirst { e -> e.id == 2 })
        assertEquals(entity1, iterator.previous())
        assertEquals(Entity.NONE, iterator.goToFirst { e -> e.id == 3 })
        assertEquals(entity1, iterator.next())
    }

    @Test
    fun `test goToLast`() {
        val entity1 = Entity(1, 0u)
        val entity2 = Entity(2, 0u)
        val entity3 = Entity(2, 1u) // same id on purpose to check that goTo stops at first entity
        val testBag = mutableEntityBagOf(entity1, entity2, entity3)

        val iterator = testBag.iterator()

        assertEquals(entity3, iterator.goToLast { e -> e.id == 2 })
        assertEquals(entity2, iterator.previous())
        assertEquals(Entity.NONE, iterator.goToLast { e -> e.id == 3 })
        assertEquals(entity1, iterator.next())
    }

}
