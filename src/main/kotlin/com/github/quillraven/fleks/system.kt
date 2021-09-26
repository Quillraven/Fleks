package com.github.quillraven.fleks

import kotlin.reflect.KClass

abstract class EntitySystem(var enabled: Boolean = true) {
    abstract fun update()
}

class SystemService(
    systemTypes: List<KClass<out EntitySystem>>,
    injectables: Map<KClass<*>, Any>
) {
    private val systems: Array<EntitySystem>

    init {
        systems = Array(systemTypes.size) { sysIdx ->
            val sysType = systemTypes[sysIdx]

            val constructors = sysType.java.declaredConstructors
            if (constructors.size > 1) {
                // sort constructors by amount of parameters
                // constructor with the least amount of parameters is used for creation
                constructors.sortBy { it.parameters.size }
            }

            val cstr = constructors.first()
            val params = cstr.parameters
            if (params.isEmpty()) {
                return@Array cstr.newInstance() as EntitySystem
            } else {
                // TODO how to support Kotlin's default constructor value assignments?
                val missingTypes = mutableListOf<String>()

                val args = Array(params.size) { paramIdx ->
                    val param = params[paramIdx]
                    val arg = injectables[param.type.kotlin]
                    if (arg == null) {
                        missingTypes.add(param.type.simpleName)
                    }
                    arg
                }

                if (missingTypes.isEmpty()) {
                    return@Array cstr.newInstance(*args) as EntitySystem
                }

                throw FleksSystemCreationException(
                    sysType,
                    "Missing injectables of type (${missingTypes.joinToString()}) for constructor with parameters (${params.joinToString { it.type.simpleName }})."
                )
            }
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
