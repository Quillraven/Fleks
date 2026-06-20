package com.github.quillraven.fleks.collection

/**
 * A fast, platform-independent binary search implementation for [IntArray].
 * Avoids Kotlin stdlib Multiplatform resolution inconsistencies.
 *
 * Returns the index of the [element] if found; otherwise, returning `-(insertionPoint + 1)`.
 */
fun binarySearch(array: IntArray, size: Int, element: Int): Int {
    var low = 0
    var high = size - 1

    while (low <= high) {
        val mid = (low + high) ushr 1
        val midVal = array[mid]

        if (midVal < element) {
            low = mid + 1
        } else if (midVal > element) {
            high = mid - 1
        } else {
            return mid // Element found
        }
    }
    return -(low + 1) // Element not found, returns insertion point
}
