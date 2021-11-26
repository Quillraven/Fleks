package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.compareEntity
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class FamilyTest {
    private fun createCmpBitmask(cmpIdx: Int): BitArray? {
        return if (cmpIdx > 0) {
            BitArray().apply { set(cmpIdx) }
        } else {
            null
        }
    }


    @TestFactory
    fun `test contains`(): Collection<DynamicTest> {
        return listOf(
            arrayOf(
                "empty family contains entity without components",
                BitArray(), // entity component mask
                0, 0, 0,    // family allOf, noneOf, anyOf indices
                true        // expected
            ),
            arrayOf(
                "empty family contains entity with any components",
                BitArray().apply { set(1) }, // entity component mask
                0, 0, 0,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
            arrayOf(
                "family with allOf does not contain entity without components",
                BitArray(), // entity component mask
                1, 0, 0,    // family allOf, noneOf, anyOf indices
                false       // expected
            ),
            arrayOf(
                "family with allOf contains entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                1, 0, 0,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
            arrayOf(
                "family with noneOf contains entity without components",
                BitArray(), // entity component mask
                0, 1, 0,    // family allOf, noneOf, anyOf indices
                true        // expected
            ),
            arrayOf(
                "family with noneOf does not contain entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                0, 1, 0,                     // family allOf, noneOf, anyOf indices
                false                        // expected
            ),
            arrayOf(
                "family with anyOf does not contain entity without components",
                BitArray(), // entity component mask
                0, 0, 1,    // family allOf, noneOf, anyOf indices
                false       // expected
            ),
            arrayOf(
                "family with anyOf contains entity with specific component",
                BitArray().apply { set(1) }, // entity component mask
                0, 0, 1,                     // family allOf, noneOf, anyOf indices
                true                         // expected
            ),
        ).map {
            dynamicTest("test ${it[0]}") {
                val eCmpMask = it[1] as BitArray
                val fAllOf = createCmpBitmask(it[2] as Int)
                val fNoneOf = createCmpBitmask(it[3] as Int)
                val fAnyOf = createCmpBitmask(it[4] as Int)
                val family = Family(fAllOf, fNoneOf, fAnyOf)
                val expected = it[5] as Boolean

                assertEquals(expected, eCmpMask in family)
            }
        }
    }

    @Test
    fun `update active entities`() {
        val family = Family()

        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()

        assertAll(
            { assertFalse { family.isDirty } },
            { assertEquals(1, family.entitiesBag.size) },
            { assertEquals(0, family.entitiesBag[0]) }
        )
    }

    @Test
    fun `call action for each entity`() {
        val family = Family()
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()
        var processedEntity = -1
        var numExecutions = 0

        family.forEach {
            numExecutions++
            processedEntity = it.id
        }

        assertAll(
            { assertEquals(0, processedEntity) },
            { assertEquals(1, numExecutions) }
        )
    }

    @Test
    fun `sort entities`() {
        val family = Family()
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.onEntityCfgChanged(Entity(2), BitArray())
        family.onEntityCfgChanged(Entity(1), BitArray())
        family.updateActiveEntities()

        // sort descending by entity id
        family.sort(compareEntity { e1, e2 -> e2.id.compareTo(e1.id) })

        assertAll(
            { assertEquals(2, family.entitiesBag[0]) },
            { assertEquals(1, family.entitiesBag[1]) },
            { assertEquals(0, family.entitiesBag[2]) },
        )
    }

    @TestFactory
    fun `test onEntityCfgChange`(): Collection<DynamicTest> {
        return listOf(
            // first = add entity to family before calling onChange
            // second = make entity part of family
            Pair(false, false),
            Pair(false, true),
            Pair(true, false),
            Pair(true, true),
        ).map {
            dynamicTest("addEntityBefore=${it.first}, addEntity=${it.second}") {
                val family = Family(BitArray().apply { set(1) }, null, null)
                val addEntityBeforeCall = it.first
                val addEntityToFamily = it.second
                val entity = Entity(1)
                if (addEntityBeforeCall) {
                    family.onEntityCfgChanged(entity, BitArray().apply { set(1) })
                    family.updateActiveEntities()
                }

                if (addEntityToFamily) {
                    family.onEntityCfgChanged(entity, BitArray().apply { set(1) })

                    assertEquals(!addEntityBeforeCall, family.isDirty)
                } else {
                    family.onEntityCfgChanged(entity, BitArray())

                    assertEquals(addEntityBeforeCall, family.isDirty)
                }
            }
        }
    }
}
