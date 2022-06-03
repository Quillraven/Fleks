package com.github.quillraven.fleks

/**
 * An [injector][Injections] which is used to inject objects from outside the [IntervalSystem].
 *
 * @throws [FleksSystemDependencyInjectException] if the Injector does not contain an entry
 * for the given type in its internal map.
 * @throws [FleksSystemComponentInjectException] if the Injector does not contain a component mapper
 * for the given type in its internal map.
 * @throws [FleksInjectableTypeHasNoName] if the dependency type has no T::class.simpleName.
 */
data class Injections(
    @PublishedApi
    internal val injectObjects: Map<String, Injectable> = mapOf(),
    @PublishedApi
    internal val mapperObjects: Map<String, ComponentMapper<*>> = mapOf()

) {
    inline fun <reified T : Any> dependency(): T {
        val injectType = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        return if (injectType in injectObjects) {
            injectObjects[injectType]!!.used = true
            injectObjects[injectType]!!.injObj as T
        } else throw FleksSystemDependencyInjectException(injectType)
    }

    inline fun <reified T : Any> dependency(type: String): T {
        return if (type in injectObjects) {
            injectObjects[type]!!.used = true
            injectObjects[type]!!.injObj as T
        } else throw FleksSystemDependencyInjectException(type)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : Any> componentMapper(): ComponentMapper<T> {
        val injectType = T::class.simpleName ?: throw FleksInjectableTypeHasNoName(T::class)
        return if (injectType in mapperObjects) {
            mapperObjects[injectType]!! as ComponentMapper<T>
        } else throw FleksSystemComponentInjectException(injectType)
    }

    companion object {
        val EMPTY = Injections()
    }
}
