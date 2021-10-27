package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.BitArray
import com.github.quillraven.fleks.collection.EntityComparator
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
    private val entities = BitArray(1)
    var isDirty = false
        private set

    operator fun contains(cmpMask: BitArray): Boolean {
        return (allOf == null || cmpMask.contains(allOf))
            && (noneOf == null || !cmpMask.intersects(noneOf))
            && (anyOf == null || cmpMask.intersects(anyOf))
    }

    fun updateActiveEntities() {
        isDirty = false
        entities.toIntBag(activeIds)
    }

    inline fun forEach(action: (Entity) -> Unit) {
        activeIds.forEach { action(Entity(it)) }
    }

    fun sort(comparator: EntityComparator) {
        activeIds.sort(comparator)
    }

    override fun onEntityCfgChanged(entity: Entity, cmpMask: BitArray) {
        if (cmpMask in this) {
            if (!isDirty && !entities[entity.id]) {
                // new entity will be added
                isDirty = true
            }
            entities.set(entity.id)
        } else {
            if (!isDirty && entities[entity.id]) {
                // existing entity will be removed
                isDirty = true
            }
            entities.clear(entity.id)
        }
    }

    companion object {
        internal val EMPTY_FAMILY = Family()
    }
}
