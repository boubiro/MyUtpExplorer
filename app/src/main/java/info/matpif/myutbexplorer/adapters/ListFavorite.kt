package info.matpif.myutbexplorer.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.gson.Gson
import info.matpif.myutbexplorer.R
import info.matpif.myutbexplorer.entities.UtbAttributes
import info.matpif.myutbexplorer.models.UtbFile
import info.matpif.myutbexplorer.models.UtbFolder

class ListFavorite(items: ArrayList<Any>, ctx: Context) :
    ArrayAdapter<Any>(ctx, R.layout.dialog_item_list_favorite, items) {

    private class AttractionItemViewHolder {
        internal var name: TextView? = null
        internal var imgFolder: ImageView? = null
        internal var imgFileMovie: ImageView? = null
        internal var imgFile: ImageView? = null
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        var view = view

        val viewHolder: AttractionItemViewHolder

        if (view == null) {
            val inflater = LayoutInflater.from(context)
            view = inflater.inflate(R.layout.dialog_item_list_favorite, viewGroup, false)

            viewHolder = AttractionItemViewHolder()
            viewHolder.name = view!!.findViewById<View>(R.id.name) as TextView
            viewHolder.imgFolder = view.findViewById<View>(R.id.imgFolder) as ImageView
            viewHolder.imgFileMovie = view.findViewById<View>(R.id.imgFileMovie) as ImageView
            viewHolder.imgFile = view.findViewById<View>(R.id.imgFile) as ImageView

        } else {
            viewHolder = view.tag as AttractionItemViewHolder
        }

        val item = getItem(i) as UtbAttributes
        val utb: Any
        if (item.type == 1) {
            utb = UtbFolder()
            utb.pushData(Gson().fromJson(item.utbModel, UtbFolder.DataUtbFolder::class.java))
        } else {
            utb = UtbFile()
            utb.pushData(Gson().fromJson(item.utbModel, UtbFile.DataUtbFile::class.java))
        }

        when (utb) {
            is UtbFolder -> {
                viewHolder.name!!.text = item.name
                viewHolder.imgFile?.visibility = View.GONE
                viewHolder.imgFileMovie?.visibility = View.GONE
                viewHolder.imgFolder?.visibility = View.VISIBLE
                viewHolder.imgFolder?.setImageResource(R.drawable.folder)
            }
            is UtbFile -> {
                viewHolder.name!!.text = item.name

                if (utb.transcoded != "null") {
                    viewHolder.imgFile?.visibility = View.GONE
                    viewHolder.imgFileMovie?.visibility = View.VISIBLE

                    viewHolder.imgFileMovie?.setImageResource(R.drawable.file_movie)
                } else {
                    viewHolder.imgFile?.visibility = View.VISIBLE
                    viewHolder.imgFileMovie?.visibility = View.GONE
                    viewHolder.imgFile?.setImageResource(R.drawable.file)
                }
                viewHolder.imgFolder?.visibility = View.GONE
            }
            else -> viewHolder.name!!.text = ""
        }

        view.tag = viewHolder

        return view
    }
}