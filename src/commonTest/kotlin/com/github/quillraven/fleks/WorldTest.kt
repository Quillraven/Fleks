package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family
import com.github.quillraven.fleks.World.Companion.inject
import com.github.quillraven.fleks.collection.compareEntity
import com.github.quillraven.fleks.collection.compareEntityBy
import kotlin.test.*

private data class WorldTestComponent(
    var x: Float = 0f,
) : Component<WorldTestComponent>,
    Comparable<WorldTestComponent> {
    override fun type(): ComponentType<WorldTestComponent> = WorldTestComponent

    var numAddCalls: Int = 0
    var numRemoveCalls: Int = 0

    override fun World.onAdd(entity: Entity) {
        assertTrue(entity has WorldTestComponent)
        numAddCalls++
    }

    override fun World.onRemove(entity: Entity) {
        assertFalse(entity has WorldTestComponent)
        numAddCalls--
    }

    companion object : ComponentType<WorldTestComponent>()

    override fun compareTo(other: WorldTestComponent) = 0
}

private class WorldTestComponent2 : Component<WorldTestComponent2> {
    override fun type(): ComponentType<WorldTestComponent2> = WorldTestComponent2

    companion object : ComponentType<WorldTestComponent2>()
}

private class WorldTestIntervalSystem : IntervalSystem() {
    var numCalls = 0
    var disposed = false

    override fun onTick() {
        ++numCalls
    }

    override fun onDispose() {
        disposed = true
    }
}

private class WorldTestIteratingSystem(
    val testInject: String = inject()
) : IteratingSystem(family { all(WorldTestComponent) }) {
    var numCalls = 0
    var numCallsEntity = 0

    override fun onTick() {
        ++numCalls
        super.onTick()
    }

    override fun onTickEntity(entity: Entity) {
        ++numCallsEntity
    }
}

