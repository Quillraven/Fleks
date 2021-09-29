package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class AllOf(val components: Array<KClass<*>> = [])

@Target(AnnotationTarget.CLASS)
annotation class NoneOf(val components: Array<KClass<*>> = [])

@Target(AnnotationTarget.CLASS)
annotation class AnyOf(val components: Array<KClass<*>> = [])

data class Family(
    internal val allOf: BitArray? = null,
    internal val noneOf: BitArray? = null,
    internal val anyOf: BitArray? = null
) : EntityListener {
    @PublishedApi
    internal var activeIds = IntArray(0)
    private val entities = mutableSetOf<Int>()
    var isDirty = false
        private set

    operator fun contains(cmpMask: BitArray): Boolean {
        if (allOf != null && !cmpMask.contains(allOf)) {
            return false
        }

        return (noneOf == null || !cmpMask.intersects(noneOf))
            && (anyOf == null || cmpMask.intersects(anyOf))
    }

    fun updateActiveEntities() {
        isDirty = false
        activeIds = entities.toIntArray()
    }

    inline fun forEach(action: (Int) -> Unit) {
        activeIds.forEach(action)
    }

    override fun onEntityCfgChanged(entityId: Int, cmpMask: BitArray) {
        isDirty = true
        if (cmpMask in this) {
            entities.add(entityId)
        } else {
            entities.remove(entityId)
        }
    }

    companion object {
        internal val EMPTY_FAMILY = Family()
    }
}
