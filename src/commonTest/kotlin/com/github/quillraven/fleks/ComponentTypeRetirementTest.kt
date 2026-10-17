package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

private data class RetiredComponent(var removed: Int = 0) : Component<RetiredComponent> {
    override fun type(): ComponentType<RetiredComponent> = RetiredComponent

    override fun World.onRemove(entity: Entity) {
        removed++
    }

    companion object : ComponentType<RetiredComponent>()
}

private data class SurvivingComponent(val value: Int = 0) : Component<SurvivingComponent> {
    override fun type(): ComponentType<SurvivingComponent> = SurvivingComponent

    companion object : ComponentType<SurvivingComponent>()
}

private data object RetiredTag : EntityTag()

private class SurvivingSystem : IteratingSystem(family { all(SurvivingComponent) }) {
    override fun onTickEntity(entity: Entity) = Unit
}

private data class OneShotComponent(val value: Int = 0) : Component<OneShotComponent> {
    override fun type(): ComponentType<OneShotComponent> = OneShotComponent

    companion object : ComponentType<OneShotComponent>()
}

private class EntityRemovingComponent : Component<EntityRemovingComponent> {
    override fun type(): ComponentType<EntityRemovingComponent> = EntityRemovingComponent

    override fun World.onRemove(entity: Entity) {
        entity.remove()
    }

    companion object : ComponentType<EntityRemovingComponent>()
}

private class RetiredIteratingSystem : IteratingSystem(family { all(RetiredComponent) }) {
    override fun onTickEntity(entity: Entity) = Unit
}

private class ExcludingSystem : IteratingSystem(family { all(SurvivingComponent).none(RetiredComponent) }) {
    override fun onTickEntity(entity: Entity) = Unit
}

internal class ComponentTypeRetirementTest {
    @Test
    fun retireComponentAndTag() {
        val world = configureWorld { systems { add(SurvivingSystem()) } }
        val retiredComponent = RetiredComponent()
        val both = world.entity { it += retiredComponent; it += SurvivingComponent(1); it += RetiredTag }
        val survivingOnly = world.entity { it += SurvivingComponent(2) }
        val retiredFamily = world.family { all(RetiredComponent) }
        val survivingFamily = world.family { all(SurvivingComponent) }

        assertEquals(1, retiredFamily.numEntities)
        assertTrue(world.componentTypesInUse().containsAll(listOf(RetiredComponent, SurvivingComponent, RetiredTag)))

        world.retireComponentTypes(RetiredComponent, RetiredTag)

        assertEquals(1, retiredComponent.removed, "the component's onRemove lifecycle ran")
        with(world) {
            assertFalse(both has RetiredComponent)
            assertFalse(both has RetiredTag)
            assertTrue(both has SurvivingComponent)
            assertTrue(survivingOnly has SurvivingComponent)
        }
        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
        assertEquals(0, retiredFamily.numEntities, "the stripped entity left the family")
        assertFalse(retiredFamily in world.allFamilies, "the family is reclaimed")
        assertEquals(2, survivingFamily.numEntities)

        // snapshot must not contain the retired component and tag anymore
        val snapshot = world.snapshot()
        assertEquals(listOf(SurvivingComponent(1)), snapshot.getValue(both).components)
        assertTrue(snapshot.getValue(both).tags.isEmpty())
    }

    @Test
    fun useRetiredTypeAgain() {
        val world = configureWorld { }
        val entity = world.entity { it += RetiredComponent() }
        val idBefore = RetiredComponent.id

        world.retireComponentTypes(RetiredComponent)
        world.entity { it += RetiredComponent() }
        with(world) { entity.configure { it += RetiredComponent() } }

        assertEquals(idBefore, RetiredComponent.id, "the id is never reused, so it is never reissued either")
        assertEquals(listOf<UniqueId<*>>(RetiredComponent), world.componentTypesInUse())
        with(world) { assertTrue(entity has RetiredComponent) }
    }

    @Test
    fun keepFamilyOfRegisteredSystemUntilALaterSweep() {
        // a system's family can only be created inside the configuration scope
        lateinit var system: RetiredIteratingSystem
        val world = configureWorld { systems { add(RetiredIteratingSystem().also { system = it }) } }
        val entity = world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertTrue(system in world.systems)
        assertTrue(system.family in world.allFamilies, "the family of a registered system is kept")
        assertTrue(system.family.isEmpty, "the stripped entity left the kept family")
        with(world) { assertFalse(entity has RetiredComponent) }
        assertTrue(RetiredComponent in world.componentTypesInUse(), "the types of the kept family stay listed")

        // a later sweep removes the family once its system is removed
        world -= system
        world.retireComponentTypes(world.componentTypesInUse().filter { it === RetiredComponent })

        assertFalse(system.family in world.allFamilies)
        assertTrue(RetiredComponent !in world.componentTypesInUse())
    }

