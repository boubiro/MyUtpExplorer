package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class MoveCopyFiles(
    @SerializedName("token")
    var token: String?,
    @SerializedName("file_codes")
    var file_codes: String?,
    @SerializedName("destination_fld_id")
    var destination_fld_id: String?,
    @SerializedName("action")
    var action: String?
) : Data