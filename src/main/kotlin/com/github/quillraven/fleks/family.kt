package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.IntBag
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
    internal var activeIds = IntBag()
    private val entities = BitArray()
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
        activeIds.ensureCapacity(entities.length())
        activeIds.clear()
        var i = entities.nextSetBit(0)
        while (i >= 0) {
            if (i == Int.MAX_VALUE) {
                break // or (i+1) would overflow
            }
            activeIds.add(i)
            i = entities.nextSetBit(i + 1)
        }
    }

    inline fun forEach(action: (Int) -> Unit) {
        activeIds.forEach(action)
    }

    override fun onEntityCfgChanged(entityId: Int, cmpMask: BitArray) {
        isDirty = true
        if (cmpMask in this) {
            entities.set(entityId)
        } else {
            entities.clear(entityId)
        }
    }

    companion object {
        internal val EMPTY_FAMILY = Family()
    }
}
