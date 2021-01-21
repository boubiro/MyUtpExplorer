package info.matpif.myutbexplorer

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import info.matpif.myutbexplorer.adapters.ListItemDownloadUploadManager
import info.matpif.myutbexplorer.entities.DownloadUploadManager
import info.matpif.myutbexplorer.entities.databases.AppDatabase
import info.matpif.myutbexplorer.services.Uptobox
import java.io.File


class DownloadUploadManager : AppCompatActivity() {

    private var adapter: ListItemDownloadUploadManager? = null
    private var lvDownloadUpload: ListView? = null
    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            this@DownloadUploadManager.loadAdapter()
            Toast.makeText(this@DownloadUploadManager, "Download Completed", Toast.LENGTH_SHORT)
                .show()
        }
    }
    private var uptobox: Uptobox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_upload_manager)

        lvDownloadUpload = findViewById(R.id.lvDownloadUpload)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val token = prefs.getString("token", "")

        if (token != null) {
            this.uptobox = Uptobox.getInstance(token, this)
        }

        lvDownloadUpload!!.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                val selectedItem = parent.getItemAtPosition(position)
                if (selectedItem is DownloadUploadManager) {
                    if (selectedItem.progress == DownloadManager.STATUS_RUNNING
                        || selectedItem.progress == DownloadManager.STATUS_PAUSED
                        || selectedItem.progress == DownloadManager.STATUS_FAILED
                        || selectedItem.progress == DownloadManager.STATUS_PENDING
                    ) {
                        val builder = AlertDialog.Builder(this)

                        builder.setTitle("Cancel")
                        builder.setIcon(R.drawable.ic_action_upload_light)
                        builder.setMessage(R.string.cancel_title)
                        builder.setPositiveButton(
                            R.string.dialog_yes
                        ) { dialog, id ->
                            if (selectedItem.download == true) {
                                val downloadManager =
                                    getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.remove(selectedItem.idRequest)
                            } else {
                                if (selectedItem.tagUpload != null) {
                                    this.uptobox?.cancel(selectedItem.tagUpload!!)
                                    selectedItem.progress = 0
                                    Thread(Runnable {
                                        AppDatabase.getDatabase(this).downloadUploadManagerDao()
                                            .insert(selectedItem)
                                    }).start()
                                }
                            }
                        }
                        builder.setNegativeButton(
                            R.string.dialog_no
                        ) { dialog, id -> }
                        builder.create().show()
                    } else if (selectedItem.progress == DownloadManager.STATUS_SUCCESSFUL) {
                        if (selectedItem.download == true) {
                            this.openDownloadedAttachment(this, selectedItem.idRequest)
                        } else {
                            if (selectedItem.fileCode != null) {
                                runOnUiThread {
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.putExtra("fileCode", selectedItem.fileCode)
                                    startActivity(intent)
                                }
                            } else {
                                Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        this.loadAdapter()
        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun openDownloadedAttachment(
        context: Context,
        downloadId: Long
    ) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val downloadStatus = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            val downloadLocalUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
            val downloadMimeType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE))
            if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL && downloadLocalUri != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                val attachmentUri = Uri.parse(downloadLocalUri)
                val file = File(attachmentUri.getPath())

                intent.setDataAndType(Uri.parse(file.absolutePath), downloadMimeType)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            }
        }
        cursor.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.browse_download_upload_manager, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.delete -> {
                val builder = AlertDialog.Builder(this)

                builder.setTitle(R.string.delete_title)
                builder.setIcon(R.drawable.ic_action_delete_light)
                builder.setMessage(R.string.delete_message)
                builder.setPositiveButton(
                    R.string.dialog_yes
                ) { dialog, id ->
                    Thread(Runnable {
                        AppDatabase.getDatabase(this).downloadUploadManagerDao().deleteAllFinished()
                        this.loadAdapter()
                    }).start()
                }
                builder.setNegativeButton(
                    R.string.dialog_no
                ) { dialog, id -> }
                builder.create().show()

                true
            }
            else -> false
        }
    }

    private fun loadAdapter() {
        Thread(Runnable {
            val listOfDownloadManager =
                AppDatabase.getDatabase(this).downloadUploadManagerDao().getAllOrderByIdDesc()
            if (listOfDownloadManager.isNotEmpty()) {
                val downloadManager =
                    this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val q = DownloadManager.Query()
                val c: Cursor = downloadManager.query(q)

                listOfDownloadManager.forEach { item ->
                    if (item.download == true) {
                        item.progress = 0
                        if (c.moveToFirst()) {
                            do {
                                val id = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_ID))
                                val status =
                                    c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                                if (id == item.idRequest.toInt()) {
                                    item.progress = status
                                }
                            } while (c.moveToNext())
                        }
                        Thread(Runnable {
                            AppDatabase.getDatabase(this).downloadUploadManagerDao()
                                .insert(item)
                        }).start()
                    }
                }

                c.close()
            }

            runOnUiThread {
                if (this.adapter == null) {
                    this.adapter = ListItemDownloadUploadManager(
                        listOfDownloadManager.toCollection(ArrayList()), this
                    )
                    this.lvDownloadUpload?.adapter = adapter
                } else {
                    this.adapter?.clear()
                    this.adapter?.addAll(listOfDownloadManager)
                    this.adapter?.notifyDataSetChanged()
                }
            }
        }).start()
    }
}