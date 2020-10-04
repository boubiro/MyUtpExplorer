package info.matpif.myutbexplorer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.TextTrackStyle
import com.google.android.gms.common.images.WebImage
import com.google.gson.Gson
import info.matpif.myutbexplorer.controllers.MediaController
import info.matpif.myutbexplorer.helpers.MyHelper
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.services.Uptobox
import pl.droidsonroids.casty.Casty
import pl.droidsonroids.casty.MediaData
import java.util.*


class StreamActivity : AppCompatActivity() {


    private var videoView: VideoView? = null
    private var casty: Casty? = null
    private var uptobox: Uptobox? = null
    private var lastOrientation: Int = 0
    private var flAll: FrameLayout? = null
    private var llText: LinearLayout? = null
    private var mediaController: MediaController? = null
    private var videoProgressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        setContentView(R.layout.activity_stream)

        this.videoView = findViewById(R.id.video_view)
        this.flAll = findViewById(R.id.flAll)
        this.llText = findViewById(R.id.llText)
        this.videoProgressBar = findViewById(R.id.videoProgressBar)

        super.onCreate(savedInstanceState)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null) {
            this.uptobox = Uptobox.getInstance(token, this)
        }

        val file: UtbFile = UtbFile().pushData(
            Gson().fromJson(
                intent.getStringExtra("file"),
                UtbFile.DataUtbFile::class.java
            )
        )
        val url: String? = intent.getStringExtra("url")
        val uri: Uri = Uri.parse(url)

        val myHelper = MyHelper(this)

        if (!myHelper.isTV()) {
            this.casty = Casty.create(this).withMiniController()
            this.casty?.setOnCastSessionUpdatedListener { it ->
                if (it.isConnected) {
                    val position = this.videoView!!.currentPosition.toLong()
                    this.videoView?.stopPlayback()
                    this.videoView?.visibility = View.GONE

                    if (this.uptobox != null) {
                        this.uptobox!!.getThumbUrl(file) { url ->
                            this.runOnUiThread {

                                val mediaMetadata =
                                    MediaMetadata(MediaData.MEDIA_TYPE_MOVIE)

                                if (!TextUtils.isEmpty(file.file_name)) mediaMetadata.putString(
                                    MediaMetadata.KEY_TITLE,
                                    file.file_name
                                )
                                if (!TextUtils.isEmpty(file.file_descr)) mediaMetadata.putString(
                                    MediaMetadata.KEY_SUBTITLE,
                                    file.file_descr
                                )

                                mediaMetadata.addImage(WebImage(Uri.parse(url)))

                                val tracks = ArrayList<MediaTrack>()

                                val size = prefs.getString("subtitle_style_size", "1f")!!.toFloat()
                                val textTrackStyle = TextTrackStyle()
                                textTrackStyle.fontScale = size

                                this.uptobox?.getSubTitles(file) { utbSubtitles ->
                                    var i: Long = 1
                                    utbSubtitles?.forEach { utbSubTitle ->
                                        val subtitle =
                                            MediaTrack.Builder(i, MediaTrack.TYPE_TEXT)
                                                .setName(utbSubTitle.label)
                                                .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
                                                .setContentId(utbSubTitle.link)
                                                .build()
                                        tracks.add(subtitle)

                                        i++
                                    }

                                    val mediaInfo: MediaInfo = MediaInfo.Builder(uri.toString())
                                        .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                                        .setContentType("videos/mp4")
                                        .setMetadata(mediaMetadata)
                                        .setStreamDuration(-1L)
                                        .setMediaTracks(tracks)
                                        .setTextTrackStyle(textTrackStyle)
                                        .build()

                                    this.runOnUiThread {
                                        casty?.player?.loadMediaAndPlay(mediaInfo, true, position)
                                    }
                                }
                            }
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

            this.videoView?.setOnPreparedListener { mediaPlayer ->

                mediaPlayer.setOnVideoSizeChangedListener { mp, width, height ->
                    this.videoProgressBar?.visibility = View.GONE
                    this.mediaController?.setAnchorView(videoView)

                    if (resources.configuration.orientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                        this.setFullScreen(true)
                    } else {
                        this.setFullScreen(false)
                    }
                }
            }

            this.mediaController = MediaController(this)
            this.mediaController?.setAnchorView(videoView)
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
        if (menu != null && casty != null) {
            casty?.addMediaRouteMenuItem(menu)
        }
        menuInflater.inflate(R.menu.browse_stream, menu)
        return true
    }

    override fun onStop() {
        if (this.videoView?.isPlaying!!) {
            this.videoView!!.pause()
        }
        super.onStop()
    }

    override fun onBackPressed() {
        if (this.mediaController!!.isShown) {
            this.mediaController?.hide()
        } else {
            super.onBackPressed()
        }
    }
}
