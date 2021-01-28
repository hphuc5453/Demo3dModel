package hphuc5453.demo_3d_model

import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Model3D(
    indicesPositions: ShortBuffer,
    verticesPositions: FloatBuffer,
    verticesNormals: FloatBuffer,
    verticesColors: FloatBuffer? = null
) {
    var indicesPositions: ShortBuffer? = null
    var verticesPositions: FloatBuffer? = null
    var verticesNormals: FloatBuffer? = null
    var verticesColors: FloatBuffer? = null

    init {
        this.indicesPositions = indicesPositions
        this.verticesPositions = verticesPositions
        this.verticesNormals = verticesNormals
        this.verticesColors = verticesColors
    }
}