package info.matpif.myutbexplorer.adapters

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.ArrayMap
import com.github.ivbaranov.mfb.MaterialFavoriteButton
import info.matpif.myutbexplorer.R
import info.matpif.myutbexplorer.entities.DownloadUploadManager
import info.matpif.myutbexplorer.listeners.MyFavoriteListener
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbFolder
import info.matpif.myutbexplorer.models.UtbModel
import info.matpif.myutbexplorer.views.MyMaterialFavoriteButton

class ListItemDownloadUploadManager(
    private var items: ArrayList<DownloadUploadManager>,
    private var ctx: Context
) :
    ArrayAdapter<DownloadUploadManager>(ctx, R.layout.list_item_download_upload_manager, items) {

    private class AttractionItemViewHolder {
        internal var name: TextView? = null
        internal var createAt: TextView? = null
        internal var ivDownload: ImageView? = null
        internal var ivUpload: ImageView? = null
    }

    private var selectedItem: ArrayMap<Int, Boolean> = ArrayMap()

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        var view = view

        val viewHolder: AttractionItemViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.list_item_download_upload_manager, viewGroup, false)

            viewHolder = AttractionItemViewHolder()
            viewHolder.name = view!!.findViewById<View>(R.id.name) as TextView
            viewHolder.createAt = view.findViewById<View>(R.id.tvCreateAt) as TextView
            viewHolder.ivDownload = view.findViewById<View>(R.id.ivDownload) as ImageView
            viewHolder.ivUpload = view.findViewById<View>(R.id.ivUpload) as ImageView

        } else {
            viewHolder = view.tag as AttractionItemViewHolder
        }

        val item = getItem(i)
        when (item?.progress) {
            DownloadManager.STATUS_SUCCESSFUL -> {
                viewHolder.name?.text = "${item.fileName} - Successful"
            }
            DownloadManager.STATUS_FAILED -> {
                viewHolder.name?.text = "${item.fileName} - Failed"
            }
            DownloadManager.STATUS_PAUSED -> {
                viewHolder.name?.text = "${item.fileName} - Paused"
            }
            DownloadManager.STATUS_PENDING -> {
                viewHolder.name?.text = "${item.fileName} - Pending"
            }
            DownloadManager.STATUS_RUNNING -> {
                viewHolder.name?.text = "${item.fileName} - Running"
            }
            else -> {
                viewHolder.name?.text = "${item?.fileName} - Cancel"
            }
        }

        viewHolder.createAt?.text = item?.createAt

        if (item?.download == true) {
            viewHolder.ivDownload?.visibility = View.VISIBLE
            viewHolder.ivUpload?.visibility = View.GONE
        } else {
            viewHolder.ivDownload?.visibility = View.GONE
            viewHolder.ivUpload?.visibility = View.VISIBLE
        }

        view.tag = viewHolder

        return view
    }

    fun removeAllSelectedItem() {
        this.selectedItem = ArrayMap()
    }

    fun getSelectedItem(): ArrayMap<Int, Boolean> {
        return this.selectedItem
    }

    fun getItems(): ArrayList<DownloadUploadManager> {
        return this.items
    }
}