package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.bag
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
        if (entity.id >= components.size) {
            components = components.copyOf(max(components.size * 2, entity.id + 1))
        }
        var cmp = components[entity.id]
        return if (cmp == null) {
            cmp = cstr.newInstance().apply(cfg)
            components[entity.id] = cmp
            cmp
        } else {
            cmp.apply(cfg)
        }
    }

    @PublishedApi
    internal fun remove(entity: Entity) {
        components[entity.id] = null
    }

    operator fun get(entity: Entity): T {
        return components[entity.id] ?: throw FleksNoSuchComponentException(entity, cstr.name)
    }

    operator fun contains(entity: Entity): Boolean = components.size > entity.id && components[entity.id] != null

    override fun toString(): String {
        return "ComponentMapper(id=$id, cstr=${cstr})"
    }
}

class ComponentService {
    @PublishedApi
    internal val mappers = HashMap<KClass<*>, ComponentMapper<*>>()
    private val mappersBag = bag<ComponentMapper<*>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> mapper(type: KClass<T>): ComponentMapper<T> {
        var mapper = mappers[type]

        if (mapper == null) {
            try {
                mapper = ComponentMapper(
                    mappers.size,
                    Array<Any?>(64) { null } as Array<T?>,
                    // use java constructor because it is ~4x faster than calling Kotlin's createInstance on a KClass
                    type.java.getDeclaredConstructor()
                )
                mappers[type] = mapper
                mappersBag.add(mapper)
            } catch (e: Exception) {
                throw FleksNoNoArgsComponentConstructorException(type)
            }
        }

        return mapper as ComponentMapper<T>
    }

    inline fun <reified T : Any> mapper(): ComponentMapper<T> = mapper(T::class)

    fun mapper(cmpId: Int) = mappersBag[cmpId]
}
