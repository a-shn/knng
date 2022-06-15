package ru.itis.knng.graph

import ru.itis.knng.models.Vertex
import ru.itis.knng.oracles.DistanceOracle
import java.util.*
import kotlin.collections.HashMap
import kotlin.random.Random

class ApproximatedKNNGraph<V>(
    private val k: Int,
    distanceOracle: DistanceOracle<V>,
    elements: List<Pair<String,V>>
) : KNNGraph<V> {
    private val vertices: MutableList<Vertex<V>> = elements.map { Vertex(it.first, it.second) }.toMutableList()
    private val idToVertex = hashMapOf<String,Vertex<V>>()
    private val knnHeaps: HashMap<Vertex<V>, NeighborsHeap<V>> = hashMapOf()
    private val reverseKnnHeap: HashMap<Vertex<V>, MutableList<Vertex<V>>> = hashMapOf()
    private val neighborsHeapFactory: NeighborsHeapFactory<V> = NeighborsHeapFactory(k, distanceOracle)
    private val ITERATIONS_NUMBER = 20

    init {
        generateGraphFromList(vertices)
    }

    override fun getNeighbors(id: String): List<Pair<String,V>> {
        val vertex = idToVertex[id] ?: error("There is no element with id: $id")
        return knnHeaps[vertex]?.toList()?.map { it.id to it.element }
            ?: error("Unknown error occurred while getting neighbors for id: $id")
    }

    override fun findNeighbors(query: V): List<Pair<String, V>> {
        val vertex = Vertex(UUID.randomUUID().toString(), query)
        val randomVertices = sampleK(vertex, k)
        val newKnnHeap = neighborsHeapFactory.createNeighborsHeap(vertex, k)
        newKnnHeap.offerAll(randomVertices)
        var iteration = ITERATIONS_NUMBER
        while (iteration != 0 && makeQueryIterationWithoutTracking(newKnnHeap, vertex) != 0) {
            iteration--
        }
        return newKnnHeap.toList().map { it.id to it.element }
    }

    override fun put(id: String, element: V) {
        if (!idToVertex.containsKey(id)) {
            put(Vertex(id, element))
        } else {
            error("Graph already contains element with id $id")
        }
    }

    override fun remove(id: String): Boolean {
        val vertex = idToVertex[id] ?: return false
        return remove(vertex)
    }

    private fun put(vertex: Vertex<V>) {
        addVertexRoutine(vertex)
        val randomVertices = sampleK(vertex, 2 * k)
        val newKnnHeap = neighborsHeapFactory.createNeighborsHeap(vertex, 2 * k)
        newKnnHeap.offerAllWithTracking(randomVertices)
        var iteration = ITERATIONS_NUMBER
        while (iteration != 0 && makeQueryIteration(newKnnHeap, vertex) != 0) {
            iteration--
        }
        knnHeaps[vertex] = newKnnHeap

        notifyAboutNewVertex(newKnnHeap, vertex)
        val finalVertexHeap = neighborsHeapFactory.createNeighborsHeap(vertex, k)
        finalVertexHeap.offerAllWithTracking(newKnnHeap.neighbors)
        knnHeaps[vertex] = finalVertexHeap
    }

    private fun remove(vertex: Vertex<V>): Boolean {
        val elementsToDeleteFrom = reverseKnnHeap[vertex] ?: return false
        elementsToDeleteFrom.forEach {
            val knnHeap = knnHeaps[it] ?: error("No heap for vertex: $it")
            if (knnHeap.remove(vertex) != null) {
                knnHeap.offerWithTracking(sampleK(vertex, 1).first())
            }
        }
        elementsToDeleteFrom.forEach { elementToDeleteFrom ->
            var lastAddedElement: Vertex<V>? = null
            val knnHeapToDeleteFrom = knnHeaps[elementToDeleteFrom] ?: error("No heap for vertex: $elementToDeleteFrom")
            knnHeapToDeleteFrom.neighbors.forEach { neighbor ->
                val neighborHeap = knnHeaps[neighbor] ?: error("No heap for vertex: $neighbor")
                neighborHeap.neighbors.forEach { neighborOfNeighbor ->
                    if (neighborOfNeighbor != elementToDeleteFrom) {
                        if (knnHeapToDeleteFrom.offerWithTracking(neighborOfNeighbor)) {
                            lastAddedElement = neighborOfNeighbor
                        }
                    }
                }
            }
            while (lastAddedElement != null) {
                val currentElement = lastAddedElement
                lastAddedElement = null
                val knnHeapOfLastAdded = knnHeaps[currentElement] ?: error("No heap for vertex: $currentElement")
                knnHeapOfLastAdded.neighbors.forEach { neighbor ->
                    if (neighbor != elementToDeleteFrom) {
                        if (knnHeapToDeleteFrom.offerWithTracking(neighbor)) {
                            lastAddedElement = neighbor
                        }
                    }
                }
            }
        }
        return true
    }

    private fun makeQueryIteration(newKnnHeap: NeighborsHeap<V>, vertex: Vertex<V>): Int {
        var numberOfChanges = 0
        newKnnHeap.neighbors.forEach { neighbor ->
            val neighborHeap = knnHeaps[neighbor] ?: error("No heap for vertex: $neighbor")
            neighborHeap.neighbors.forEach { neighborOfNeighbor ->
                if (neighborOfNeighbor != vertex) {
                    if (newKnnHeap.offerWithTracking(neighborOfNeighbor)) {
                        numberOfChanges++
                    }
                }
            }
        }
        //println("(New vertex insertion) Number of changes = $numberOfChanges, counter = $counter")
        return numberOfChanges
    }

    private fun makeQueryIterationWithoutTracking(newKnnHeap: NeighborsHeap<V>, vertex: Vertex<V>): Int {
        var numberOfChanges = 0
        newKnnHeap.neighbors.forEach { neighbor ->
            val neighborHeap = knnHeaps[neighbor] ?: error("No heap for vertex: $neighbor")
            neighborHeap.neighbors.forEach { neighborOfNeighbor ->
                if (neighborOfNeighbor != vertex) {
                    if (newKnnHeap.offer(neighborOfNeighbor) != null) {
                        numberOfChanges++
                    }
                }
            }
        }
        //println("(New vertex insertion) Number of changes = $numberOfChanges, counter = $counter")
        return numberOfChanges
    }

    private fun notifyAboutNewVertex(heap: NeighborsHeap<V>, newVertex: Vertex<V>): Int {
        var numberOfChanges = 0

        var candidates = mutableListOf<Vertex<V>>()
        heap.neighbors.forEach {
            val neighborHeap = knnHeaps[it] ?: error("No reverse-heap for vertex: $it")
            candidates.addAll(neighborHeap.neighbors)
            if (neighborHeap.offerWithTracking(it)) {
                numberOfChanges++
            }
        }
        while (candidates.isNotEmpty()) {
            val newCandidates = mutableListOf<Vertex<V>>()
            candidates.forEach { neighbor ->
                val neighborHeap = knnHeaps[neighbor] ?: error("No heap for vertex: $neighbor")
                if (neighborHeap.offerWithTracking(newVertex)) {
                    newCandidates.addAll(neighborHeap.neighbors)
                    numberOfChanges++
                }
            }
            candidates = newCandidates
        }
        //println("(Notifying about new vertex) Number of changes = $numberOfChanges")
        return numberOfChanges
    }

    private fun generateGraphFromList(vertices: List<Vertex<V>>) {
        sampleRandomGraph()
        var iteration = ITERATIONS_NUMBER
        val threshold = vertices.size * k * k * 0.01
        while (iteration != 0 && makeIteration() > threshold) {
            iteration--
        }
    }

    private fun sampleRandomGraph() {
        knnHeaps.clear()
        reverseKnnHeap.clear()
        idToVertex.clear()
        vertices.forEach { vertex ->
            val nNeighborsHeap = neighborsHeapFactory.createNeighborsHeap(vertex)
            sampleK(vertex).forEach { sampledVertex ->
                nNeighborsHeap.offerWithTracking(sampledVertex)
            }
            knnHeaps[vertex] = nNeighborsHeap
        }
    }

    private fun sampleK(vertex: Vertex<V>, k: Int = this.k): List<Vertex<V>> {
        val randomSet = mutableSetOf<Vertex<V>>()
        while (randomSet.size < k) {
            val randV = vertices[Random.nextInt(0, vertices.size)]
            if (randV != vertex) {
                randomSet.add(randV)
            }
        }
        return randomSet.toList()
    }

    private fun makeIteration(): Int {
        var numberOfChanges = 0
        var counter = 0
        knnHeaps.keys.forEach { vertex ->
            val mainHeap = knnHeaps[vertex] ?: error("No vertex in heap: $vertex")
            mainHeap.neighbors.forEach { neighbor ->
                val neighborHeap = knnHeaps[neighbor] ?: error("No vertex in heap: $vertex")
                neighborHeap.neighbors.forEach { neighborOfNeighbor ->
                    if (neighborOfNeighbor != vertex) {
                        if (mainHeap.offerWithTracking(neighborOfNeighbor)) {
                            numberOfChanges++
                        }
                    }
                    counter++
                }
            }
        }
        println("(Graph building) Number of changes = $numberOfChanges, counter = $counter")
        return numberOfChanges
    }

    private fun addVertexRoutine(vertex: Vertex<V>) {
        vertices.add(vertex)
        reverseKnnHeap[vertex] = mutableListOf()
        idToVertex[vertex.id] = vertex
    }

    private fun NeighborsHeap<V>.offerWithTracking(candidate: Vertex<V>): Boolean {
        val deletedElement = this.offer(candidate)
        if (reverseKnnHeap.containsKey(candidate)) {
            reverseKnnHeap[candidate]?.add(this.headElement) ?: error("No reverse-heap for vertex: $candidate")
        } else {
            reverseKnnHeap[candidate] = mutableListOf(candidate)
        }
        if (deletedElement != null) {
            reverseKnnHeap[deletedElement]?.remove(this.headElement) ?: error("No reverse-heap for vertex: $deletedElement")
            return true
        }
        return false
    }

    private fun NeighborsHeap<V>.offerAllWithTracking(candidates: List<Vertex<V>>) {
        candidates.forEach { offerWithTracking(it) }
    }
}