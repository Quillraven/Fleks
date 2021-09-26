package com.github.quillraven.fleks

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

abstract class EntitySystem(var enabled: Boolean = true) {
    abstract fun update()
}

class SystemService(
    systemTypes: List<KClass<out EntitySystem>>,
    injectables: MutableMap<KClass<*>, Any>,
    cmpService: ComponentService
) {
    private val systems: Array<EntitySystem>

    init {
        systems = Array(systemTypes.size) { sysIdx ->
            val sysType = systemTypes[sysIdx]

            val primaryConstructor = sysType.primaryConstructor ?: throw FleksSystemCreationException(
                sysType,
                "No primary constructor found"
            )

            val args = primaryConstructor.parameters
                // filter out default value assignments in the constructor
                .filterNot { it.isOptional }
                // for any non-default value parameter assign the value of the injectables map
                // or a ComponentMapper of the ComponentService
                .associateWith {
                    val paramClass = it.type.classifier as KClass<*>
                    if (paramClass == ComponentMapper::class) {
                        val cmpClazz = it.type.arguments[0].type?.classifier as KClass<*>
                        cmpService.mappers[cmpClazz]
                    } else {
                        injectables[it.type.classifier]
                    }
                }


            val missingInjectables = args
                .filter { !it.key.type.isMarkedNullable && it.value == null }
                .map { it.key.type.classifier }
            if (missingInjectables.isNotEmpty()) {
                throw FleksSystemCreationException(
                    sysType,
                    "Missing injectables of type $missingInjectables"
                )
            }

            primaryConstructor.callBy(args)
        }
    }

    fun update() {
        systems.forEach { system ->
            if (system.enabled) {
                system.update()
            }
        }
    }
}
