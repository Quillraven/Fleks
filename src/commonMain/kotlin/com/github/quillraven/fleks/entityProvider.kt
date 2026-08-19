package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray

/**
 * Interface to describe the functionality needed by an [EntityService]
 * to create and remove [entities][Entity]. The provider can be specified
 * via the [WorldConfiguration]. Per default a [DefaultEntityProvider] will be used.
 */
interface EntityProvider {

    /**
     * Reference to the [World] of the [EntityProvider].
     * It is necessary for the [forEach] implementation.
     */
    val world: World

    /**
     * Returns the total number of active [entities][Entity].
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
 * The first [entity][Entity] starts with [id][Entity.id] zero.
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
     * Returns the total number of active [entities][Entity].
     */
    override fun numEntities(): Int = nextId - recycledEntities.size

    /**
     * Bit array tracking which entity ids are currently active.
     */
    private val activeEntities = BitArray(initialEntityCapacity)

    /**
     * Creates a new [entity][Entity]. If there are [recycledEntities] then they will be preferred
     * over creating new entities.
     */
    override fun create(): Entity {
        return if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            val recycled = recycledEntities.removeLast()

            // Because of the load snapshot functionality of the world, it is
            // possible that an entity with an ID higher than nextId gets recycled
            // and immediately created afterward. In such a case we need to correct the
            // nextId to avoid creating duplicated entities.
            if (recycled.id >= nextId) {
                nextId = recycled.id + 1
            }

            recycled
        }.also {
            activeEntities.set(it.id)
        }
    }

    /**
     * Creates a new [Entity] with a specific [id].
     */
    override fun create(id: Int): Entity {
        if (id >= nextId) {
            // entity with a given id was never created before -> create all missing entities ...
            repeat(id - nextId + 1) {
                this -= Entity(nextId + it)
            }
            // ... and then create the entity to guarantee that it has the correct ID.
            // The entity is at the end of the recycled list.

            // adjust ID for future entities to be created
            nextId = id + 1
            return create()
        } else {
            // entity with a given id was already created before and is part of the recycled entities
            // -> move it to the end to be used by the next call to 'create'
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
        activeEntities.clear(entity.id)
    }

    /**
     * Returns true if and only if the given [entity] is active and part of the provider.
     */
    override fun contains(entity: Entity): Boolean = activeEntities[entity.id]

    /**
     * Resets the provider by removing and recycling all [entities][Entity].
     * Also, resets the [nextId] to zero.
     */
    override fun reset() {
        nextId = 0
        recycledEntities.clear()
        activeEntities.clearAll()
    }

    /**
     * Performs the given [action] for all active [entities][Entity].
     */
    override fun forEach(action: World.(Entity) -> Unit) {
        activeEntities.forEachSetBitAsc { id ->
            action(world, Entity(id))
        }
    }
}
