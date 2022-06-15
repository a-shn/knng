package ru.itis.knng.graph

import com.google.common.collect.MinMaxPriorityQueue
import ru.itis.knng.models.Vertex
import ru.itis.knng.oracles.DistanceOracle

class NeighborsHeapFactory<V>(
    private val maxSize: Int,
    private val distanceOracle: DistanceOracle<V>,
) {

    fun createNeighborsHeap(headElement: Vertex<V>): NeighborsHeap<V> {
        return NeighborsHeap(headElement, maxSize, distanceOracle)
    }

    fun createNeighborsHeap(headElement: Vertex<V>, size: Int): NeighborsHeap<V> {
        return NeighborsHeap(headElement, size, distanceOracle)
    }
}

class NeighborsHeap<V>(
    val headElement: Vertex<V>,
    private val maxSize: Int,
    private val distanceOracle: DistanceOracle<V>,
) {

    private val queue: MinMaxPriorityQueue<Vertex<V>> = MinMaxPriorityQueue
        .orderedBy(compareBy { v: Vertex<V> -> distanceOracle.distanceOf(headElement.element, v.element) })
        .maximumSize(maxSize)
        .create()

    val neighbors: List<Vertex<V>>
        get() {
            return queue.toList()
        }

    fun offer(vertex: Vertex<V>): Vertex<V>? {
        if (headElement == vertex) {
            return null
        }
        if (queue.contains(vertex)) {
            return null
        }
        val last = queue.peekLast()
        if (last == null || queue.size < maxSize || distanceOracle.distanceOf(headElement.element, last.element)
            > distanceOracle.distanceOf(headElement.element, vertex.element)
        ) {
            val added = queue.offer(vertex)
            if (added) {
                return last
            }
        }
        return null
    }

    fun offerAll(vertices: List<Vertex<V>>): List<Vertex<V>> {
        val deletedElements = mutableListOf<Vertex<V>>()
        for (vertex in vertices) {
            val addedElement = offer(vertex)
            if (addedElement != null) {
                deletedElements.add(addedElement)
            }
        }
        return deletedElements
    }

    fun remove(vertex: Vertex<V>): Vertex<V>? {
        if (queue.remove(vertex)) {
            return vertex
        }
        return null
    }

    fun toList(): List<Vertex<V>> {
        return queue.toList()
    }
}