package hphuc5453.demo_3d_model

import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import processing.android.PFragment
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PShape
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        const val key = "AIzaSyB8jJtGtVQaEvRySNtmMNo3lxtAUpdup_E"
        const val baseURL = "https://poly.googleapis.com/v1"
    }

    var objFileURL: String? = null
    var mtlFileURL: String? = null
    var mtlFileName: String? = null

    lateinit var frameLayout : FrameLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        frameLayout = findViewById(R.id.canvas_holder)

        val listURL = "$baseURL/assets"
        listURL.httpGet(
            listOf(
                "category" to "animals",
                "key" to key,
                "format" to "OBJ"
            )
        ).responseJson { _, _, result ->
            result.fold({
                // Get assets array
                val assets = it.obj().getJSONArray("assets")

                for (i in 0 until assets.length()){
                    val currentAssets = assets.getJSONObject(i)
                    val formats = currentAssets.getJSONArray("formats")
                    // get url obj ! first !
                    for (j in 0..formats.length()) {
                        val currentFormat = formats.getJSONObject(j)
                        if (currentFormat.getString("formatType") == "OBJ") {
                            objFileURL = currentFormat.getJSONObject("root")
                                .getString("url")
                            mtlFileURL = currentFormat.getJSONArray("resources")
                                .getJSONObject(0)
                                .getString("url")

                            mtlFileName = currentFormat.getJSONArray("resources")
                                .getJSONObject(0)
                                .getString("relativePath")
                            break
                        }
                    }

                    // dowload obj
                    objFileURL?.httpDownload()?.destination { response, url ->
                        File(filesDir, "assets_test.obj")
                    }?.response { _, _, result ->
                        result.fold({}, {
                            Log.e("POLY", "An error occurred")
                        })
                    }

                    mtlFileURL?.httpDownload()?.destination { response, url ->
                        File(filesDir, mtlFileName)
                    }?.response { _, _, result ->
                        result.fold({}, {
                            Log.e("POLY", "An error occurred")
                        })
                    }
                    break
                }
            }, {
                // In case of an error
                Log.e("POLY", "An error occurred")
            })
            // read JSON data
        }

        var myPolyAsset: PShape? = null
        val canvas = object : PApplet(){
            override fun settings() {
                fullScreen(PConstants.P3D)
            }

            override fun setup() {
                val file = File (filesDir, "assets_test.obj")
                Log.d("file", file.name)
                myPolyAsset = loadShape (file.path)
            }

            val color = getColor(R.color.color_7d6d6d6d)
            override fun draw() {
                background(color)
                // More code here
                scale(-50f)
                translate(-4f,-14f)
                shape(myPolyAsset)
            }
        }
        val fragment = PFragment(canvas)
        this.supportFragmentManager.beginTransaction()
            .replace(frameLayout.id, fragment as Fragment)
            .commit()
    }
}