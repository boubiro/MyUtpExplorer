package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class MoveFolder(
    @SerializedName("token")
    var token: String?,
    @SerializedName("fld_id")
    var fld_id: String?,
    @SerializedName("destination_fld_id")
    var destination_fld_id: String?,
    @SerializedName("action")
    var action: String?
) : Data