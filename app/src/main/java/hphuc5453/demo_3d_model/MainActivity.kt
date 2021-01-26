package hphuc5453.demo_3d_model

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {

    lateinit var sfView : GLSurfaceView
    lateinit var torus: Torus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sfView = findViewById(R.id.sfView)
        sfView.setEGLContextClientVersion(2)

        sfView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onDrawFrame(gl: GL10?) {
                torus.draw()
            } // More code goes here

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0,0, width, height)
            }

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                sfView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                torus = Torus(this@MainActivity)
            }
        })
    }
}