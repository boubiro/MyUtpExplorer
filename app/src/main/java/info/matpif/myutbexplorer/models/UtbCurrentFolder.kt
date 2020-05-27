package info.matpif.myutbexplorer.models

class UtbCurrentFolder {

    var currentFolder: UtbFolder? = null
    var folders: Array<UtbFolder>? = null
    var files: Array<UtbFile>? = null
    var pageCount: Int = 1
    var totalFileCount: Int = 0
    var totalFileSize: Int = 0
}