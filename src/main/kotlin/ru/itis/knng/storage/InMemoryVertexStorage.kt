package ru.itis.knng.storage

import ru.itis.knng.graph.NeighborsHeap
import ru.itis.knng.models.Vertex

class InMemoryVertexStorage<V> : VertexStorage<V> {

    private val idToVertex = hashMapOf<String, Vertex<V>>()
    private val knnHeaps: HashMap<Vertex<V>, NeighborsHeap<V>> = hashMapOf()
    private val reverseKnnHeap: HashMap<Vertex<V>, MutableList<Vertex<V>>> = hashMapOf()

    override fun getNeighborsById(id: String): List<Pair<String, V>> {
        val vertex = idToVertex[id] ?: error("There is no element with id: $id")
        return knnHeaps[vertex]?.toList()?.map { it.id to it.element }
            ?: error("Unknown error occurred while getting neighbors for id: $id")
    }

    override fun put(vertex: Vertex<V>) {
        TODO("Not yet implemented")
    }

    override fun delete(vertex: Vertex<V>) {
        TODO("Not yet implemented")
    }

    override fun offerNeighbor(vertex: Vertex<V>, neighbor: Vertex<V>): Boolean {
        val heap = knnHeaps[vertex] ?: error("There is no vertex: $vertex")
        val deletedElement = heap.offer(neighbor) ?: return false
        if (reverseKnnHeap.containsKey(neighbor)) {
            reverseKnnHeap[neighbor]?.add(heap.headElement) ?: error("No reverse-heap for vertex: $neighbor")
        } else {
            reverseKnnHeap[neighbor] = mutableListOf(neighbor)
        }
        reverseKnnHeap[deletedElement]?.remove(heap.headElement) ?: error("No reverse-heap for vertex: $deletedElement")
        return true
    }
}