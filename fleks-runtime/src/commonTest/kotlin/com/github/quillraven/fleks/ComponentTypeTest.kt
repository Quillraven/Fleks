package com.github.quillraven.fleks

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// Minimal hand-written components that simulate what the compiler plugin will generate.
// (No @Component annotation here — the plugin isn't wired yet.)

private class TypeA : Component {
    companion object : ComponentType<TypeA>()
}

private class TypeB : Component {
    companion object : ComponentType<TypeB>()
}

private class TypeC : Component {
    companion object : ComponentType<TypeC>()
}

class ComponentTypeTest {

    @Test
    fun `each ComponentType gets a unique id`() {
        assertNotEquals(TypeA.id, TypeB.id)
        assertNotEquals(TypeB.id, TypeC.id)
        assertNotEquals(TypeA.id, TypeC.id)
    }

    @Test
    fun `id is stable - same companion returns same id on repeated access`() {
        assertEquals(TypeA.id, TypeA.id)
    }

    @Test
    fun `ids are non-negative`() {
        assertTrue(TypeA.id >= 0)
        assertTrue(TypeB.id >= 0)
        assertTrue(TypeC.id >= 0)
    }

    @Test
    fun `ids are dense - fit as array indices up to count`() {
        // All ids must be strictly less than ComponentType.count, guaranteeing
        // they can serve as indices into an array of that size.
        val count = ComponentType.count
        assertTrue(TypeA.id < count)
        assertTrue(TypeB.id < count)
        assertTrue(TypeC.id < count)
    }
}
