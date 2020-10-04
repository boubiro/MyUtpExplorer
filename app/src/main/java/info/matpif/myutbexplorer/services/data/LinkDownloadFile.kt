package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class LinkDownloadFile(
    @SerializedName("token")
    var token: String?,
    @SerializedName("file_code")
    var file_code: String?
) : Data