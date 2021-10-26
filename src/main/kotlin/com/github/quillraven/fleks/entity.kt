package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.bag

@JvmInline
value class Entity(val id: Int)

interface EntityListener {
    fun onEntityCfgChanged(entity: Entity, cmpMask: BitArray) = Unit
}

class EntityCreateCfg(
    @PublishedApi
    internal val cmpService: ComponentService
) {
    @PublishedApi
    internal var entity = Entity(0)

    @PublishedApi
    internal lateinit var cmpMask: BitArray

    inline fun <reified T : Any> add(cfg: T.() -> Unit = {}): T {
        val mapper = cmpService.mapper<T>()
        cmpMask.set(mapper.id)
        return mapper.add(entity, cfg)
    }
}

class EntityUpdateCfg(
    @PublishedApi
    internal val cmpService: ComponentService
) {
    @PublishedApi
    internal lateinit var cmpMask: BitArray

    inline fun <reified T : Any> ComponentMapper<T>.add(entity: Entity, cfg: T.() -> Unit = {}): T {
        cmpMask.set(this.id)
        return this.add(entity, cfg)
    }

    inline fun <reified T : Any> ComponentMapper<T>.remove(entity: Entity) {
        cmpMask.clear(this.id)
        this.remove(entity)
    }
}

class EntityService(
    initialEntityCapacity: Int,
    cmpService: ComponentService
) {
    @PublishedApi
    internal var nextId = 0

    @PublishedApi
    internal val recycledEntities = ArrayDeque<Entity>()

    @PublishedApi
    internal val cmpMasks = bag<BitArray>(initialEntityCapacity)

    @PublishedApi
    internal val createCfg = EntityCreateCfg(cmpService)

    @PublishedApi
    internal val updateCfg = EntityUpdateCfg(cmpService)

    @PublishedApi
    internal val listeners = bag<EntityListener>()

    inline fun create(cfg: EntityCreateCfg.() -> Unit): Entity {
        val newEntity = if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            recycledEntities.removeLast()
        }

        if (newEntity.id >= cmpMasks.size) {
            cmpMasks[newEntity.id] = BitArray(64)
        }
        val cmpMask = cmpMasks[newEntity.id]
        createCfg.run {
            this.entity = newEntity
            this.cmpMask = cmpMask
            cfg()
        }
        listeners.forEach { it.onEntityCfgChanged(newEntity, cmpMask) }

        return newEntity
    }

    inline fun configureEntity(entity: Entity, cfg: EntityUpdateCfg.(Entity) -> Unit) {
        val cmpMask = cmpMasks[entity.id]
        updateCfg.run {
            this.cmpMask = cmpMask
            cfg(entity)
        }
        listeners.forEach { it.onEntityCfgChanged(entity, cmpMask) }
    }

    fun remove(entity: Entity) {
        recycledEntities.add(entity)
        cmpMasks[entity.id].clear()
    }

    fun addEntityListener(listener: EntityListener) = listeners.add(listener)

    fun removeEntityListener(listener: EntityListener) = listeners.remove(listener)
}
