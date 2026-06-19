package com.github.quillraven.fleks

/**
 * Marks a class or data class as a Fleks component.
 *
 * The Fleks compiler plugin will:
 *   1. Make the class implement [Component].
 *   2. Inject a `companion object : ComponentType<T>()` if one does not exist.
 *
 * Example:
 * ```kotlin
 * @Component
 * data class Position(var x: Float = 0f, var y: Float = 0f)
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class FleksComponent
