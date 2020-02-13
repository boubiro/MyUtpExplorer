package info.matpif.myutbexplorer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import info.matpif.myutbexplorer.controllers.MediaController
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.services.Uptobox
import pl.droidsonroids.casty.Casty
import pl.droidsonroids.casty.MediaData


class StreamActivity : AppCompatActivity() {


    private var videoView: VideoView? = null
    private var casty: Casty? = null
    private var uptobox: Uptobox? = null
    private var lastOrientation: Int = 0
    private var flAll: FrameLayout? = null
    private var llText: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_stream)

        this.videoView = findViewById(R.id.video_view)
        this.flAll = findViewById(R.id.flAll)
        this.llText = findViewById(R.id.llText)

        val videoProgressBar: ProgressBar = findViewById(R.id.videoProgressBar)
        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null) {
            this.uptobox = Uptobox(token, this)
        }

        val file: UtbFile = UtbFile().pushData(
            Gson().fromJson(
                intent.getStringExtra("file"),
                UtbFile.DataUtbFile::class.java
            )
        )
        val url: String? = intent.getStringExtra("url")
        val uri: Uri = Uri.parse(url)

        this.casty = Casty.create(this).withMiniController()
        this.casty?.setOnCastSessionUpdatedListener {
            if (it.isConnected) {
                this.videoView?.stopPlayback()
                this.videoView?.visibility = View.GONE

                if (this.uptobox != null) {
                    this.uptobox!!.getThumbUrl(file) { url ->
                        this.runOnUiThread {
                            val mediaData: MediaData = MediaData.Builder(url)
                                .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                                .setPosition(this.videoView!!.currentPosition.toLong())
                                .setContentType("videos/mp4")
                                .setMediaType(MediaData.MEDIA_TYPE_MOVIE)
                                .setTitle(file.file_name)
                                .setSubtitle(file.file_descr)
                                .addPhotoUrl(url)
                                .build()
                            casty?.player?.loadMediaAndPlay(mediaData)
                        }
                    }
                }
            }
        }

        lastOrientation = resources.configuration.orientation

        if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            this.setFullScreen(true)
        } else {
            this.setFullScreen(false)
        }

        val tvName: TextView = findViewById(R.id.tvName)
        val tvDescription: TextView = findViewById(R.id.tvDescription)

        tvName.text = file.file_name
        tvDescription.text = file.file_descr

        if (this.videoView != null) {

            this.videoView?.setOnPreparedListener {
                videoProgressBar.visibility = View.GONE
                if (resources.configuration.orientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    this.setFullScreen(true)
                } else {
                    this.setFullScreen(false)
                }
            }

            val mediaController = MediaController(this)
            this.videoView?.setMediaController(mediaController)

            this.videoView!!.setVideoURI(uri)
            this.videoView!!.seekTo(5)
            this.videoView!!.start()
            this.videoView!!.setBackgroundColor(0)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (lastOrientation != newConfig.orientation) {
            lastOrientation = newConfig.orientation
            if (resources.configuration.orientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                this.setFullScreen(true)
            } else {
                this.setFullScreen(false)
            }
        }
    }

    private fun setFullScreen(fullscreen: Boolean) {
        if (fullscreen) {
            this.supportActionBar?.hide()
            this.flAll?.setBackgroundColor(this.resources.getColor(R.color.background_color_fullscreen))
            this.llText?.visibility = View.GONE
            val videoLayoutParams: FrameLayout.LayoutParams =
                this.videoView?.layoutParams as FrameLayout.LayoutParams
            videoLayoutParams.apply {
                gravity = Gravity.CENTER
            }
            this.videoView?.layoutParams = videoLayoutParams
        } else {
            this.supportActionBar?.show()
            this.flAll?.setBackgroundColor(this.resources.getColor(R.color.background_color_without_fullscreen))
            this.llText?.visibility = View.VISIBLE

            val videoLayoutParams: FrameLayout.LayoutParams =
                this.videoView?.layoutParams as FrameLayout.LayoutParams
            videoLayoutParams.apply {
                gravity = Gravity.TOP
            }
            this.videoView?.layoutParams = videoLayoutParams

            val llTextLayoutParams: FrameLayout.LayoutParams =
                this.llText?.layoutParams as FrameLayout.LayoutParams
            llTextLayoutParams.topMargin = this.videoView?.height?.plus(24) ?: 0
            this.llText?.layoutParams = llTextLayoutParams
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            casty?.addMediaRouteMenuItem(menu)
        }
        menuInflater.inflate(R.menu.browse_stream, menu)
        return true
    }

    override fun onStop() {
        super.onStop()
        if (this.videoView?.isPlaying!!) {
            this.videoView!!.pause()
        }
    }

    override fun onBackPressed() {
        if (this.videoView?.isPlaying!!) {
            this.videoView!!.stopPlayback()
        }
        super.onBackPressed()
    }
}
