package info.matpif.myutbexplorer.controllers

import android.view.View
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity


class MediaController(_context: AppCompatActivity) : MediaController(_context) {

    private var context: AppCompatActivity = _context
    override fun hide() {
        super.hide()
        this.context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }
}