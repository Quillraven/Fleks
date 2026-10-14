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
    // only hold ids so retired types aren't kept alive
    private val typeIds = types.map { it.id }.toIntArray()

    override fun onTickEntity(entity: Entity) {
        entity.configure {
            typeIds.forEach {
                world.componentService.holderByIndexOrNull(it)?.minusAssign(entity)
                compMasks[entity.id].clear(it)
            }
        }
    }

}
