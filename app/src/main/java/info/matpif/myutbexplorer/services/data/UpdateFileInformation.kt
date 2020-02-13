package info.matpif.myutbexplorer.services.data

import com.google.gson.annotations.SerializedName

data class UpdateFileInformation(
    @SerializedName("token")
    var token: String?,
    @SerializedName("file_code")
    var file_code: String?,
    @SerializedName("new_name")
    var new_name: String?,
    @SerializedName("description")
    var description: String?,
    @SerializedName("password")
    var password: String?,
    @SerializedName("public")
    var public: String?
) : Data