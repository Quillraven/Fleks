package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System ${system.simpleName} is already part of the ${WorldConfiguration::class.simpleName}")

class FleksSystemCreationException(system: KClass<*>, details: String) :
    FleksException("Cannot create ${system.simpleName}. Did you add all necessary injectables?\nDetails: $details")

class FleksNoSuchSystemException(system: KClass<*>) :
    FleksException("There is no system of type ${system.simpleName} in the world")

class FleksInjectableAlreadyAddedException(name: String) :
    FleksException("Injectable with name $name is already part of the ${WorldConfiguration::class.simpleName}")

class FleksInjectableWithoutNameException :
    FleksException("Injectables must be registered with a non-null name")

class FleksMissingNoArgsComponentConstructorException(component: KClass<*>) :
    FleksException("Component ${component.simpleName} is missing a no-args constructor")

class FleksNoSuchComponentException(entity: Entity, component: String) :
    FleksException("Entity $entity has no component of type $component")

class FleksComponentListenerAlreadyAddedException(listener: KClass<out ComponentListener<*>>) :
    FleksException("ComponentListener ${listener.simpleName} is already part of the ${WorldConfiguration::class.simpleName}")

class FleksUnusedInjectablesException(unused: List<KClass<*>>) :
    FleksException("There are unused injectables of following types: ${unused.map { it.simpleName }}")

class FleksReflectionException(type: KClass<*>, details: String) :
    FleksException("Cannot create ${type.simpleName}.\nDetails: $details")

class FleksFamilyException(
    allOf: List<ComponentMapper<*>>?,
    noneOf: List<ComponentMapper<*>>?,
    anyOf: List<ComponentMapper<*>>?,
) : FleksException(
    """Family must have at least one of allOf, noneOf or anyOf.
        |allOf: $allOf
        |noneOf: $noneOf
        |anyOf: $anyOf""".trimMargin()
)
