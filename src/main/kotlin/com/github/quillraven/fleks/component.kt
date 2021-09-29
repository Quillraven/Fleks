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
    internal inline fun add(entityId: Int, cfg: T.() -> Unit = {}): T {
        val newCmp = cstr.newInstance().apply(cfg)
        if (entityId >= components.size) {
            components = components.copyOf(max(components.size * 2, entityId + 1))
        }
        components[entityId] = newCmp
        return newCmp
    }

    @PublishedApi
    internal fun remove(entityId: Int) {
        components[entityId] = null
    }

    operator fun get(entityId: Int): T {
        return components[entityId] ?: throw FleksNoSuchComponentException(entityId, cstr::class)
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
