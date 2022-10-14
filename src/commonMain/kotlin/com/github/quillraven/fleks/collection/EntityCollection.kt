package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

interface EntityCollection {
    val size: Int

    operator fun contains(entity: Entity): Boolean

    fun containsAll(entities: Collection<Entity>): Boolean

    fun containsAll(entities: EntityBag): Boolean

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean

    fun all(predicate: (Entity) -> Boolean): Boolean

    fun any(predicate: (Entity) -> Boolean): Boolean

    fun none(predicate: (Entity) -> Boolean): Boolean

    fun <K, V> associate(transform: (Entity) -> Pair<K, V>): Map<K, V>

    fun <K> associateBy(
        keySelector: (Entity) -> K
    ): Map<K, Entity>

    fun <K, V> associateBy(
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): Map<K, V>

    fun <K, V, M : MutableMap<in K, in V>> associateTo(
        destination: M,
        transform: (Entity) -> Pair<K, V>
    ): M

    fun <K, M : MutableMap<in K, Entity>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K
    ): M

    fun <K, V, M : MutableMap<in K, in V>> associateByTo(
        destination: M,
        keySelector: (Entity) -> K,
        valueTransform: (Entity) -> V
    ): M

    fun count(): Int

    fun count(predicate: (Entity) -> Boolean): Int

    fun filter(predicate: (Entity) -> Boolean): List<Entity>

    fun filterNot(predicate: (Entity) -> Boolean): List<Entity>

    fun filterIndexed(predicate: (index: Int, Entity) -> Boolean): List<Entity>

    fun <C : MutableCollection<Entity>> filterTo(
        destination: C,
        predicate: (Entity) -> Boolean
    ): C

    fun <C : MutableCollection<Entity>> filterNotTo(
        destination: C, predicate: (Entity) -> Boolean
    ): C

    fun <C : MutableCollection<Entity>> filterIndexedTo(
        destination: C, predicate: (index: Int, Entity) -> Boolean
    ): C

    fun find(predicate: (Entity) -> Boolean): Entity?

    fun first(): Entity

    fun first(predicate: (Entity) -> Boolean): Entity

    fun firstOrNull(): Entity?

    fun firstOrNull(predicate: (Entity) -> Boolean): Entity?

    fun <R> fold(
        initial: R,
        operation: (acc: R, Entity) -> R
    ): R

    fun <R> foldIndexed(
        initial: R,
        operation: (index: Int, acc: R, Entity) -> R
    ): R

    fun forEach(action: (Entity) -> Unit)

    fun forEachIndexed(action: (index: Int, Entity) -> Unit)

    fun <R> map(transform: (Entity) -> R): List<R>

    fun <R> mapIndexed(transform: (index: Int, Entity) -> R): List<R>

    fun <R, C : MutableCollection<in R>> mapTo(
        destination: C,
        transform: (Entity) -> R
    ): C

    fun <R, C : MutableCollection<in R>> mapIndexedTo(
        destination: C,
        transform: (index: Int, Entity) -> R
    ): C

    fun random(): Entity

    fun randomOrNull(): Entity?

    fun take(n: Int): List<Entity>
}
