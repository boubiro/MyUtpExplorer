package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class DeleteFiles(
    @SerializedName("token")
    var token: String?,
    @SerializedName("file_codes")
    var file_codes: String?
) : Data