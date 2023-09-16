package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.test.Test
import kotlin.test.assertContentEquals

internal class EntityComparatorTests {

    /** verify that [EntityComparator] extends [Comparator] and can be used to sort normal collections */
    @Test
    fun useEntityComparatorToSortRegularList() {

        // create entities, with IDs descending from 10 to 1
        val entities = List(10) { Entity(10 - it, version = 0) }

        val sorted = entities.sortedWith { a, b -> a.id.compareTo(b.id) }

        assertContentEquals(
            message = "Expect entities are sorted by ascending ID, from 1 to 10",
            expected = listOf(
                Entity(1, version = 0),
                Entity(2, version = 0),
                Entity(3, version = 0),
                Entity(4, version = 0),
                Entity(5, version = 0),
                Entity(6, version = 0),
                Entity(7, version = 0),
                Entity(8, version = 0),
                Entity(9, version = 0),
                Entity(10, version = 0),
            ),
            actual = sorted,
        )
    }
}
