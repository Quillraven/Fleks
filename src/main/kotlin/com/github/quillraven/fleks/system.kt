package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

abstract class IntervalSystem(
    private val tickRate: Float = 0f,
    var enabled: Boolean = true
) {
    private var accumulator: Float = 0.0f

    open fun update(deltaTime: Float) {
        if (tickRate == 0f) {
            // no tick rate specified -> call every frame
            onTick(deltaTime)
            return
        }

        accumulator += deltaTime
        while (accumulator >= tickRate) {
            onTick(tickRate)
            accumulator -= tickRate
        }

        onAlpha(accumulator / tickRate)
    }

    abstract fun onTick(deltaTime: Float)

    open fun onAlpha(alpha: Float) = Unit
}

abstract class IteratingSystem(
    tickRate: Float = 0f,
    enabled: Boolean = true,
    private val family: Family = Family.EMPTY_FAMILY
) : IntervalSystem(tickRate, enabled) {
    lateinit var world: World

    override fun update(deltaTime: Float) {
        if (family.isDirty) {
            family.updateActiveEntities()
        }
        super.update(deltaTime)
    }

    override fun onTick(deltaTime: Float) {
        family.forEach { onEntityAction(it, deltaTime) }
    }

    abstract fun onEntityAction(entity: Entity, deltaTime: Float)
}

class SystemService(
    world: World,
    systemTypes: List<KClass<out IntervalSystem>>,
    injectables: MutableMap<KClass<*>, Any>,
    entityService: EntityService = world.entityService,
    cmpService: ComponentService = world.componentService
) {
    private val systems: Array<IntervalSystem>

    init {
        val allFamilies = mutableListOf<Family>()
        systems = Array(systemTypes.size) { sysIdx ->
            val sysType = systemTypes[sysIdx]

            val primaryConstructor = sysType.primaryConstructor ?: throw FleksSystemCreationException(
                sysType,
                "No primary constructor found"
            )
            val args = systemArgs(primaryConstructor, cmpService, injectables, sysType)

            if (sysType.isSubclassOf(IteratingSystem::class)) {
                val family = family(sysType, entityService, cmpService, allFamilies)
                val newSystem = primaryConstructor.callBy(args) as IteratingSystem

                val famField = field(newSystem, "family")
                famField.isAccessible = true
                famField.set(newSystem, family)

                val worldField = field(newSystem, "world")
                worldField.isAccessible = true
                worldField.set(newSystem, world)

                return@Array newSystem
            }

            return@Array primaryConstructor.callBy(args)
        }
    }

    private fun systemArgs(
        primaryConstructor: KFunction<IntervalSystem>,
        cmpService: ComponentService,
        injectables: MutableMap<KClass<*>, Any>,
        sysType: KClass<out IntervalSystem>
    ): Map<KParameter, Any?> {
        val args = primaryConstructor.parameters
            // filter out default value assignments in the constructor
            .filterNot { it.isOptional }
            // for any non-default value parameter assign the value of the injectables map
            // or a ComponentMapper of the ComponentService
            .associateWith {
                val paramClass = it.type.classifier as KClass<*>
                if (paramClass == ComponentMapper::class) {
                    val cmpClazz = it.type.arguments[0].type?.classifier as KClass<*>
                    cmpService.mapper(cmpClazz)
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

        return args
    }

    private fun family(
        sysType: KClass<out IntervalSystem>,
        entityService: EntityService,
        cmpService: ComponentService,
        allFamilies: MutableList<Family>
    ): Family {
        val allOfAnn = sysType.findAnnotation<AllOf>()
        val allOfCmps = if (allOfAnn != null && allOfAnn.components.isNotEmpty()) {
            allOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        val noneOfAnn = sysType.findAnnotation<NoneOf>()
        val noneOfCmps = if (noneOfAnn != null && noneOfAnn.components.isNotEmpty()) {
            noneOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        val anyOfAnn = sysType.findAnnotation<AnyOf>()
        val anyOfCmps = if (anyOfAnn != null && anyOfAnn.components.isNotEmpty()) {
            anyOfAnn.components.map { cmpService.mapper(it) }
        } else {
            null
        }

        if ((allOfCmps == null || allOfCmps.isEmpty())
            && (noneOfCmps == null || noneOfCmps.isEmpty())
            && (anyOfCmps == null || anyOfCmps.isEmpty())
        ) {
            throw FleksSystemCreationException(
                sysType,
                "IteratingSystem must define at least one of AllOf, NoneOf or AnyOf"
            )
        }

        val allBs = if (allOfCmps == null) null else BitArray().apply { allOfCmps.forEach { this.set(it.id) } }
        val noneBs = if (noneOfCmps == null) null else BitArray().apply { noneOfCmps.forEach { this.set(it.id) } }
        val anyBs = if (anyOfCmps == null) null else BitArray().apply { anyOfCmps.forEach { this.set(it.id) } }

        var family = allFamilies.find { it.allOf == allBs && it.noneOf == noneBs && it.anyOf == anyBs }
        if (family == null) {
            family = Family(allBs, noneBs, anyBs)
            entityService.addEntityListener(family)
            allFamilies.add(family)
        }
        return family
    }

    private fun field(system: IteratingSystem, fieldName: String): Field {
        var sysClass: Class<*> = system::class.java
        var classField: Field? = null
        while (classField == null) {
            try {
                classField = sysClass.getDeclaredField(fieldName)
            } catch (e: NoSuchFieldException) {
                sysClass = sysClass.superclass
                if (sysClass == null) {
                    throw FleksSystemCreationException(system::class, "No '$fieldName' field found")
                }
            }

        }
        return classField
    }

    fun update(deltaTime: Float) {
        systems.forEach { system ->
            if (system.enabled) {
                system.update(deltaTime)
            }
        }
    }
}
