package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System ${system.simpleName} is already part of the ${WorldConfiguration::class.simpleName}")

class FleksInjectableAlreadyAddedException(injectable: KClass<*>) :
    FleksException("Injectable ${injectable.simpleName} is already part of the ${WorldConfiguration::class.simpleName}")

class FleksSystemCreationException(system: KClass<*>, details: String) :
    FleksException("Cannot create ${system.simpleName}. Did you add all necessary injectables?\nDetails: $details")

class FleksNoNoArgsComponentConstructorException(component: KClass<*>) :
    FleksException("Component ${component.simpleName} is missing a no-args constructor")

class FleksNoSuchComponentException(entity: Entity, component: KClass<*>) :
    FleksException("Entity $entity has no component of type ${component.simpleName}")
