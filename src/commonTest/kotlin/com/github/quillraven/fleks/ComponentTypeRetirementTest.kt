package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    var ticks = 0

    override fun onTickEntity(entity: Entity) {
        ticks++
    }
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
        assertTrue(retiredFamily in world.allFamilies, "the family is kept")
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
    fun keepFamilyOfSystemWhenRetiringItsType() {
        // a system's family can only be created inside the configuration scope
        lateinit var system: RetiredIteratingSystem
        val world = configureWorld { systems { add(RetiredIteratingSystem().also { system = it }) } }
        world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertEquals(0, system.family.numEntities, "the stripped entity left the family")
        assertTrue(system.family in world.allFamilies)
        assertTrue(RetiredComponent !in world.componentTypesInUse(), "a family only references the id of the type")
        world.update(1f)
        assertEquals(0, system.ticks)

        // the family still works when the type gets used again
        world.entity { it += RetiredComponent() }
        world.update(1f)
        assertEquals(1, system.ticks)
    }

    @Test
    fun keepFamilyThatExcludesRetiredType() {
        val world = configureWorld { }
        val family = world.family { all(SurvivingComponent).none(RetiredComponent) }
        world.entity { it += SurvivingComponent() }
        val entity = world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        assertEquals(1, family.numEntities)

        world.retireComponentTypes(RetiredComponent)

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse())
        assertEquals(2, family.numEntities)
        assertTrue(entity in family)
    }

    @Test
    fun typeOnlyReferencedByFamilyIsNotInUse() {
        val world = configureWorld { }
        world.family { all(SurvivingComponent).none(RetiredComponent) }
        world.entity { it += SurvivingComponent() }

        assertEquals(listOf<UniqueId<*>>(SurvivingComponent), world.componentTypesInUse(), "a family only references the id of a type")
    }

    @Test
    fun retireOneShotType() {
        val world = configureWorld { oneShotComponents(RetiredComponent, OneShotComponent) }
        world.entity { it += RetiredComponent() }

        world.retireComponentTypes(RetiredComponent)

        assertTrue(RetiredComponent !in world.componentTypesInUse())
        // remaining one-shot types are still removed
        val entity = world.entity { it += OneShotComponent() }
        world.update(1f)
        with(world) { assertFalse(entity has OneShotComponent) }
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
    fun callFamilyHooks() {
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

        assertEquals(1, addCalls, "the entity entered the family that excludes the retired type")
        assertEquals(1, removeCalls, "the entity left the family that includes the retired type")
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
        lateinit var excludingFamily: Family
        var addCalls = 0
        val world = configureWorld {
            retiringFamily = family { all(SurvivingComponent) }
            excludingFamily = family { all(SurvivingComponent).none(RetiredComponent) }
            families {
                onAdd(retiringFamily) { retireComponentTypes(RetiredComponent) }
                onAdd(excludingFamily) { addCalls++ }
            }
        }

        val entity = world.entity { it += SurvivingComponent(); it += RetiredComponent() }

        assertEquals(1, addCalls, "the add hook is called once although the entity entered the family during its creation")
        assertTrue(entity in excludingFamily)
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
