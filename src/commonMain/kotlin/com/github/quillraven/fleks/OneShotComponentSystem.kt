package com.github.quillraven.fleks

import com.github.quillraven.fleks.World.Companion.family

/**
 * An [IteratingSystem] that automatically removes any [components][Component]
 * or [tags][EntityTag] of an [entity][Entity] that are passed as the [types] argument.
 *
 * This system is only used internally and is called at the end of a [World.update].
 */
class OneShotComponentSystem(
    private val types: Array<out UniqueId<*>>,
    world: World,
) : IteratingSystem(
    family = family { any(*types) },
    world = world,
) {

    override fun onTickEntity(entity: Entity) {
        entity.configure {
            types.forEach { componentType ->
                entity -= componentType
            }
        }
    }

}
