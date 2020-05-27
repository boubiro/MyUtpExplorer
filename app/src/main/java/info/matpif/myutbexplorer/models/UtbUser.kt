package info.matpif.myutbexplorer.models

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

class UtbUser {

    var login: String? = null
    var email: String? = null
    var point: Int? = null
    var premium_expire: Date? = null
    var securityLock: Int? = null
    var directDownload: Int? = null
    var sslDownload: Int? = null
    var token: String? = null
    var premium: Int? = null

    @SuppressLint("SimpleDateFormat")
    fun setPremiumExpire(date: String) {
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        this.premium_expire = parser.parse(date)
    }
}