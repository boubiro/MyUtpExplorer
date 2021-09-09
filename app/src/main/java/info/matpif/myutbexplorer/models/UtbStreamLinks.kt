package info.matpif.myutbexplorer.models

class UtbStreamLinks {
    companion object {
        val VERSION_1 = "1"
        val VERSION_2 = "2"
    }

    var streamLinks: Array<UtbStreamLink>? = null
    var subtitles: Array<UtbSubTitle>? = null
    var assetId: String? = null
    var duration: Int? = null
    var host: String? = null
    var version: String? = null
}