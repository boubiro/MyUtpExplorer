package info.matpif.myutbexplorer

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
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.gson.Gson
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.entities.databases.AppDatabase
import info.matpif.myutbexplorer.helpers.MyHelper
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbSubTitle
import info.matpif.myutbexplorer.services.Uptobox
import org.json.JSONArray
import pl.droidsonroids.casty.Casty
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


class Stream2Activity : AppCompatActivity() {

    private var playerView: PlayerView? = null
    private var player: SimpleExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var uptobox: Uptobox? = null
    private var currentFile: UtbFile? = null
    private var currentFileAttribute: UtbAttributes? = null
    private var currentSubtitles: Array<UtbSubTitle>? = null
    private var subtitleButton: ImageButton? = null
    private var controllerExoView: PlayerControlView? = null
    private var casty: Casty? = null
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream2)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null) {
            this.uptobox = Uptobox.getInstance(token, this)
        }
        this.timer = Timer()

        this.currentFile = UtbFile().pushData(
            Gson().fromJson(
                intent.getStringExtra("file"),
                UtbFile.DataUtbFile::class.java
            )
        )

        Thread(Runnable {
            this@Stream2Activity.currentFileAttribute =
                AppDatabase.getDatabase(this@Stream2Activity).utbAttributeDao()
                    .findByCode(this@Stream2Activity.currentFile?.file_code!!)

            if (this@Stream2Activity.currentFileAttribute == null) {
                this@Stream2Activity.currentFileAttribute = UtbAttributes(
                    0,
                    1,
                    this@Stream2Activity.currentFile?.file_code,
                    this@Stream2Activity.currentFile?.file_name,
                    false,
                    false,
                    Gson().toJson(this@Stream2Activity.currentFile?.getData())
                )
            } else {
                val time = this@Stream2Activity.currentFileAttribute?.time!!
                if (time > 0) {
                    // >= 1 hour
                    val titleTime = java.lang.String.format(
                        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(time),
                        TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(
                            TimeUnit.MILLISECONDS.toHours(
                                time
                            )
                        ),
                        TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(time)
                        )
                    )
                    this@Stream2Activity.runOnUiThread {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle(
                            getString(R.string.title_resume).replace(
                                "X",
                                titleTime,
                                false
                            )
                        )
                        builder.setPositiveButton(
                            R.string.dialog_ok
                        ) { dialog, id ->
                            this@Stream2Activity.player?.seekTo(this@Stream2Activity.currentFileAttribute?.time!!)
                            hideSystemUi()
                        }
                            .setNegativeButton(
                                R.string.dialog_cancel
                            ) { dialog, id ->
                                hideSystemUi()
                            }

                        builder.create().show()
                    }
                }
            }
        }).start()

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

        this.timer?.schedule(timerTask {
            this@Stream2Activity.runOnUiThread {
                if (this@Stream2Activity.currentFileAttribute != null && this@Stream2Activity.player != null && this@Stream2Activity.player?.currentPosition!! > 0) {
                    this@Stream2Activity.currentFileAttribute?.time =
                        this@Stream2Activity.player?.currentPosition!!
                    Thread(Runnable {
                        AppDatabase.getDatabase(this@Stream2Activity).utbAttributeDao()
                            .insert(this@Stream2Activity.currentFileAttribute!!)
                    }).start()
                }
            }
        }, 5000, 5000)
    }

    private fun selectSubtitle(utbSubtitle: UtbSubTitle?) {
        hideSystemUi()
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
            runOnUiThread {
                this.playerView?.subtitleView?.visibility = View.GONE
            }
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {

        val userAgent = "myutbexplorer-exoplayer"
        return if (uri.lastPathSegment!!.contains("mp3") || uri.lastPathSegment!!.contains("mp4")) {
            val dataSourceFactory: DataSource.Factory =
                DefaultDataSourceFactory(this, userAgent)
            ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(uri)
        } else {
            HlsMediaSource.Factory(DefaultHttpDataSourceFactory(userAgent))
                .createMediaSource(uri)
        }
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

        if (this.timer != null) {
            this.timer?.cancel()
            this.timer?.purge()
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
