package info.matpif.myutbexplorer.helpers

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.content.res.Configuration

class MyHelper(_context: Context) {

    private val context: Context = _context;

    fun isTV(): Boolean {
        val uiModeManager = this.context.getSystemService(UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }
}