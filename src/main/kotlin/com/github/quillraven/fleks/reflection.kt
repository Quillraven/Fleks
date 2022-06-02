package com.github.quillraven.fleks

import java.lang.reflect.Constructor
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

/**
 * Creates a new instance of type [T] by using dependency injection if necessary.
 * Dependencies are looked up in the [injectables] map.
 * If it is a [ComponentMapper] then the mapper is retrieved from the [cmpService].
 *
 * @throws [FleksReflectionException] if there is no single primary constructor or if dependencies are missing.
 *
 * @throws [FleksMissingNoArgsComponentConstructorException] if a component of a [ComponentMapper] has no no-args constructor.
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> newInstance(
    type: KClass<T>,
    cmpService: ComponentService,
    injectables: Map<String, Injectable>
): T {
    val constructors = type.java.declaredConstructors
    if (constructors.size != 1) {
        throw FleksReflectionException(
            type,
            "Found ${constructors.size} constructors. Only a single primary constructor is supported!"
        )
    }

    // get constructor arguments
    val args = systemArgs(constructors.first(), cmpService, injectables, type)
    // create new instance using arguments from above
    return constructors.first().newInstance(*args) as T
}

/**
 * Returns array of arguments for the given [primaryConstructor].
 * Arguments are either an object of [injectables] or [ComponentMapper] instances.
 *
 * @throws [FleksReflectionException] if [injectables] are missing for the [primaryConstructor].
 *
 * @throws [FleksMissingNoArgsComponentConstructorException] if the [primaryConstructor] requires a [ComponentMapper]
 * with a component type that does not have a no argument constructor.
 */
@PublishedApi
internal fun systemArgs(
    primaryConstructor: Constructor<*>,
    cmpService: ComponentService,
    injectables: Map<String, Injectable>,
    type: KClass<out Any>
): Array<Any> {
    val params = primaryConstructor.parameters
    val paramAnnotations = primaryConstructor.parameterAnnotations

    val args = Array(params.size) { idx ->
        val param = params[idx]
        val paramClass = param.type.kotlin
        if (paramClass == ComponentMapper::class) {
            val cmpType = (param.parameterizedType as ParameterizedType).actualTypeArguments[0] as Class<*>
            cmpService.mapper(cmpType.kotlin)
        } else {
            // check if qualifier annotation is specified
            val qualifierAnn = paramAnnotations[idx].firstOrNull { it is Qualifier } as Qualifier?
            // if yes -> use qualifier name
            // if no -> use qualified class name
            val name = qualifierAnn?.name ?: paramClass.qualifiedName
            val injectable = injectables[name] ?: throw FleksReflectionException(
                type,
                "Missing injectable of type ${paramClass.qualifiedName}"
            )
            injectable.used = true
            injectable.injObj
        }
    }

    return args
}

/**
 * Returns [Annotation] of the specific type if the class has that annotation. Otherwise, returns null.
 */
inline fun <reified T : Annotation> KClass<*>.annotation(): T? {
    return this.java.getAnnotation(T::class.java)
}
