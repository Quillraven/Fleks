package com.github.quillraven.fleks

/**
 * The World coordinator for the ECS. Tying together Entity registry,
 * Archetype storage, and the Command Buffer for deferred execution.
 */
class World(initialCapacity: Int = 1024) {
    val entityRegistry = EntityRegistry(initialCapacity)
    val archetypeRegistry = ArchetypeRegistry()
    private val commandBuffer = CommandBuffer(this)

    /**
     * Creates a new entity handle immediately.
     * Places the entity in the root (empty) archetype.
     */
    fun create(): Entity {
        val entity = entityRegistry.create()
        val index = entity.index
        val root = archetypeRegistry.root
        val row = root.addEntity(index)
        entityRegistry.setRecord(index, root.id, row)
        return entity
    }

    /**
     * Defers the destruction of the entity until the next [flush].
     */
    fun destroy(entity: Entity) {
        commandBuffer.queueDestroy(entity)
    }

    /**
     * Defers adding a component to the entity until the next [flush].
     */
    fun <T : Component> add(entity: Entity, type: ComponentType<T>, component: T) {
        commandBuffer.queueAdd(entity, type.id, component)
    }

    /**
     * Defers removing a component type from the entity until the next [flush].
     */
    fun remove(entity: Entity, type: ComponentType<*>) {
        commandBuffer.queueRemove(entity, type.id)
    }

    /**
     * Checks if the entity contains the specified component type.
     */
    fun has(entity: Entity, type: ComponentType<*>): Boolean {
        if (!entityRegistry.isAlive(entity)) return false
        val archetypeId = entityRegistry.getArchetypeId(entity)
        val archetype = archetypeRegistry.archetypes[archetypeId]
        return archetype.hasComponent(type.id)
    }

    /**
     * Retrieves the component of type [T] on the entity, or null if it does not exist.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(entity: Entity, type: ComponentType<T>): T? {
        if (!entityRegistry.isAlive(entity)) return null
        val archetypeId = entityRegistry.getArchetypeId(entity)
        val archetype = archetypeRegistry.archetypes[archetypeId]
        val colIdx = archetype.getColumnIndex(type.id)
        if (colIdx < 0) return null
        val row = entityRegistry.getRow(entity)
        return archetype.columns[colIdx][row] as T?
    }

    /**
     * Flushes all deferred commands in the command buffer.
     */
    fun flush() {
        commandBuffer.flush()
    }

    // --- Immediate Operations (Useful for initialization, scripting, and testing) ---

    /**
     * Destroys the entity immediately.
     */
    fun destroyImmediate(entity: Entity) {
        if (!entityRegistry.isAlive(entity)) return
        val index = entity.index
        val archetypeId = entityRegistry.getArchetypeId(entity)
        val row = entityRegistry.getRow(entity)

        val archetype = archetypeRegistry.archetypes[archetypeId]
        val movedEntityIndex = archetype.removeEntity(row)
        if (movedEntityIndex != -1) {
            // Update metadata for the entity that was swapped to fill the gap
            entityRegistry.setRecord(movedEntityIndex, archetypeId, row)
        }

        entityRegistry.destroy(entity)
    }

    /**
     * Adds a component immediately.
     */
    fun <T : Component> addImmediate(entity: Entity, type: ComponentType<T>, component: T) {
        addImmediateRaw(entity, type.id, component)
    }

    /**
     * Removes a component immediately.
     */
    fun removeImmediate(entity: Entity, type: ComponentType<*>) {
        removeImmediateRaw(entity, type.id)
    }

    /**
     * Internal raw component addition, avoiding any generics overhead during deferred execution.
     */
    internal fun addImmediateRaw(entity: Entity, typeId: Int, component: Any) {
        if (!entityRegistry.isAlive(entity)) return
        val index = entity.index
        val srcId = entityRegistry.getArchetypeId(entity)
        val src = archetypeRegistry.archetypes[srcId]

        // 1. If component type is already in src archetype, overwrite in-place
        val existingColIdx = src.getColumnIndex(typeId)
        if (existingColIdx >= 0) {
            val row = entityRegistry.getRow(entity)
            src.columns[existingColIdx][row] = component
            return
        }

        // 2. Resolve destination archetype (transition)
        val dst = archetypeRegistry.getTransitionAdd(src, typeId)
        val dstId = dst.id
        val srcRow = entityRegistry.getRow(entity)

        // 3. Insert entity into destination table
        val dstRow = dst.addEntity(index)

        // 4. Merge copy matching component columns
        var srcIdx = 0
        var dstIdx = 0
        val srcIds = src.componentIds
        val dstIds = dst.componentIds
        while (srcIdx < srcIds.size && dstIdx < dstIds.size) {
            val srcCompId = srcIds[srcIdx]
            val dstCompId = dstIds[dstIdx]
            if (srcCompId == dstCompId) {
                dst.columns[dstIdx][dstRow] = src.columns[srcIdx][srcRow]
                srcIdx++
                dstIdx++
            } else if (srcCompId < dstCompId) {
                srcIdx++
            } else {
                dstIdx++
            }
        }

        // 5. Write the new component column
        val newColIdx = dst.getColumnIndex(typeId)
        dst.columns[newColIdx][dstRow] = component

        // 6. Clean up source archetype using swap-and-pop
        val movedEntityIndex = src.removeEntity(srcRow)
        if (movedEntityIndex != -1) {
            entityRegistry.setRecord(movedEntityIndex, srcId, srcRow)
        }

        // 7. Update registry record
        entityRegistry.setRecord(index, dstId, dstRow)
    }

    /**
     * Internal raw component removal, avoiding any generics overhead during deferred execution.
     */
    internal fun removeImmediateRaw(entity: Entity, typeId: Int) {
        if (!entityRegistry.isAlive(entity)) return
        val index = entity.index
        val srcId = entityRegistry.getArchetypeId(entity)
        val src = archetypeRegistry.archetypes[srcId]

        // 1. If component type is not in the source, there's nothing to remove
        if (!src.hasComponent(typeId)) return

        // 2. Resolve destination archetype (transition)
        val dst = archetypeRegistry.getTransitionRemove(src, typeId)
        val dstId = dst.id
        val srcRow = entityRegistry.getRow(entity)

        // 3. Insert entity into destination table
        val dstRow = dst.addEntity(index)

        // 4. Merge copy matching component columns
        var srcIdx = 0
        var dstIdx = 0
        val srcIds = src.componentIds
        val dstIds = dst.componentIds
        while (srcIdx < srcIds.size && dstIdx < dstIds.size) {
            val srcCompId = srcIds[srcIdx]
            val dstCompId = dstIds[dstIdx]
            if (srcCompId == dstCompId) {
                dst.columns[dstIdx][dstRow] = src.columns[srcIdx][srcRow]
                srcIdx++
                dstIdx++
            } else if (srcCompId < dstCompId) {
                srcIdx++
            } else {
                dstIdx++
            }
        }

        // 5. Clean up source archetype using swap-and-pop
        val movedEntityIndex = src.removeEntity(srcRow)
        if (movedEntityIndex != -1) {
            entityRegistry.setRecord(movedEntityIndex, srcId, srcRow)
        }

        // 6. Update registry record
        entityRegistry.setRecord(index, dstId, dstRow)
    }
}
