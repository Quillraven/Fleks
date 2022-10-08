package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.compareEntity
import kotlin.test.*

private class FamilyTestComponent : Component<FamilyTestComponent> {
    override fun type() = FamilyTestComponent

    companion object : ComponentType<FamilyTestComponent>()
}

internal class FamilyTest {
    private val testWorld = world { }
    private val emptyTestFamily = Family(world = testWorld)

    private fun createCmpBitmask(cmpIdx: Int): BitArray? {
        return if (cmpIdx > 0) {
            BitArray().apply { set(cmpIdx) }
        } else {
            null
        }
    }


    @Test
    fun testContains() {
        val testCases = listOf(
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
        )

        testCases.forEach {
            val eCmpMask = it[1] as BitArray
            val fAllOf = createCmpBitmask(it[2] as Int)
            val fNoneOf = createCmpBitmask(it[3] as Int)
            val fAnyOf = createCmpBitmask(it[4] as Int)
            val family = Family(fAllOf, fNoneOf, fAnyOf, testWorld)
            val expected = it[5] as Boolean

            assertEquals(expected, eCmpMask in family)
        }
    }

    @Test
    fun updateActiveEntities() {
        val family = emptyTestFamily

        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()

        assertFalse { family.isDirty }
        assertEquals(1, family.entitiesBag.size)
        assertEquals(0, family.entitiesBag[0])
    }

    @Test
    fun callActionForEachEntity() {
        val family = emptyTestFamily
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.updateActiveEntities()
        var processedEntity = -1
        var numExecutions = 0

        family.forEach {
            numExecutions++
            processedEntity = it.id
        }

        assertEquals(0, processedEntity)
        assertEquals(1, numExecutions)
    }

    @Test
    fun sortEntities() {
        val family = emptyTestFamily
        family.onEntityCfgChanged(Entity(0), BitArray())
        family.onEntityCfgChanged(Entity(2), BitArray())
        family.onEntityCfgChanged(Entity(1), BitArray())
        family.updateActiveEntities()

        // sort descending by entity id
        family.sort(compareEntity(testWorld) { e1, e2 -> e2.id.compareTo(e1.id) })

        assertEquals(2, family.entitiesBag[0])
        assertEquals(1, family.entitiesBag[1])
        assertEquals(0, family.entitiesBag[2])
    }

    @Test
    fun testOnEntityCfgChange() {
        val testCases = listOf(
            // first = add entity to family before calling onChange
            // second = make entity part of family
            Pair(false, false),
            Pair(false, true),
            Pair(true, false),
            Pair(true, true),
        )

        testCases.forEach {
            val family = Family(BitArray().apply { set(1) }, null, null, testWorld)
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

    @Test
    fun testNestedIteration() {
        // delayRemoval and cleanup should only get called once for the first iteration
        val f1 = Family(world = testWorld)
        val f2 = Family(world = testWorld)
        testWorld.allFamilies += f1
        testWorld.allFamilies += f2
        val e1 = testWorld.entity { }
        testWorld.entity { }
        var remove = true

        var numOuterIterations = 0
        var numInnerIterations = 0
        f1.forEach {
            assertTrue { this.entityService.delayRemoval }

            f2.forEach {
                assertTrue { this.entityService.delayRemoval }
                if (remove) {
                    remove = false
                    entityService -= e1
                }
                ++numInnerIterations
            }
            ++numOuterIterations

            // check that inner iteration is not clearing the delayRemoval flag
            assertTrue { this.entityService.delayRemoval }
            // check that inner iteration is not cleaning up the delayed removals
            assertEquals(0, this.entityService.removedEntities.length())
        }

        assertFalse { f1.entityService.delayRemoval }
        assertEquals(1, f1.entityService.removedEntities.length())
        assertEquals(2, numOuterIterations)
        assertEquals(4, numInnerIterations)
    }

    @Test
    fun testFamilyHook() {
        val requiredComps = BitArray().apply { set(1) }
        val e = Entity(0)
        var numAddCalls = 0
        var numRemoveCalls = 0
        val family = Family(allOf = requiredComps, world = testWorld)

        val onAdd: FamilyHook = { entity ->
            assertEquals(this, testWorld)
            assertEquals(0, entity.id)
            numAddCalls++
        }

        val onRemove: FamilyHook = { entity ->
            assertEquals(this, testWorld)
            assertEquals(0, entity.id)
            numRemoveCalls++
        }

        family.addHook = onAdd
        family.removeHook = onRemove

        family.onEntityCfgChanged(e, requiredComps)
        assertEquals(1, numAddCalls)
        assertEquals(0, numRemoveCalls)

        family.onEntityCfgChanged(e, BitArray())
        assertEquals(1, numAddCalls)
        assertEquals(1, numRemoveCalls)
    }

    @Test
    fun configureEntityViaFamily() {
        val family = Family(world = testWorld)
        testWorld.allFamilies += family
        val entity = testWorld.entity { }

        family.forEach { e -> e.configure { it += FamilyTestComponent() } }

        assertNotNull(testWorld.componentService.holder(FamilyTestComponent)[entity])
    }

    @Test
    fun testFamilyDefinition() {
        val testDefinition = FamilyDefinition()
        assertNull(testDefinition.allOf)
        assertNull(testDefinition.noneOf)
        assertNull(testDefinition.anyOf)

        testDefinition.all(FamilyTestComponent)
        assertEquals(1, testDefinition.allOf?.numBits())
        assertEquals(true, testDefinition.allOf?.get(FamilyTestComponent.id))
        assertEquals(0, testDefinition.noneOf?.numBits() ?: 0)
        assertEquals(0, testDefinition.anyOf?.numBits() ?: 0)

        testDefinition.none(FamilyTestComponent)
        assertEquals(1, testDefinition.allOf?.numBits())
        assertEquals(1, testDefinition.noneOf?.numBits())
        assertEquals(true, testDefinition.noneOf?.get(FamilyTestComponent.id))
        assertEquals(0, testDefinition.anyOf?.numBits() ?: 0)

        testDefinition.any(FamilyTestComponent)
        assertEquals(1, testDefinition.allOf?.numBits())
        assertEquals(1, testDefinition.noneOf?.numBits())
        assertEquals(1, testDefinition.anyOf?.numBits())
        assertEquals(true, testDefinition.anyOf?.get(FamilyTestComponent.id))
    }

    @Test
    fun testFamilyContainsEntity() {
        val family = testWorld.family { all(FamilyTestComponent) }
        val e1 = testWorld.entity { it += FamilyTestComponent() }
        val e2 = testWorld.entity()

        assertTrue(e1 in family)
        assertFalse(e2 in family)
    }
}

