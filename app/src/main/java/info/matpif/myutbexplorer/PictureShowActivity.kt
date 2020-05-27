package info.matpif.myutbexplorer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.services.Uptobox

class PictureShowActivity : AppCompatActivity() {

    private var files: ArrayList<String>? = null
    private var uptobox: Uptobox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_show)
        this.files = intent.extras?.getStringArrayList("files")

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null) {
            this.uptobox = Uptobox(token, this)
        }
        val ivPicture: ImageView? = this.findViewById(R.id.picture_holder)
        val tvImageName: TextView? = this.findViewById(R.id.tvImageName)
        if (ivPicture != null) {
            this.files?.forEach { fileJson ->
                val file = UtbFile().pushData(
                    Gson().fromJson(
                        fileJson,
                        UtbFile.DataUtbFile::class.java
                    )
                )
                tvImageName?.text = file.file_name
                val picasso = Picasso.get()
                this.uptobox?.getDirectDownloadLink(file.file_code!!) { url ->
                    this.runOnUiThread {
                        picasso.load(url)
                            .stableKey(file.file_code!!)
                            .into(ivPicture)
                    }
                }
            }
        }

        this.hideSystemUi()
    }

    private fun hideSystemUi() {
        this.window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )
    }
}
