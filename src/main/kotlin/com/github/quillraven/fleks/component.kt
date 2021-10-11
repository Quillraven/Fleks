package com.github.quillraven.fleks

import java.lang.reflect.Constructor
import kotlin.math.max
import kotlin.reflect.KClass

class ComponentMapper<T>(
    @PublishedApi
    internal val id: Int,
    @PublishedApi
    internal var components: Array<T?>,
    @PublishedApi
    internal val cstr: Constructor<T>
) {
    @PublishedApi
    internal inline fun add(entity: Entity, cfg: T.() -> Unit = {}): T {
        val newCmp = cstr.newInstance().apply(cfg)
        if (entity.id >= components.size) {
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }
        components[entity.id] = newCmp
        return newCmp
    }

    @PublishedApi
    internal fun remove(entity: Entity) {
        components[entity.id] = null
    }

    operator fun get(entity: Entity): T {
        return components[entity.id] ?: throw FleksNoSuchComponentException(entity, cstr::class)
    }

    override fun toString(): String {
        return "ComponentMapper(id=$id, cstr=${cstr})"
    }
}

class ComponentService {
    @PublishedApi
    internal val mappers = HashMap<KClass<*>, ComponentMapper<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> mapper(type: KClass<T>): ComponentMapper<T> {
        var mapper = mappers[type]

        if (mapper == null) {
            try {
                // use java constructor because it is ~4x faster than calling Kotlin's createInstance on a KClass
                val cstr = type.java.getDeclaredConstructor()

                mapper = ComponentMapper(
                    mappers.size,
                    Array<Any?>(64) { null } as Array<T?>,
                    cstr
                )
                mappers[type] = mapper
            } catch (e: Exception) {
                throw FleksNoNoArgsComponentConstructorException(type)
            }
        }

        return mapper as ComponentMapper<T>
    }

    inline fun <reified T : Any> mapper(): ComponentMapper<T> = mapper(T::class)
}
