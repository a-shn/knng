package ru.itis.knng.models


class Vertex<V>(
    public val id: String,
    public val element: V
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vertex<*>
        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}