package info.matpif.myutbexplorer.views

import android.content.Context
import android.util.AttributeSet
import com.github.ivbaranov.mfb.MaterialFavoriteButton

class MyMaterialFavoriteButton : MaterialFavoriteButton {

    var isListenerable: Boolean = true

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )
}