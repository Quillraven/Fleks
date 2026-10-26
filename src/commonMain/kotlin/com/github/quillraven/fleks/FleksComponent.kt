package com.github.quillraven.fleks

/**
 * Marks a class as a Fleks [Component]. It requires the Fleks compiler plugin to be applied.
 *
 * The compiler plugin will:
 * 1. make the class implement [Component] of its own type,
 * 2. add a companion object that is a [ComponentType] if the class does not have one yet, and
 * 3. generate the [Component.type] function that returns the companion object.
 *
 * Example:
 * ```kotlin
 *     @FleksComponent
 *     data class Position(var x: Float = 0f, var y: Float = 0f)
 * ```
 *
 * It is equivalent to:
 * ```kotlin
 *     data class Position(var x: Float = 0f, var y: Float = 0f) : Component<Position> {
 *         override fun type(): ComponentType<Position> = Position
 *
 *         companion object : ComponentType<Position>()
 *     }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class FleksComponent
