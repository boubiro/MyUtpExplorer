package info.matpif.myutbexplorer.services

import android.content.Context
import info.matpif.myutbexplorer.models.*
import info.matpif.myutbexplorer.services.data.*
import org.json.JSONObject
import org.jsoup.Jsoup


class Uptobox(_token: String, _context: Context) {

    companion object {
        private const val SCHEMA = "https"
        private const val HOST = "uptobox.com"
        private const val HOST_STREAM = "uptostream.com"
        private const val URL_BASE = "$SCHEMA://$HOST"
        private const val URL_BASE_STREAM = "$SCHEMA://$HOST_STREAM"
        private const val URL_API_PATH = "api"
        private const val TAG = "UTB"
    }

    private var token: String = _token
    private var request: Request =
        Request(
            SCHEMA, HOST, URL_API_PATH
        )

    fun getUser(listener: (UtbUser) -> Unit) {
        request.getRequest(
            "/user/me", listOf("token" to this.token)
        ) { utbResponse ->
            val data = utbResponse.data
            val utbUser = UtbUser()
            utbUser.login = data?.getString("login")
            utbUser.email = data?.getString("email")
            utbUser.point = data?.getInt("point")
            utbUser.premium_expire = data?.getString("premium_expire")
            utbUser.securityLock = data?.getInt("securityLock")
            utbUser.directDownload = data?.getInt("directDownload")
            utbUser.sslDownload = data?.getInt("sslDownload")

            listener.invoke(utbUser)
        }
    }

    fun getFiles(path: String, listener: (UtbCurrentFolder) -> Unit) {
        request.getRequest(
            "/user/files",
            listOf(
                "token" to this.token,
                "path" to path,
                "limit" to "100",
                "orderBy" to "file_name",
                "dir" to "asc"
            )
        ) { utbResponse ->

            val currentFolderResponse = JSONObject(utbResponse.data?.getString("currentFolder"))
            val foldersResponse = utbResponse.data?.getJSONArray("folders")
            val filesResponse = utbResponse.data?.getJSONArray("files")

            val utbCurrentFolder = UtbCurrentFolder()
            val utbFolder = UtbFolder()
            utbFolder.fileCount = currentFolderResponse.getInt("fileCount")
            utbFolder.fld_id = currentFolderResponse.getString("fld_id")
            utbFolder.hash = currentFolderResponse.getString("hash")
            utbFolder.totalFileSize = currentFolderResponse.getString("totalFileSize")

            if (!utbFolder.fld_id.equals("0")) {
                utbFolder.fld_name = currentFolderResponse.getString("fld_name")
                utbFolder.fld_parent_id = currentFolderResponse.getString("fld_parent_id")
                utbFolder.name = currentFolderResponse.getString("name")
            }

            utbCurrentFolder.currentFolder = utbFolder

            if (foldersResponse != null) {
                utbCurrentFolder.folders = Array(foldersResponse.length()) { UtbFolder() }
                for (i in 0 until foldersResponse.length()) {
                    val item = foldersResponse.getJSONObject(i)

                    val utbF = UtbFolder()
                    utbF.fld_id = item.getString("fld_id")
                    utbF.fld_descr = item.getString("fld_descr")
                    utbF.fld_name = item.getString("fld_name")
                    utbF.fld_password = item.getString("fld_password")
                    utbF.fullPath = item.getString("fullPath")
                    utbF.name = item.getString("name")
                    utbF.hash = item.getString("hash")
                    utbCurrentFolder.folders!![i] = utbF
                }
            }

            if (filesResponse != null) {
                utbCurrentFolder.files = Array(filesResponse.length()) { UtbFile() }
                for (i in 0 until filesResponse.length()) {
                    val item = filesResponse.getJSONObject(i)

                    val utbF = UtbFile()
                    utbF.file_code = item.getString("file_code")
                    utbF.file_created = item.getString("file_created")
                    utbF.file_descr = item.getString("file_descr")
                    utbF.file_downloads = item.getInt("file_downloads")
                    utbF.file_last_download = item.getString("file_last_download")
                    utbF.file_name = item.getString("file_name")
                    utbF.file_password = item.getString("file_password")
                    utbF.file_public = (item.getInt("file_public") == 1)
                    utbF.file_size = item.getInt("file_size")
                    utbF.last_stream = item.getString("last_stream")
                    utbF.nb_stream = item.getInt("nb_stream")
                    utbF.transcoded = item.getString("transcoded")
                    utbCurrentFolder.files!![i] = utbF
                }
            }

            listener.invoke(utbCurrentFolder)
        }
    }

    fun updateFileInformations(
        file: UtbFile,
        newName: String,
        description: String,
        password: String,
        public: Boolean,
        listener: (UtbFile, Boolean) -> Unit
    ) {
        var publicStr = "0"
        if (public) {
            publicStr = "1"
        }
        request.patchRequest(
            "/user/files",
            UpdateFileInformation(
                this.token,
                file.file_code,
                newName,
                description,
                password,
                publicStr
            )
        ) {
            if (it.data?.get("updated") == 1) {
                file.file_name = newName
                file.file_descr = description
                file.file_password = password
                file.file_public = public
            }

            listener.invoke(file, (it.data?.get("updated") == 1))
        }
    }

    fun moveFilesFolders(
        model: Array<UtbModel>,
        destination: UtbFolder,
        listener: (Boolean) -> Unit
    ) {
        val files: Array<UtbFile>
        var i = 0
        model.forEach {
            if (it is UtbFolder) {
                this.moveFolder(it, destination) { }
            } else if (it is UtbFile) {
                i++
            }
        }

        files = Array(i) { UtbFile() }
        i = 0
        model.forEach {
            if (it is UtbFile) {
                files[i] = it
            }
        }

        this.moveFiles(files, destination) {

        }

        listener.invoke(true)
    }

