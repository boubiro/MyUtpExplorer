package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class DeleteFolder(
    @SerializedName("token")
    var token: String?,
    @SerializedName("fld_id")
    var fld_id: String?
) : Data