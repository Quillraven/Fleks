package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

class ComponentMapper<T>(
    @PublishedApi
    internal val id: Int,
    @PublishedApi
    internal val components: Bag<T>,
    @PublishedApi
    internal val cstr: Constructor<T>
) {
    @PublishedApi
    internal inline fun add(entityId: Int, cfg: T.() -> Unit = {}): T {
        val newCmp = cstr.newInstance().apply(cfg)
        components[entityId] = newCmp
        return newCmp
    }

    operator fun get(entityId: Int): T {
        return components[entityId]
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
                    bag<Any?>() as Bag<T>,
                    cstr
                )
                mappers[type] = mapper
            } catch (e: Exception) {
                throw FleksNoNoArgsComponentConstructor(type)
            }
        }

        return mapper as ComponentMapper<T>
    }

    inline fun <reified T : Any> mapper(): ComponentMapper<T> = mapper(T::class)
}