    fun moveFiles(
        files: Array<UtbFile>,
        destination: UtbFolder,
        listener: (Boolean) -> Unit
    ) {
        var file_codes: String = ""
        files.forEach {
            file_codes += it.file_code + ","
        }
        file_codes = file_codes.dropLast(1)
        request.patchRequest(
            "/user/files",
            MoveCopyFiles(
                this.token,
                file_codes,
                destination.fld_id,
                "move"
            )
        ) {
            listener.invoke((files.count() == it.data?.getInt("updated")))
        }
    }

    fun moveFolder(
        folder: UtbFolder,
        destination: UtbFolder,
        listener: () -> Unit
    ) {
        request.patchRequest(
            "/user/files",
            MoveFolder(
                this.token,
                folder.fld_id,
                destination.fld_id,
                "move"
            )
        ) {
            listener.invoke()
        }
    }

    fun updateFolderInformations(
        folder: UtbFolder,
        newName: String,
        listener: (UtbFolder) -> Unit
    ) {
        request.patchRequest(
            "/user/files",
            UpdateFolderInformation(
                this.token,
                folder.fld_id,
                newName
            )
        ) {
            folder.name = newName
            listener.invoke(folder)
        }
    }

    fun getListAvailableFile(file_code: String, listener: (UtbStreamLinks) -> Unit) {
        request.getRequest(
            "/streaming", listOf("token" to this.token, "file_code" to file_code)
        ) { utbResponse ->
            val utbStreamLinks = UtbStreamLinks()
            var count: Int = 0
            val streamLinks = JSONObject(utbResponse.data?.getString("streamLinks"))
            var keys = streamLinks.keys()

            while (keys.hasNext()) {
                val key = keys.next()
                val languages = JSONObject(streamLinks.getString(key))

                val keysLanguage = languages.keys()
                while (keysLanguage.hasNext()) {
                    val keyLanguage = keysLanguage.next()
                    val url = languages.getString(keyLanguage)
                    count++
                }
            }

            this.getDirectDownloadLink(file_code) {

                if (it != null) {
                    count++
                }
                utbStreamLinks.streamLinks = Array(count) { UtbStreamLink() }

                keys = streamLinks.keys()
                count = 0

                if (it != null) {

                    val utbStreamLink = UtbStreamLink()
                    utbStreamLink.language = "link"
                    utbStreamLink.resolution = "direct"
                    utbStreamLink.url = it

                    utbStreamLinks.streamLinks!![count] = utbStreamLink

                    count++
                }

                while (keys.hasNext()) {
                    val key = keys.next()
                    val languages = JSONObject(streamLinks.getString(key))

                    val keysLanguage = languages.keys()
                    while (keysLanguage.hasNext()) {
                        val keyLanguage = keysLanguage.next()
                        val url = languages.getString(keyLanguage)
                        val utbStreamLink = UtbStreamLink()
                        utbStreamLink.language = keyLanguage
                        utbStreamLink.resolution = key
                        utbStreamLink.url = url

                        utbStreamLinks.streamLinks!![count] = utbStreamLink
                        count++
                    }
                }

                listener.invoke(utbStreamLinks)
            }
        }
    }

    fun getDirectDownloadLink(file_code: String, listener: (String?) -> Unit) {
        request.getRequest(
            "/link",
            listOf("token" to this.token, "file_code" to file_code)
        ) {
            listener.invoke(it.data?.getString("dlLink"))
        }
    }

    fun createFolder(path: String?, folderName: String, listener: (Boolean) -> Unit) {

        var rootPath = path
        if (path == null || path == "") {
            rootPath = "//"
        }

        request.putRequest(
            "/user/files",
            CreateFolder(
                this.token,
                rootPath,
                folderName
            )
        ) {
            listener.invoke(true)
        }
    }

    fun deleteFiles(files: Array<UtbFile>, listener: (Boolean) -> Unit) {

        var fileCodes: String = ""

        files.forEach {
            fileCodes += it.file_code + ","
        }

        fileCodes = fileCodes.dropLast(1)

        request.deleteRequest(
            "/user/files",
            DeleteFiles(
                this.token,
                fileCodes
            )
        ) {
            listener.invoke(true)
        }
    }

    fun deleteFolder(folder: UtbFolder, listener: (Boolean) -> Unit) {

        request.deleteRequest(
            "/user/files",
            DeleteFolder(
                this.token,
                folder.fld_id
            )
        ) {
            listener.invoke(true)
        }
    }

    fun generatePublicFolderLink(folder: UtbFolder): String {
        return "$URL_BASE/user_public?hash=" + folder.hash + "&folder=" + folder.fld_id
    }

    fun generatePublicFileLink(file: UtbFile): String {
        return if (file.transcoded != "null") {
            "$URL_BASE_STREAM/" + file.file_code
        } else {
            "$URL_BASE/" + file.file_code
        }
    }

    fun getThumbUrl(videoFile: UtbFile?, listener: (String?) -> Unit) {
        if (videoFile != null) {
            Thread(Runnable {
                val doc = Jsoup.connect("$URL_BASE_STREAM/" + videoFile.file_code).get()
                val url =
                    "https[a-z:\\/0-9\\.]*\\.uptostream\\.com\\/thumbnail\\/[a-z:\\/0-9\\.]*_preview\\.jpg".toRegex()
                        .find(doc.html())?.value
                listener.invoke(url)
            }).start()
        }
    }
}