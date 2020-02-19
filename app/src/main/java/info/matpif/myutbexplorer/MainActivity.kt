package info.matpif.myutbexplorer

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ShareCompat
import androidx.preference.PreferenceManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.common.images.WebImage
import com.google.gson.Gson
import info.matpif.myutbexplorer.adapters.ListFavorite
import info.matpif.myutbexplorer.adapters.ListItemFoldersFiles
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.entities.databases.AppDatabase
import info.matpif.myutbexplorer.listeners.MyFavoriteListener
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbFolder
import info.matpif.myutbexplorer.models.UtbModel
import info.matpif.myutbexplorer.services.Uptobox
import pl.droidsonroids.casty.Casty
import pl.droidsonroids.casty.MediaData


class MainActivity : AppCompatActivity() {

    private var listFoldersFiles: ListView? = null
    private var uptobox: Uptobox? = null
    private var progressBar: ProgressBar? = null
    private var currentFolder: UtbFolder? = null
    private var adapter: ListItemFoldersFiles? = null
    private var breadcrumbTv: TextView? = null
    private var casty: Casty? = null
    private var cutElement: ConstraintLayout? = null
    private var btnPaste: ImageButton? = null
    private var cancelPaste: ImageButton? = null
    private var tvCutElement: TextView? = null
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.casty = Casty.create(this).withMiniController()

        this.listFoldersFiles = findViewById(R.id.list_folders_files)
        this.progressBar = findViewById(R.id.progressBar)
        this.breadcrumbTv = findViewById(R.id.breadcrumb)
        this.cutElement = findViewById(R.id.cutElement)
        this.btnPaste = findViewById(R.id.paste)
        this.cancelPaste = findViewById(R.id.cancel_paste)
        this.tvCutElement = findViewById(R.id.tvCutElement)

        val pullToRefresh: SwipeRefreshLayout = findViewById(R.id.pullToRefresh)

        pullToRefresh.setOnRefreshListener {
            this.reloadCurrentPath()
            pullToRefresh.isRefreshing = false
        }

        this.btnPaste?.setOnClickListener {
            if (this.adapter?.getSelectedItemToPaste() != null && this.currentFolder != null) {
                this.uptobox?.moveFilesFolders(
                    this.adapter?.getSelectedItemToPaste()!!,
                    this.currentFolder!!
                ) {
                    this.adapter?.removeAllSelectedItem()
                    this.adapter?.setSelectedItemToPaste()
                    this.invalidateOptionsMenu()
                    this.adapter?.notifyDataSetChanged()
                }
            }
        }

        this.cancelPaste?.setOnClickListener {
            this.adapter?.removeAllSelectedItem()
            this.adapter?.setSelectedItemToPaste()
            this.invalidateOptionsMenu()
            this.reloadCurrentPath()
        }

