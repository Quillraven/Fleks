package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.IntBag
import com.github.quillraven.fleks.collection.bag

/**
 * An entity of a [world][World]. It represents a unique id.
 */
data class Entity(val id: Int)

@DslMarker
annotation class EntityCtxMarker

@EntityCtxMarker
class EntityHookContext(
    @PublishedApi
    internal val compService: ComponentService
) {
    inline operator fun <reified T : Component<*>> Entity.get(type: ComponentType<T>): T =
        compService.holder(type)[this]

    inline fun <reified T : Component<*>> Entity.getOrNull(type: ComponentType<T>): T? =
        compService.holder(type).getOrNull(this)

    inline operator fun <reified T : Component<*>> Entity.contains(type: ComponentType<T>): Boolean =
        compService.holder(type).contains(this)
}

/**
 * A DSL class to add components to a newly created [entity][Entity].
 */
@EntityCtxMarker
class EntityCreateContext(
    @PublishedApi
    internal val compService: ComponentService
) {
    @PublishedApi
    internal lateinit var compMask: BitArray

    inline operator fun <reified T : Component<*>> Entity.get(type: ComponentType<T>): T =
        compService.holder(type)[this]

    inline fun <reified T : Component<*>> Entity.getOrNull(type: ComponentType<T>): T? =
        compService.holder(type).getOrNull(this)

    inline operator fun <reified T : Component<*>> Entity.contains(type: ComponentType<T>): Boolean =
        compService.holder(type).contains(this)

    inline operator fun <reified T : Component<T>> Entity.plusAssign(component: T) {
        val compType: ComponentType<T> = component.type()
        compMask.set(compType.id)
        val holder: ComponentsHolder<T> = compService.holder(compType)
        holder[this] = component
    }
}

/**
 * A DSL class to update components of an already existing [entity][Entity].
 * It contains extension functions for [ComponentsHolder] which is how the component configuration of
 * existing entities is changed. This usually happens within [IteratingSystem] classes.
 */
