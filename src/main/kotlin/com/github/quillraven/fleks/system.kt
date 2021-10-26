package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

sealed interface Interval
object EachFrame : Interval
data class Fixed(val step: Float) : Interval

abstract class IntervalSystem(
    val interval: Interval = EachFrame,
    var enabled: Boolean = true
) {
    lateinit var world: World
        private set
    private var accumulator: Float = 0.0f
    val deltaTime: Float
        get() = if (interval is Fixed) interval.step else world.deltaTime

    open fun update() {
        when (interval) {
            is EachFrame -> onTick()
            is Fixed -> {
                accumulator += world.deltaTime
                val stepRate = interval.step
                while (accumulator >= stepRate) {
                    onTick()
                    accumulator -= stepRate
                }

                onAlpha(accumulator / stepRate)
            }
        }
    }

    abstract fun onTick()

    open fun onAlpha(alpha: Float) = Unit
}

abstract class IteratingSystem(
    interval: Interval = EachFrame,
    enabled: Boolean = true,
    private val family: Family = Family.EMPTY_FAMILY
) : IntervalSystem(interval, enabled) {
    @PublishedApi
    internal lateinit var entityService: EntityService

    inline fun configureEntity(entity: Entity, cfg: EntityUpdateCfg.(Entity) -> Unit) {
        entityService.configureEntity(entity, cfg)
    }

    override fun update() {
        if (family.isDirty) {
            family.updateActiveEntities()
        }
        super.update()
    }

    override fun onTick() {
        family.forEach { onTickEntity(it) }
    }

    abstract fun onTickEntity(entity: Entity)

    override fun onAlpha(alpha: Float) {
        family.forEach { onAlphaEntity(it, alpha) }
    }

    open fun onAlphaEntity(entity: Entity, alpha: Float) = Unit
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
            val newSystem = primaryConstructor.callBy(args)

            val worldField = field(newSystem, "world")
            worldField.isAccessible = true
            worldField.set(newSystem, world)

            if (sysType.isSubclassOf(IteratingSystem::class)) {
                val family = family(sysType, entityService, cmpService, allFamilies)
                val famField = field(newSystem, "family")
                famField.isAccessible = true
                famField.set(newSystem, family)

                val eServiceField = field(newSystem, "entityService")
                eServiceField.isAccessible = true
                eServiceField.set(newSystem, entityService)
            }

            newSystem
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

    private fun field(system: IntervalSystem, fieldName: String): Field {
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

    fun update() {
        systems.forEach { system ->
            if (system.enabled) {
                system.update()
            }
        }
    }
}
