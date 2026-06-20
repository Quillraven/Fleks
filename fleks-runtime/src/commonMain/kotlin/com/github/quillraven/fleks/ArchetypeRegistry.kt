package com.github.quillraven.fleks

import com.github.quillraven.fleks.collection.Bag
import com.github.quillraven.fleks.collection.binarySearch

/**
 * Key for archetype mapping. Wraps a sorted [IntArray] of component type IDs.
 */
private class ComponentIdSet(val ids: IntArray) {
    private val hash = ids.contentHashCode()
    override fun hashCode(): Int = hash
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ComponentIdSet) return false
        return this.ids.contentEquals(other.ids)
    }
}

/**
 * Manages the collection of all Archetypes and lazy transitions between them.
 */
class ArchetypeRegistry(initialCapacity: Int = 16) {
    private var nextArchetypeId = 0

    // Fast O(1) lookup of archetype by ID
    val archetypes = Bag<Archetype>(initialCapacity)

    // Map component IDs combination to existing archetypes
    private val archetypeMap = HashMap<ComponentIdSet, Archetype>()

    val root: Archetype

    init {
        // Create root archetype representing the empty component set
        root = Archetype(nextArchetypeId++, IntArray(0))
        archetypes.add(root)
        archetypeMap[ComponentIdSet(root.componentIds)] = root
    }

    /**
     * Finds an archetype with the exact set of component type IDs, or creates one if it doesn't exist.
     * The input [componentIds] array MUST be sorted.
     */
    fun getOrCreate(componentIds: IntArray): Archetype {
        val key = ComponentIdSet(componentIds)
        var archetype = archetypeMap[key]
        if (archetype == null) {
            archetype = Archetype(nextArchetypeId++, componentIds)
            archetypes.add(archetype)
            archetypeMap[key] = archetype
        }
        return archetype
    }

    /**
     * Resolves the target archetype when adding a component to an entity in [src].
     * Caches transitions in the graph to ensure subsequent lookups are O(1).
     */
    fun getTransitionAdd(src: Archetype, componentId: Int): Archetype {
        var dst = src.addTransitions[componentId]
        if (dst == null) {
            val nextIds = merge(src.componentIds, componentId)
            dst = getOrCreate(nextIds)
            src.addTransitions[componentId] = dst
            dst.removeTransitions[componentId] = src
        }
        return dst
    }

    /**
     * Resolves the target archetype when removing a component from an entity in [src].
     * Caches transitions in the graph to ensure subsequent lookups are O(1).
     */
    fun getTransitionRemove(src: Archetype, componentId: Int): Archetype {
        var dst = src.removeTransitions[componentId]
        if (dst == null) {
            val nextIds = filter(src.componentIds, componentId)
            dst = getOrCreate(nextIds)
            src.removeTransitions[componentId] = dst
            dst.addTransitions[componentId] = src
        }
        return dst
    }

    private fun merge(ids: IntArray, newId: Int): IntArray {
        val idx = binarySearch(ids, ids.size, newId)
        if (idx >= 0) return ids // Component already present
        val result = IntArray(ids.size + 1)
        val insertIdx = -idx - 1
        ids.copyInto(result, 0, 0, insertIdx)
        result[insertIdx] = newId
        ids.copyInto(result, insertIdx + 1, insertIdx, ids.size)
        return result
    }

    private fun filter(ids: IntArray, removeId: Int): IntArray {
        val idx = binarySearch(ids, ids.size, removeId)
        if (idx < 0) return ids // Component not present
        val result = IntArray(ids.size - 1)
        ids.copyInto(result, 0, 0, idx)
        ids.copyInto(result, idx, idx + 1, ids.size)
        return result
    }
}
