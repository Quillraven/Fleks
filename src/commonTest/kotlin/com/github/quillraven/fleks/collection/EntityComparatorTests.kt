package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.entity
import kotlin.test.Test
import kotlin.test.assertContentEquals

internal class EntityComparatorTests {

    /** verify that [EntityComparator] extends [Comparator] and can be used to sort normal collections */
    @Test
    fun useEntityComparatorToSortRegularList() {

        // create entities, with IDs descending from 10 to 1
        val entities = List(10) { entity(10 - it, version = 0u) }

        val sorted = entities.sortedWith { a, b -> a.id.compareTo(b.id) }

        assertContentEquals(
            message = "Expect entities are sorted by ascending ID, from 1 to 10",
            expected = listOf(
                entity(1, version = 0u),
                entity(2, version = 0u),
                entity(3, version = 0u),
                entity(4, version = 0u),
                entity(5, version = 0u),
                entity(6, version = 0u),
                entity(7, version = 0u),
                entity(8, version = 0u),
                entity(9, version = 0u),
                entity(10, version = 0u),
            ),
            actual = sorted,
        )
    }
}
