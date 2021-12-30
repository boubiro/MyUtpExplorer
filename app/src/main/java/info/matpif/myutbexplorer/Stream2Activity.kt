package info.matpif.myutbexplorer

import android.app.AlertDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.MappingTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionUtil
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TrackSelectionDialogBuilder
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoSize
import com.google.gson.Gson
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.entities.databases.AppDatabase
import info.matpif.myutbexplorer.helpers.MyHelper
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbSubTitle
import info.matpif.myutbexplorer.services.Uptobox
import org.json.JSONArray
import pl.droidsonroids.casty.Casty
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask


class Stream2Activity : AppCompatActivity() {

    private var trackSelector: DefaultTrackSelector? = null
    private var playerView: PlayerView? = null
    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentWindow = 0
    private var playbackPosition: Long = 0
    private var uptobox: Uptobox? = null
    private var currentFile: UtbFile? = null
    private var currentFileAttribute: UtbAttributes? = null
    private var currentSubtitles: Array<UtbSubTitle>? = null
    private var subtitleButton: ImageButton? = null
    private var languagesButton: ImageButton? = null
    private var qualityButton: Button? = null
    private var autoQuality: Boolean = true
    private var trackDialog: Dialog? = null
    private var listLanguage: ArrayList<String> = ArrayList<String>()
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
        this.languagesButton = controllerExoView?.findViewById(R.id.exo_languages)
        this.qualityButton = controllerExoView?.findViewById(R.id.exo_quality)

        this.qualityButton?.setOnClickListener {
            if (trackDialog == null) {
                initPopupQuality()
            }
            trackDialog?.show()
        }

        this.languagesButton?.setOnClickListener {
            if (this.listLanguage.size > 0) {
                val labels: Array<String>? = Array(this.listLanguage.size) { "" }
                var i = 0

                this.listLanguage.forEach { language ->
                    labels?.set(
                        i,
                        language
                    )
                    i++
                }

                val builder = AlertDialog.Builder(this)
                builder.setTitle("Choose")
                    .setItems(
                        labels
                    ) { dialog, which ->
                        this.player?.trackSelectionParameters =
                            this.player?.trackSelectionParameters?.buildUpon()
                                ?.setPreferredAudioLanguage(
                                    this.listLanguage[which]
                                )!!.build()

                        this.hideSystemUi()
                    }
                    .setCancelable(false)
                builder.create().show()
            }
        }

//        this.subtitleButton?.setOnClickListener {
//            if (this.currentSubtitles != null) {
//                val labels: Array<String>? = Array(this.currentSubtitles!!.size + 1) { "" }
//                var i = 0
//                labels?.set(
//                    i,
//                    "No subtitles"
//                )
//                i++
//
//                this.currentSubtitles!!.forEach { utbSubtitle ->
//                    labels?.set(
//                        i,
//                        "${utbSubtitle.label}"
//                    )
//                    i++
//                }
//
//                val builder = AlertDialog.Builder(this)
//                builder.setTitle("Choose")
//                    .setItems(
//                        labels
//                    ) { dialog, which ->
//                        this.player?.trackSelectionParameters =
//                            this.player?.trackSelectionParameters?.buildUpon()
//                                ?.setPreferredTextLanguage(
//                                    "fre"
//                                )!!.build()
//                        this.playerView?.subtitleView?.visibility = View.VISIBLE
//                    }
//                    .setCancelable(false)
//                builder.create().show()
//            }
//        }


        val myHelper = MyHelper(this)

