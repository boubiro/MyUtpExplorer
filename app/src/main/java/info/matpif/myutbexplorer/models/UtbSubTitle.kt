package info.matpif.myutbexplorer.models

class UtbSubTitle {

    data class DataUtbSubTitle(
        var type: String? = null,
        var link: String? = null,
        var src: String? = null,
        var label: String? = null,
        var srcLang: String? = null
    )

    var type: String? = null
    var link: String? = null
    var src: String? = null
    var label: String? = null
    var srcLang: String? = null

    fun getData(): UtbSubTitle.DataUtbSubTitle {
        return UtbSubTitle.DataUtbSubTitle(
            this.type,
            this.link,
            this.src,
            this.label,
            this.srcLang
        )
    }

    fun pushData(data: UtbSubTitle.DataUtbSubTitle): UtbSubTitle {
        this.type = data.type
        this.link = data.link
        this.src = data.src
        this.label = data.label
        this.srcLang = data.srcLang

        return this
    }
}