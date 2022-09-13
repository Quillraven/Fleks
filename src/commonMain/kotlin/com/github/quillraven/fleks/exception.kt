package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System '${system.simpleName}' is already part of the '${WorldConfiguration::class.simpleName}'.")

class FleksNoSuchSystemException(system: KClass<*>) :
    FleksException("There is no system of type '${system.simpleName}' in the world.")

class FleksNoSuchComponentException(component: String) :
    FleksException("There is no component of type '$component' in the ComponentMapper. Did you add the component to the '${WorldConfiguration::class.simpleName}'?")

class FleksInjectableAlreadyAddedException(type: String) :
    FleksException(
        "Injectable with type name '$type' is already part of the '${WorldConfiguration::class.simpleName}'. Please add a unique 'type' string as parameter " +
            "to inject() function in world configuration and to Inject.dependency() in your systems or component listeners."
    )

class FleksInjectableTypeHasNoName(type: KClass<*>) :
    FleksException("Injectable '$type' does not have simpleName in its class type.")

class FleksNoSuchEntityComponentException(entity: Entity, component: String) :
    FleksException("Entity '$entity' has no component of type '$component'.")

class FleksUnusedInjectablesException(unused: List<KClass<*>>) :
    FleksException("There are unused injectables of following types: ${unused.map { it.simpleName }}")

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

class FleksSnapshotException(reason: String) : FleksException("Cannot load snapshot: $reason!")

class FleksNoSuchInjectable(name: String) : FleksException("There is no injectable with name $name registered!")
