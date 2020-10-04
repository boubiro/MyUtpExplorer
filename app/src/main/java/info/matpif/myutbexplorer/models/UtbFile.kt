package info.matpif.myutbexplorer.models

class UtbFile : UtbModel(), Comparable<UtbFile> {

    data class DataUtbFile(
        var file_code: String? = null,
        var file_created: String? = null,
        var file_descr: String? = null,
        var file_downloads: Int? = null,
        var file_last_download: String? = null,
        var file_name: String? = null,
        var file_password: String? = null,
        var file_public: Boolean? = null,
        var file_size: Int? = null,
        var last_stream: String? = null,
        var nb_stream: Int? = null,
        var transcoded: String? = null,
        var available_uts: Boolean? = null
    )

    var file_code: String? = null
    var file_created: String? = null
    var file_descr: String? = null
    var file_downloads: Int? = null
    var file_last_download: String? = null
    var file_name: String? = null
    var file_password: String? = null
    var file_public: Boolean? = null
    var file_size: Int? = null
    var last_stream: String? = null
    var nb_stream: Int? = null
    var transcoded: String? = null
    var available_uts: Boolean? = null

    fun getData(): DataUtbFile {
        return DataUtbFile(
            this.file_code,
            this.file_created,
            this.file_descr,
            this.file_downloads,
            this.file_last_download,
            this.file_name,
            this.file_password,
            this.file_public,
            this.file_size,
            this.last_stream,
            this.nb_stream,
            this.transcoded,
            this.available_uts
        )
    }

    fun pushData(data: DataUtbFile): UtbFile {
        this.file_code = data.file_code
        this.file_created = data.file_created
        this.file_descr = data.file_descr
        this.file_downloads = data.file_downloads
        this.file_last_download = data.file_last_download
        this.file_name = data.file_name
        this.file_password = data.file_password
        this.file_public = data.file_public
        this.file_size = data.file_size
        this.last_stream = data.last_stream
        this.nb_stream = data.nb_stream
        this.transcoded = data.transcoded
        this.available_uts = data.available_uts

        return this
    }

    fun isPicture(): Boolean {
        return this.file_name!!.endsWith(".jpg", true)
                || this.file_name!!.endsWith(".jpeg", true)
                || this.file_name!!.endsWith(".png", true)
                || this.file_name!!.endsWith(".bmp", true)
                || this.file_name!!.endsWith(".gif", true)
    }

    override fun toString(): String {
        return "" + this.file_name
    }

    override fun compareTo(other: UtbFile): Int {
        return when {
            other.file_name == null -> {
                1
            }
            this.file_name == null -> {
                -1
            }
            else -> {
                this.file_name!!.compareTo(other.file_name!!)
            }
        }
    }
}