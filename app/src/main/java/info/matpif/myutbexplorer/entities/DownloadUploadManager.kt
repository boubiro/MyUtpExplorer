package info.matpif.myutbexplorer.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "download_upload_manager")
data class DownloadUploadManager(
    @PrimaryKey(autoGenerate = true) var uid: Int,
    @ColumnInfo(name = "id_request") var idRequest: Long,
    @ColumnInfo(name = "file_name") var fileName: String?,
    @ColumnInfo(name = "progress") var progress: Int?,
    @ColumnInfo(name = "download") var download: Boolean?,
    @ColumnInfo(name = "create_at") var createAt: String?
)