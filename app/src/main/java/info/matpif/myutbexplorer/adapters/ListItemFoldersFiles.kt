package info.matpif.myutbexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.ArrayMap
import info.matpif.myutbexplorer.R
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbFolder
import info.matpif.myutbexplorer.models.UtbModel

class ListItemFoldersFiles(items: ArrayList<Any>, ctx: Context) :
    ArrayAdapter<Any>(ctx, R.layout.list_item_folders_files, items) {

    private class AttractionItemViewHolder {
        internal var name: TextView? = null
        internal var imgFolder: ImageView? = null
        internal var imgFileMovie: ImageView? = null
        internal var imgFile: ImageView? = null
    }

    private var selectedItem: ArrayMap<Int, Boolean> = ArrayMap()
    private var selectedItemToPaste: Array<UtbModel>? = null

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        var view = view

        val viewHolder: AttractionItemViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.list_item_folders_files, viewGroup, false)

            viewHolder = AttractionItemViewHolder()
            viewHolder.name = view!!.findViewById<View>(R.id.name) as TextView
            viewHolder.imgFolder = view.findViewById<View>(R.id.imgFolder) as ImageView
            viewHolder.imgFileMovie = view.findViewById<View>(R.id.imgFileMovie) as ImageView
            viewHolder.imgFile = view.findViewById<View>(R.id.imgFile) as ImageView
        } else {
            viewHolder = view.tag as AttractionItemViewHolder
        }

        when (val item = getItem(i)) {
            is UtbFolder -> {
                viewHolder.name!!.text = item.name
                viewHolder.imgFile?.visibility = View.GONE
                viewHolder.imgFileMovie?.visibility = View.GONE
                viewHolder.imgFolder?.visibility = View.VISIBLE
                if (this.selectedItem.containsKey(i) || this.containsItemToPaste(item)) {
                    viewHolder.imgFolder?.setImageResource(R.drawable.folder_black)
                } else {
                    viewHolder.imgFolder?.setImageResource(R.drawable.folder)
                }
            }
            is UtbFile -> {
                viewHolder.name!!.text = item.file_name

                if (item.transcoded != "null") {
                    viewHolder.imgFile?.visibility = View.GONE
                    viewHolder.imgFileMovie?.visibility = View.VISIBLE

                    if (this.selectedItem.containsKey(i) || this.containsItemToPaste(item)) {
                        viewHolder.imgFileMovie?.setImageResource(R.drawable.file_movie_black)
                    } else {
                        viewHolder.imgFileMovie?.setImageResource(R.drawable.file_movie)
                    }
                } else {
                    viewHolder.imgFile?.visibility = View.VISIBLE
                    viewHolder.imgFileMovie?.visibility = View.GONE

                    if (this.selectedItem.containsKey(i) || this.containsItemToPaste(item)) {
                        viewHolder.imgFile?.setImageResource(R.drawable.file_black)
                    } else {
                        viewHolder.imgFile?.setImageResource(R.drawable.file)
                    }
                }
                viewHolder.imgFolder?.visibility = View.GONE
            }
            else -> viewHolder.name!!.text = ""
        }

        view.tag = viewHolder

        return view
    }

    fun selectItem(position: Int, selected: Boolean) {
        if (selected) {
            this.selectedItem[position] = selected
        } else {
            this.selectedItem.remove(position)
        }
        this.notifyDataSetChanged()
    }

    fun setSelectedItem() {
        if (this.selectedItemToPaste != null && this.selectedItemToPaste!!.count() > 0) {
            this.selectedItem = ArrayMap()
            this.selectedItemToPaste!!.forEach {
                this.selectedItem[this.getPosition(it)] = true
            }
            this.selectedItemToPaste = null
        } else {
            this.selectedItem = ArrayMap()
        }
        this.notifyDataSetChanged()
    }

    private fun containsItemToPaste(item: UtbModel): Boolean {

        if (this.selectedItemToPaste != null) {
            this.selectedItemToPaste!!.forEach {
                if (it is UtbFile && item is UtbFile) {
                    if (it.file_code == item.file_code) return true
                } else if (it is UtbFolder && item is UtbFolder) {
                    if (it.fld_id == item.fld_id) return true
                }
            }
        }

        return false
    }

    fun removeAllSelectedItem() {
        this.selectedItem = ArrayMap()
    }

    fun getSelectedItem(): ArrayMap<Int, Boolean> {
        return this.selectedItem
    }

    fun setSelectedItemToPaste() {
        if (this.selectedItem.count() > 0) {
            this.selectedItemToPaste = Array(this.selectedItem.count()) { UtbModel() }
            var i = 0
            this.selectedItem.forEach {
                this.selectedItemToPaste!![i] = this.getItem(it.key) as UtbModel
                i++
            }
        } else {
            this.selectedItemToPaste = null
        }
    }

    fun getSelectedItemToPaste(): Array<UtbModel>? {
        return this.selectedItemToPaste
    }
}