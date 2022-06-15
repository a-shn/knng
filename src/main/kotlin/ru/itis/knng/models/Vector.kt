package ru.itis.knng.models

class Vector(
    val vector: Array<Double>,
) {
    @Volatile private var hashCode = 0

    init {
        hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vector

        if (hashCode() != other.hashCode()) return false
        if (hashCode() == other.hashCode()) {
            return vector.contentEquals(other.vector)
        }
        error("Couldn't determine if vectors are equal")
    }

    override fun hashCode(): Int {
        var result = hashCode
        if (result == 0) {
            result = vector.contentHashCode()
            hashCode = result
        }
        return result
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("[")
        vector.forEach {
            sb.append(it)
            sb.append(",")
        }
        sb.deleteAt(sb.length - 1)
        sb.append("]")
        return sb.toString()
    }
}