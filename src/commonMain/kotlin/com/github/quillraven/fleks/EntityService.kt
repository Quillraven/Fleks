package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.MutableEntityBag
import com.github.quillraven.fleks.collection.bag

/**
 * A service class that is responsible for creation and removal of [entities][Entity].
 * It also stores the component configuration of each entity as a [BitArray] to have quick access to
 * what kind of components an entity has or doesn't have.
 */
class EntityService(
    @PublishedApi
    internal val world: World,
    initialEntityCapacity: Int,
    @PublishedApi
    internal var entityProvider: EntityProvider = DefaultEntityProvider(world, initialEntityCapacity),
    private val compService: ComponentService = world.componentService,
) {

    /**
     * Returns the number of active entities.
     */
    val numEntities: Int
        get() = entityProvider.numEntities()

    /**
     * Returns the maximum capacity of active entities.
     */
    val capacity: Int
        get() = compMasks.capacity

    /**
     * The component configuration per [entity][Entity].
     */
    @PublishedApi
    internal val compMasks = bag<BitArray>(initialEntityCapacity)

    @PublishedApi
    internal val createCtx = EntityCreateContext(compService, compMasks)

    @PublishedApi
    internal var createId = -1

    @PublishedApi
    internal val updateCtx = EntityUpdateContext(compService, compMasks)

    @PublishedApi
    internal var updateId = -1

    /**
     * Flag that indicates if an iteration of an [IteratingSystem] is currently in progress.
     * In such cases entities will not be removed immediately.
     * Refer to [IteratingSystem.onTick] for more details.
     */
    @PublishedApi
    internal var delayRemoval = false

    /**
     * The entities that get removed at the end of an [IteratingSystem] iteration.
     */
    internal val delayedEntities = MutableEntityBag()

    /**
     * An optional [EntityHook] that gets called whenever an [entity][Entity] gets created and
     * after its [components][Component] are assigned and [families][Family] are updated.
     */
    @PublishedApi
    internal var addHook: EntityHook? = null

    /**
     * An optional [EntityHook] that gets called whenever an [entity][Entity] gets removed and
     * before its [components][Component] are removed and [families][Family] are updated.
     */
    @PublishedApi
    internal var removeHook: EntityHook? = null

    /**
     * Creates and returns a new [entity][Entity] and applies the given [configuration].
     * Notifies all [families][World.allFamilies].
     */
    inline fun create(configuration: EntityCreateContext.(Entity) -> Unit): Entity =
        postCreate(entityProvider.create(), configuration)

    /**
     * Creates and returns a new [entity][Entity] with the given [id] and applies the given [configuration].
     * Notifies all [families][World.allFamilies].
     */
    inline fun create(id: Int, configuration: EntityCreateContext.(Entity) -> Unit): Entity =
        postCreate(entityProvider.create(id), configuration)

    /**
     * Applies the given [configuration] to the [entity] and notifies all [families][World.allFamilies].
     * The [addHook] is invoked at the end, if provided.
     */
    @PublishedApi
    internal inline fun postCreate(
        entity: Entity,
        configuration: EntityCreateContext.(Entity) -> Unit
    ): Entity {
        // add components
        if (entity.id >= compMasks.size) {
            compMasks[entity.id] = BitArray(64)
        }

        val prevCreateId = createId
        createId = entity.id
        createCtx.configuration(entity)
        createId = prevCreateId

        // update families
        val compMask = compMasks[entity.id]
        world.allFamilies.forEach { it.onEntityAdded(entity, compMask) }

        // trigger optional add hook
        addHook?.invoke(world, entity)

        return entity
    }

    /**
     * Updates an [entity] with the given [configuration].
     * Notifies all [families][World.allFamilies].
     */
    inline fun configure(entity: Entity, configuration: EntityUpdateContext.(Entity) -> Unit) {
        val skipFamilyNotify = updateId == entity.id || createId == entity.id

        val prevUpdateId = updateId
        updateId = entity.id
        updateCtx.configuration(entity)
        updateId = prevUpdateId

        // notify families
        if (skipFamilyNotify) {
            return
        }
        val compMask = compMasks[entity.id]
        world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Updates an [entity] with the given [snapshot][Snapshot].
     * Notifies all [families][World.allFamilies].
     * This function is only used by [World.loadSnapshot] and [World.loadSnapshotOf],
     * and is therefore working with unsafe wildcards ('*').
     */
    internal fun configure(entity: Entity, snapshot: Snapshot) {
        val compMask = compMasks[entity.id]
        val components = snapshot.components

        // remove any existing components that are not part of the new components to set
        compMask.clearAndForEachSetBit { cmpId ->
            if (components.any { it.type().id == cmpId }) return@clearAndForEachSetBit

            // we can use holderByIndex because we can be sure that the holder already exists
            // because otherwise the entity would not even have the component
            compService.holderByIndexOrNull(cmpId)?.minusAssign(entity)
        }

        // set new components
        components.forEach { cmp ->
            compMask.set(cmp.type().id)
            val holder = compService.wildcardHolder(cmp.type())
            holder.setWildcard(entity, cmp)
        }

        // set new tags
        snapshot.tags.forEach {
            compMask.set(it.id)
            world.tagCache[it.id] = it
        }

        // notify families
        world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Recycles the given [entity] and resets its component mask with an empty [BitArray].
     * This function is only used by [World.loadSnapshot].
     */
    internal fun recycle(entity: Entity) {
        entityProvider -= entity
        compMasks[entity.id] = BitArray(64)
    }

    /**
     * Removes the given [entity]. If [delayRemoval] is set, then the [entity]
     * is not removed immediately and instead will be cleaned up within the [cleanupDelays] function.
     *
     * Notifies all [families][World.allFamilies] when the [entity] gets removed.
     */
    operator fun minusAssign(entity: Entity) {
        if (entity !in entityProvider) {
            // entity is already removed
            return
        }

        if (delayRemoval) {
            delayedEntities += entity
        } else {
            entityProvider -= entity
            val compMask = compMasks[entity.id]

            // trigger optional remove hook
            removeHook?.invoke(world, entity)

            // update families
            world.allFamilies.forEach { it.onEntityRemoved(entity) }

            // remove components
            compMask.clearAndForEachSetBit { compId ->
                compService.holderByIndexOrNull(compId)?.minusAssign(entity)
            }

        }
    }

    /**
     * Removes all [entities][Entity]. If [clearRecycled] is true, then the
     * recycled entities are cleared and the ids for newly created entities start at 0 again.
     *
     * Refer to [remove] for more details.
     */
    fun removeAll(clearRecycled: Boolean = false) {
        entityProvider.forEach { this -= it }

        if (clearRecycled) {
            entityProvider.reset()
            compMasks.clear()
        }
    }

    /**
     * Returns true if and only if the [entity] is not removed and is part of the [EntityService].
     */
    operator fun contains(entity: Entity): Boolean = entity in entityProvider

    /**
     * Performs the given [action] on each active [entity][Entity].
     */
    fun forEach(action: World.(Entity) -> Unit) {
        entityProvider.forEach(action)
    }

    /**
     * Clears the [delayRemoval] flag and removes [entities][Entity] which are part of the [delayedEntities].
     */
    fun cleanupDelays() {
        delayRemoval = false
        if (delayedEntities.isNotEmpty()) {
            delayedEntities.forEach { this -= it }
            delayedEntities.clear()
        }
    }
}
