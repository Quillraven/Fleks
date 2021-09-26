package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.bag
import java.util.BitSet

interface EntityListener {
    fun onEntityCfgChanged(entityId: Int, cmpMask: BitSet) = Unit
}

class EntityConfiguration(
    @PublishedApi
    internal val cmpService: ComponentService
) {
    @PublishedApi
    internal var entityId = 0

    @PublishedApi
    internal lateinit var cmpMask: BitSet

    inline fun <reified T : Any> add(cfg: T.() -> Unit = {}): T {
        val mapper = cmpService.mapper<T>()
        cmpMask.set(mapper.id)
        return mapper.add(entityId, cfg)
    }
}

class EntityService(
    initialEntityCapacity: Int,
    cmpService: ComponentService
) {
    @PublishedApi
    internal var nextId = 0

    @PublishedApi
    internal val recycledIds = ArrayDeque<Int>()

    @PublishedApi
    internal val cmpMasks = bag<BitSet>(initialEntityCapacity)

    @PublishedApi
    internal val entityConfiguration = EntityConfiguration(cmpService)

    @PublishedApi
    internal val listeners = bag<EntityListener>()

    inline fun create(cfg: EntityConfiguration.() -> Unit): Int {
        val newId = if (recycledIds.isEmpty()) {
            nextId++
        } else {
            recycledIds.removeLast()
        }

        if (newId >= cmpMasks.size) {
            cmpMasks[newId] = BitSet(64)
        }
        val cmpMask = cmpMasks[newId]

        entityConfiguration.run {
            this.entityId = newId
            this.cmpMask = cmpMask
            cfg()
        }
        listeners.forEach { it.onEntityCfgChanged(newId, cmpMask) }

        return newId
    }

    fun remove(entityId: Int) {
        recycledIds.add(entityId)
        cmpMasks[entityId].clear()
    }

    fun addEntityListener(listener: EntityListener) = listeners.add(listener)

    fun removeEntityListener(listener: EntityListener) = listeners.remove(listener)
}
