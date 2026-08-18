package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.BitArray
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
 * [IntervalSystem], [World] or [com.github.quillraven.fleks.collection.compareEntity].
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
     * Otherwise, you will get wrong behavior for families. E.g., don't do this:
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
     * This is the case if it gets [removed][remove] during the system's iteration.
     */
    fun Entity.isMarkedForRemoval() = this in componentService.world.entityService.delayedEntities

    /**
     * Returns true if and only if the [entity][Entity] was removed and is not part of the [World] anymore.
     */
    fun Entity.wasRemoved() = this !in componentService.world
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
     * If the [entity][Entity] already has such a component, then the [onRemove][Component.onRemove]
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
        // We need to remember used tags to correctly return and load them using
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
