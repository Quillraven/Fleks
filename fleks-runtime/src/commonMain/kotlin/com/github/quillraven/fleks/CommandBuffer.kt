package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.IntBag
import com.github.quillraven.fleks.collection.LongBag

/**
 * A zero-allocation deferred command queue.
 * Stores operations in flat primitive/object bags.
 */
class CommandBuffer(private val world: World) {
    private val opTypes = IntBag()
    private val entities = LongBag()
    private val componentTypeIds = IntBag()
    private val componentValues = Bag<Any?>()

    companion object {
        const val OP_DESTROY = 1
        const val OP_ADD = 2
        const val OP_REMOVE = 3
    }

    fun queueDestroy(entity: Entity) {
        opTypes.add(OP_DESTROY)
        entities.add(entity.id)
        componentTypeIds.add(-1)
        componentValues.add(null)
    }

    fun queueAdd(entity: Entity, componentTypeId: Int, component: Any) {
        opTypes.add(OP_ADD)
        entities.add(entity.id)
        componentTypeIds.add(componentTypeId)
        componentValues.add(component)
    }

    fun queueRemove(entity: Entity, componentTypeId: Int) {
        opTypes.add(OP_REMOVE)
        entities.add(entity.id)
        componentTypeIds.add(componentTypeId)
        componentValues.add(null)
    }

    fun flush() {
        if (opTypes.isEmpty) return

        for (i in 0 until opTypes.size) {
            val op = opTypes[i]
            val entity = Entity(entities[i])
            val typeId = componentTypeIds[i]
            val comp = componentValues[i]

            when (op) {
                OP_DESTROY -> {
                    world.destroyImmediate(entity)
                }

                OP_ADD -> {
                    if (comp != null) {
                        world.addImmediateRaw(entity, typeId, comp)
                    }
                }

                OP_REMOVE -> {
                    world.removeImmediateRaw(entity, typeId)
                }
            }
        }

        // Reset buffers without reallocating
        opTypes.clear()
        entities.clear()
        componentTypeIds.clear()
        componentValues.clear()
    }
}
