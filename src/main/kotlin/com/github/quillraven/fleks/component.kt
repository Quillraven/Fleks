package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import java.lang.reflect.Constructor
import kotlin.math.max
import kotlin.reflect.KClass

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
 * A component of the [components] array at index X belongs to the [Entity] of id X.
 *
 * Refer to [ComponentService] for more details.
 */
class ComponentMapper<T>(
    @PublishedApi
    internal val id: Int,
    @PublishedApi
    internal var components: Array<T?>,
    @PublishedApi
    internal val cstr: Constructor<T>
) {
    @PublishedApi
    internal val listeners = bag<ComponentListener<T>>(2)

    /**
     * Creates and returns a new component of the specific type for the given [entity] and applies the [configuration].
     * If the [entity] already has a component of that type then no new instance will be created.
     * Notifies any registered [ComponentListener].
     */
    @PublishedApi
    internal inline fun addInternal(entity: Entity, configuration: T.() -> Unit = {}): T {
        if (entity.id >= components.size) {
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }
        val cmp = components[entity.id]
        return if (cmp == null) {
            val newCmp = cstr.newInstance().apply(configuration)
            components[entity.id] = newCmp
            listeners.forEach { it.onComponentAdded(entity, newCmp) }
            newCmp
        } else {
            // component already added -> reuse it and do not create a new instance.
            // Call onComponentRemoved first in case users do something special in onComponentAdded.
            // Otherwise, onComponentAdded will be executed twice on a single component without executing onComponentRemoved
            // which is not correct.
            listeners.forEach { it.onComponentRemoved(entity, cmp) }
            val existingCmp = cmp.apply(configuration)
            listeners.forEach { it.onComponentAdded(entity, existingCmp) }
            existingCmp
        }
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
     * @throws [ArrayIndexOutOfBoundsException] if the id of the [entity] exceeds the components' capacity.
     */
    @PublishedApi
    internal fun removeInternal(entity: Entity) {
        components[entity.id]?.let { cmp ->
            listeners.forEach { it.onComponentRemoved(entity, cmp) }
        }
        components[entity.id] = null
    }

    /**
     * Returns a component of the specific type of the given [entity].
     *
     * @throws [FleksNoSuchComponentException] if the [entity] does not have such a component.
     */
    operator fun get(entity: Entity): T {
        return components[entity.id] ?: throw FleksNoSuchComponentException(entity, cstr.name)
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
        return "ComponentMapper(id=$id, component=${cstr.name})"
    }
}

/**
 * A service class that is responsible for managing [ComponentMapper] instances.
 * It creates a [ComponentMapper] for every unique component type and assigns a unique id for each mapper.
 */
class ComponentService {
    /**
     * Returns map of [ComponentMapper] that stores mappers by its component type.
     * It is used by the [SystemService] during system creation and by the [EntityService] for entity creation.
     */
    @PublishedApi
    internal val mappers = HashMap<KClass<*>, ComponentMapper<*>>()

    /**
     * Returns [Bag] of [ComponentMapper]. The id of the mapper is the index of the bag.
     * It is used by the [EntityService] to fasten up the cleanup process of delayed entity removals.
     */
    private val mappersBag = bag<ComponentMapper<*>>()

    /**
     * Returns a [ComponentMapper] for the given [type]. If the mapper does not exist then it will be created.
     *
     * @throws [FleksMissingNoArgsComponentConstructorException] if the component of the given [type] does not have
     * a no argument constructor.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> mapper(type: KClass<T>): ComponentMapper<T> {
        var mapper = mappers[type]

        if (mapper == null) {
            try {
                mapper = ComponentMapper(
                    mappers.size,
                    Array<Any?>(64) { null } as Array<T?>,
                    // use java constructor because it is ~4x faster than calling Kotlin's createInstance on a KClass
                    type.java.getDeclaredConstructor()
                )
                mappers[type] = mapper
                mappersBag.add(mapper)
            } catch (e: Exception) {
                throw FleksMissingNoArgsComponentConstructorException(type)
            }
        }

        return mapper as ComponentMapper<T>
    }

    /**
     * Returns a [ComponentMapper] for the specific type. If the mapper does not exist then it will be created.
     *
     * @throws [FleksMissingNoArgsComponentConstructorException] if the component of the specific type does not have
     * a no argument constructor.
     */
    inline fun <reified T : Any> mapper(): ComponentMapper<T> = mapper(T::class)

    /**
     * Returns an already existing [ComponentMapper] for the given [cmpId].
     */
    fun mapper(cmpId: Int) = mappersBag[cmpId]
}
