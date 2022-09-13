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
class ComponentMapper<T>(
    private val name: String,
    @PublishedApi
    internal var components: Bag<T>
) {
    @PublishedApi
    internal val listeners = bag<ComponentListener<T>>(2)

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
        listeners.forEach { it.onComponentAdded(entity, component) }
    }

    fun add(entity: Entity, component: T) {
        components.getOrNull(entity.id)?.let { existingCmp ->
            listeners.forEach { it.onComponentRemoved(entity, existingCmp) }
        }

        components[entity.id] = component
        listeners.forEach { it.onComponentAdded(entity, component) }
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
        TODO("to be removed")
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
            return components[entity.id]
        }
        // entity is not part of mapper
        return null
    }

    /**
     * Returns true if and only if the given [entity] has a component of the specific type.
     */
    operator fun contains(entity: Entity): Boolean = components.size > entity.id && components[entity.id] != null

    /**
     * Adds the given [listener] to the list of [ComponentListener].
     */
    fun addComponentListener(listener: ComponentListener<T>) = listeners.add(listener)

    /**
     * Adds the given [listener] to the list of [ComponentListener]. This function is only used internally
     * to add listeners through the [WorldConfiguration].
     */
    @Suppress("UNCHECKED_CAST")
    internal fun addComponentListenerInternal(listener: ComponentListener<*>) =
        addComponentListener(listener as ComponentListener<T>)

    /**
     * Removes the given [listener] from the list of [ComponentListener].
     */
    fun removeComponentListener(listener: ComponentListener<T>) = listeners.removeValue(listener)

    /**
     * Returns true if and only if the given [listener] is part of the list of [ComponentListener].
     */
    operator fun contains(listener: ComponentListener<T>) = listener in listeners

    override fun toString(): String {
        return "ComponentMapper($name)"
    }
}

/**
 * A service class that is responsible for managing [ComponentMapper] instances.
 * It creates a [ComponentMapper] for every unique component type and assigns a unique id for each mapper.
 */
class ComponentService {
    /**
     * Returns [Bag] of [ComponentMapper]. The id of the mapper is the index of the bag.
     * It is used by the [EntityService] to fasten up the cleanup process of delayed entity removals.
     */
    @PublishedApi
    internal val mappersBag = bag<ComponentMapper<*>>()

    fun wildcardMapper(compType: ComponentType<*>): ComponentMapper<*> {
        if (!mappersBag.hasValueAtIndex(compType.id)) {
            // This function is called by wildcard component types (=ComponentType<*>).
            // We cannot use the simpleName because it returns "Companion".
            // And I don't like the qualifiedName.
            // Therefore, we do some string manipulation to get the same result as the reified version.
            // Received format is "package.ComponentName.Companion". We strip of package and Companion.
            val name = compType::class.qualifiedName?.substringBeforeLast(".")?.substringAfterLast(".")
            mappersBag[compType.id] = ComponentMapper(name ?: "anonymous", bag<Any>(64) as Bag<*>)
        }
        return mappersBag[compType.id]
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> mapper(compType: ComponentType<T>): ComponentMapper<T> {
        if (!mappersBag.hasValueAtIndex(compType.id)) {
            mappersBag[compType.id] = ComponentMapper(T::class.simpleName ?: "anonymous", bag<T>(64))
        }
        return mappersBag[compType.id] as ComponentMapper<T>
    }
}
