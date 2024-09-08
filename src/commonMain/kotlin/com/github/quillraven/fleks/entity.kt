package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.*
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmName

/**
 * An entity of a [world][World]. It represents a unique identifier that is the combination
 * of an index (=[id]) and a [version].
 *
 * It is possible to have two entities with the same [id] but different [version] but only
 * one of these entities is part of the [world][World] at any given time.
 */
@Serializable
data class Entity(val id: Int, val version: UInt) {
    companion object {
        val NONE = Entity(-1, 0u)
    }
}

/**
 * Type alias for an optional hook function for an [EntityService].
 * Such a function runs within a [World] and takes the [Entity] as an argument.
 */
typealias EntityHook = World.(Entity) -> Unit

/**
 * A class for basic [Entity] extension functions within a [Family],
 * [IntervalSystem], [World] or [compareEntity].
 */
abstract class EntityComponentContext(
    @PublishedApi
    internal val componentService: ComponentService
) {
    /**
     * Returns a [component][Component] of the given [type] for the [entity][Entity].
     *
     * @throws [FleksNoSuchEntityComponentException] if the [entity][Entity] does not have such a component.
     */
    inline operator fun <reified T : Component<*>> Entity.get(type: ComponentType<T>): T =
        componentService.holder(type)[this]

    /**
     * Returns a [component][Component] of the given [type] for the [entity][Entity]
     * or null if the [entity][Entity] does not have such a [component][Component].
     */
    inline fun <reified T : Component<*>> Entity.getOrNull(type: ComponentType<T>): T? =
        componentService.holder(type).getOrNull(this)

    /**
     * Returns true if and only if the [entity][Entity] has a [component][Component] or [tag][EntityTag] of the given [type].
     */
    operator fun Entity.contains(type: UniqueId<*>): Boolean =
        componentService.world.entityService.compMasks.getOrNull(this.id)?.get(type.id) ?: false

    /**
     * Returns true if and only if the [entity][Entity] has a [component][Component] or [tag][EntityTag] of the given [type].
     */
    infix fun Entity.has(type: UniqueId<*>): Boolean =
        componentService.world.entityService.compMasks.getOrNull(this.id)?.get(type.id) ?: false

    /**
     * Returns true if and only if the [entity][Entity] doesn't have a [component][Component] or [tag][EntityTag] of the given [type].
     */
    infix fun Entity.hasNo(type: UniqueId<*>): Boolean =
        componentService.world.entityService.compMasks.getOrNull(this.id)?.get(type.id)?.not() ?: true

    /**
     * Updates the [entity][Entity] using the given [configuration] to add and remove [components][Component].
     *
     * **Attention** Make sure that you only modify the entity of the current scope.
     * Otherwise, you will get wrong behavior for families. E.g. don't do this:
     *
     * ```
     * entity.configure {
     *     // modifying the current entity is allowed ✅
     *     it += Position()
     *     // don't modify other entities ❌
     *     someOtherEntity += Position()
     * }
     * ```
     */
    inline fun Entity.configure(configuration: EntityUpdateContext.(Entity) -> Unit) =
        componentService.world.entityService.configure(this, configuration)

    /**
     * Removes the [entity][Entity] from the world. The [entity][Entity] will be recycled and reused for
     * future calls to [World.entity].
     */
    fun Entity.remove() = componentService.world.minusAssign(this)

    /**
     * Returns true, if and only if an [entity][Entity] will be removed at the end of the current [IteratingSystem].
     * This is the case, if it gets [removed][remove] during the system's iteration.
     */
    fun Entity.isMarkedForRemoval() = this in componentService.world.entityService.delayedEntities
}

/**
 * A class that extends the extension functionality of an [EntityComponentContext] by also providing
 * the possibility to create [components][Component].
 */
