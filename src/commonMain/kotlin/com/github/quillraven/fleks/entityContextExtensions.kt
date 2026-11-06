package com.github.quillraven.fleks

/*
 * Context-parameter counterparts of the [EntityComponentContext] extensions.
 * They allow helper functions outside a system to use the same entity syntax by requiring an
 * [EntityComponentContext] (e.g. a [World], [IntervalSystem], [EntityCreateContext] or [EntityUpdateContext]):
 *
 * ```
 * context(ctx: EntityComponentContext)
 * fun Entity.moveBy(dx: Float) {
 *     this[Position].x += dx
 * }
 * ```
 *
 * Inside an [EntityComponentContext] the member extensions take precedence, so there is no ambiguity.
 */

/** @see EntityComponentContext.get */
context(ctx: EntityComponentContext)
inline operator fun <reified T : Component<*>> Entity.get(type: ComponentType<T>): T =
    with(ctx) { this@get[type] }

/** @see EntityComponentContext.getOrNull */
context(ctx: EntityComponentContext)
inline fun <reified T : Component<*>> Entity.getOrNull(type: ComponentType<T>): T? =
    with(ctx) { this@getOrNull.getOrNull(type) }

/** @see EntityComponentContext.contains */
context(ctx: EntityComponentContext)
operator fun Entity.contains(type: UniqueId<*>): Boolean =
    with(ctx) { type in this@contains }

/** @see EntityComponentContext.has */
context(ctx: EntityComponentContext)
infix fun Entity.has(type: UniqueId<*>): Boolean =
    with(ctx) { this@has has type }

/** @see EntityComponentContext.hasNo */
context(ctx: EntityComponentContext)
infix fun Entity.hasNo(type: UniqueId<*>): Boolean =
    with(ctx) { this@hasNo hasNo type }

/** @see EntityComponentContext.configure */
context(ctx: EntityComponentContext)
inline fun Entity.configure(configuration: EntityUpdateContext.(Entity) -> Unit) =
    with(ctx) { this@configure.configure(configuration) }

/** @see EntityComponentContext.remove */
context(ctx: EntityComponentContext)
fun Entity.remove() = with(ctx) { this@remove.remove() }

/** @see EntityComponentContext.isMarkedForRemoval */
context(ctx: EntityComponentContext)
fun Entity.isMarkedForRemoval() = with(ctx) { this@isMarkedForRemoval.isMarkedForRemoval() }

/** @see EntityComponentContext.wasRemoved */
context(ctx: EntityComponentContext)
fun Entity.wasRemoved() = with(ctx) { this@wasRemoved.wasRemoved() }

/** @see EntityComponentContext.getRef */
context(ctx: EntityComponentContext)
fun Entity.getRef(): EntityRef = with(ctx) { this@getRef.getRef() }

/** @see EntityComponentContext.get */
context(ctx: EntityComponentContext)
inline operator fun <reified T : Component<*>> EntityRef.get(type: ComponentType<T>): T =
    with(ctx) { this@get[type] }

/** @see EntityComponentContext.getOrNull */
context(ctx: EntityComponentContext)
inline fun <reified T : Component<*>> EntityRef.getOrNull(type: ComponentType<T>): T? =
    with(ctx) { this@getOrNull.getOrNull(type) }

/** @see EntityComponentContext.contains */
context(ctx: EntityComponentContext)
operator fun EntityRef.contains(type: UniqueId<*>): Boolean =
    with(ctx) { type in this@contains }

/** @see EntityComponentContext.has */
context(ctx: EntityComponentContext)
infix fun EntityRef.has(type: UniqueId<*>): Boolean =
    with(ctx) { this@has has type }

/** @see EntityComponentContext.hasNo */
context(ctx: EntityComponentContext)
infix fun EntityRef.hasNo(type: UniqueId<*>): Boolean =
    with(ctx) { this@hasNo hasNo type }

/** @see EntityComponentContext.configure */
context(ctx: EntityComponentContext)
inline fun EntityRef.configure(configuration: EntityUpdateContext.(Entity) -> Unit) =
    with(ctx) { this@configure.configure(configuration) }

/** @see EntityComponentContext.remove */
context(ctx: EntityComponentContext)
fun EntityRef.remove() = with(ctx) { this@remove.remove() }
