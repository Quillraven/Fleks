package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.IntBag
import com.github.quillraven.fleks.collection.bag

/**
 * An entity of a [world][World]. It represents a unique id.
 */
@JvmInline
value class Entity(val id: Int)

/**
 * Interface of an [entity][Entity] listener that gets notified when the component configuration changes.
 * The [onEntityCfgChanged] function gets also called when an [entity][Entity] gets created and removed.
 */
interface EntityListener {
    /**
     * Function that gets called when an [entity's][Entity] component configuration changes.
     * This happens when a component gets added or removed or the [entity] gets added or removed from the [world][World].
     *
     * @param entity the [entity][Entity] with the updated component configuration.
     *
     * @param cmpMask the [BitArray] representing what type of components the entity has. Each component type has a
     * unique id. Refer to [ComponentMapper] for more details.
     */
    fun onEntityCfgChanged(entity: Entity, cmpMask: BitArray) = Unit
}

@DslMarker
annotation class EntityCfgMarker

/**
 * A DSL class to add components to a newly created [entity][Entity].
 */
@EntityCfgMarker
class EntityCreateCfg(
    @PublishedApi
    internal val cmpService: ComponentService
) {
    @PublishedApi
    internal var entity = Entity(0)

    @PublishedApi
    internal lateinit var cmpMask: BitArray

    /**
     * Adds and returns a component of the given type to the [entity] and
     * applies the [configuration] to the component.
     */
    inline fun <reified T : Any> add(configuration: T.() -> Unit = {}): T {
        val mapper = cmpService.mapper<T>()
        cmpMask.set(mapper.id)
        return mapper.add(entity, configuration)
    }
}

/**
 * A DSL class to update components of an already existing [entity][Entity].
 * It contains extension functions for [ComponentMapper] which is how the component configuration of
 * existing entities is changed. This usually happens within [IteratingSystem] classes.
 */
@EntityCfgMarker
class EntityUpdateCfg {
    @PublishedApi
    internal lateinit var cmpMask: BitArray

    /**
     * Adds and returns a component of the given type to the [entity] and applies the [configuration] to that component.
     * If the [entity] already has a component of the given type then no new component is created and instead
     * the existing one will be updated.
     */
    inline fun <reified T : Any> ComponentMapper<T>.add(entity: Entity, configuration: T.() -> Unit = {}): T {
        cmpMask.set(this.id)
        return this.add(entity, configuration)
    }

    /**
     * Removes a component of the given type from the [entity].
     */
    inline fun <reified T : Any> ComponentMapper<T>.remove(entity: Entity) {
        cmpMask.clear(this.id)
        this.remove(entity)
    }
}

/**
 * A service class that is responsible for creation and removal of [entities][Entity].
 * It also stores the component configuration of each entity as a [BitArray] to have quick access to
 * what kind of components an entity has or doesn't have.
 */
class EntityService(
    initialEntityCapacity: Int,
    private val cmpService: ComponentService
) {
    /**
     * The id that will be given to a newly created [entity][Entity] if there are no [recycledEntities].
     */
    @PublishedApi
    internal var nextId = 0

    /**
     * The already removed [entities][Entity] which can be reused whenever a new entity is needed.
     */
    @PublishedApi
    internal val recycledEntities = ArrayDeque<Entity>()

    /**
     * The component configuration per [entity][Entity].
     */
    @PublishedApi
    internal val cmpMasks = bag<BitArray>(initialEntityCapacity)

    @PublishedApi
    internal val createCfg = EntityCreateCfg(cmpService)

    @PublishedApi
    internal val updateCfg = EntityUpdateCfg()

    @PublishedApi
    internal val listeners = bag<EntityListener>()

    /**
     * Flag that indicates if an iteration of an [IteratingSystem] is currently in progress.
     * In such cases entities will not be removed immediately.
     * Refer to [IteratingSystem.onTick] for more details.
     */
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
    inline fun create(configuration: EntityCreateCfg.() -> Unit): Entity {
        val newEntity = if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            recycledEntities.removeLast()
        }

        if (newEntity.id >= cmpMasks.size) {
            cmpMasks[newEntity.id] = BitArray(64)
        }
        val cmpMask = cmpMasks[newEntity.id]
        createCfg.run {
            this.entity = newEntity
            this.cmpMask = cmpMask
            configuration()
        }
        listeners.forEach { it.onEntityCfgChanged(newEntity, cmpMask) }

        return newEntity
    }

    /**
     * Updates an [entity] with the given [configuration].
     * Notifies any registered [EntityListener].
     */
    inline fun configureEntity(entity: Entity, configuration: EntityUpdateCfg.(Entity) -> Unit) {
        val cmpMask = cmpMasks[entity.id]
        updateCfg.run {
            this.cmpMask = cmpMask
            configuration(entity)
        }
        listeners.forEach { it.onEntityCfgChanged(entity, cmpMask) }
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
        if (delayRemoval) {
            delayedEntities.add(entity.id)
        } else {
            val cmpMask = cmpMasks[entity.id]
            recycledEntities.add(entity)
            cmpMask.forEachSetBit { cmpId ->
                cmpService.mapper(cmpId).remove(entity)
            }
            cmpMask.clear()
            listeners.forEach { it.onEntityCfgChanged(entity, cmpMask) }
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

    /**
     * Adds the given [listener] to the list of [EntityListener].
     */
    fun addEntityListener(listener: EntityListener) = listeners.add(listener)

    /**
     * Removes the given [listener] from the list of [EntityListener].
     */
    fun removeEntityListener(listener: EntityListener) = listeners.remove(listener)
}