    @Test
    fun retireTypeAfterRemovingItsSystem() {
        lateinit var system: RetiredIteratingSystem
        val world = configureWorld { systems { add(RetiredIteratingSystem().also { system = it }) } }
        world.entity { it += RetiredComponent() }

        world -= system
        world.retireComponentTypes(RetiredComponent)

        assertFalse(system.family in world.allFamilies)
        assertTrue(system.family.isEmpty)
        assertTrue(RetiredComponent !in world.componentTypesInUse())
    }

    @Test
    fun createFamilyOfRetiredTypeAgain() {
        val world = configureWorld { }
        val family = world.family { all(RetiredComponent) }
        world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)
        val entity = world.entity { it += RetiredComponent() }
        val newFamily = world.family { all(RetiredComponent) }

        assertNotSame(family, newFamily)
        assertTrue(entity in newFamily)
    }

    @Test
    fun removeFamilyThatExcludesRetiredType() {
        val world = configureWorld { }
        val family = world.family { all(SurvivingComponent).none(RetiredComponent) }
        world.entity { it += SurvivingComponent() }
        world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
        assertFalse(family in world.allFamilies)
        assertTrue(family.isEmpty)
    }

    @Test
    fun removeFamilyWithRetiredTypeInAny() {
        val world = configureWorld { }
        val family = world.family { any(RetiredComponent, SurvivingComponent) }
        world.entity { it += SurvivingComponent() }

        assertTrue(RetiredComponent in world.componentTypesInUse(), "a type that only a family uses is listed")
        world.retireComponentTypes(RetiredComponent)

        assertFalse(family in world.allFamilies)
        assertTrue(RetiredComponent !in world.componentTypesInUse(), "a retired type is no longer listed")
    }

    @Test
    fun typeOnlyReferencedByFamilyIsInUse() {
        val world = configureWorld { }
        world.family { all(SurvivingComponent).none(RetiredComponent) }
        world.entity { it += SurvivingComponent() }

        assertEquals(setOf<UniqueId<*>>(SurvivingComponent, RetiredComponent), world.componentTypesInUse().toSet())
    }

    @Test
    fun retireTypeOnlyReferencedByFamily() {
        val world = configureWorld { }
        val family = world.family { all(RetiredComponent) }

        world.retireComponentTypes(world.componentTypesInUse())

        assertFalse(family in world.allFamilies)
        assertTrue(world.componentTypesInUse().isEmpty())
    }

    @Test
    fun familyCountStaysStableWhenReloadedTypesAreRetired() {
        val world = configureWorld { }
        world.entity { it += SurvivingComponent() }
        val numFamilies = world.allFamilies.size

        repeat(3) {
            // a reloaded plugin defines a new type object with a new id, which no entity ever gets
            val pluginType = object : ComponentType<SurvivingComponent>() {}
            world.family { all(pluginType) }
            world.family { all(SurvivingComponent, pluginType) }
            world.family { all(SurvivingComponent).none(pluginType) }
            world.family { any(SurvivingComponent, pluginType) }

            // like an engine's reclaimer: retire the listed types that belong to the unloaded plugin
            world.retireComponentTypes(world.componentTypesInUse().filter { it === pluginType })

            assertEquals(numFamilies, world.allFamilies.size)
        }
    }

    @Test
    fun componentTypesInUseAreDistinctAndSortedById() {
        val world = configureWorld { }
        world.entity { it += RetiredTag; it += SurvivingComponent(); it += RetiredComponent() }
        world.family { all(SurvivingComponent).none(RetiredComponent, RetiredTag) }

        val inUse = world.componentTypesInUse()

        assertEquals(3, inUse.size)
        assertEquals(inUse.sortedBy { it.id }, inUse)
    }

    @Test
    fun repeatedDefinitionPartReplacesItsTypes() {
        val world = configureWorld { }

        world.family { all(RetiredComponent).all(SurvivingComponent) }

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
    }

    @Test
    fun reclaimFamilyWhoseAnyTypesAreAllRetired() {
        val world = configureWorld { }
        val anyFamily = world.family { any(RetiredComponent, RetiredTag) }
        val allAndAnyFamily = world.family { all(SurvivingComponent).any(RetiredComponent) }
        world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent, RetiredTag)

        assertFalse(anyFamily in world.allFamilies)
        assertFalse(allAndAnyFamily in world.allFamilies)
        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
    }

    @Test
    fun keepExcludingFamilyOfRegisteredSystem() {
        lateinit var system: ExcludingSystem
        val world = configureWorld { systems { add(ExcludingSystem().also { system = it }) } }
        val entity = world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertTrue(system.family in world.allFamilies)
        assertTrue(entity in system.family, "the kept family keeps working")
        assertTrue(RetiredComponent in world.componentTypesInUse())
    }

    @Test
    fun retireOneShotType() {
        val world = configureWorld { oneShotComponents(RetiredComponent, OneShotComponent) }
        world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertTrue(RetiredComponent in world.componentTypesInUse(), "the one-shot family is kept with its types")
        // remaining one-shot types are still removed
        val entity = world.entity { it += OneShotComponent() }
        world.update(1f)
        with(world) { assertFalse(entity has OneShotComponent) }
    }

    @Test
    fun cannotReAddSystemWhoseFamilyWasReclaimed() {
        lateinit var system: RetiredIteratingSystem
        val world = configureWorld { systems { add(RetiredIteratingSystem().also { system = it }) } }
        world -= system
        world.retireComponentTypes(RetiredComponent)

        assertFailsWith<FleksSystemFamilyReclaimedException> { world += system }
        assertFalse(system in world.systems)
    }

    @Test
    fun keepOneShotFamilyWhenAllItsTypesAreRetired() {
        val world = configureWorld { oneShotComponents(RetiredComponent) }
        world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertTrue(RetiredComponent in world.componentTypesInUse(), "the one-shot family is kept with its types")
        // the one-shot type is still removed when it gets used again
        val entity = world.entity { it += RetiredComponent() }
        world.update(1f)
        with(world) { assertFalse(entity has RetiredComponent) }
    }

    @Test
    fun ignoreTypeNotInUse() {
        val world = configureWorld { }
        world.entity { it += SurvivingComponent() }
        world.family { all(SurvivingComponent) }

        world.retireComponentTypes(RetiredComponent, RetiredTag)

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
    }

    @Test
    fun noHooksOfRemovedFamilies() {
        lateinit var excludingFamily: Family
        lateinit var includingFamily: Family
        var addCalls = 0
        var removeCalls = 0
        val world = configureWorld {
            excludingFamily = family { all(SurvivingComponent).none(RetiredComponent) }
            includingFamily = family { all(RetiredComponent) }
            families {
                onAdd(excludingFamily) { addCalls++ }
                onRemove(includingFamily) { removeCalls++ }
            }
        }
        world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertEquals(0, addCalls, "the entity does not enter a removed family")
        assertEquals(0, removeCalls, "the entity does not leave a removed family")
        assertFalse(excludingFamily in world.allFamilies)
        assertFalse(includingFamily in world.allFamilies)
        assertNull(includingFamily.removeHook, "the hooks of a removed family are dropped")
    }

    @Test
    fun cannotRetireDuringFamilyIteration() {
        val world = configureWorld { }
        world.entity { it += RetiredComponent() }
        val family = world.family { all(RetiredComponent) }

        family.forEach {
            assertFailsWith<FleksRetireComponentTypesException> { world.retireComponentTypes(RetiredComponent) }
        }
        assertEquals(1, family.numEntities)
    }

    @Test
    fun cannotRetireDuringEntityCreationOrConfiguration() {
        val world = configureWorld { }

        val entity = world.entity {
            assertFailsWith<FleksRetireComponentTypesException> { world.retireComponentTypes(RetiredComponent) }
        }
        with(world) {
            entity.configure {
                assertFailsWith<FleksRetireComponentTypesException> { world.retireComponentTypes(RetiredComponent) }
            }
        }
    }

    @Test
    fun retireAfterFailedCreationConfigurationAndIteration() {
        val world = configureWorld { }
        val entity = world.entity { it += RetiredComponent() }
        val family = world.family { all(RetiredComponent) }

        assertFailsWith<IllegalStateException> { world.entity { error("creation failed") } }
        assertFailsWith<IllegalStateException> { with(world) { entity.configure { error("configuration failed") } } }
        assertFailsWith<IllegalStateException> { family.forEach { error("iteration failed") } }
        world.retireComponentTypes(RetiredComponent)

        with(world) { assertFalse(entity has RetiredComponent) }
    }

    @Test
    fun retireFromFamilyHookDuringCreation() {
        lateinit var retiringFamily: Family
        lateinit var countingFamily: Family
        var addCalls = 0
        val world = configureWorld {
            retiringFamily = family { all(SurvivingComponent) }
            countingFamily = family { any(SurvivingComponent) }
            families {
                onAdd(retiringFamily) { retireComponentTypes(RetiredComponent) }
                onAdd(countingFamily) { addCalls++ }
            }
        }

        val entity = world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        assertEquals(1, addCalls, "the add hook is called once although the entity entered the family during its creation")
        assertTrue(entity in countingFamily)
        with(world) { assertFalse(entity has RetiredComponent) }
    }

    @Test
    fun retireTypeWhoseOnRemoveRemovesTheEntity() {
        val world = configureWorld { }
        val family = world.family { none(SurvivingComponent) }
        val entity = world.entity { it += EntityRemovingComponent() }

        world.retireComponentTypes(EntityRemovingComponent)

        assertFalse(entity in world)
        assertEquals(0, family.numEntities, "the removed entity is not added to a family again")
    }

    @Test
    fun retireNothing() {
        val world = configureWorld { }
        world.entity { it += SurvivingComponent() }

        world.retireComponentTypes()

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
    }
}
