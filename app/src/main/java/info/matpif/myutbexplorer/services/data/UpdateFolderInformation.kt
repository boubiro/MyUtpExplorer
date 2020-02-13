package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName


data class UpdateFolderInformation(
    @SerializedName("token")
    var token: String?,
    @SerializedName("fld_id")
    var fld_id: String?,
    @SerializedName("new_name")
    var new_name: String?
) : Data