        if (!myHelper.isTV()) {
            // Casty
        }
    }

    private fun initializePlayer() {
        this.trackSelector = DefaultTrackSelector(this)

        this.player = ExoPlayer.Builder(this).setTrackSelector(this.trackSelector!!).build()
        this.player!!.addListener(object : Player.Listener {

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                this@Stream2Activity.listLanguage = ArrayList<String>()
                if (this@Stream2Activity.listLanguage.size == 0) {
                    for (i in 0 until this@Stream2Activity.player!!.currentTracksInfo.trackGroupInfos.size) {
                        val format =
                            this@Stream2Activity.player!!.currentTracksInfo.trackGroupInfos[i].trackGroup.getFormat(
                                0
                            ).sampleMimeType
                        val lang =
                            this@Stream2Activity.player!!.currentTracksInfo.trackGroupInfos[i].trackGroup.getFormat(
                                0
                            ).language
                        val id =
                            this@Stream2Activity.player!!.currentTracksInfo.trackGroupInfos[i].trackGroup.getFormat(
                                0
                            ).id

                        if (format!!.contains("audio") && id != null && lang != null) {
                            this@Stream2Activity.listLanguage.add(lang)
                        }
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                var videoTextSize = videoSize.height.toString()
                if (this@Stream2Activity.autoQuality) {
                    videoTextSize = "(auto) $videoTextSize"
                }
                videoTextSize += "p"
                this@Stream2Activity.qualityButton!!.text = videoTextSize
            }
        })

        this.playerView?.player = player

        val url: String? = intent.getStringExtra("url")
        val uri: Uri = Uri.parse(url)
        val mediaSource = buildMediaSource(uri, this.currentSubtitles!!)

        this.player?.playWhenReady = playWhenReady
        this.player?.seekTo(currentWindow, playbackPosition)
        this.player?.setMediaSource(mediaSource)

        this.player?.prepare()

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

    private fun initPopupQuality() {
        val mappedTrackInfo = this.trackSelector?.currentMappedTrackInfo
        var videoRenderer: Int? = null

        if (mappedTrackInfo == null) return else this.qualityButton?.visibility = View.VISIBLE

        for (i in 0 until mappedTrackInfo.rendererCount) {
            if (isVideoRenderer(mappedTrackInfo, i)) {
                videoRenderer = i
            }
        }

        if (videoRenderer == null) {
            this.qualityButton?.visibility = View.GONE
            return
        }

        val trackSelectionDialogBuilder = TrackSelectionDialogBuilder(
            this,
            "Quality",
            this.trackSelector!!.currentMappedTrackInfo!!,
            videoRenderer,
            TrackSelectionDialogBuilder.DialogCallback { isDisabled, overrides ->
                this@Stream2Activity.autoQuality = overrides.isEmpty()
                trackSelector!!.parameters = TrackSelectionUtil.updateParametersWithOverride(
                    this@Stream2Activity.trackSelector!!.parameters,
                    videoRenderer,
                    this@Stream2Activity.trackSelector!!.currentMappedTrackInfo!!.getTrackGroups(
                        videoRenderer
                    ),
                    isDisabled,
                    if (overrides.isEmpty()) null else overrides[0]
                )
            }
        )
        trackSelectionDialogBuilder.setTrackNameProvider { f ->
            (f.height.toString() + "p")
        }

        trackDialog = trackSelectionDialogBuilder.build()
        trackDialog!!.setOnDismissListener {
            this@Stream2Activity.hideSystemUi()
        }
    }

    private fun isVideoRenderer(
        mappedTrackInfo: MappingTrackSelector.MappedTrackInfo,
        rendererIndex: Int
    ): Boolean {
        val trackGroupArray = mappedTrackInfo.getTrackGroups(rendererIndex)
        if (trackGroupArray.length == 0) {
            return false
        }
        val trackType = mappedTrackInfo.getRendererType(rendererIndex)
        return C.TRACK_TYPE_VIDEO == trackType
    }

    private fun buildMediaSource(uri: Uri, utbSubTitle: Array<UtbSubTitle>): MediaSource {

        val listSubtitle = ArrayList<MediaItem.SubtitleConfiguration>()

        utbSubTitle.forEach { it ->
            val subtitle: MediaItem.SubtitleConfiguration =
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(it.src))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(it.srcLang)
                    .setLabel(it.label)
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                    .build()
            listSubtitle.add(subtitle)
        }

        val dataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
        return when (@C.ContentType val type = Util.inferContentType(uri)) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.Builder().setUri(uri).setSubtitleConfigurations(listSubtitle).build()
            )
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.Builder().setUri(uri).setSubtitleConfigurations(listSubtitle).build()
            )
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.Builder().setUri(uri).setSubtitleConfigurations(listSubtitle).build()
            )
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
                MediaItem.Builder().setUri(uri).setSubtitleConfigurations(listSubtitle).build()
            )
            else -> throw IllegalStateException("Unsupported type: $type")
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
