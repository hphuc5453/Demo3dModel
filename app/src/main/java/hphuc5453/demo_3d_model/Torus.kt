package hphuc5453.demo_3d_model

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import org.apache.commons.io.IOUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.charset.Charset
import java.util.*

class Torus(context: Context) {
    private var verticesList: MutableList<String> = mutableListOf()
    private var facesList: MutableList<String> = mutableListOf()

    private var verticesBuffer: FloatBuffer? = null
    private var facesBuffer: ShortBuffer? = null

    private var program: Int? = null

    init {
        verticesList = mutableListOf()
        facesList = mutableListOf()

        // More code goes here
        val scanner = Scanner(context.assets.open("torus.obj"))
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine()
            if (line.startsWith("v ")) {
                verticesList.add(line)
            } else if (line.startsWith("f ")) {
                facesList.add(line)
            }
        }
        scanner.close()

        val bufferVerticals = ByteBuffer.allocateDirect(verticesList.size * 3 * 4)
        bufferVerticals.order(ByteOrder.nativeOrder())
        verticesBuffer = bufferVerticals.asFloatBuffer()

        val bufferSurface = ByteBuffer.allocateDirect(facesList.size * 3 * 2)
        bufferSurface.order(ByteOrder.nativeOrder())
        facesBuffer = bufferSurface.asShortBuffer()

        for (vertex in verticesList) {
            val coords =
                vertex.split(" ".toRegex()).toTypedArray() // Split by space
            val x = coords[1].toFloat()
            val y = coords[2].toFloat()
            val z = coords[3].toFloat()
            verticesBuffer?.put(x)
            verticesBuffer?.put(y)
            verticesBuffer?.put(z)
        }
        verticesBuffer?.position(0)

        for (face in facesList) {
            val vertexIndices =
                face.split("""[/, ]""".toRegex()).toTypedArray()
            val vertex1 = vertexIndices[1].toShort()
            val vertex2 = vertexIndices[2].toShort()
            val vertex3 = vertexIndices[3].toShort()
            facesBuffer?.put((vertex1 - 1).toShort())
            facesBuffer?.put((vertex2 - 1).toShort())
            facesBuffer?.put((vertex3 - 1).toShort())
        }
        facesBuffer?.position(0)

        val vertexShaderStream = context.resources.openRawResource(R.raw.vertex_shader)
        val vertexShaderCode = IOUtils.toString(vertexShaderStream, Charset.defaultCharset())
        vertexShaderStream.close()

        val fragmentShaderStream = context.resources.openRawResource(R.raw.fragment_shader)
        val fragmentShaderCode = IOUtils.toString(fragmentShaderStream, Charset.defaultCharset())
        fragmentShaderStream.close()

        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShader, vertexShaderCode)

        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode)

        GLES20.glCompileShader(vertexShader)
        GLES20.glCompileShader(fragmentShader)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program!!, vertexShader)
        GLES20.glAttachShader(program!!, fragmentShader)
        GLES20.glLinkProgram(program!!)
        GLES20.glUseProgram(program!!)
    }

    fun draw() {
        val position = GLES20.glGetAttribLocation(program!!, "position")
        GLES20.glEnableVertexAttribArray(position)

        GLES20.glVertexAttribPointer(
            position,
            3, GLES20.GL_FLOAT, false, 3 * 4, verticesBuffer
        )

        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)

        val productMatrix = FloatArray(16)

        Matrix.frustumM(
            projectionMatrix, 0,
            -1f, 1f,
            -1f, 1f,
            2f, 9f
        )

        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 3f, -4f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )

        Matrix.multiplyMM(
            productMatrix, 0,
            projectionMatrix, 0,
            viewMatrix, 0
        )

        val matrix = GLES20.glGetUniformLocation(program!!, "matrix")
        GLES20.glUniformMatrix4fv(matrix, 1, false, productMatrix, 0)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            facesList.size * 3, GLES20.GL_UNSIGNED_SHORT, facesBuffer
        )

        GLES20.glDisableVertexAttribArray(position)
    }
}