private class WorldTestInitSystem : IteratingSystem(family { all(WorldTestComponent) }) {
    init {
        world.entity { it += WorldTestComponent() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestInitSystemExtraFamily : IteratingSystem(family { all(WorldTestComponent) }) {
    val extraFamily = world.family { any(WorldTestComponent2).none(WorldTestComponent) }

    init {
        world.entity { it += WorldTestComponent2() }
    }

    override fun onTickEntity(entity: Entity) = Unit
}

private class WorldTestNamedDependencySystem(
    val injName: String = inject("name")
) : IntervalSystem() {
    val level: String = world.inject("level")
    val name: String = injName

    override fun onTick() = Unit
}

private class WorldEntityProvider(
    override val world: World
) : EntityProvider {
    private var id = 10
    private var entities = mutableListOf<Entity>()

    override fun numEntities(): Int = entities.size

    override fun create(): Entity = Entity(id++, version = 0u).also { entities += it }

    override fun create(id: Int): Entity = Entity(id, version = 0u).also { entities += it }

    override fun minusAssign(entity: Entity) {
        entities -= entity
    }

    override fun contains(entity: Entity): Boolean = entity in entities

    override fun reset() {
        id = 10
        entities.clear()
    }

    override fun forEach(action: World.(Entity) -> Unit) {
        entities.forEach { world.action(it) }
    }
}


internal class WorldTest {
    @Test
    fun createEmptyWorldFor32Entities() {
        val w = configureWorld(32) { }

        assertEquals(0, w.numEntities)
        assertEquals(32, w.capacity)
    }

    @Test
    fun createEmptyWorldWith1NoArgsIntervalSystem() {
        val w = configureWorld {
            systems {
                add(WorldTestIntervalSystem())
            }
        }

        assertNotNull(w.system<WorldTestIntervalSystem>())
    }

    @Test
    fun createEmptyWorldWith1InjectableArgsIteratingSystem() {
        val w = configureWorld {
            injectables {
                add("42")
            }

            systems {
                add(WorldTestIteratingSystem())
            }
        }

        assertNotNull(w.system<WorldTestIteratingSystem>())
        assertEquals("42", w.system<WorldTestIteratingSystem>().testInject)
    }

    @Test
    fun createEmptyWorldWith2NamedInjectablesSystem() {
        val expectedName = "myName"
        val expectedLevel = "myLevel"
        val w = configureWorld {
            injectables {
                add("name", expectedName)
                add("level", "myLevel")
            }

            systems {
                add(WorldTestNamedDependencySystem())
            }
        }

        assertNotNull(w.system<WorldTestNamedDependencySystem>())
        assertEquals(expectedName, w.system<WorldTestNamedDependencySystem>().name)
        assertEquals(expectedLevel, w.system<WorldTestNamedDependencySystem>().level)
    }

    @Test
    fun cannotAddTheSameSystemTwice() {
        assertFailsWith<FleksSystemAlreadyAddedException> {
            configureWorld {
                systems {
                    add(WorldTestIntervalSystem())
                    add(WorldTestIntervalSystem())
                }
            }
        }
    }

    @Test
    fun cannotAccessSystemThatWasNotAdded() {
        val w = configureWorld {}

        assertFailsWith<FleksNoSuchSystemException> { w.system<WorldTestIntervalSystem>() }
    }

    @Test
    fun cannotCreateSystemWhenInjectablesAreMissing() {
        assertFailsWith<FleksNoSuchInjectableException> {
            configureWorld {
                systems {
                    add(WorldTestIteratingSystem())
                }
            }
        }
    }

    @Test
    fun cannotInjectTheSameTypeTwice() {
        assertFailsWith<FleksInjectableAlreadyAddedException> {
            configureWorld {
                injectables {
                    add("42")
                    add("42")
                }
            }
        }
    }

    @Test
    fun createNewEntity() {
        val w = configureWorld {
            injectables {
                add("42")
            }

            systems {
                add(WorldTestIteratingSystem())
            }
        }

        val e = w.entity {
            it += WorldTestComponent(x = 5f)
        }

        assertEquals(1, w.numEntities)
        assertEquals(0, e.id)
        assertEquals(5f, with(w) { e[WorldTestComponent].x })
    }

    @Test
    fun removeExistingEntity() {
        val w = configureWorld {}
        val e = w.entity()

        w -= e

        assertEquals(0, w.numEntities)
    }

    @Test
    fun doNotRemoveEntityWithWrongVersion() {
        val w = configureWorld {}
        val e = w.entity()

        w -= Entity(e.id, e.version - 1u)
        w -= Entity(e.id, e.version + 1u)

        assertEquals(1, w.numEntities)
    }

    @Test
    fun updateWorldWithDeltaTimeOf1() {
        val w = configureWorld {
            injectables {
                add("42")
            }

            systems {
                add(WorldTestIntervalSystem())
                add(WorldTestIteratingSystem())
            }
        }
        w.system<WorldTestIteratingSystem>().enabled = false

        w.update(1f)

        assertEquals(1f, w.deltaTime)
        assertEquals(1, w.system<WorldTestIntervalSystem>().numCalls)
        assertEquals(0, w.system<WorldTestIteratingSystem>().numCalls)
    }

    @Test
    fun removeAllEntities() {
        val w = configureWorld {}
        w.entity()
        w.entity()

        w.removeAll()

        assertEquals(0, w.numEntities)
    }

    @Test
    fun disposeWorld() {
        val w = configureWorld {
            systems {
                add(WorldTestIntervalSystem())
            }
        }
        w.entity()
        w.entity()

        w.dispose()

        assertTrue(w.system<WorldTestIntervalSystem>().disposed)
        assertEquals(0, w.numEntities)
    }

    @Test
    fun getUnusedInjectables() {
        val world = configureWorld {
            injectables {
                add("42", "myString")
                add(1337)
                add("used")
            }
        }
        val expected = mapOf("42" to "myString", "Int" to 1337)
        // this sets the 'used' string injectable to used=true
        world.inject<String>()

        val actual = world.unusedInjectables()

        assertEquals(expected, actual)
    }

    @Test
    fun iterateOverAllActiveEntities() {
        val w = configureWorld {}
        val e1 = w.entity()
        val e2 = w.entity()
        val e3 = w.entity()
        w -= e2
        val actualEntities = mutableListOf<Entity>()

        w.forEach {
            actualEntities.add(it)
        }

        assertContentEquals(listOf(e1, e3), actualEntities)
    }

    @Test
    fun createTwoWorldsWithDifferentDependencies() {
        val w1 = configureWorld {
            injectables {
                add("name", "name1")
                add("level", "level1")
            }

            systems {
                add(WorldTestNamedDependencySystem())
            }
        }
        val w2 = configureWorld {
            injectables {
                add("name", "name2")
                add("level", "level2")
            }

            systems {
                add(WorldTestNamedDependencySystem())
            }
        }
        val s1 = w1.system<WorldTestNamedDependencySystem>()
        val s2 = w2.system<WorldTestNamedDependencySystem>()

        assertEquals("name1", s1.injName)
        assertEquals("level1", s1.level)
        assertEquals("name2", s2.injName)
        assertEquals("level2", s2.level)
    }

    @Test
    fun configureEntityAfterCreation() {
        val w = configureWorld {
            injectables {
                add("test")
            }

            systems {
                add(WorldTestIteratingSystem())
            }
        }
        val e = w.entity()

        with(w) { e.configure { it += WorldTestComponent() } }
        w.update(0f)

        assertEquals(1, w.system<WorldTestIteratingSystem>().numCallsEntity)
    }

    @Test
    fun getFamilyAfterWorldCreation() {
        // WorldTestInitSystem creates an entity in its init block
        // -> family must be dirty and has a size of 1
        val w = configureWorld {
            systems {
                add(WorldTestInitSystem())
            }
        }

        val wFamily = w.family { all(WorldTestComponent) }

        assertEquals(1, wFamily.mutableEntities.size)
        assertEquals(1, wFamily.numEntities)
    }

    @Test
    fun getFamilyWithinSystemConstructor() {
        // WorldTestInitSystemExtraFamily creates an entity in its init block and
        // also a family with a different configuration that the system itself
        // -> system family is empty and extra family contains 1 entity
        val w = configureWorld {
            systems {
                add(WorldTestInitSystemExtraFamily())
            }
        }
        val s = w.system<WorldTestInitSystemExtraFamily>()

        assertEquals(1, s.extraFamily.numEntities)
        assertEquals(0, s.family.numEntities)
    }

    @Test
    fun iterateOverFamily() {
        val w = configureWorld { }
        val e1 = w.entity { it += WorldTestComponent() }
        val e2 = w.entity { it += WorldTestComponent() }
        val f = w.family { all(WorldTestComponent) }
        val actualEntities = mutableListOf<Entity>()

        f.forEach { actualEntities.add(it) }

        assertTrue(actualEntities.containsAll(arrayListOf(e1, e2)))
    }

    @Test
    fun sortedIterationOverFamily() {
        val world = configureWorld { }
        val entity1 = world.entity { it += WorldTestComponent(x = 15f) }
        val entity2 = world.entity { it += WorldTestComponent(x = 10f) }
        val family = world.family { all(WorldTestComponent) }
        val actualEntities = mutableListOf<Entity>()

        family.sort(compareEntity(world) { e1, e2 -> e1[WorldTestComponent].x.compareTo(e2[WorldTestComponent].x) })
        family.forEach { actualEntities.add(it) }

        assertEquals(arrayListOf(entity2, entity1), actualEntities)
    }

    @Test
    fun cannotCreateFamilyWithoutAnyConfiguration() {
        val w = configureWorld {}

        assertFailsWith<FleksFamilyException> { w.family { } }
        assertFailsWith<FleksFamilyException> { w.family { all().none().any() } }
    }

    @Test
    fun notifyComponentHooksDuringSystemCreation() {
        val w = configureWorld {
            systems {
                add(WorldTestInitSystem())
            }
        }

        val testComp = with(w) {
            val entity = w.family { all(WorldTestComponent) }.first()
            return@with entity[WorldTestComponent]
        }

        assertEquals(1, testComp.numAddCalls)
        assertEquals(0, testComp.numRemoveCalls)
    }

    @Test
    fun createWorldWithFamilyListener() {
        lateinit var testFamily: Family
        configureWorld {
            testFamily = family { all(WorldTestComponent) }
            families {
                onAdd(testFamily) { }
                onRemove(testFamily) { }
            }
        }

        assertNotNull(testFamily.addHook)
        assertNotNull(testFamily.removeHook)
    }

    @Test
    fun cannotAddSameFamilyListenerTwice() {
        assertFailsWith<FleksHookAlreadyAddedException> {
            configureWorld {
                val testFamily = family { all(WorldTestComponent) }
                families {
                    onAdd(testFamily) { }
                    onAdd(testFamily) { }
                }
            }
        }
    }

    @Test
    fun notifyFamilyListenerDuringSystemCreation() {
        var numAddCalls = 0
        var numRemoveCalls = 0
        lateinit var testFamily: Family

        val w = configureWorld {
            testFamily = family { all(WorldTestComponent) }
            families {
                onAdd(testFamily) { ++numAddCalls }
                onRemove(testFamily) { ++numRemoveCalls }
            }

            systems {
                add(WorldTestInitSystem())
            }
        }

        assertEquals(1, numAddCalls)
        assertEquals(0, numRemoveCalls)
        // verify that listener and system are not creating the same family twice
        assertEquals(1, w.allFamilies.size)
    }

    @Test
    fun testFamilyFirstAndEmptyFunctions() {
        val w = configureWorld { }

        val f = w.family { all(WorldTestComponent) }
        assertTrue(f.isEmpty)
        assertFalse(f.isNotEmpty)
        assertFailsWith<NoSuchElementException> { f.first() }
        assertNull(f.firstOrNull())

        val e = w.entity { it += WorldTestComponent() }
        assertFalse(f.isEmpty)
        assertTrue(f.isNotEmpty)
        assertEquals(e, f.first())
        assertEquals(e, f.firstOrNull())
    }

    @Test
    fun testFamilyFirstDuringIterationWithModifications() {
        val w = configureWorld {
        }
        val f = w.family { all(WorldTestComponent) }
        // create entity with id 0 that is not part of family because 0 is the default value for IntBag
        // and could potentially lead to a false verification in this test case
        w.entity { }
        val e1 = w.entity { it += WorldTestComponent() }
        val e2 = w.entity { it += WorldTestComponent() }
        val e3 = w.entity { it += WorldTestComponent() }
        val expectedEntities = listOf(e3, e2, e1)

        val actualEntities = mutableListOf<Entity>()
        f.forEach { entity ->
            if (actualEntities.isEmpty()) {
                // remove second entity on first iteration
                // this will not flag the family as 'dirty' because removal is delayed
                w -= e2
                // that's why we add an entity to flag the family
                w.entity { it += WorldTestComponent() }
            }
            // a call to 'first' updates the entities bag of a family internally
            // but should not mess up current iteration
            f.first()
            actualEntities.add(entity)
        }

        assertEquals(expectedEntities.size, actualEntities.size)
        assertEquals(expectedEntities.toSet(), actualEntities.toSet())
        assertEquals(3, f.numEntities)
    }

    @Test
    fun testEntityRemovalWithNoneOfFamily() {
        // entity that gets removed has no components and is therefore
        // part of any family that only has a noneOf configuration.
        // However, such entities still need to be removed of those families.
        val w = configureWorld { }
        val family = w.family { none(WorldTestComponent) }
        val e = w.entity { }

        assertTrue(e in family.mutableEntities)

        w -= e
        assertFalse(e in family.mutableEntities)
    }

    @Test
    fun testSnapshot() {
        val w = configureWorld { }
        val comp1 = WorldTestComponent()
        val e1 = w.entity { it += comp1 }
        val e2 = w.entity { }
        val comp31 = WorldTestComponent()
        val comp32 = WorldTestComponent2()
        val e3 = w.entity {
            it += comp31
            it += comp32
        }
        val expected = mapOf(
            e1 to listOf(comp1),
            e2 to emptyList(),
            e3 to listOf(comp31, comp32)
        )

        val actual = w.snapshot()

        assertEquals(expected.size, actual.size)
        expected.forEach { (entity, expectedComps) ->
            val actualComps = actual[entity]
            assertNotNull(actualComps)
            assertEquals(expectedComps.size, actualComps.size)
            assertTrue(expectedComps.containsAll(actualComps) && actualComps.containsAll(expectedComps))
        }
    }

    @Test
    fun testSnapshotOf() {
        val w = configureWorld { }
        val comp1 = WorldTestComponent()
        val e1 = w.entity { it += comp1 }
        val e2 = w.entity { }
        val expected1 = listOf<Any>(comp1)
        val expected2 = emptyList<Any>()

        assertEquals(expected1, w.snapshotOf(e1))
        assertEquals(expected2, w.snapshotOf(e2))
        assertEquals(expected2, w.snapshotOf(Entity(42, version = 0u)))
    }

    @Test
    fun testLoadEmptySnapshot() {
        val w = configureWorld { }
        // loading snapshot will remove any existing entity
        w.entity { }

        w.loadSnapshot(emptyMap())

        assertEquals(0, w.numEntities)
    }

    @Test
    fun testLoadSnapshotWhileFamilyIterationInProcess() {
        val w = configureWorld { }
        val f = w.family { all(WorldTestComponent) }
        w.entity { it += WorldTestComponent() }

        f.forEach {
            assertFailsWith<FleksSnapshotException> { w.loadSnapshot(emptyMap()) }
        }
    }

    @Test
    fun testLoadSnapshotWithOneEntity() {
        val w = configureWorld { }
        val entity = w.entity()
        val comps = listOf(WorldTestComponent())
        val snapshot = mapOf(entity to comps)

        w.loadSnapshot(snapshot)
        val actual = w.snapshotOf(entity)

        assertEquals(1, w.numEntities)
        assertEquals(comps, actual)
    }

    @Test
    fun testLoadSnapshotWithOneRecycledEntity() {
        val w = configureWorld { }
        val removedEntities = (1..3).map {
            w.entity().also { w -= it }
        }
        val entity = w.entity()
        val comps = listOf(WorldTestComponent())
        val snapshot = mapOf(entity to comps)

        w.loadSnapshot(snapshot)
        val actual = w.snapshotOf(entity)

        assertEquals(1, w.numEntities)
        removedEntities.forEach {
            assertFalse(it in w)
        }
        assertTrue(entity in w)
        assertEquals(comps, actual)
    }

    @Test
    fun testLoadSnapshotWithThreeEntities() {
        val w = configureWorld {
            injectables {
                add("42")
            }

            systems {
                add(WorldTestIteratingSystem())
            }
        }
        val comp1 = WorldTestComponent()
        val comp2 = WorldTestComponent()
        val snapshot = mapOf(
            Entity(3, version = 0u) to listOf(comp1, WorldTestComponent2()),
            Entity(5, version = 0u) to listOf(comp2),
            Entity(7, version = 0u) to listOf()
        )

        w.loadSnapshot(snapshot)
        val actual = w.snapshot()
        w.update(1f)

        // 3 entities should be loaded
        assertEquals(3, w.numEntities)
        // actual snapshot after loading the test snapshot is loaded should match
        assertEquals(snapshot.size, actual.size)
        snapshot.forEach { (entity, expectedComps) ->
            val actualComps = actual[entity]
            assertNotNull(actualComps)
            assertEquals(expectedComps.size, actualComps.size)
            assertTrue(expectedComps.containsAll(actualComps) && actualComps.containsAll(expectedComps))
        }
        // 2 out of 3 loaded entities should be part of the IteratingSystem family
        assertEquals(2, w.system<WorldTestIteratingSystem>().numCallsEntity)
        // 2 out of 3 loaded entities have components with lifecycle methods
        assertEquals(1, comp1.numAddCalls)
        assertEquals(0, comp1.numRemoveCalls)
        assertEquals(1, comp2.numAddCalls)
        assertEquals(0, comp2.numRemoveCalls)

        assertEquals(3, actual.size)
    }

    @Test
    fun testCreateEntityAfterSnapshotLoaded() {
        val w = configureWorld { }
        val snapshot = mapOf(
            Entity(1, version = 0u) to listOf<Component<*>>()
        )

        w.loadSnapshot(snapshot)

        // first created entity should be recycled Entity 0
        assertEquals(Entity(0, version = 0u), w.entity())
        // next created entity should be new Entity 2
        assertEquals(Entity(2, version = 0u), w.entity())
    }

    @Test
    fun testLoadSnapshotOfEmptyWorld() {
        val w = configureWorld { }
        val family = w.family { all(WorldTestComponent) }
        val entity = Entity(0, version = 0u)
        val components = listOf(WorldTestComponent())

        assertFalse { entity in family }
        w.loadSnapshotOf(entity, components)

        assertEquals(1, w.numEntities)
        assertTrue { with(w) { entity has WorldTestComponent } }
        assertTrue { entity in family }
    }

    @Test
    fun testLoadSnapshotOfEmptyWorldWithRecycling() {
        val w = configureWorld { }
        val family = w.family { all(WorldTestComponent) }
        val entity = Entity(1, version = 0u)
        val components = listOf(WorldTestComponent())

        assertFalse { entity in family }
        w.loadSnapshotOf(entity, components)

        assertEquals(1, w.numEntities)
        assertTrue { with(w) { entity has WorldTestComponent } }
        assertTrue { entity in family }
    }

    @Test
    fun testLoadSnapshotOfWithExistingEntity() {
        val w = configureWorld { }
        val family = w.family { any(WorldTestComponent) }
        val family2 = w.family { any(WorldTestComponent2) }
        val entity = w.entity {
            it += WorldTestComponent2()
        }
        val components = listOf(WorldTestComponent())

        assertTrue { entity in family2 }
        assertFalse { entity in family }
        w.loadSnapshotOf(entity, components)

        assertTrue { with(w) { entity has WorldTestComponent } }
        assertTrue { with(w) { entity hasNo WorldTestComponent2 } }
        assertFalse { entity in family2 }
        assertTrue { entity in family }
    }

    @Test
    fun testLoadSnapshotOfWhileFamilyIterationInProcess() {
        val w = configureWorld { }
        val f = w.family { all(WorldTestComponent) }
        val entity = Entity(0, version = 0u)
        val components = listOf(WorldTestComponent())
        w.entity { it += WorldTestComponent() }

        f.forEach {
            assertFailsWith<FleksSnapshotException> { w.loadSnapshotOf(entity, components) }
        }
    }

    data class FollowerComponent(val leader:Entity) : Component<FollowerComponent>{
        override fun type() = FollowerComponent
        companion object : ComponentType<FollowerComponent>()
    }

    @Test
    fun testLoadSnapshotWithReferenceToEntity(){
        val w = configureWorld {  }
        val leaderA = w.entity {  }
        val followerA = w.entity{
            it += FollowerComponent(leaderA)
        }
        w -= leaderA
        val leaderB = w.entity {  }
        val followerB = w.entity{
            it += FollowerComponent(leaderB)
        }

        val snapshot = w.snapshot()
        w.loadSnapshot(snapshot)

        with(w){
            assertFalse { leaderA in w }
            assertTrue { followerA in w }
            assertFalse { followerA[FollowerComponent].leader in w }

            assertTrue { leaderB in w }
            assertTrue { followerB in w }
            assertTrue { followerB[FollowerComponent].leader in w }
        }
    }

    @Test
    fun globalWorldFunctionsMustBeUsedWithinConfigurationScope() {
        // calls BEFORE configuration block
        assertFailsWith<FleksWrongConfigurationUsageException> {
            inject<String>()
        }
        assertFailsWith<FleksWrongConfigurationUsageException> {
            compareEntity { _, _ -> 0 }
            compareEntityBy(WorldTestComponent)
        }
        assertFailsWith<FleksWrongConfigurationUsageException> {
            family { all(WorldTestComponent) }
        }

        // calls AFTER configuration block
        assertFailsWith<FleksWrongConfigurationUsageException> {
            configureWorld { }
            inject<String>()
        }
        assertFailsWith<FleksWrongConfigurationUsageException> {
            configureWorld { }
            compareEntity { _, _ -> 0 }
            compareEntityBy(WorldTestComponent)
        }
        assertFailsWith<FleksWrongConfigurationUsageException> {
            configureWorld { }
            family { all(WorldTestComponent) }
        }
    }

    @Test
    fun testAsEntityBag() {
        val world = configureWorld { }
        val e1 = world.entity()
        val e2 = world.entity()

        var entities = world.asEntityBag()

        assertTrue(e1 in entities)
        assertTrue(e2 in entities)
        assertEquals(2, entities.size)

        with(world) { e1.remove() }
        entities = world.asEntityBag()

        assertFalse(e1 in entities)
        assertTrue(e2 in entities)
        assertEquals(1, entities.size)
    }

    @Test
    fun testEntityHook() {
        val dummyEntity = Entity.NONE
        var actualAddEntity: Entity
        var actualRemoveEntity: Entity
        lateinit var family: Family
        val world = configureWorld {
            family = family { all(WorldTestComponent) }

            onAddEntity { entity ->
                actualAddEntity = entity
                assertTrue { entity has WorldTestComponent }
                assertTrue { entity in family }
            }

            onRemoveEntity { entity ->
                actualRemoveEntity = entity
                assertTrue { entity has WorldTestComponent }
                assertTrue { entity in family }
            }
        }

        // test onAdd
        actualAddEntity = dummyEntity
        actualRemoveEntity = dummyEntity
        val entity = world.entity { it += WorldTestComponent() }
        assertEquals(entity, actualAddEntity)
        assertEquals(dummyEntity, actualRemoveEntity)

        // test onRemove
        actualAddEntity = dummyEntity
        actualRemoveEntity = dummyEntity
        world -= entity
        assertEquals(dummyEntity, actualAddEntity)
        assertEquals(entity, actualRemoveEntity)
        with(world) {
            assertFalse { entity has WorldTestComponent }
            assertFalse { entity in family }
        }
    }

    @Test
    fun cannotAddEntityHookTwice() {
        assertFailsWith<FleksHookAlreadyAddedException> {
            configureWorld {
                onAddEntity { }
                onAddEntity { }
            }
        }

        assertFailsWith<FleksHookAlreadyAddedException> {
            configureWorld {
                onRemoveEntity { }
                onRemoveEntity { }
            }
        }
    }

    @Test
    fun testCustomEntityProvider() {
        val world = configureWorld {
            systems {
                add(WorldTestInitSystem())
            }

            entityProvider { WorldEntityProvider(this) }
        }

        assertEquals(1, world.numEntities)
        assertEquals(10, world.asEntityBag().first().id)
    }
}
