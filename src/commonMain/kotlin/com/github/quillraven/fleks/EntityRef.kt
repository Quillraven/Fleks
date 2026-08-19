package com.github.quillraven.fleks

/**
 * A stable reference to an [Entity] that can be stored in components.
 * Unlike raw [Entity] values, an [EntityRef] becomes invalid when the referenced
 * [entity][Entity] is removed from the [World], allowing safe detection of stale references.
 */
class EntityRef internal constructor(
    val entity: Entity,
) {
    var valid: Boolean = true
        internal set

    val id: Int get() = entity.id

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EntityRef

        if (valid != other.valid) return false
        if (entity != other.entity) return false

        return true
    }

    override fun hashCode(): Int {
        var result = valid.hashCode()
        result = 31 * result + entity.hashCode()
        return result
    }

    override fun toString(): String {
        return "EntityRef(entity=$entity, valid=$valid)"
    }

    companion object {
        val NONE = EntityRef(Entity.NONE).also { it.valid = false }

        /**
         * Returns true if and only if the given [EntityRef] is not null and references a valid entity in the world.
         */
        fun EntityRef?.isValid(): Boolean = this?.valid ?: false

        /**
         * Returns true if and only if the given [EntityRef] is null or references an entity that is not part of the world.
         */
        fun EntityRef?.isNotValid(): Boolean = this == null || !this.valid
    }
}
