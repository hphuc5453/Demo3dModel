package hphuc5453.demo_3d_model

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import org.apache.commons.io.IOUtils
import java.nio.charset.Charset
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Model3DRenderer(context: Context, loader: Model3DLoader) : GLSurfaceView.Renderer {
    /**
     * Store the model matrix. This matrix is used to move models from
     * object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private val mModelMatrix = FloatArray(16)

    /**
     * Store the view matrix. This can be thought of as our camera.
     * This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private val mViewMatrix = FloatArray(16)

    /** Store the projection matrix. This is used to project the scene
     * onto a 2D viewport.  */
    private val mProjectionMatrix = FloatArray(16)

    private val mMVMatrix = FloatArray(16)

    /** Allocate storage for the final combined matrix. This will be
     * passed into the shader program.  */
    private val mMVPMatrix = FloatArray(16)
    private var loader: Model3DLoader? = null
    private val bytesPerFloat = 4
    private val bytesPerShort = 2

    /** These will be used to pass in the matrices.  */
    private var mMVPMatrixHandle = 0
    private var mMVMatrixHandle = 0

    /** These will be used to pass in model information.  */
    private var mPositionHandle = 0
    private var mLightPosHandle = 0
    private var mNormalHandle = 0
    private var mTextureCoordsHandle = 0
    private var mTextureUniformHandle = 0

    private var mProgramHandle = 0
    private var mPointProgramHandle = 0
    /** Store the current rotation.  */
    private val mCurrentRotation = FloatArray(16)
    /**
     * Stores a copy of the model matrix specifically for the light position.
     */
    private val mLightModelMatrix = FloatArray(16)
    /** A temporary matrix.  */
    private val mTemporaryMatrix = FloatArray(16)

    private var verticesPBuffer = 0
    private var verticesNBuffer = 0
    private var verticesTCBuffer = 0
    private var indicesPBuffer = 0
    private var context: Context? = null

    /** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
     * we multiply this by our transformation matrices.  */
    private val mLightPosInModelSpace = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    /** Used to hold the current position of the light in world space (after transformation via model matrix).  */
    private val mLightPosInWorldSpace = FloatArray(4)

    /** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix)  */
    private val mLightPosInEyeSpace = FloatArray(4)

    private var pointVertexShader: String? = null
    private var pointFragmentShader: String? = null
    private var vertexShader: String? = null
    private var fragmentShader: String? = null

    private var helper: GLHelper? = null

    /** These are handles to our texture data.  */
    private var mTextureColorDataHandle = 0
    /** Store the accumulated rotation.  */
    private val mAccumulatedRotation = FloatArray(16)

    // These still work without volatile, but refreshes are not guaranteed to happen.
    @Volatile
    var mDeltaX = 0f

    @Volatile
    var mDeltaY = 0f

    private fun getPointVertexShader(): String {
        val input = context?.resources?.openRawResource(R.raw.point_vertex_shader)
        val code = IOUtils.toString(input, Charset.defaultCharset())
        input?.close()
        return code
    }

    private fun getPointFragmentShader(): String {
        val input = context?.resources?.openRawResource(R.raw.point_fragment_shader)
        val code = IOUtils.toString(input, Charset.defaultCharset())
        input?.close()
        return code
    }

    private fun getVertexShader(): String {
        val input = context?.resources?.openRawResource(R.raw.vertex_shader)
        val code = IOUtils.toString(input, Charset.defaultCharset())
        input?.close()
        return code
    }

    private fun getFragmentShader(): String {
        val input = context?.resources?.openRawResource(R.raw.fragment_shader)
        val code = IOUtils.toString(input, Charset.defaultCharset())
        input?.close()
        return code
    }

    init {
        this.context = context
        this.loader = loader
        pointVertexShader = getPointVertexShader()
        pointFragmentShader = getPointFragmentShader()
        vertexShader = getVertexShader()
        fragmentShader = getFragmentShader()
        helper = GLHelper()
        this.loader?.parseObject("penguin.obj")
    }

    override fun onDrawFrame(gl: GL10?) {
        if (mProgramHandle != 0) {
            GLES20.glUseProgram(mProgramHandle)
        } else {
            throw RuntimeException("Program isn't valid")
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position")
        mNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal")
        mTextureCoordsHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate")
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix")
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix")
        mLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos")

        Matrix.multiplyMV(
            mLightPosInWorldSpace,
            0,
            mLightModelMatrix,
            0,
            mLightPosInModelSpace,
            0
        )
        Matrix.multiplyMV(
            mLightPosInEyeSpace,
            0,
            mViewMatrix,
            0,
            mLightPosInWorldSpace,
            0
        )

        // Draw the triangle facing straight on.
        Matrix.setIdentityM(mModelMatrix, 0)
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.8f, -1.5f)
//        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 10.0f, 2.0f)

        // Set a matrix that contains the current rotation.
        Matrix.setIdentityM(mCurrentRotation, 0)
        Matrix.rotateM(mCurrentRotation, 0, mDeltaX, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(mCurrentRotation, 0, mDeltaY, 1.0f, 0.0f, 0.0f)
        mDeltaX = 0.0f
        mDeltaY = 0.0f

        // Multiply the current rotation by the accumulated rotation, and then set the accumulated rotation to the result.
        Matrix.multiplyMM(
            mTemporaryMatrix,
            0,
            mCurrentRotation,
            0,
            mAccumulatedRotation,
            0
        )
        System.arraycopy(mTemporaryMatrix, 0, mAccumulatedRotation, 0, 16)
        // Rotate the cube taking the overall rotation into account.
        Matrix.multiplyMM(
            mTemporaryMatrix,
            0,
            mModelMatrix,
            0,
            mAccumulatedRotation,
            0
        )
        System.arraycopy(mTemporaryMatrix, 0, mModelMatrix, 0, 16)

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureColorDataHandle)
        GLES20.glUniform1i(mTextureUniformHandle, 0)

        drawModel()
        // Draw a point to indicate the light.
        GLES20.glUseProgram(mPointProgramHandle)

        // Calculate position of the light. Rotate and then push into the distance.
        // Do a complete rotation every 10 seconds.
//        val time = SystemClock.uptimeMillis() % 10000L
//        val angleInDegrees = 360.0f / 10000.0f * time.toInt()

//        Matrix.setIdentityM(mLightModelMatrix, 0)
//        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -2.0f)
//        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f)
//        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 3.5f)
//        drawLight()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        //Set the viewport to be the size of the screen.
        GLES20.glViewport(0, 0, width, height)
        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        val ratio = width.toFloat() / height
        val left = -ratio
        val bottom = -1.0f
        val top = 1.0f
        val near = 1.0f
        val far = 10.0f

        Matrix.frustumM(mProjectionMatrix, 0, left, ratio, bottom, top, near, far)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f)
        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        //generate the VBOS and IBOS for the vertices and indices
        val buffers = IntArray(4)
        GLES20.glGenBuffers(4, buffers, 0)
        //The VBO generation
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            loader!!.getModels()!![0].verticesPositions!!.capacity() * bytesPerFloat,
            loader!!.getModels()!![0].verticesPositions,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[1])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            loader!!.getModels()!![0].verticesNormals!!.capacity() * bytesPerFloat,
            loader!!.getModels()!![0].verticesNormals,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffers[2])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            loader!!.getModels()!![0].verticesColors!!.capacity() * bytesPerFloat,
            loader!!.getModels()!![0].verticesColors,
            GLES20.GL_STATIC_DRAW
        )
        //The IBO generation
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, buffers[3])
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            loader!!.getModels()!![0].indicesPositions!!.capacity() * bytesPerShort,
            loader!!.getModels()!![0].indicesPositions,
            GLES20.GL_STATIC_DRAW
        )
        verticesPBuffer = buffers[0]
        verticesNBuffer = buffers[1]
        verticesTCBuffer = buffers[2]
        indicesPBuffer = buffers[3]
        // Position the eye behind the origin.
        val eyeX = 0.0f
        val eyeY = 0.0f
        val eyeZ = 5.0f
        // We are looking toward the distance
        val lookX = 0.0f
        val lookY = 0.0f
        val lookZ = -5.0f
        // Set our up vector. This is where our head would be pointing were we holding the camera.
        val upX = 0.0f
        val upY = 1.0f
        val upZ = 0.0f
        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(
            mViewMatrix,
            0,
            eyeX,
            eyeY,
            eyeZ,
            lookX,
            lookY,
            lookZ,
            upX,
            upY,
            upZ
        )
        val vertexShaderHandle: Int = helper!!.loadShader(
            GLES20.GL_VERTEX_SHADER,
            vertexShader
        )
        val fragmentShaderHandle: Int = helper!!.loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            fragmentShader
        )
        mProgramHandle = helper!!.createAndLinkProgram(
            vertexShaderHandle, fragmentShaderHandle, arrayOf(
                "a_Position", "a_Normal",
                "a_TexCoordinate"
            )
        )
        val pointVertexShaderHandle: Int =
            helper!!.loadShader(GLES20.GL_VERTEX_SHADER, pointVertexShader)
        val pointFragmentShaderHandle: Int =
            helper!!.loadShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader)
        mPointProgramHandle = helper!!.createAndLinkProgram(
            pointVertexShaderHandle,
            pointFragmentShaderHandle,
            arrayOf("a_Position")
        )
        // Load the texture
        mTextureColorDataHandle =
            helper!!.loadTexture(context!!, R.drawable.metal)
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)

        // Initialize the accumulated rotation matrix
        Matrix.setIdentityM(mAccumulatedRotation, 0)
    }

    private fun drawLight() {
        val pointMVPMatrixHandle =
            GLES20.glGetUniformLocation(mPointProgramHandle, "u_MVPMatrix")
        val pointPositionHandle = GLES20.glGetAttribLocation(mPointProgramHandle, "a_Position")
        // Pass in the position.
        GLES20.glVertexAttrib3f(
            pointPositionHandle,
            mLightPosInModelSpace[0],
            mLightPosInModelSpace[1],
            mLightPosInModelSpace[2]
        )
        // Since we are not using a buffer object, disable vertex arrays for this attribute.
        GLES20.glDisableVertexAttribArray(pointPositionHandle)
        // Pass in the transformation matrix.
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mLightModelMatrix, 0)
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16)
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1)
    }

    private fun drawModel() {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesPBuffer)
        GLES20.glEnableVertexAttribArray(mPositionHandle)
        GLES20.glVertexAttribPointer(
            mPositionHandle, 3, GLES20.GL_FLOAT,
            false, 0, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesNBuffer)
        GLES20.glEnableVertexAttribArray(mNormalHandle)
        GLES20.glVertexAttribPointer(
            mNormalHandle, 3, GLES20.GL_FLOAT,
            false, 0, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, verticesTCBuffer)
        GLES20.glEnableVertexAttribArray(mTextureCoordsHandle)
        GLES20.glVertexAttribPointer(
            mTextureCoordsHandle, 2, GLES20.GL_FLOAT,
            false, 0, 0
        )
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indicesPBuffer)
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0)
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVMatrix, 0)
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mTemporaryMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0)
        System.arraycopy(mTemporaryMatrix, 0, mMVPMatrix, 0, 16)
//         Pass in the light position in eye space.
        GLES20.glUniform3f(
            mLightPosHandle,
            mLightPosInEyeSpace[0],
            mLightPosInEyeSpace[1],
            mLightPosInEyeSpace[2]
        )
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            loader!!.getModels()!![0].indicesPositions!!.capacity(), GLES20.GL_UNSIGNED_SHORT, 0
        )
    }
}