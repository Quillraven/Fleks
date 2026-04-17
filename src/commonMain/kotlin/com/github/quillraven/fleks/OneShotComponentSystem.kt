package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family

/**
 * An [IteratingSystem] that automatically removes any [components][Component]
 * or [tags][EntityTag] of an [entity][Entity] that are passed as the [types] argument.
 *
 * This system is only used internally and is called at the end of a [World.update].
 */
class OneShotComponentSystem(
    types: Array<out UniqueId<*>>,
    world: World,
) : IteratingSystem(
    family = family { any(*types) },
    world = world,
) {
    private val typeIds = types.map { it.id }.toIntArray()
    private val holders: Array<ComponentsHolder<*>> = types.filterIsInstance<ComponentType<*>>()
        .map { world.componentService.wildcardHolder(it) }
        .toTypedArray()

    override fun onTickEntity(entity: Entity) {
        entity.configure {
            holders.forEach { it -= entity }
            typeIds.forEach { compMasks[entity.id].clear(it) }
        }
    }

}
