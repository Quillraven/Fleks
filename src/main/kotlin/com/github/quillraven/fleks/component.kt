package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.bag
import java.lang.reflect.Constructor
import kotlin.reflect.KClass

class ComponentMapper<T : Any>(
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
    internal val cmpIndices = mutableMapOf<KClass<*>, Int>()

    @PublishedApi
    internal val mappers = bag<ComponentMapper<*>>()

    inline fun <reified T : Any> cmpIdx(): Int = cmpIdx(T::class)

    @PublishedApi
    internal fun <T : Any> cmpIdx(type: KClass<T>): Int {
        var cmpIdx = cmpIndices[type]

        if (cmpIdx == null) {
            cmpIdx = cmpIndices.size
            cmpIndices[type] = cmpIdx

            if (cmpIdx >= mappers.size) {
                try {
                    // use java constructor because it is ~10x faster than calling Kotlin's createInstance on a KClass
                    val cstr = type.java.getDeclaredConstructor()

                    @Suppress("UNCHECKED_CAST")
                    val mapper = ComponentMapper(
                        bag<Any?>() as Bag<T>,
                        cstr
                    )
                    mappers[cmpIdx] = mapper
                } catch (e: Exception) {
                    throw FleksNoNoArgsComponentConstructor(type)
                }
            }
        }

        return cmpIdx
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> mapper(cmpIdx: Int): ComponentMapper<T> = mappers[cmpIdx] as ComponentMapper<T>
}
