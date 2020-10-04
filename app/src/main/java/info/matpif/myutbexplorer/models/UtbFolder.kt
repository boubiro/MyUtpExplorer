package info.matpif.myutbexplorer.models

class UtbFolder : UtbModel(), Comparable<UtbFolder> {

    data class DataUtbFolder(
        var fileCount: Int? = null,
        var fld_id: String? = null,
        var fld_name: String? = null,
        var fld_parent_id: String? = null,
        var hash: String? = null,
        var name: String? = null,
        var totalFileSize: String? = null,
        var fld_descr: String? = null,
        var fld_password: String? = null,
        var fullPath: String? = null
    )

    var fileCount: Int? = null
    var fld_id: String? = null
    var fld_name: String? = null
    var fld_parent_id: String? = null
    var hash: String? = null
    var name: String? = null
    var totalFileSize: String? = null
    var fld_descr: String? = null
    var fld_password: String? = null
    var fullPath: String? = null


    fun getData(): DataUtbFolder {
        return DataUtbFolder(
            this.fileCount,
            this.fld_id,
            this.fld_name,
            this.fld_parent_id,
            this.hash,
            this.name,
            this.totalFileSize,
            this.fld_descr,
            this.fld_password,
            this.fullPath
        )
    }

    fun pushData(data: DataUtbFolder): UtbFolder {
        this.fileCount = data.fileCount
        this.fld_id = data.fld_id
        this.fld_name = data.fld_name
        this.fld_parent_id = data.fld_parent_id
        this.hash = data.hash
        this.name = data.name
        this.totalFileSize = data.totalFileSize
        this.fld_descr = data.fld_descr
        this.fld_password = data.fld_password
        this.fullPath = data.fullPath
        return this
    }

    override fun toString(): String {
        return "" + this.name
    }

    override fun compareTo(other: UtbFolder): Int {
        return when {
            other.fld_name == null -> {
                1
            }
            this.fld_name == null -> {
                -1
            }
            else -> {
                this.fld_name!!.compareTo(other.fld_name!!)
            }
        }
    }
}