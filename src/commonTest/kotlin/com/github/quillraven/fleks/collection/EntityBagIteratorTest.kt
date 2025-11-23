package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.emptyEntity
import com.github.quillraven.fleks.entity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityBagIteratorTest {

    @Test
    fun `test iterator on empty bag`() {
        val emptyBag = emptyEntityBag()

        val iterator = emptyBag.iterator()

        assertFalse(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(emptyEntity(), iterator.next())
        assertEquals(emptyEntity(), iterator.previous())
        assertEquals(emptyEntity(), iterator.next(loop = true))
        assertEquals(emptyEntity(), iterator.previous(loop = true))
    }

    @Test
    fun `test iterator on bag with single entity`() {
        val entity = entity(1, 0u)
        val testBag = mutableEntityBagOf(entity)

        val iterator = testBag.iterator()

        assertTrue(iterator.hasNext())
        assertFalse(iterator.hasPrevious())
        assertEquals(entity, iterator.next())
        assertEquals(emptyEntity(), iterator.previous())
        assertEquals(emptyEntity(), iterator.next())

        // verify loop version
        iterator.reset()
        assertEquals(entity, iterator.next(loop = true))
        assertEquals(entity, iterator.next(loop = true))
        assertEquals(entity, iterator.previous(loop = true))
        assertEquals(entity, iterator.previous(loop = true))
    }


    @Test
    fun `test next and previous with three entities`() {
        val entity1 = entity(1, 0u)
        val entity2 = entity(2, 0u)
        val entity3 = entity(3, 0u)
        val testBag = mutableEntityBagOf(entity1, entity2, entity3)

        val iterator = testBag.iterator()

        // iterate to end and to beginning
        assertEquals(entity1, iterator.next())
        assertEquals(entity2, iterator.next())
        assertEquals(entity3, iterator.next())
        assertEquals(emptyEntity(), iterator.next())
        assertEquals(entity2, iterator.previous())
        assertEquals(entity1, iterator.previous())
        assertEquals(emptyEntity(), iterator.previous())

        // iterate back and forth
        iterator.reset()
        assertEquals(entity1, iterator.next())
        assertEquals(emptyEntity(), iterator.previous())
        assertEquals(entity2, iterator.next())
        assertEquals(entity1, iterator.previous())
        assertEquals(entity2, iterator.next())
        assertEquals(entity3, iterator.next())
        assertEquals(entity2, iterator.previous())

        // iterate to end and to beginning with loop
        iterator.reset()
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity2, iterator.next(loop = true))
        assertEquals(entity3, iterator.next(loop = true))
        assertEquals(entity1, iterator.next(loop = true))
        assertEquals(entity3, iterator.previous(loop = true))
        assertEquals(entity2, iterator.previous(loop = true))
        assertEquals(entity1, iterator.previous(loop = true))
        assertEquals(entity3, iterator.previous(loop = true))

        // start with previous call in loop version
        iterator.reset()
        assertEquals(entity3, iterator.previous(loop = true))
    }

    @Test
    fun `test goToFirst`() {
        val entity1 = entity(1, 0u)
        val entity2 = entity(2, 0u)
        val entity3 = entity(2, 1u) // same id on purpose to check that goTo stops at first entity
        val testBag = mutableEntityBagOf(entity1, entity2, entity3)

        val iterator = testBag.iterator()

        assertEquals(entity2, iterator.goToFirst { e -> e.id == 2 })
        assertEquals(entity1, iterator.previous())
        assertEquals(emptyEntity(), iterator.goToFirst { e -> e.id == 3 })
        assertEquals(entity1, iterator.next())
    }

    @Test
    fun `test goToLast`() {
        val entity1 = entity(1, 0u)
        val entity2 = entity(2, 0u)
        val entity3 = entity(2, 1u) // same id on purpose to check that goTo stops at first entity
        val testBag = mutableEntityBagOf(entity1, entity2, entity3)

        val iterator = testBag.iterator()

        assertEquals(entity3, iterator.goToLast { e -> e.id == 2 })
        assertEquals(entity2, iterator.previous())
        assertEquals(emptyEntity(), iterator.goToLast { e -> e.id == 3 })
        assertEquals(entity1, iterator.next())
    }

}
