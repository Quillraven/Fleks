package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class FleksException(message: String) : RuntimeException(message)

class FleksSystemAlreadyAddedException(system: KClass<*>) :
    FleksException("System '${system.simpleName}' is already part of the '${WorldConfiguration::class.simpleName}'.")

class FleksNoSuchSystemException(system: KClass<*>) :
    FleksException("There is no system of type '${system.simpleName}' in the world.")

class FleksInjectableAlreadyAddedException(name: String) :
    FleksException(
        "Injectable with name '$name' is already part of the '${WorldConfiguration::class.simpleName}'. " +
            "Please add a unique 'name' string as parameter to the 'add' function of the 'injectables' block."
    )

class FleksNoSuchEntityComponentException(entity: Entity, component: String) :
    FleksException("Entity '$entity' has no component of type '$component'.")

class FleksFamilyException(familyDefinition: FamilyDefinition) :
    FleksException("Family must have at least one of allOf, noneOf or anyOf. Definition: $familyDefinition}")

class FleksSnapshotException(reason: String) : FleksException("Cannot load snapshot: $reason!")

class FleksNoSuchInjectableException(name: String) :
    FleksException("There is no injectable with name $name registered! Make sure to define 'injectables' before your 'systems' in the WorldConfiguration.")

class FleksHookAlreadyAddedException(hookType: String, objType: String) :
    FleksException("$hookType for $objType already available!")

class FleksWrongConfigurationUsageException :
    FleksException(
        "The global functions 'inject' and 'family' must be used inside a WorldConfiguration scope." +
            "The same applies for 'compareEntityBy' and 'compareEntity' unless you specify the world parameter explicitly."
    )

class FleksWrongSystemInterfaceException(system: KClass<*>, `interface`: KClass<*>) :
    FleksException("System ${system.simpleName} cannot have interface ${`interface`.simpleName}")
