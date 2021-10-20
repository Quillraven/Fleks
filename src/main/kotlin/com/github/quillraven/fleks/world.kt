package com.github.quillraven.fleks

import kotlin.reflect.KClass

class WorldConfiguration {
    var entityCapacity = 512

    @PublishedApi
    internal val systemTypes = mutableListOf<KClass<out IntervalSystem>>()

    @PublishedApi
    internal val injectables = mutableMapOf<KClass<*>, Any>()

    inline fun <reified T : IntervalSystem> system() {
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
    var deltaTime = 0f
        private set
    private val systemService: SystemService

    @PublishedApi
    internal val componentService = ComponentService()

    @PublishedApi
    internal val entityService: EntityService

    init {
        val worldCfg = WorldConfiguration().apply(cfg)
        entityService = EntityService(worldCfg.entityCapacity, componentService)
        systemService = SystemService(this, worldCfg.systemTypes, worldCfg.injectables)
    }

    inline fun entity(cfg: EntityConfiguration.() -> Unit = {}): Entity {
        return entityService.create(cfg)
    }

    fun remove(entity: Entity) {
        entityService.remove(entity)
    }

    fun update(deltaTime: Float) {
        this.deltaTime = deltaTime.coerceAtMost(1 / 30f)
        systemService.update()
    }
}
