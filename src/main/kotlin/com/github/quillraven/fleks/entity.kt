package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.bag

@JvmInline
value class Entity(val id: Int)

interface EntityListener {
    fun onEntityCfgChanged(entity: Entity, cmpMask: BitArray) = Unit
}

class EntityConfiguration(
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

    inline fun <reified T : Any> add(mapper: ComponentMapper<T>, cfg: T.() -> Unit = {}): T {
        cmpMask.set(mapper.id)
        return mapper.add(entity, cfg)
    }

    inline fun <reified T : Any> remove(mapper: ComponentMapper<T>) {
        cmpMask.clear(mapper.id)
        mapper.remove(entity)
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
    internal val entityConfiguration = EntityConfiguration(cmpService)

    @PublishedApi
    internal val listeners = bag<EntityListener>()

    inline fun create(cfg: EntityConfiguration.() -> Unit): Entity {
        val newEntity = if (recycledEntities.isEmpty()) {
            Entity(nextId++)
        } else {
            recycledEntities.removeLast()
        }

        if (newEntity.id >= cmpMasks.size) {
            cmpMasks[newEntity.id] = BitArray(64)
        }
        configureEntity(newEntity, cfg)

        return newEntity
    }

    inline fun configureEntity(entity: Entity, cfg: EntityConfiguration.() -> Unit) {
        val cmpMask = cmpMasks[entity.id]
        entityConfiguration.run {
            this.entity = entity
            this.cmpMask = cmpMask
            cfg()
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
