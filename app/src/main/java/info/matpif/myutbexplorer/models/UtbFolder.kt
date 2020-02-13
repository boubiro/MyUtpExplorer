package info.matpif.myutbexplorer.models

class UtbFolder: UtbModel() {

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

    override fun toString(): String {
        return "" + this.name
    }
}