package info.matpif.myutbexplorer.models

import info.matpif.myutbexplorer.entities.UtbAttributes

open class UtbModel {
    private var utbAttributes: UtbAttributes? = null

    fun setUtbAttributes(utbAttributes: UtbAttributes?) {
        this.utbAttributes = utbAttributes
    }

    fun getUtbAttributes(): UtbAttributes? {
        return this.utbAttributes
    }
}