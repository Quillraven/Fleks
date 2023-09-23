package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.compareEntity
import kotlin.test.*

private class FamilyTestComponent : Component<FamilyTestComponent> {
    override fun type() = FamilyTestComponent

    companion object : ComponentType<FamilyTestComponent>()
}

private class FamilyTestComponent2 : Component<FamilyTestComponent2> {
    override fun type() = FamilyTestComponent2

    companion object : ComponentType<FamilyTestComponent2>()
}

internal class FamilyTest {
    private val testWorld = configureWorld { }
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

        val entity = testWorld.entity()
        family.onEntityCfgChanged(entity, BitArray())

        // accessing the entities will trigger an internal update
        assertEquals(1, family.mutableEntities.size)
        assertEquals(entity, family.mutableEntities[0])
    }

    @Test
    fun callActionForEachEntity() {
        val family = emptyTestFamily
        family.onEntityCfgChanged(testWorld.entity(), BitArray())
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
    fun callActionForEachEntityWithVersion() {
        val family = emptyTestFamily
        fun createRecycledEntity(count: Int): Entity {
            repeat(count) {
                testWorld -= testWorld.entity()
            }
            return testWorld.entity()
        }
        repeat(3) {
            family.onEntityCfgChanged(createRecycledEntity(it), BitArray())
        }

        val entities = mutableListOf<Entity>()
        family.forEach {
            entities.add(it)
        }

        assertEquals(entities.toSet(), family.entities.map { it }.toSet())
        assertEquals(3, entities.size)
        assertContains(entities, Entity(id = 0, version = 0))
        assertContains(entities, Entity(id = 1, version = 1))
        assertContains(entities, Entity(id = 2, version = 2))
    }

    @Test
    fun sortEntities() {
        val family = emptyTestFamily
        val e0 = testWorld.entity()
        val e1 = testWorld.entity()
        val e2 = testWorld.entity()
        family.onEntityCfgChanged(e0, BitArray())
        family.onEntityCfgChanged(e2, BitArray())
        family.onEntityCfgChanged(e1, BitArray())

        // sort descending by entity id
        family.sort(compareEntity(testWorld) { eA, eB -> eB.id.compareTo(eA.id) })

        assertEquals(Entity(2, version = 0), family.mutableEntities[0])
        assertEquals(Entity(1, version = 0), family.mutableEntities[1])
        assertEquals(Entity(0, version = 0), family.mutableEntities[2])
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
            val entity = testWorld.entity()
            if (addEntityBeforeCall) {
                family.onEntityCfgChanged(entity, BitArray().apply { set(1) })
                // accessing entities triggers an internal family update
                family.mutableEntities
            }

            if (addEntityToFamily) {
                family.onEntityCfgChanged(entity, BitArray().apply { set(1) })

                assertEquals(1, family.mutableEntities.size)
            } else {
                family.onEntityCfgChanged(entity, BitArray())

                assertEquals(0, family.mutableEntities.size)
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
            assertTrue(this.entityService.delayRemoval)
            // check that inner iteration is not cleaning up the delayed removals
            assertTrue(e1 in this.entityService)
        }

        assertFalse(f1.entityService.delayRemoval)
        assertFalse(e1 in f1.entityService)
        assertEquals(2, numOuterIterations)
        assertEquals(4, numInnerIterations)
    }

    @Test
    fun testFamilyHook() {
        val requiredComps = BitArray().apply { set(1) }
        val e = Entity(0, version = 0)
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

    @Test
    fun testNestedFamilyUpdate() {
        val f1 = testWorld.family { all(FamilyTestComponent) }
        val f2 = testWorld.family { all(FamilyTestComponent2) }
        testWorld.entity {
            it += FamilyTestComponent()
            it += FamilyTestComponent2()
        }

        // initially the entity is in both families
        // verify via 'entities' bag to also test the behavior outside of usual system access
        assertTrue(f1.entities.isNotEmpty())
        assertTrue(f2.entities.isNotEmpty())

        f1.forEach {
            // configuration change to remove the entity from both families
            it.configure { e ->
                e -= FamilyTestComponent
                e -= FamilyTestComponent2
            }
            // f2 should get updated immediately because there is no iteration of this family happening.
            // f1 should NOT get updated because an iteration is currently in progress.
            f2.forEach { }
            assertTrue(f1.entities.isNotEmpty())
            assertTrue(f2.entities.isEmpty())
        }

        // f1 should get updated when 'entities' is accessed outside an iteration
        assertTrue(f1.entities.isEmpty())
        assertTrue(f2.entities.isEmpty())
    }

    @Test
    fun testFamilyForIteration() {
        val f = testWorld.family { all(FamilyTestComponent) }
        val e1 = testWorld.entity { it += FamilyTestComponent() }
        val e2 = testWorld.entity { it += FamilyTestComponent() }

        var numIterations = 0
        for (i in 0 until f.numEntities) {
            ++numIterations
            for (j in i + 1 until f.numEntities) {
                ++numIterations
                val forE1 = f.entities[i]
                val forE2 = f.entities[j]
                assertTrue(forE1 == e1 || forE1 == e2)
                assertTrue(forE2 == e1 || forE2 == e2)
            }
        }

        assertEquals(3, numIterations)
    }

    @Test
    fun numEntities() {
        val f = testWorld.family { all(FamilyTestComponent) }

        val entities = (1..5).map {
            testWorld.entity { it += FamilyTestComponent() }
        }
        entities.drop(1).take(3).forEach {
            testWorld -= it
        }
        testWorld.entity { it += FamilyTestComponent() }

        // 0 + 5 - 3 + 1 = 3
        assertEquals(3, f.numEntities)
    }

    @Test
    fun isEmptyForEmptyFamily() {
        val f = testWorld.family { all(FamilyTestComponent) }

        repeat(3) {
            testWorld -= testWorld.entity { it += FamilyTestComponent() }
        }

        assertTrue(f.isEmpty)
        assertFalse(f.isNotEmpty)
    }

    @Test
    fun isEmptyForNonEmptyFamily() {
        val f = testWorld.family { all(FamilyTestComponent) }
        testWorld.entity { it += FamilyTestComponent() }

        assertFalse(f.isEmpty)
        assertTrue(f.isNotEmpty)
    }
}

