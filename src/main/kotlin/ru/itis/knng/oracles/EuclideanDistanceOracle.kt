package ru.itis.knng.oracles

import ru.itis.knng.models.Vector
import kotlin.math.sqrt

class EuclideanDistanceOracle : DistanceOracle<Vector> {

    override fun distanceOf(object1: Vector, object2: Vector): Double {
        return sqrt(squaredSumOfDiff(object1.vector, object2.vector))
    }

    private fun squaredSumOfDiff(array1: Array<Double>, array2: Array<Double>): Double {
        var squaredSum = 0.0
        var tmp: Double
        for (idx in array1.indices) {
            tmp = array1[idx] - array2[idx]
            squaredSum += tmp * tmp
        }
        return squaredSum
    }
}