open class EntityCreateContext(
    compService: ComponentService,
    @PublishedApi
    internal val compMasks: Bag<BitArray>,
) : EntityComponentContext(compService) {

    /**
     * Adds the [component] to the [entity][Entity].
     *
     * The [onAdd][Component.onAdd] lifecycle method
     * gets called after the [component] is assigned to the [entity][Entity].
     *
     * If the [entity][Entity] already had such a [component] then the [onRemove][Component.onRemove]
     * lifecycle method gets called on the previously assigned component before the [onAdd][Component.onAdd]
     * lifecycle method is called on the new component.
     */
    inline operator fun <reified T : Component<T>> Entity.plusAssign(component: T) {
        val compType: ComponentType<T> = component.type()
        compMasks[this.id].set(compType.id)
        val holder: ComponentsHolder<T> = componentService.holder(compType)
        holder[this] = component
    }

    /**
     * Adds the [components] to the [entity][Entity]. This function should only be used
     * in exceptional cases.
     * It is preferred to use the [plusAssign] function whenever possible to have type-safety.
     *
     * The [onAdd][Component.onAdd] lifecycle method
     * gets called after each component is assigned to the [entity][Entity].
     *
     * If the [entity][Entity] already has such a component then the [onRemove][Component.onRemove]
     * lifecycle method gets called on the previously assigned component before the [onAdd][Component.onAdd]
     * lifecycle method is called on the new component.
     */
    operator fun Entity.plusAssign(components: List<Component<*>>) {
        components.forEach { cmp ->
            val compType = cmp.type()
            compMasks[this.id].set(compType.id)
            val holder = componentService.wildcardHolder(compType)
            holder.setWildcard(this, cmp)
        }
    }

    /**
     * Sets the [tag][EntityTag] to the [entity][Entity].
     */
    operator fun Entity.plusAssign(tag: EntityTags) {
        compMasks[this.id].set(tag.id)
        // We need to remember used tags in order to correctly return and load them using
        // the snapshot functionality, because tags are not managed via ComponentHolder and
        // the entity's component mask just knows about the tag's id.
        // However, a snapshot should contain the real object instances related to an entity.
        componentService.world.tagCache[tag.id] = tag
    }

    /**
     * Sets all [tags][EntityTag] on the given [entity][Entity].
     */
    @JvmName("plusAssignTags")
    operator fun Entity.plusAssign(tags: List<EntityTags>) {
        tags.forEach { this += it }
    }

}

/**
 * A class that extends the extension functionality of an [EntityCreateContext] by also providing
 * the possibility to update [components][Component].
 */
class EntityUpdateContext(
    compService: ComponentService,
    compMasks: Bag<BitArray>,
) : EntityCreateContext(compService, compMasks) {

    /**
     * Removes a [component][Component] of the given [type] from the [entity][Entity].
     *
     * Calls the [onRemove][Component.onRemove] lifecycle method on the component being removed.
     *
     * @throws [IndexOutOfBoundsException] if the id of the [entity][Entity] exceeds the internal components' capacity.
     * This can only happen when the [entity][Entity] never had such a component.
     */
    inline operator fun <reified T : Component<*>> Entity.minusAssign(type: ComponentType<T>) {
        compMasks[this.id].clear(type.id)
        componentService.holder(type) -= this
    }

    /**
     * Returns a [component][Component] of the given [type] for the [entity][Entity].
     *
     * If the [entity][Entity] does not have such a [component][Component] then [add] is called
     * to assign it to the [entity][Entity] and return it.
     */
    inline fun <reified T : Component<T>> Entity.getOrAdd(type: ComponentType<T>, add: () -> T): T {
        val holder: ComponentsHolder<T> = componentService.holder(type)
        val existingCmp = holder.getOrNull(this)
        if (existingCmp != null) {
            return existingCmp
        }

        compMasks[this.id].set(type.id)
        val newCmp = add()
        holder[this] = newCmp
        return newCmp
    }

    /**
     * Removes the [tag][EntityTag] from the [entity][Entity].
     */
    operator fun Entity.minusAssign(tag: UniqueId<*>) = compMasks[this.id].clear(tag.id)
}

/**
 * Interface to describe the functionality needed by an [EntityService]
 * to create and remove [entities][Entity]. The provider can be specified
 * via the [WorldConfiguration]. Per default a [DefaultEntityProvider] will be used.
 */
interface EntityProvider {

    /**
     * Reference to the [World] of the [EntityProvider].
     * It is needed for the [forEach] implementation.
     */
    val world: World

    /**
     * Returns the total amount of active [entities][Entity].
     */
    fun numEntities(): Int

    /**
     * Creates a new [entity][Entity].
     */
    fun create(): Entity

