package com.github.quillraven.fleks

import kotlin.jvm.JvmInline

/**
 * A lightweight entity handle.
 *
 * The backing [Long] packs two fields:
 *   - bits  0–31 : entity index (stable slot in the entity registry)
 *   - bits 32–63 : version     (generation counter, incremented on recycle)
 *
 * Using a value class keeps entities as raw `long` primitives in all
 * non-generic, non-nullable call sites — no heap allocation.
 *
 * Internal arrays store entities as [LongArray] to stay unboxed.
 */
@JvmInline
value class Entity(val id: Long) {

    /** The slot index — stable across recycles of this slot. */
    inline val index: Int get() = (id and INDEX_MASK).toInt()

    /** The generation counter — incremented each time this slot is recycled. */
    inline val version: Int get() = (id ushr VERSION_SHIFT).toInt()

    override fun toString(): String = "Entity(index=$index, version=$version)"

    companion object {
        const val INDEX_MASK: Long = 0x00000000FFFFFFFFL
        const val VERSION_SHIFT: Int = 32

        /** Pack an index and version into a single [Long]. */
        fun of(index: Int, version: Int): Entity =
            Entity((version.toLong() shl VERSION_SHIFT) or (index.toLong() and INDEX_MASK))

        /** Sentinel value representing "no entity" / null entity. */
        val NONE: Entity = of(-1, 0)
    }
}
