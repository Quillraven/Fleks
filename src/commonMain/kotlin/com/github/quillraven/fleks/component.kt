package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import kotlin.math.max
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass

/**
 * Type alias for an optional hook function for a [ComponentsHolder].
 * Such a function runs within a [World] and takes the [Entity] and [component][Component] as an argument.
 */
typealias ComponentHook<T> = World.(Entity, T) -> Unit

/**
 * A class that assigns a unique [id] per type of [Component] starting from 0.
 * This [id] is used internally by Fleks as an index for some arrays.
 * Every [Component] class must have at least one [ComponentType].
 */
abstract class ComponentType<T> {
    val id: Int = nextId++

    @ThreadLocal
    companion object {
        private var nextId = 0
    }
}

/**
 * An interface that must be implemented by any component that is used for Fleks.
 * A component must have at least one [ComponentType] that is provided via the [type] function.
 *
 * One convenient approach is to use the unnamed companion object of a Kotlin class as a [ComponentType].
 * Sample code for a component that stores the position of an entity:
 *
 *     data class Position(
 *         var x: Float,
 *         var y: Float,
 *     ) : Component<Position> {
 *         override fun type(): ComponentType<Position> = Position
 *
 *         companion object : ComponentType<Position>()
 *     }
 */
interface Component<T> {
    /**
     * Returns the [ComponentType] of a [Component].
     */
    fun type(): ComponentType<T>
}

/**
 * A class that is responsible to store components of a specific type for all [entities][Entity] in a [world][World].
 * The index of the [components] array is linked to the id of an [entity][Entity]. If an [entity][Entity] has
 * a component of this specific type then the value at index 'entity.id' is not null.
 *
 * Refer to [ComponentService] for more details.
 */
class ComponentsHolder<T : Component<*>>(
    private val world: World,
    private val name: String,
    private var components: Array<T?>,
) {
    /**
     * An optional [ComponentHook] that gets called whenever a [component][Component] gets set for an [entity][Entity].
     */
    @PublishedApi
    internal var addHook: ComponentHook<T>? = null

    /**
     * An optional [ComponentHook] that gets called whenever a [component][Component] gets removed from an [entity][Entity].
     */
    @PublishedApi
    internal var removeHook: ComponentHook<T>? = null

    /**
     * Sets the [component] for the given [entity]. This function is only
     * used by [World.loadSnapshot] where we don't have the correct type information
     * during runtime, and therefore we can only provide 'Any' as a type and need to cast it internally.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun setWildcard(entity: Entity, component: Any) = set(entity, component as T)

    /**
     * Sets the [component] for the given [entity].
     * If a [removeHook] is defined then it gets called if the [entity] already had a component.
     * If an [addHook] is defined then it gets called after the [component] is assigned to the [entity].
     */
    operator fun set(entity: Entity, component: T) {
        if (entity.id >= components.size) {
            // not enough space to store the new component -> resize array
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }

        // check if removeHook needs to be called
        components[entity.id]?.let { existingCmp ->
            // assign current component to null in order for 'contains' calls inside the hook
            // to correctly return false
            components[entity.id] = null
            removeHook?.invoke(world, entity, existingCmp)
        }

        // set component and call addHook if necessary
        components[entity.id] = component
        addHook?.invoke(world, entity, component)
    }

    /**
     * Removes a component of the specific type from the given [entity].
     * If a [removeHook] is defined then it gets called if the [entity] has such a component.
     *
     * @throws [IndexOutOfBoundsException] if the id of the [entity] exceeds the components' capacity.
     */
    operator fun minusAssign(entity: Entity) {
        if (entity.id < 0 || entity.id >= components.size) throw IndexOutOfBoundsException("$entity.id is not valid for components of size ${components.size}")

        val existingCmp = components[entity.id]
        // assign null before running the removeHook in order for 'contains' calls to correctly return false
        components[entity.id] = null
        if (existingCmp != null) {
            removeHook?.invoke(world, entity, existingCmp)
        }
    }

    /**
     * Updates a [component][Component] for the given [entity] by calling [update].
     *
     * If the [entity] has no [component][Component] yet then [factory] is called to
     * provide an instance for the [set] call, before calling [update].
     */
    inline fun setOrUpdate(
        entity: Entity,
        factory: () -> T,
        update: (T) -> Unit,
    ) {
        // use getOrNull here to be safe if entity id > components.size.
        // Array gets resized within the 'set' call if necessary.
        val existingComp = getOrNull(entity)
        if (existingComp == null) {
            set(entity, factory().also(update))
        } else {
            existingComp.also(update)
        }
    }

    /**
     * Returns a component of the specific type of the given [entity].
     *
     * @throws [FleksNoSuchEntityComponentException] if the [entity] does not have such a component.
     */
    operator fun get(entity: Entity): T {
        return components[entity.id] ?: throw FleksNoSuchEntityComponentException(entity, name)
    }

    /**
     * Returns a component of the specific type of the given [entity] or null if the entity does not have such a component.
     */
    fun getOrNull(entity: Entity): T? =
        components.getOrNull(entity.id)

    /**
     * Returns true if and only if the given [entity] has a component of the specific type.
     */
    operator fun contains(entity: Entity): Boolean =
        components.size > entity.id && components[entity.id] != null

    override fun toString(): String {
        return "ComponentsHolder($name)"
    }
}

