package com.github.quillraven.fleks.collection

import com.github.quillraven.fleks.Entity

class EntityBagIterable(private val values: MutableEntityBag) : MutableIterable<Entity> {

    private lateinit var iterator1: MutableEntityBagIterator
    private lateinit var iterator2: MutableEntityBagIterator

    override fun iterator(): MutableIterator<Entity> {
        if (!::iterator1.isInitialized) {
            iterator1 = MutableEntityBagIterator(values)
            iterator2 = MutableEntityBagIterator(values)
        }
        if (!iterator2.valid) {
            iterator2.reset()
            iterator2.valid = true
            iterator1.valid = false
            return iterator2
        }
        iterator1.reset()
        iterator1.valid = true
        iterator2.valid = false

        return iterator1
    }

}

class MutableEntityBagIterator(val values: MutableEntityBag) : MutableIterator<Entity> {

    var index: Int = 0
    internal var valid: Boolean = true

    override fun hasNext(): Boolean {
        if (!valid) {
            throw RuntimeException("#iterator() cannot be used nested.")
        }
        return index < values.size
    }

    override fun next(): Entity {
        if (index >= values.size) throw NoSuchElementException(index.toString())
        if (!valid) {
            throw RuntimeException("#iterator() cannot be used nested.")
        }
        return values[index++]
    }

    override fun remove() {
        index--
        values.removeAt(index)
    }

    fun reset() {
        index = 0
    }
}