    /**
     * Creates a new [Entity] with a specific [id].
     * Internally, this is only needed by [World.loadSnapshotOf] and
     * if that is not used, then this function can be omitted.
     */
    fun create(id: Int): Entity

    /**
     * Removes an [entity][Entity].
     */
    operator fun minusAssign(entity: Entity)

    /**
     * Returns true if and only if the given [entity] is active and part of the provider.
     */
    operator fun contains(entity: Entity): Boolean

    /**
     * Resets the provider by removing all [entities][Entity].
     */
    fun reset()

    /**
     * Performs the given [action] for all active [entities][Entity].
     */
    fun forEach(action: World.(Entity) -> Unit)
}

/**
 * Default implementation of an [EntityProvider] which uses an [entity][Entity]
 * recycling mechanism to reuse [entities][Entity] that get removed.
 * The first [entity][Entity] starts with [id][Entity.id] zero and [version][Entity.version] zero.
 */
class DefaultEntityProvider(
    override val world: World,
    initialEntityCapacity: Int
) : EntityProvider {

    /**
     * The id that will be given to a newly created [entity][Entity] if there are no [recycledEntities].
     */
    private var nextId = 0

    /**
     * The already removed [entities][Entity] which can be reused whenever a new entity is needed.
     */
    private val recycledEntities = ArrayDeque<Entity>()

    /**
     * Returns the total amount of active [entities][Entity].
     */
    override fun numEntities(): Int = nextId - recycledEntities.size

    /**
     * Bag of all currently active [entities][Entity].
     */
    private val activeEntities = bag<Entity>(initialEntityCapacity)

    /**
     * Creates a new [entity][Entity]. If there are [recycledEntities] then they will be preferred
     * over creating new entities.
     */
    override fun create(): Entity {
        return if (recycledEntities.isEmpty()) {
            Entity(nextId++, version = 0u)
        } else {
            val recycled = recycledEntities.removeLast()

            // because of the load snapshot functionality of the world, it is
            // possible that an entity with an ID higher than nextId gets recycled
            // and immediately created afterward. In such a case we need to correct the
            // nextId to avoid creating duplicated entities.
            if (recycled.id >= nextId) {
                nextId = recycled.id + 1
            }

            recycled.copy(version = recycled.version + 1u)
        }.also {
            activeEntities[it.id] = it
        }
    }

    /**
     * Creates a new [Entity] with a specific [id].
     */
    override fun create(id: Int): Entity {
        if (id >= nextId) {
            // entity with given id was never created before -> create all missing entities ...
            repeat(id - nextId + 1) {
                this -= Entity(nextId + it, version = 0u)
            }
            // ... and then create the entity to guarantee that it has the correct ID.
            // The entity is at the end of the recycled list.

            // adjust ID for future entities to be created
            nextId = id + 1
            return create()
        } else {
            // entity with given id was already created before and is part of the recycled entities
            // -> move it to the end to be used by the next create call
            val index = recycledEntities.indexOfFirst { it.id == id }
            val entity = recycledEntities.removeAt(index)
            recycledEntities.addLast(entity)
            return create()
        }
    }

    /**
     * Removes an [entity][Entity].
     */
    override operator fun minusAssign(entity: Entity) {
        recycledEntities.add(entity)
        activeEntities.removeAt(entity.id)
    }

    /**
     * Returns true if and only if the given [entity] is active and part of the provider.
     */
    override fun contains(entity: Entity): Boolean = activeEntities.getOrNull(entity.id)?.version == entity.version

    /**
     * Resets the provider by removing and recycling all [entities][Entity].
     * Also, resets the [nextId] to zero.
     */
    override fun reset() {
        nextId = 0
        recycledEntities.clear()
        activeEntities.clear()
    }

    /**
     * Performs the given [action] for all active [entities][Entity].
     */
    override fun forEach(action: World.(Entity) -> Unit) {
        activeEntities.forEach { world.action(it) }
    }
}

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
     * Returns the amount of active entities.
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
    internal val delayedEntities: MutableEntityBag = ArrayEntityBag()

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
     * Removes the given [entity]. If [delayRemoval] is set then the [entity]
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
     * Removes all [entities][Entity]. If [clearRecycled] is true then the
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