/**
 * A service class that is responsible for managing [ComponentsHolder] instances.
 * It creates a [ComponentsHolder] for every unique [ComponentType].
 */
class ComponentService {
    @PublishedApi
    internal lateinit var world: World

    /**
     * Returns [Bag] of [ComponentsHolder].
     */
    @PublishedApi
    internal val holdersBag = bag<ComponentsHolder<*>>()

    // Format of toString() is package.Component$Companion
    // This method returns 'Component' which is the short name of the component class.
    fun KClass<*>.toComponentName(): String =
        this.toString().substringAfterLast(".").substringBefore("$")

    /**
     * Returns a [ComponentsHolder] for the given [componentType]. This function is only
     * used by [World.loadSnapshot] where we don't have the correct type information
     * during runtime, and therefore we can only provide '*' as a type and need to cast it internally.
     */
    fun wildcardHolder(componentType: ComponentType<*>): ComponentsHolder<*> {
        if (holdersBag.hasNoValueAtIndex(componentType.id)) {
            // We cannot use simpleName here because it returns "Companion".
            // Therefore, we do some string manipulation to get the name of the component correctly.
            // Format of toString() is package.Component$Companion
            val name = componentType::class.toComponentName()
            holdersBag[componentType.id] = ComponentsHolder(world, name, Array<Component<*>?>(world.capacity) { null })
        }
        return holdersBag[componentType.id]
    }

    /**
     * Returns a [ComponentsHolder] for the given [componentType]. If no such holder exists yet, then it
     * will be created and added to the [holdersBag].
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Component<*>> holder(componentType: ComponentType<T>): ComponentsHolder<T> {
        if (holdersBag.hasNoValueAtIndex(componentType.id)) {
            val name = T::class.simpleName ?: T::class.toComponentName()
            holdersBag[componentType.id] = ComponentsHolder(world, name, Array<T?>(world.capacity) { null })
        }
        return holdersBag[componentType.id] as ComponentsHolder<T>
    }

    /**
     * Returns the [ComponentsHolder] of the given [index] inside the [holdersBag]. The index
     * is linked to the id of a [ComponentType].
     * This function is only used internally at safe areas to speed up certain processes like
     * removing an [entity][Entity] or creating a snapshot via [World.snapshot].
     *
     * @throws [IndexOutOfBoundsException] if the [index] exceeds the bag's capacity.
     */
    internal fun holderByIndex(index: Int): ComponentsHolder<*> {
        return holdersBag[index]
    }
}