        with(this.listFoldersFiles) {
            this!!.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            setMultiChoiceModeListener(object : AbsListView.MultiChoiceModeListener {

                private var isCut: Boolean = false
                private var itemEdit: MenuItem? = null
                private var itemDelete: MenuItem? = null
                private var itemShare: MenuItem? = null

                override fun onItemCheckedStateChanged(
                    mode: ActionMode, position: Int,
                    id: Long, checked: Boolean
                ) {
                    this@MainActivity.adapter?.selectItem(position, checked)

                    if (this@MainActivity.adapter?.getSelectedItem()?.size!! > 1) {
                        this.itemEdit?.isVisible = false
                        this.itemDelete?.isVisible = false
                        this.itemShare?.isVisible = false
                    } else {
                        this.itemEdit?.isVisible = true
                        this.itemDelete?.isVisible = true
                        this.itemShare?.isVisible = true
                    }
                }

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return when (item.itemId) {
                        R.id.cut -> {
                            this@MainActivity.adapter?.setSelectedItemToPaste()
                            this@MainActivity.invalidateOptionsMenu()
                            this.isCut = true
                            mode.finish()
                            true
                        }
                        R.id.edit -> {
                            if (this@MainActivity.adapter?.getSelectedItem()?.size == 1) {
                                this@MainActivity.adapter?.getSelectedItem()?.forEach {
                                    this@MainActivity.editForm(it.key)
                                }
                            }
                            mode.finish()
                            true
                        }
                        R.id.share -> {
                            if (this@MainActivity.adapter?.getSelectedItem()?.size == 1) {
                                this@MainActivity.adapter?.getSelectedItem()?.forEach {
                                    this@MainActivity.shareForm(it.key)
                                }
                            }
                            mode.finish()
                            true
                        }
                        R.id.delete -> {
                            if (this@MainActivity.adapter?.getSelectedItem()?.size == 1) {
                                this@MainActivity.adapter?.getSelectedItem()?.forEach {
                                    this@MainActivity.deleteForm(it.key)
                                }
                            }
                            mode.finish()
                            true
                        }
                        else -> false
                    }
                }

                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    val menuInflater: MenuInflater = mode.menuInflater
                    menuInflater.inflate(R.menu.browse_edit, menu)

                    this.itemEdit = menu.findItem(R.id.edit)
                    this.itemDelete = menu.findItem(R.id.delete)
                    this.itemShare = menu.findItem(R.id.share)

                    this.isCut = false
                    this@MainActivity.adapter?.removeAllSelectedItem()
                    this@MainActivity.adapter?.setSelectedItemToPaste()

                    return true
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    if (isCut) {
                        this@MainActivity.adapter?.removeAllSelectedItem()
                    } else {
                        this@MainActivity.adapter?.removeAllSelectedItem()
                        this@MainActivity.adapter?.setSelectedItemToPaste()
                    }
                    this@MainActivity.invalidateOptionsMenu()
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                    return true
                }
            })
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")
        if (token != null && token != "") {
            this.uptobox = Uptobox(token, this)
            this.listFoldersFiles!!.onItemClickListener =
                AdapterView.OnItemClickListener { parent, view, position, id ->
                    val selectedItem = parent.getItemAtPosition(position)

                    if (selectedItem is UtbFolder) {
                        this.reload(selectedItem.fullPath)
                    } else if (selectedItem is UtbFile) {
                        if (selectedItem.transcoded != "null") {
                            this.showAvailableFiles(selectedItem.file_code, selectedItem)
                        }
                    }
                }


            this.reload("//")

            val data: Uri? = intent?.data

            if (data != null) {
                val segments = data.path?.split("/")
                val key: String = segments?.get(segments.size - 1) ?: ""
                val externalFile = UtbFile()
                externalFile.file_code = key

                this.showAvailableFiles(key, externalFile)
            }
        } else {
            this.progressBar!!.visibility = View.INVISIBLE
            Toast.makeText(this, "Set your Token in settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun reloadCurrentPath() {
        if (this.currentFolder != null) {
            val path: String? =
                if (this.currentFolder!!.fld_id != "0") {
                    this.currentFolder!!.fld_name
                } else {
                    "//"
                }
            this.reload(path)
        }
    }

    private fun reload(path: String?) {

        this.runOnUiThread {
            this.progressBar!!.visibility = View.VISIBLE
        }

        if (path != null) {
            this.uptobox!!.getFiles(path) {
                val listFolder = it.folders!!.toCollection(ArrayList())
                val listFile = it.files!!.toCollection(ArrayList())

                this.currentFolder = it.currentFolder
                this.runOnUiThread {
                    if (this.currentFolder!!.fld_id != "0") {
                        this.breadcrumbTv?.text = this.currentFolder?.fld_name
                    } else {
                        this.breadcrumbTv?.text = "//"
                    }
                }

                val list = listFolder + listFile

                if (this.adapter == null) {
                    this.runOnUiThread {
                        this.adapter = ListItemFoldersFiles(list.toCollection(ArrayList()), this)
                        this.adapter?.setOnMyFavoriteListener(object : MyFavoriteListener {
                            override fun onChange(position: Int, favorite: Boolean) {
                                val item: UtbModel =
                                    this@MainActivity.adapter?.getItem(position) as UtbModel

                                if (favorite) {
                                    val builder = AlertDialog.Builder(this@MainActivity)
                                    builder.setTitle(R.string.create_favorite)
                                    val mView = this@MainActivity.layoutInflater.inflate(
                                        R.layout.dialog_edit_favorite,
                                        null
                                    )

                                    val etName: EditText = mView.findViewById(R.id.etName)

                                    var utbAttributes = item.getUtbAttributes()
                                    if (utbAttributes != null) {
                                        etName.setText(utbAttributes.name)
                                    } else {
                                        etName.setText(item.toString())
                                    }

                                    builder.setView(mView)
                                    builder.setPositiveButton(
                                        R.string.dialog_ok
                                    ) { dialog, id ->

                                        if (item is UtbFolder) {
                                            if (utbAttributes == null) {
                                                utbAttributes = UtbAttributes(
                                                    0,
                                                    1,
                                                    item.fld_id,
                                                    etName.text.toString(),
                                                    true,
                                                    false,
                                                    Gson().toJson(item.getData())
                                                )
                                            } else {
                                                utbAttributes!!.isFavorite = favorite
                                                utbAttributes!!.utbModel =
                                                    Gson().toJson(item.getData())

                                            }
                                            Thread(Runnable {
                                                AppDatabase.getDatabase(this@MainActivity)
                                                    .utbAttributeDao()
                                                    .insert(utbAttributes!!)
                                            }).start()

                                            item.setUtbAttributes(utbAttributes)
                                        } else if (item is UtbFile) {
                                            if (utbAttributes == null) {
                                                utbAttributes = UtbAttributes(
                                                    0,
                                                    2,
                                                    item.file_code,
                                                    etName.text.toString(),
                                                    true,
                                                    false,
                                                    Gson().toJson(item.getData())
                                                )
                                            } else {
                                                utbAttributes!!.isFavorite = favorite
                                                utbAttributes!!.utbModel =
                                                    Gson().toJson(item.getData())

                                            }
                                            Thread(Runnable {
                                                AppDatabase.getDatabase(this@MainActivity)
                                                    .utbAttributeDao()
                                                    .insert(utbAttributes!!)
                                            }).start()
                                        }
                                    }

                                    builder.create().show()
                                } else {
                                    val utbAttributes = item.getUtbAttributes()
                                    if (utbAttributes != null) {
                                        utbAttributes.isFavorite = false

                                        if (item is UtbFolder) {
                                            utbAttributes.utbModel = Gson().toJson(item.getData())

                                        } else if (item is UtbFile) {
                                            utbAttributes.utbModel = Gson().toJson(item.getData())
                                        }

                                        Thread(Runnable {
                                            AppDatabase.getDatabase(this@MainActivity)
                                                .utbAttributeDao()
                                                .insert(utbAttributes)
                                        }).start()

                                    }
                                }
                            }
                        })
                        this.listFoldersFiles!!.adapter = adapter
                    }
                } else {
                    this.runOnUiThread {
                        this.adapter!!.clear()
                        this.adapter!!.addAll(list.toCollection(ArrayList()))
                        this.adapter!!.notifyDataSetChanged()
                    }
                }

                this.runOnUiThread {
                    this.progressBar!!.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onBackPressed() {

        if (this.currentFolder != null && this.currentFolder!!.fld_id != "0") {

            val position = this.currentFolder!!.fld_name!!.lastIndexOf("/")

            var path: String = this.currentFolder!!.fld_name!!.substring(0, position)

            if (path == "/") {
                path = "//"
            }

            this.reload(path)
        } else {
            super.onBackPressed()
        }
    }

    private fun editForm(position: Int) {
        val item = this.adapter?.getItem(position)
        if (item is UtbFolder) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.edit_folder)
            val mView = this.layoutInflater.inflate(R.layout.dialog_edit_folder, null)

            val etName: EditText = mView.findViewById(R.id.etName)
            etName.setText(item.name)

            builder.setView(mView)
            builder.setPositiveButton(
                R.string.dialog_ok
            ) { dialog, id ->
                this.uptobox?.updateFolderInformations(
                    item,
                    etName.text.toString()
                ) { folder: UtbFolder ->
                    this.reloadCurrentPath()
                }
            }
                .setNegativeButton(
                    R.string.dialog_cancel
                ) { dialog, id ->

                }

            builder.create().show()


        } else if (item is UtbFile) {

            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.edit_file)
            val mView = this.layoutInflater.inflate(R.layout.dialog_edit_file, null)

            val etName: EditText = mView.findViewById(R.id.etName)
            val etDescr: EditText = mView.findViewById(R.id.etDescr)
            val etPassword: EditText = mView.findViewById(R.id.etPassword)
            val swPublic: Switch = mView.findViewById(R.id.swPublic)
            val ivPreview: ImageView = mView.findViewById(R.id.ivPreview)
            if (item.transcoded != "null") {
                this.uptobox?.getThumbUrl(item) {
                    this.runOnUiThread {
                        if (it != null && it != "") {
                            ivPreview.visibility = View.VISIBLE
                            Glide.with(mView)
                                .load(it)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(ivPreview)
                        } else {
                            ivPreview.visibility = View.GONE
                        }
                    }
                }
            } else {
                ivPreview.visibility = View.GONE
            }

            builder.setView(mView)
            builder.setPositiveButton(
                R.string.dialog_ok
            ) { dialog, id ->
                this.uptobox?.updateFileInformations(
                    item,
                    etName.text.toString(),
                    etDescr.text.toString(),
                    etPassword.text.toString(),
                    swPublic.isChecked
                ) { file: UtbFile, updated: Boolean ->
                    if (!updated) {
                        this.runOnUiThread {
                            Toast.makeText(
                                this,
                                "An error has occurred",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    } else {
                        this.reloadCurrentPath()
                    }
                }
            }
                .setNegativeButton(
                    R.string.dialog_cancel
                ) { dialog, id ->

                }

            etName.setText(item.file_name)
            etDescr.setText(item.file_descr)
            etPassword.setText(item.file_password)
            item.file_public?.let { swPublic.isChecked = it }
            builder.create().show()
        }
    }

    private fun shareForm(position: Int) {
        val item = this.adapter?.getItem(position)
        var url = ""
        if (item is UtbFolder) {
            url = this.uptobox!!.generatePublicFolderLink(item)
        } else if (item is UtbFile) {
            url = this.uptobox!!.generatePublicFileLink(item)
        }
        ShareCompat.IntentBuilder.from(this)
            .setType("text/plain")
            .setChooserTitle("Share URL")
            .setText(url)
            .startChooser()
    }

    private fun deleteForm(position: Int) {
        val item = this.adapter?.getItem(position)
        if (item is UtbFolder) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete")
                .setMessage("Do you want to Delete")
                .setIcon(R.drawable.ic_action_delete_light)
                .setPositiveButton(
                    R.string.dialog_ok
                ) { dialog, id ->
                    this.uptobox?.deleteFolder(item) {
                        if (it) {
                            this.reloadCurrentPath()
                        }
                    }
                }
                .setNegativeButton(
                    R.string.dialog_cancel
                ) { dialog, id ->
                }
            builder.create().show()
        } else if (item is UtbFile) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Delete")
                .setMessage("Do you want to Delete")
                .setIcon(R.drawable.ic_action_delete_light)
                .setPositiveButton(
                    R.string.dialog_ok
                ) { dialog, id ->
                    val listFiles: Array<UtbFile> = Array(1) { UtbFile() }
                    listFiles[0] = item

                    this.uptobox?.deleteFiles(listFiles) {
                        if (it) {
                            this.reloadCurrentPath()
                        }
                    }
                }
                .setNegativeButton(
                    R.string.dialog_cancel
                ) { dialog, id ->
                }
            builder.create().show()
        }
    }

    private fun showAvailableFiles(fileCode: String?, file: UtbFile) {
        if (fileCode != null) {
            this.uptobox?.getListAvailableFile(fileCode) { currentStreamLinks ->

                val nbVal: Int? = currentStreamLinks.streamLinks?.size

                if (nbVal != null) {
                    val labels: Array<String>? = Array(nbVal) { "" }
                    var i = 0
                    val streamLinks = currentStreamLinks.streamLinks
                    streamLinks?.forEach { utbStreamLink ->
                        labels?.set(
                            i,
                            "${utbStreamLink.resolution} - ${utbStreamLink.language}"
                        )
                        i++
                    }

                    this.createDialogChoose(labels) { index ->
                        val streamLink = streamLinks?.get(index)
                        if (casty == null || !casty?.isConnected!!) {
                            val intent = Intent(this, StreamActivity::class.java)
                            intent.putExtra("url", streamLink?.url)
                            intent.putExtra("file", Gson().toJson(file.getData()))
                            startActivity(intent)
                        } else {
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

                                    val mediaInfo: MediaInfo = MediaInfo.Builder(streamLink?.url)
                                        .setStreamType(MediaData.STREAM_TYPE_BUFFERED)
                                        .setContentType("videos/mp4")
                                        .setMetadata(mediaMetadata)
                                        .build()

                                    casty?.player?.loadMediaAndPlay(mediaInfo, true, 0)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createDialogChoose(list: Array<String>?, listener: (Int) -> Unit) {
        if (list != null) {
            this.runOnUiThread {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Choose")
                    .setItems(
                        list
                    ) { dialog, which ->
                        listener.invoke(which)
                    }
                builder.create().show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        if (menu != null) {
            casty?.addMediaRouteMenuItem(menu)
        }
        menuInflater.inflate(R.menu.browse, menu)

        if (this.adapter?.getSelectedItemToPaste() != null) {
            this.cutElement?.visibility =
                if (this.adapter?.getSelectedItemToPaste()?.count()!! > 0) {
                    this.tvCutElement?.text = getString(R.string.element_selected).replace(
                        "X",
                        this.adapter?.getSelectedItemToPaste()?.count()!!.toString(),
                        false
                    )
                    View.VISIBLE
                } else View.GONE
        } else {
            this.cutElement?.visibility = View.GONE
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.favorite -> {
                val db = AppDatabase.getDatabase(this)

                Thread(Runnable {
                    val adapterDialog =
                        ListFavorite(
                            db.utbAttributeDao().getFavorite().toCollection(ArrayList()),
                            this
                        )

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.menu_favorite)

                    val rowList: View = layoutInflater.inflate(R.layout.dialog_list_favorite, null)
                    val listView: ListView = rowList.findViewById(R.id.lv_favorite)
                    listView.adapter = adapterDialog

                    listView.onItemClickListener =
                        AdapterView.OnItemClickListener { parent, view, position, id ->
                            val selectedItem: UtbAttributes =
                                parent.getItemAtPosition(position) as UtbAttributes

                            val utb: Any
                            if (selectedItem.type == 1) {
                                utb = UtbFolder()
                                utb.pushData(
                                    Gson().fromJson(
                                        selectedItem.utbModel,
                                        UtbFolder.DataUtbFolder::class.java
                                    )
                                )
                            } else {
                                utb = UtbFile()
                                utb.pushData(
                                    Gson().fromJson(
                                        selectedItem.utbModel,
                                        UtbFile.DataUtbFile::class.java
                                    )
                                )
                            }

                            if (utb is UtbFolder) {
                                this.reload(utb.fullPath)
                            } else if (utb is UtbFile) {
                                if (utb.transcoded != "null") {
                                    this.showAvailableFiles(utb.file_code, utb)
                                }
                            }

                            this.alertDialog?.dismiss()
                        }

                    adapterDialog.notifyDataSetChanged()

                    builder.setView(rowList)
                    builder.setNegativeButton(R.string.dialog_cancel) { dialog, id ->

                    }

                    this.runOnUiThread {
                        this.alertDialog = builder.create()
                        this.alertDialog?.show()
                    }
                }).start()
                true
            }
            R.id.createFolder -> {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.create_folder)
                val mView = this.layoutInflater.inflate(R.layout.dialog_edit_folder, null)

                val etName: EditText = mView.findViewById(R.id.etName)

                builder.setView(mView)
                builder.setPositiveButton(
                    R.string.dialog_ok
                ) { dialog, id ->
                    this.uptobox?.createFolder(
                        this.currentFolder?.fld_name,
                        etName.text.toString()
                    ) {
                        this.reloadCurrentPath()
                    }
                }
                    .setNegativeButton(
                        R.string.dialog_cancel
                    ) { dialog, id ->

                    }

                builder.create().show()
                true
            }
            R.id.reload -> {
                this.reloadCurrentPath()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