@EntityCtxMarker
class EntityUpdateContext(
    @PublishedApi
    internal val compService: ComponentService
) {
    @PublishedApi
    internal lateinit var compMask: BitArray

    inline operator fun <reified T : Component<*>> Entity.get(type: ComponentType<T>): T =
        compService.holder(type)[this]

    inline fun <reified T : Component<*>> Entity.getOrNull(type: ComponentType<T>): T? =
        compService.holder(type).getOrNull(this)

    inline operator fun <reified T : Component<*>> Entity.contains(type: ComponentType<T>): Boolean =
        compService.holder(type).contains(this)

    inline operator fun <reified T : Component<T>> Entity.plusAssign(component: T) {
        val compType: ComponentType<T> = component.type()
        compMask.set(compType.id)
        val holder: ComponentsHolder<T> = compService.holder(compType)
        holder[this] = component
    }

    inline operator fun <reified T : Component<*>> Entity.minusAssign(componentType: ComponentType<T>) {
        compMask.clear(componentType.id)
        compService.holder(componentType) -= this
    }

    inline fun <reified T : Component<T>> Entity.addOrUpdate(
        componentType: ComponentType<T>,
        add: () -> T,
        update: (T) -> Unit,
    ) {
        compMask.set(componentType.id)
        val holder: ComponentsHolder<T> = compService.holder(componentType)
        holder.setOrUpdate(this, add, update)
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
    private val compService: ComponentService = world.componentService,
) {
    /**
     * The id that will be given to a newly created [entity][Entity] if there are no [recycledEntities].
     */
    @PublishedApi
    internal var nextId = 0

    /**
     * Separate BitArray to remember if an [entity][Entity] was already removed.
     * This is faster than looking up the [recycledEntities].
     */
    @PublishedApi
    internal val removedEntities = BitArray(initialEntityCapacity)

    /**
     * The already removed [entities][Entity] which can be reused whenever a new entity is needed.
     */
    @PublishedApi
    internal val recycledEntities = ArrayDeque<Entity>()

    /**
     * Returns the amount of active entities.
     */
    val numEntities: Int
        get() = nextId - recycledEntities.size

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
    internal val createCtx = EntityCreateContext(compService)

    @PublishedApi
    internal val updateCtx = EntityUpdateContext(compService)

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
    private val delayedEntities = IntBag()

    /**
     * Creates and returns a new [entity][Entity] and applies the given [configuration].
     * If there are [recycledEntities] then they will be preferred over creating new entities.
     * Notifies any registered [EntityListener].
     */
    inline fun create(configuration: EntityCreateContext.(Entity) -> Unit): Entity {
        val newEntity = if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            val recycled = recycledEntities.removeLast()
            removedEntities.clear(recycled.id)
            recycled
        }

        if (newEntity.id >= compMasks.size) {
            compMasks[newEntity.id] = BitArray(64)
        }
        val compMask = compMasks[newEntity.id]
        createCtx.run {
            this.compMask = compMask
            configuration(newEntity)
        }
        world.allFamilies.forEach { it.onEntityAdded(newEntity, compMask) }

        return newEntity
    }

    /**
     * Updates an [entity] with the given [configuration].
     * Notifies any registered [EntityListener].
     */
    inline fun configure(entity: Entity, configuration: EntityUpdateContext.(Entity) -> Unit) {
        val compMask = compMasks[entity.id]
        updateCtx.run {
            this.compMask = compMask
            configuration(entity)
        }
        world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Updates an [entity] with the given [components].
     * Notifies any registered [EntityListener].
     * This function is only used by [World.loadSnapshot].
     */
    internal fun configure(entity: Entity, components: List<Component<*>>) {
        val compMask = compMasks[entity.id]
        components.forEach { cmp ->
            val mapper = compService.wildcardHolder(cmp.type())
            mapper.setWildcard(entity, cmp)
            compMask.set(cmp.type().id)
        }
        world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Recycles the given [entity] by adding it to the [recycledEntities]
     * and also resetting its component mask with an empty [BitArray].
     * This function is only used by [World.loadSnapshot].
     */
    internal fun recycle(entity: Entity) {
        recycledEntities.add(entity)
        removedEntities.set(entity.id)
        compMasks[entity.id] = BitArray(64)
    }

    /**
     * Removes the given [entity] and adds it to the [recycledEntities] for future use.
     *
     * If [delayRemoval] is set then the [entity] is not removed immediately and instead will be cleaned up
     * within the [cleanupDelays] function.
     *
     * Notifies any registered [EntityListener] when the [entity] gets removed.
     */
    fun remove(entity: Entity) {
        if (removedEntities[entity.id]) {
            // entity is already removed
            return
        }

        if (delayRemoval) {
            delayedEntities.add(entity.id)
        } else {
            removedEntities.set(entity.id)
            val compMask = compMasks[entity.id]
            recycledEntities.add(entity)
            compMask.forEachSetBit { compId ->
                compService.holderByIndex(compId) -= entity
            }
            compMask.clearAll()
            world.allFamilies.forEach { it.onEntityRemoved(entity) }
        }
    }

    /**
     * Removes all [entities][Entity] and adds them to the [recycledEntities] for future use.
     * If [clearRecycled] is true then the recycled entities are cleared and the ids for newly
     * created entities start at 0 again.
     *
     * Refer to [remove] for more details.
     */
    fun removeAll(clearRecycled: Boolean = false) {
        for (id in 0 until nextId) {
            val entity = Entity(id)
            if (removedEntities[entity.id]) {
                continue
            }
            remove(entity)
        }

        if (clearRecycled) {
            nextId = 0
            recycledEntities.clear()
            removedEntities.clearAll()
            compMasks.clear()
        }
    }

    /**
     * Returns true if and only if the [entity] is not removed and is part of the [EntityService].
     */
    operator fun contains(entity: Entity): Boolean {
        return entity.id in 0 until nextId && !removedEntities[entity.id]
    }

    /**
     * Performs the given [action] on each active [entity][Entity].
     */
    fun forEach(action: (Entity) -> Unit) {
        for (id in 0 until nextId) {
            val entity = Entity(id)
            if (removedEntities[entity.id]) {
                continue
            }
            entity.run(action)
        }
    }

    /**
     * Clears the [delayRemoval] flag and removes [entities][Entity] which are part of the [delayedEntities].
     */
    fun cleanupDelays() {
        delayRemoval = false
        if (delayedEntities.isNotEmpty) {
            delayedEntities.forEach { remove(Entity(it)) }
            delayedEntities.clear()
        }
    }
}
