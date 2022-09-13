package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import kotlin.native.concurrent.ThreadLocal

abstract class ComponentType<T> {
    val id: Int = nextId++

    @ThreadLocal
    companion object {
        private var nextId = 0
    }
}

interface Component<T> {
    fun type(): ComponentType<T>
}

/**
 * Interface of a component listener that gets notified when a component of a specific type
 * gets added or removed from an [entity][Entity].
 */
interface ComponentListener<T> {
    fun onComponentAdded(entity: Entity, component: T)
    fun onComponentRemoved(entity: Entity, component: T)
}

/**
 * A class that is responsible to store components of a specific type for all [entities][Entity] in a [world][World].
 * Each component is assigned a unique [id] for fast access and to avoid lookups via a class which is slow.
 * Hint: A component at index [id] in the [components] array belongs to [Entity] with the same [id].
 *
 * Refer to [ComponentService] for more details.
 */
class ComponentMapper<T : Any>(
    private val world: World,
    private val name: String,
    @PublishedApi
    internal var components: Bag<T>
) {
    @PublishedApi
    internal var addHook: ((World, Entity, T) -> Unit)? = null

    @PublishedApi
    internal var removeHook: ((World, Entity, T) -> Unit)? = null

    // TODO to be removed
    var id = 0

    /**
     * Creates and returns a new component of the specific type for the given [entity] and applies the [configuration].
     * If the [entity] already has a component of that type then no new instance will be created.
     * Notifies any registered [ComponentListener].
     */
    @PublishedApi
    internal inline fun addInternal(entity: Entity, configuration: T.() -> Unit = {}): T {
        TODO("to be removed")
    }

    /**
     * Adds the [component] to the given [entity]. This function is only
     * used by [World.loadSnapshot].
     */
    @Suppress("UNCHECKED_CAST")
    internal fun addInternal(entity: Entity, component: Any) {
        components[entity.id] = component as T
        addHook?.invoke(world, entity, component)
    }

    fun add(entity: Entity, component: T) {
        components.getOrNull(entity.id)?.let { existingCmp ->
            removeHook?.invoke(world, entity, existingCmp)
        }

        components[entity.id] = component
        addHook?.invoke(world, entity, component)
    }

    /**
     * Creates a new component if the [entity] does not have it yet. Otherwise, updates the existing component.
     * Applies the [configuration] in both cases and returns the component.
     * Notifies any registered [ComponentListener] if a new component is created.
     */
    @PublishedApi
    internal inline fun addOrUpdateInternal(entity: Entity, configuration: T.() -> Unit = {}): T {
        return if (entity in this) {
            this[entity].apply(configuration)
        } else {
            addInternal(entity, configuration)
        }
    }

    /**
     * Removes a component of the specific type from the given [entity].
     * Notifies any registered [ComponentListener].
     *
     * @throws [IndexOutOfBoundsException] if the id of the [entity] exceeds the components' capacity.
     */
    @PublishedApi
    internal fun removeInternal(entity: Entity) {
        components.getOrNull(entity.id)?.let { existingComp ->
            removeHook?.invoke(world, entity, existingComp)
        }
        components.clearIndex(entity.id)

    }

    /**
     * Returns a component of the specific type of the given [entity].
     *
     * @throws [FleksNoSuchEntityComponentException] if the [entity] does not have such a component.
     */
    operator fun get(entity: Entity): T {
        return components.getOrNull(entity.id) ?: throw FleksNoSuchEntityComponentException(entity, name)
    }

    /**
     * Returns a component of the specific type of the given [entity] or null if the entity does not have this component.
     */
    fun getOrNull(entity: Entity): T? {
        if (components.size > entity.id) {
            // entity potentially has this component. However, return value can still be null
            return components.getOrNull(entity.id)
        }
        // entity is not part of mapper
        return null
    }

    /**
     * Returns true if and only if the given [entity] has a component of the specific type.
     */
    operator fun contains(entity: Entity): Boolean =
        components.size > entity.id && components.getOrNull(entity.id) != null

    override fun toString(): String {
        return "ComponentMapper($name)"
    }
}

/**
 * A service class that is responsible for managing [ComponentMapper] instances.
 * It creates a [ComponentMapper] for every unique component type and assigns a unique id for each mapper.
 */
class ComponentService(
    @PublishedApi
    internal val world: World,
) {
    /**
     * Returns [Bag] of [ComponentMapper]. The id of the mapper is the index of the bag.
     * It is used by the [EntityService] to fasten up the cleanup process of delayed entity removals.
     */
    @PublishedApi
    internal val mappersBag = bag<ComponentMapper<*>>()

    fun wildcardMapper(compType: ComponentType<*>): ComponentMapper<*> {
        if (!mappersBag.hasValueAtIndex(compType.id)) {
            // We cannot use simpleName here because it returns "Companion".
            // Therefore, we do some string manipulation to get the name of the component correctly.
            // Format of toString() is package.Component$Companion
            val name = compType::class.toString().substringAfterLast(".").substringBefore("$")
            mappersBag[compType.id] = ComponentMapper(world, name, bag(64))
        }
        return mappersBag[compType.id]
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> mapper(compType: ComponentType<T>): ComponentMapper<T> {
        if (!mappersBag.hasValueAtIndex(compType.id)) {
            mappersBag[compType.id] = ComponentMapper(world, T::class.simpleName ?: "anonymous", bag<T>(64))
        }
        return mappersBag[compType.id] as ComponentMapper<T>
    }
}
