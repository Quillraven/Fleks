package com.github.quillraven.fleks

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Marker interface injected by the compiler plugin into every [@FleksComponent][FleksComponent]-annotated class.
 *
 * Gives the ECS a common supertype for all components without any user boilerplate.
 * The companion object (a [ComponentType]) carries the unique integer ID for the type.
 */
interface Component

/**
 * Holds a unique, compact integer ID for a component type.
 *
 * The ID is assigned once — when the companion object's class initializer first runs —
 * via a global atomic counter. IDs are stable for the lifetime of the process and
 * dense enough to serve as direct array indices.
 *
 * The compiler plugin generates:
 * ```kotlin
 * @Component
 * data class Position(...) : FleksComponent {
 *     companion object : ComponentType<Position>()   // ← injected
 * }
 * ```
 * so users can reference the type via `Position` (the companion) wherever a
 * `ComponentType<Position>` is expected.
 */
@OptIn(ExperimentalAtomicApi::class)
abstract class ComponentType<T : Component> {

    /** Unique, dense integer ID for this component type. Used as an array index. */
    val id: Int = nextId.fetchAndAdd(1)

    companion object {
        private val nextId = AtomicInt(0)

        /** Total number of [ComponentType] instances created so far. */
        val count: Int get() = nextId.load()
    }
}
