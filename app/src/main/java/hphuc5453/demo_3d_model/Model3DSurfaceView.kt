package hphuc5453.demo_3d_model

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

@SuppressLint("ViewConstructor")
class Model3DSurfaceView(context: Context) :
    GLSurfaceView(context) {

    private var render: Model3DRenderer? = null


    fun setRender(render: Model3DRenderer, destiny: Float){
        this.render = render
        this.mDensity = destiny
        super.setRenderer(render)
    }
    // Offsets for touch events
    private var mPreviousX = 0f
    private var mPreviousY = 0f

    private var mDensity = 0f


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            val x = event.x
            val y = event.y

            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    if (render != null) {
//                        queueEvent { render!!.switchMode() }
//                    }
//                }
                MotionEvent.ACTION_MOVE -> {
                    var deltaX = x - mPreviousX
                    var deltaY = y - mPreviousY
                    if(y > height / 2){
                        deltaX *= -1
                    }
                    if(x > width / 2){
                        deltaY *= -1
                    }
                    if (render != null) {
                        render!!.mDeltaX += ((deltaX * AppConstants.TOUCH_SCALE_FACTOR) + 360) % 360
                        render!!.mDeltaY += ((deltaY * AppConstants.TOUCH_SCALE_FACTOR) + 360) % 360
                    }

                }
            }
            requestRender()
            mPreviousX = x
            mPreviousY = y
            return true
        } else {
            return super.onTouchEvent(event)
        }
    }
}