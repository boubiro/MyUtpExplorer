package info.matpif.myutbexplorer.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "utb_attributes")
data class UtbAttributes(
    @PrimaryKey(autoGenerate = true) var uid: Int,
    @ColumnInfo(name = "type") var type: Int?,
    @ColumnInfo(name = "code") var code: String?,
    @ColumnInfo(name = "name") var name: String?,
    @ColumnInfo(name = "is_favorite") var isFavorite: Boolean?,
    @ColumnInfo(name = "is_seen") var isSeen: Boolean?,
    @ColumnInfo(name = "utb_model") var utbModel: String?,
    @ColumnInfo(name = "time") var time: Long = 0
)