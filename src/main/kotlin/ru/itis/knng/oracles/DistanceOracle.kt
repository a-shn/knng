package ru.itis.knng.oracles

interface DistanceOracle<V> {

    fun distanceOf(object1: V, object2: V): Double
}