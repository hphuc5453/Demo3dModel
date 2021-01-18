package hphuc5453.demo_3d_model

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet

class MainActivity : AppCompatActivity() {

    companion object {
        const val key = "AIzaSyB8jJtGtVQaEvRySNtmMNo3lxtAUpdup_E"
        const val baseURL = "https://poly.googleapis.com/v1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listURL = "$baseURL/assets"
        listURL.httpGet(listOf(
                "category" to "animals",
                "key" to key,
                "format" to "OBJ"
        )).responseJson { _, _, result ->
            result.fold({
                // Get assets array
                val assets = it.obj().getJSONArray("assets")

                // Loop through array
                for(i in 0 until assets.length()) {
                    // Get id and displayName
                    val id = assets.getJSONObject(i).getString("name")
                    val displayName =
                            assets.getJSONObject(i).getString("displayName")

                    // Print id and displayName
                    Log.d("POLY", "(ID: $id) -- (NAME: $displayName)")
                }
            }, {
                // In case of an error
                Log.e("POLY", "An error occurred")
            })
            // read JSON data
        }
    }
}