package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class EntityTest {

    @Test
    fun `of packs index and version correctly`() {
        val entity = Entity.of(index = 42, version = 7)
        assertEquals(42, entity.index)
        assertEquals(7, entity.version)
    }

    @Test
    fun `index zero and version zero`() {
        val entity = Entity.of(index = 0, version = 0)
        assertEquals(0, entity.index)
        assertEquals(0, entity.version)
    }

    @Test
    fun `max index value`() {
        val entity = Entity.of(index = Int.MAX_VALUE, version = 0)
        assertEquals(Int.MAX_VALUE, entity.index)
        assertEquals(0, entity.version)
    }

    @Test
    fun `max version value`() {
        val entity = Entity.of(index = 0, version = Int.MAX_VALUE)
        assertEquals(0, entity.index)
        assertEquals(Int.MAX_VALUE, entity.version)
    }

    @Test
    fun `same index different version are not equal`() {
        val v0 = Entity.of(index = 1, version = 0)
        val v1 = Entity.of(index = 1, version = 1)
        assertNotEquals(v0, v1)
    }

    @Test
    fun `same index and version are equal`() {
        val a = Entity.of(index = 5, version = 3)
        val b = Entity.of(index = 5, version = 3)
        assertEquals(a, b)
    }

    @Test
    fun `NONE has consistent index and version`() {
        // NONE must be distinguishable from any valid entity (index >= 0, version >= 0)
        val none = Entity.NONE
        assertEquals(Entity.NONE, none)
        assertNotEquals(Entity.of(index = 0, version = 0), none)
    }

    @Test
    fun `index and version do not bleed into each other`() {
        // Setting all bits in index must not affect version
        val entity = Entity.of(index = -1, version = 0)
        assertEquals(0, entity.version)
        // Setting all bits in version must not affect index
        val entity2 = Entity.of(index = 0, version = -1)
        assertEquals(0, entity2.index)
    }
}
