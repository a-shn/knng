package ru.itis.knng.graph

interface KNNGraph<V> {
    fun getNeighbors(id: String): List<Pair<String,V>>
    fun findNeighbors(query: V): List<Pair<String,V>>
    fun put(id: String, element: V)
    fun remove(id: String): Boolean
}