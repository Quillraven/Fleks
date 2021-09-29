package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.bag

interface EntityListener {
    fun onEntityCfgChanged(entityId: Int, cmpMask: BitArray) = Unit
}

class EntityConfiguration(
    @PublishedApi
    internal val cmpService: ComponentService
) {
    @PublishedApi
    internal var entityId = 0

    @PublishedApi
    internal lateinit var cmpMask: BitArray

    inline fun <reified T : Any> add(cfg: T.() -> Unit = {}): T {
        val mapper = cmpService.mapper<T>()
        cmpMask.set(mapper.id)
        return mapper.add(entityId, cfg)
    }

    inline fun <reified T : Any> add(mapper: ComponentMapper<T>, cfg: T.() -> Unit = {}): T {
        cmpMask.set(mapper.id)
        return mapper.add(entityId, cfg)
    }

    inline fun <reified T : Any> remove(mapper: ComponentMapper<T>) {
        cmpMask.clear(mapper.id)
        mapper.remove(entityId)
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
    internal val cmpMasks = bag<BitArray>(initialEntityCapacity)

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
            cmpMasks[newId] = BitArray(64)
        }
        configureEntity(newId, cfg)

        return newId
    }

    inline fun configureEntity(entityId: Int, cfg: EntityConfiguration.() -> Unit) {
        val cmpMask = cmpMasks[entityId]
        entityConfiguration.run {
            this.entityId = entityId
            this.cmpMask = cmpMask
            cfg()
        }
        listeners.forEach { it.onEntityCfgChanged(entityId, cmpMask) }
    }

    fun remove(entityId: Int) {
        recycledIds.add(entityId)
        cmpMasks[entityId].clear()
    }

    fun addEntityListener(listener: EntityListener) = listeners.add(listener)

    fun removeEntityListener(listener: EntityListener) = listeners.remove(listener)
}
