package com.github.quillraven.fleks

import com.github.quillraven.fleks.EntityRef.Companion.isNotValid
import com.github.quillraven.fleks.EntityRef.Companion.isValid
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class EntityRefTest {
    @Test
    fun `reference is valid if entity is not removed`() {
        val w = configureWorld { }
        val e = w.entity { }

        val ref = with(w) { e.getRef() }

        assertTrue { ref.valid }
    }

    @Test
    fun `references are reused for the same entity`() {
        val w = configureWorld { }
        val e = w.entity { }

        val ref1 = with(w) { e.getRef() }
        val ref2 = with(w) { e.getRef() }

        assertTrue { ref1 === ref2 }
    }

    @Test
    fun `reference is invalid if entity is removed`() {
        val w = configureWorld { }
        val e = w.entity { }
        val ref = with(w) { e.getRef() }

        w -= e

        assertFalse { ref.valid }
    }

    @Test
    fun `reference remains invalid if entity is recycled`() {
        val w = configureWorld { }
        val e1 = w.entity { }
        val ref1 = with(w) { e1.getRef() }

        w -= e1
        val e2 = w.entity()
        val ref2 = with(w) { e2.getRef() }

        assertTrue { e1 == e2 }
        assertFalse { ref1.valid }
        assertTrue { ref2.valid }
        assertFalse { ref1 == ref2 }
    }

    @Test
    fun `null or NONE references are not valid`() {
        var ref: EntityRef? = EntityRef.NONE
        assertFalse { ref.isValid() }
        assertTrue { ref.isNotValid() }

        ref = null
        assertFalse { ref.isValid() }
        assertTrue { ref.isNotValid() }
    }
}
