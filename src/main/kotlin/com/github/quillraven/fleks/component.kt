package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import java.lang.reflect.Constructor
import kotlin.math.max
import kotlin.reflect.KClass

/**
 * A class that is responsible to store components of a specific type for all [entities][Entity] in a [world][World].
 * Each component is assigned a unique [id] for fast access and to avoid lookups via a class which is slow.
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
    /**
     * Creates and returns a new component of the specific type for the given [entity] and applies the [configuration].
     * If the [entity] already has a component of that type then no new instance will be created.
     */
    @PublishedApi
    internal inline fun add(entity: Entity, configuration: T.() -> Unit = {}): T {
        if (entity.id >= components.size) {
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }
        var cmp = components[entity.id]
        return if (cmp == null) {
            cmp = cstr.newInstance().apply(configuration)
            components[entity.id] = cmp
            cmp
        } else {
            cmp.apply(configuration)
        }
    }

    /**
     * Removes a component of the specific type from the given [entity].
     */
    @PublishedApi
    internal fun remove(entity: Entity) {
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
     * Returns true if and only if the given [entity] has a component of the specific type.
     */
    operator fun contains(entity: Entity): Boolean = components.size > entity.id && components[entity.id] != null

    override fun toString(): String {
        return "ComponentMapper(id=$id, cstr=${cstr})"
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
