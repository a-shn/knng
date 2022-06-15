package ru.itis.knng.storage

import ru.itis.knng.models.Vertex

interface VertexStorage<V> {

    fun getNeighborsById(id: String): List<Pair<String, V>>
    fun put(vertex: Vertex<V>)
    fun delete(vertex: Vertex<V>)
    fun offerNeighbor(vertex: Vertex<V>, neighbor: Vertex<V>): Boolean
}