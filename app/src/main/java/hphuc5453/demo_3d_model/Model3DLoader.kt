package hphuc5453.demo_3d_model

import android.content.res.AssetManager
import hphuc5453.demo_3d_model.AppConstants.Companion.FACE
import hphuc5453.demo_3d_model.AppConstants.Companion.NORMAL
import hphuc5453.demo_3d_model.AppConstants.Companion.TEXTURE
import hphuc5453.demo_3d_model.AppConstants.Companion.VERTEX
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

class Model3DLoader(assetManager: AssetManager) {

    private var fileReader: BufferedReader? = null
    private var vertices: FloatArray? = null
    private var indicesVertices: ShortArray? = null
    private var normals: FloatArray? = null
    private var colors: FloatArray? = null

    private val vPositions = ArrayList<Float>()
    private val vNormals = ArrayList<Float>()
    private val vColors = ArrayList<Float>()

    private val iPositions = ArrayList<Short>()
    private val iNormals = ArrayList<Short>()
    private val iColors = ArrayList<Short>()

    private val deIndexedVertices: ArrayList<Vertex> = ArrayList()
    private val indices = ArrayList<Short>()
    private var scanner: StringTokenizer? = null
    private var collectionOfModels: ArrayList<Model3D>? = null
    private var assets: AssetManager? = null

    init {
        this.assets = assetManager
        collectionOfModels = arrayListOf()
    }

