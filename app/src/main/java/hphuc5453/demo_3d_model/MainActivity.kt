package hphuc5453.demo_3d_model

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    lateinit var mglView : GLSurfaceView
    private var renderSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val activityManager =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo

        val supportsEs2 =
            configurationInfo.reqGlEsVersion >= 0x20000 || isProbablyEmulator()

        if(supportsEs2){
            mglView = GLSurfaceView(this)
            if(isProbablyEmulator()){
                mglView.setEGLConfigChooser(8, 8, 8, 8, 16, 0)
            }
            mglView.setEGLContextClientVersion(2)
            mglView.preserveEGLContextOnPause = true
            val loader = Model3DLoader(assets)
            mglView.setRenderer(Model3DRenderer(this, loader))
            renderSet = true
        }
        setContentView(mglView)
    }

    private fun hasGLES20(): Boolean {
        val am =
            getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = am.deviceConfigurationInfo
        return info.reqGlEsVersion >= 0x20000
    }

    private fun isProbablyEmulator(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
                && (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")))
    }

    override fun onResume() {
        super.onResume()
        if(renderSet){
            mglView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if(renderSet){
            mglView.onPause()
        }
    }
}