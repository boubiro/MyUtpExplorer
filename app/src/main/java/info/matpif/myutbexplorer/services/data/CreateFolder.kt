package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class CreateFolder(
    @SerializedName("token")
    var token: String?,
    @SerializedName("path")
    var path: String?,
    @SerializedName("name")
    var name: String?
) : Data