package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System ${system.simpleName} is already part of the ${WorldConfiguration::class.simpleName}.")

class FleksComponentAlreadyAddedException(comp: KClass<*>) :
    FleksException("Component ${comp.simpleName} is already part of the ${WorldConfiguration::class.simpleName}.")

class FleksSystemCreationException(system: IteratingSystem) :
    FleksException("Cannot create system '$system'. IteratingSystem must define at least one of AllOf, NoneOf or AnyOf properties.")

class FleksNoSuchSystemException(system: KClass<*>) :
    FleksException("There is no system of type ${system.simpleName} in the world.")

class FleksNoSuchComponentException(component: KClass<*>) :
    FleksException("There is no component of type ${component.simpleName} in the ComponentMapper. Did you add the component to the ${WorldConfiguration::class.simpleName}?")

class FleksInjectableAlreadyAddedException(type: KClass<*>) :
    FleksException("Injectable with name ${type.simpleName} is already part of the ${WorldConfiguration::class.simpleName}.")

class FleksSystemInjectException(injectType: KClass<*>) :
    FleksException("Injection object of type ${injectType.simpleName} cannot be found. Did you add all necessary injectables?")

class FleksNoSuchEntityComponentException(entity: Entity, component: String) :
    FleksException("Entity $entity has no component of type $component.")

class FleksComponentListenerAlreadyAddedException(listener: KClass<*>) :
    FleksException("ComponentListener ${listener.simpleName} is already part of the ${WorldConfiguration::class.simpleName}.")

class FleksUnusedInjectablesException(unused: List<KClass<*>>) :
    FleksException("There are unused injectables of following types: ${unused.map { it.simpleName }}")
