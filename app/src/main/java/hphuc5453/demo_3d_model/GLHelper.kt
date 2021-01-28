package hphuc5453.demo_3d_model

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils

class GLHelper {
    /**
     * Handles all the GLES20 calls to load and compile a vertexShader
     * Some code taken from learnopengles.com
     * @param shader
     * @return
     */
    fun loadShader(typeOfShader: Int, shader: String?): Int {
        if (typeOfShader == GLES20.GL_VERTEX_SHADER) {
            var vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
            if (vertexShaderHandle != 0) {
                //Pass in the shader source.
                GLES20.glShaderSource(vertexShaderHandle, shader)

                //Compile the shader.
                GLES20.glCompileShader(vertexShaderHandle)
                //Get the compile status, if it remains 0, terminate the shader.
                val compileStatus = intArrayOf(0)
                GLES20.glGetShaderiv(
                    vertexShaderHandle, GLES20.GL_COMPILE_STATUS,
                    compileStatus, 0
                )
                if (compileStatus[0] == 0) {
                    GLES20.glDeleteShader(vertexShaderHandle)
                    vertexShaderHandle = 0
                }
            }
            //If the shader didn't get created properly, throw an exception.
            if (vertexShaderHandle == 0) {
                throw RuntimeException("The Vertex shader didn't get created")
            }
            return vertexShaderHandle
        } else if (typeOfShader == GLES20.GL_FRAGMENT_SHADER) {
            var fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)

            //Create and compile the shader source.
            if (fragmentShaderHandle != 0) {
                //Pass in the shader source.
                GLES20.glShaderSource(fragmentShaderHandle, shader)
                //Compile the shader.
                GLES20.glCompileShader(fragmentShaderHandle)
                //Get compile status. If it's 0, terminate the shader.
                val compileStatus = intArrayOf(0)
                GLES20.glGetShaderiv(
                    fragmentShaderHandle, GLES20.GL_COMPILE_STATUS,
                    compileStatus, 0
                )
                if (compileStatus[0] == 0) {
                    GLES20.glDeleteShader(fragmentShaderHandle)
                    fragmentShaderHandle = 0
                }
            }
            return fragmentShaderHandle
        }
        return 0
    }

    fun createAndLinkProgram(
        vertexShaderHandle: Int,
        fragmentShaderHandle: Int,
        attributes: Array<String?>?
    ): Int {
        var programHandle = GLES20.glCreateProgram()
        if (programHandle != 0) {
            // Bind the vertex shader to the program.
            GLES20.glAttachShader(programHandle, vertexShaderHandle)

            // Bind the fragment shader to the program.
            GLES20.glAttachShader(programHandle, fragmentShaderHandle)

            // Bind attributes
            if (attributes != null) {
                val size = attributes.size
                for (i in 0 until size) {
                    GLES20.glBindAttribLocation(programHandle, i, attributes[i])
                }
            }

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programHandle)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)

            // If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programHandle)
                programHandle = 0
            }
        }
        if (programHandle == 0) {
            throw java.lang.RuntimeException("Error creating program.")
        }
        return programHandle
    }

    fun loadTexture(context: Context, resourceID: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false
            val bitmap = BitmapFactory.decodeResource(
                context.resources,
                resourceID, options
            )
            bitmap.config

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
            )

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle()
        }
        if (textureHandle[0] == 0) {
            throw java.lang.RuntimeException("Error loading texture.")
        }
        return textureHandle[0]
    }
}