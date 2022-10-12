package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.*

/**
 * An entity of a [world][World]. It represents a unique id.
 */
data class Entity(val id: Int)

/**
 * A class for basic [Entity] extension functions within an add/remove hook of a [Component], [Family],
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
     * Returns true if and only if the [entity][Entity] has a [component][Component] of the given [type].
     */
    inline operator fun <reified T : Component<*>> Entity.contains(type: ComponentType<T>): Boolean =
        this in componentService.holder(type)

    /**
     * Returns true if and only if the [entity][Entity] has a [component][Component] of the given [type].
     */
    inline infix fun <reified T : Component<*>> Entity.has(type: ComponentType<T>): Boolean =
        this in componentService.holder(type)

    /**
     * Returns true if and only if the [entity][Entity] doesn't have a [component][Component] of the given [type].
     */
    inline infix fun <reified T : Component<*>> Entity.hasNo(type: ComponentType<T>): Boolean =
        this !in componentService.holder(type)

    /**
     * Updates the [entity][Entity] using the given [configuration] to add and remove [components][Component].
     *
     * **Attention** Make sure that you only modify the entity of the current scope.
     * Otherwise you will get wrong behavior for families. E.g. don't do this:
     *
     *     entity.configure {
     *         // don't do this
     *         somOtherEntity += Position()
     *     }
     */
    inline fun Entity.configure(configuration: EntityUpdateContext.(Entity) -> Unit) =
        componentService.world.entityService.configure(this, configuration)

    /**
     * Removes the [entity][Entity] from the world. The [entity][Entity] will be recycled and reused for
     * future calls to [World.entity].
     */
    fun Entity.remove() = componentService.world.minusAssign(this)
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
     * If a component [addHook][ComponentsHolder.addHook] is defined then it
     * gets called after the [component] is assigned to the [entity][Entity].
     *
     * If a component [removeHook][ComponentsHolder.removeHook] is defined and the [entity][Entity]
     * already had such a [component] then it gets called with the previous assigned component before
     * the [addHook][ComponentsHolder.addHook] is called.
     */
    inline operator fun <reified T : Component<T>> Entity.plusAssign(component: T) {
        val compType: ComponentType<T> = component.type()
        compMasks[this.id].set(compType.id)
        val holder: ComponentsHolder<T> = componentService.holder(compType)
        holder[this] = component
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
     * If a component [removeHook][ComponentsHolder.removeHook] is defined then it gets called
     * if the [entity][Entity] has such a component.
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
    inline fun <reified T : Component<T>> Entity.getOrAdd(
        type: ComponentType<T>,
        add: () -> T,
    ): T {
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
    internal val createCtx = EntityCreateContext(compService, compMasks)

    @PublishedApi
    internal val updateCtx = EntityUpdateContext(compService, compMasks)

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
    private val delayedEntities = EntityBag()

    /**
     * Creates and returns a new [entity][Entity] and applies the given [configuration].
     * If there are [recycledEntities] then they will be preferred over creating new entities.
     * Notifies all [families][World.allFamilies].
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
        createCtx.configuration(newEntity)
        world.allFamilies.forEach { it.onEntityAdded(newEntity, compMask) }

        return newEntity
    }

    /**
     * Updates an [entity] with the given [configuration].
     * Notifies all [families][World.allFamilies].
     */
    inline fun configure(entity: Entity, configuration: EntityUpdateContext.(Entity) -> Unit) {
        val compMask = compMasks[entity.id]
        updateCtx.configuration(entity)
        world.allFamilies.forEach { it.onEntityCfgChanged(entity, compMask) }
    }

    /**
     * Updates an [entity] with the given [components].
     * Notifies all [families][World.allFamilies].
     * This function is only used by [World.loadSnapshot] and is therefore working
     * with unsafe wildcards ('*').
     */
    internal fun configure(entity: Entity, components: List<Component<*>>) {
        val compMask = compMasks[entity.id]
        components.forEach { cmp ->
            val holder = compService.wildcardHolder(cmp.type())
            holder.setWildcard(entity, cmp)
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
     * Notifies all [families][World.allFamilies] when the [entity] gets removed.
     */
    operator fun minusAssign(entity: Entity) {
        if (removedEntities[entity.id]) {
            // entity is already removed
            return
        }

        if (delayRemoval) {
            delayedEntities += entity
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
            this -= entity
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
    inline fun forEach(action: World.(Entity) -> Unit) {
        for (id in 0 until nextId) {
            val entity = Entity(id)
            if (removedEntities[entity.id]) {
                continue
            }
            world.action(entity)
        }
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
