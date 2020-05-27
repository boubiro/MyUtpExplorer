package info.matpif.myutbexplorer

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import info.matpif.myutbexplorer.helpers.MyHelper
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbSubTitle
import info.matpif.myutbexplorer.services.Uptobox
import org.json.JSONArray
import pl.droidsonroids.casty.Casty


class Stream2Activity : AppCompatActivity() {

    private var playerView: PlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var uptobox: Uptobox? = null
    private var currentFile: UtbFile? = null
    private var currentSubtitles: Array<UtbSubTitle>? = null
    private var subtitleButton: ImageButton? = null
    private var controllerExoView: PlayerControlView? = null
    private var casty: Casty? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream2)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null) {
            this.uptobox = Uptobox(token, this)
        }

        this.currentFile = UtbFile().pushData(
            Gson().fromJson(
                intent.getStringExtra("file"),
                UtbFile.DataUtbFile::class.java
            )
        )

        val jsonSubtitle = JSONArray(intent.getStringExtra("subtitles"))

        if (jsonSubtitle.length() > 0) {
            this.currentSubtitles = Array(jsonSubtitle.length()) { UtbSubTitle() }

            for (i in 0 until jsonSubtitle.length()) {
                val item = jsonSubtitle.getString(i)
                this.currentSubtitles!![i] = UtbSubTitle().pushData(
                    Gson().fromJson(
                        item,
                        UtbSubTitle.DataUtbSubTitle::class.java
                    )
                )
            }
        }

        this.playerView = findViewById(R.id.video_view)
        this.controllerExoView = this.playerView?.findViewById(R.id.exo_controller)
        this.subtitleButton = controllerExoView?.findViewById(R.id.exo_subtitles)

        this.subtitleButton?.setOnClickListener {
            if (this.currentSubtitles != null) {
                val labels: Array<String>? = Array(this.currentSubtitles!!.size + 1) { "" }
                var i = 0
                labels?.set(
                    i,
                    "No subtitles"
                )
                i++

                this.currentSubtitles!!.forEach { utbSubtitle ->
                    labels?.set(
                        i,
                        "${utbSubtitle.label}"
                    )
                    i++
                }

                runOnUiThread {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Choose")
                        .setItems(
                            labels
                        ) { dialog, which ->
                            if (which == 0) {
                                this.selectSubtitle(null)
                            } else {
                                this.selectSubtitle(this.currentSubtitles!![which - 1])
                            }
                        }
                        .setCancelable(false)
                    builder.create().show()
                }
            }
        }


        val myHelper = MyHelper(this)

        if (!myHelper.isTV()) {
            // Casty
        }
    }

    private fun initializePlayer() {
        val trackSelector = DefaultTrackSelector(this)
        this.player = SimpleExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        this.playerView?.player = player

        val url: String? = intent.getStringExtra("url")
        val uri: Uri = Uri.parse(url)
        val mediaSource = buildMediaSource(uri)

        this.player?.playWhenReady = playWhenReady
        this.player?.seekTo(currentWindow, playbackPosition)
        this.player?.prepare(mediaSource, false, false)
    }

    private fun selectSubtitle(utbSubtitle: UtbSubTitle?) {
        hideSystemUi();
        if (utbSubtitle != null) {
            val url: String? = intent.getStringExtra("url")
            val uri: Uri = Uri.parse(url)
            var mediaSource = buildMediaSource(uri)
            val dataSourceFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, "MyUtbExplorer")
            )

            val subtitleFormat: Format = Format.createTextSampleFormat(
                utbSubtitle.label,
                MimeTypes.TEXT_VTT,
                Format.NO_VALUE,
                null
            );
            val subtitleSource = SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(utbSubtitle.link), subtitleFormat, C.TIME_UNSET);

            mediaSource = MergingMediaSource(mediaSource, subtitleSource)
            runOnUiThread {
                this.player?.prepare(mediaSource, false, false)
            }
        } else {
            this.playerView?.subtitleView?.visibility = View.GONE
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory: DataSource.Factory =
            DefaultDataSourceFactory(this, "exoplayer-codelab")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(uri)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer();
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        if (player != null) {
            playWhenReady = this.player?.playWhenReady!!
            playbackPosition = this.player?.currentPosition!!
            currentWindow = this.player?.currentWindowIndex!!
            this.player?.release()
            player = null
        }
    }

    private fun hideSystemUi() {
        this.playerView!!.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            return this.myDispatchKey(event) || this.playerView?.dispatchKeyEvent(event)!!
        }

        return true;
    }

    private fun myDispatchKey(event: KeyEvent?): Boolean {
        val keyCode = event?.keyCode

        if (keyCode == KeyEvent.KEYCODE_BACK)
            return super.dispatchKeyEvent(event)

        return false
    }
}