    fun parseObject(fileName: String) {
        val fileIn = assets?.open(fileName)
        fileReader = BufferedReader(InputStreamReader(fileIn))
        //Read from the file
        var line = fileReader?.readLine()

        while (line != null) {
            line = when {
                line.startsWith(VERTEX) -> {
                    processVertex(line)
                    fileReader?.readLine()
                }
                line.startsWith(NORMAL) -> {
                    processNormal(line)
                    fileReader?.readLine()
                }
                line.startsWith(FACE) -> {
                    processFace(line)
                    fileReader?.readLine()
                }
                line.startsWith(TEXTURE) -> {
                    processColor(line)
                    fileReader?.readLine()
                }
                else -> {
                    fileReader?.readLine()
                }
            }
        }
        try {
            fileReader?.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
        }
        if (vPositions.isEmpty()) {
            return
        }
        var maxIndex: Short = 0
        for (i in iPositions.indices) {
            val vertex = Vertex()
            vertex.positionX = vPositions[iPositions[i].toInt() * 3]
            vertex.positionY = vPositions[iPositions[i].toInt() * 3 + 1]
            vertex.positionZ = vPositions[iPositions[i].toInt() * 3 + 2]

            vertex.normalX = vNormals[iNormals[i].toInt() * 3]
            vertex.normalY = vNormals[iNormals[i].toInt() * 3 + 1]
            vertex.normalZ = vNormals[iNormals[i].toInt() * 3 + 2]

            if (iColors.isNotEmpty()) {
                vertex.textureU = vColors[iColors[i].toInt() * 2]
                vertex.textureV = vColors[iColors[i].toInt() * 2 + 1]
            }

            var isContained = false
            var index = 0 //if the deIndexedVertices contains the vertex, then
            //we can simply take the index of where it is and
            //put it in the indices again.
            for (k in deIndexedVertices.indices) {
                //Search the deIndexed vertices for the vertex.
                if (vertex == deIndexedVertices[k]) {
                    isContained = true
                    index = k
                }
            }
            //If the vertex isn't contained, then make a new one
            //in the VBO and expand the maxIndex.
            if (!isContained) {
                deIndexedVertices.add(vertex)
                indices.add(maxIndex)
                maxIndex++
            } else {
                //The vertex is contained, and all we need to do is
                //add the index of it to the indices array list.
                indices.add(index.toShort())
            }
        }
        vertices = FloatArray(deIndexedVertices.size * 3)
        normals = FloatArray(deIndexedVertices.size * 3)
        colors = FloatArray(deIndexedVertices.size * 2)
        indicesVertices = ShortArray(indices.size)
        var positionCount = 0
        var colorCount = 0
        //Get all the data into the proper arrays.
        for (i in deIndexedVertices.indices) {
            vertices!![positionCount] = deIndexedVertices[i].positionX
            vertices!![positionCount + 1] = deIndexedVertices[i].positionY
            vertices!![positionCount + 2] = deIndexedVertices[i].positionZ
            normals!![positionCount] = deIndexedVertices[i].normalX
            normals!![positionCount + 1] = deIndexedVertices[i].normalY
            normals!![positionCount + 2] = deIndexedVertices[i].normalZ
            colors!![colorCount] = deIndexedVertices[i].textureU
            colors!![colorCount + 1] = deIndexedVertices[i].textureV
            positionCount += 3
            colorCount += 2
        }
        for (i in indices.indices) {
            indicesVertices!![i] = indices[i]
        }
        //Make the buffers for the positions, normals, and texture colors
        val verticesPBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertices!!.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        verticesPBuffer.position(0)
        verticesPBuffer.put(vertices)
        verticesPBuffer.position(0)

        val verticesNBuffer: FloatBuffer = ByteBuffer.allocateDirect(normals!!.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        verticesNBuffer.position(0)
        verticesNBuffer.put(normals)
        verticesNBuffer.position(0)

        val verticesTCBuffer: FloatBuffer = ByteBuffer.allocateDirect(colors!!.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        verticesTCBuffer.position(0)
        verticesTCBuffer.put(colors)
        verticesTCBuffer.position(0)
        //Do the same for the indices.
        val indicesPBuffer: ShortBuffer = ByteBuffer.allocateDirect(indicesVertices!!.size * 2)
            .order(ByteOrder.nativeOrder()).asShortBuffer()
        indicesPBuffer.position(0)
        indicesPBuffer.put(indicesVertices)
        indicesPBuffer.position(0)

        collectionOfModels!!.add(
            Model3D(
                indicesPBuffer, verticesPBuffer, verticesNBuffer,
                verticesTCBuffer
            )
        )
    }

    /**
     * Processes a line of text and gets the vertices out of it
     * @param vertex
     */
    private fun processVertex(vertex: String?) {
        scanner = StringTokenizer(vertex)
        var element: String
        while (scanner!!.hasMoreTokens()) {
            element = scanner!!.nextToken()
            if (element != VERTEX.trim()) {
                vPositions.add(element.toFloat())
            }
        }
    }

    /**
     * Parses the string for the vertex colors and adds them to the
     * color array
     * @param color
     */
    private fun processColor(color: String?) {
        scanner = StringTokenizer(color)
        var element: String
        while (scanner!!.hasMoreTokens()) {
            element = scanner!!.nextToken()
            if (element != TEXTURE) {
                vColors.add(element.toFloat())
            }
        }
    }

    /**
     * Same thing as above but with normals
     */
    private fun processNormal(normals: String?) {
        scanner = StringTokenizer(normals)
        var element: String
        while (scanner!!.hasMoreTokens()) {
            element = scanner!!.nextToken()
            if (element != NORMAL) {
                vNormals.add(element.toFloat())
            }
        }
    }

    private fun processFace(face: String?) {
        val strAr: Array<String>? = face?.replace("f", "")?.trim()?.split(" ")?.toTypedArray()
        val isDouble = strAr!![0].contains("//")
        if (isDouble) {
            for (s in strAr) {
                val cornerAr = s.split("//".toRegex()).toTypedArray()
                if (cornerAr.size == 2) {
                    iPositions.add((cornerAr[0].trim { it <= ' ' }.toInt() - 1).toShort())
                    iNormals.add((cornerAr[1].trim { it <= ' ' }.toInt() - 1).toShort())
                } else {
                    iPositions.add((cornerAr[0].trim { it <= ' ' }.toInt() - 1).toShort())
                    iColors.add((cornerAr[1].trim { it <= ' ' }.toInt() - 1).toShort())
                    iNormals.add((cornerAr[2].trim { it <= ' ' }.toInt() - 1).toShort())
                }
            }
        } else {
            for (s in strAr) {
                val cornerAr = s.split("/".toRegex()).toTypedArray()
                if (cornerAr.size == 2) {
                    iPositions.add((cornerAr[0].trim { it <= ' ' }.toInt() - 1).toShort())
                    iColors.add((cornerAr[1].trim { it <= ' ' }.toInt() - 1).toShort())
                } else {
                    iPositions.add((cornerAr[0].trim { it <= ' ' }.toInt() - 1).toShort())
                    iColors.add((cornerAr[1].trim { it <= ' ' }.toInt() - 1).toShort())
                    iNormals.add((cornerAr[2].trim { it <= ' ' }.toInt() - 1).toShort())
                }
            }
        }
    }

    fun getModels(): ArrayList<Model3D>? {
        return collectionOfModels
    }
}