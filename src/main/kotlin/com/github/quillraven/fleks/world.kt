package com.github.quillraven.fleks

import kotlin.reflect.KClass

class WorldConfiguration {
    var entityCapacity = 512

    @PublishedApi
    internal val systemTypes = mutableListOf<KClass<out EntitySystem>>()

    @PublishedApi
    internal val injectables = mutableMapOf<KClass<*>, Any>()

    inline fun <reified T : EntitySystem> system() {
        val systemType = T::class
        if (systemType in systemTypes) {
            throw FleksSystemAlreadyAddedException(systemType)
        }
        systemTypes.add(systemType)
    }

    inline fun <reified T : Any> inject(value: T) {
        val injectType = T::class
        if (injectType in injectables) {
            throw FleksInjectableAlreadyAddedException(injectType)
        }
        injectables[injectType] = value
    }
}

class World(
    cfg: WorldConfiguration.() -> Unit
) {
    private val systemService: SystemService

    @PublishedApi
    internal val componentService = ComponentService()

    @PublishedApi
    internal val entityService: EntityService

    init {
        val worldCfg = WorldConfiguration().apply(cfg)
        systemService = SystemService(worldCfg.systemTypes, worldCfg.injectables, componentService)
        entityService = EntityService(worldCfg.entityCapacity, componentService)
    }

    inline fun entity(cfg: EntityConfiguration.() -> Unit = {}): Int {
        return entityService.create(cfg)
    }

    fun remove(entityId: Int) {
        entityService.remove(entityId)
    }

    fun update() {
        systemService.update()
    }
}
