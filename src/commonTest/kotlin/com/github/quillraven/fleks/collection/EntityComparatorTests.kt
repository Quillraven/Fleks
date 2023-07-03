package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity
import kotlin.test.Test
import kotlin.test.assertContentEquals

internal class EntityComparatorTests {

    /** verify that [EntityComparator] extends [Comparator] and can be used to sort normal collections */
    @Test
    fun useEntityComparatorToSortRegularList() {

        // create entities, with IDs descending from 10 to 1
        val entities = List(10) { Entity(10 - it) }

        val sorted = entities.sortedWith(EntityComparator { a, b -> a.id.compareTo(b.id) })

        assertContentEquals(
            message = "Expect entities are sorted by ascending ID, from 1 to 10",
            expected = listOf(
                Entity(1),
                Entity(2),
                Entity(3),
                Entity(4),
                Entity(5),
                Entity(6),
                Entity(7),
                Entity(8),
                Entity(9),
                Entity(10),
            ),
            actual = sorted,
        )
    }
}
