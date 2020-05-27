package info.matpif.myutbexplorer.services

import com.google.gson.Gson
import info.matpif.myutbexplorer.models.UtbResponse
import info.matpif.myutbexplorer.services.data.Data
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL


class Request(
    private var schema: String,
    private var host: String,
    private var url_api_path: String
) {

    private var requestListener: RequestListener? = null

    fun getRequest(
        method: String,
        params: List<Pair<String, Any?>>,
        listener: (UtbResponse) -> Unit
    ) = Thread(Runnable {
        val client = OkHttpClient()
        val url = HttpUrl.Builder()
            .scheme(schema)
            .host(host)
            .addPathSegment(url_api_path)

        method.split("/").forEach {
            url.addPathSegment(it)
        }

        params.forEach {
            url.addQueryParameter(it.first, it.second as String?)
        }

        val request: Request = Request.Builder()
            .url(url.build())
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val jsonResult = JSONObject(response.body?.string())
                val utbResponse = UtbResponse()
                utbResponse.statusCode = jsonResult.getInt("statusCode")
                utbResponse.message = jsonResult.getString("message")
                if (utbResponse.statusCode == 0) {
                    utbResponse.data = JSONObject(jsonResult.getString("data"))
                }

                listener.invoke(utbResponse)
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()

    fun patchRequest(
        method: String,
        params: Data,
        listener: (UtbResponse) -> Unit
    ) = Thread(Runnable {

        val client = OkHttpClient()
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            Gson().toJson(params)
        )

        val url = HttpUrl.Builder()
            .scheme(schema)
            .host(host)
            .addPathSegment(url_api_path)

        method.split("/").forEach {
            url.addPathSegment(it)
        }

        val request: Request = Request.Builder()
            .url(url.build())
            .patch(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body!!.string()
                val jsonResult = JSONObject(body)
                val utbResponse = UtbResponse()
                utbResponse.statusCode = jsonResult.getInt("statusCode")
                utbResponse.message = jsonResult.getString("message")
                try {
                    utbResponse.data = JSONObject(jsonResult.getString("data"))
                } catch (ex: Exception) {
                }

                listener.invoke(utbResponse)
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()

    fun putRequest(
        method: String,
        params: Data,
        listener: (UtbResponse) -> Unit
    ) = Thread(Runnable {

        val client = OkHttpClient()
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            Gson().toJson(params)
        )

        val url = HttpUrl.Builder()
            .scheme(schema)
            .host(host)
            .addPathSegment(url_api_path)

        method.split("/").forEach {
            url.addPathSegment(it)
        }

        val request: Request = Request.Builder()
            .url(url.build())
            .put(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body!!.string()
                val jsonResult = JSONObject(body)
                val utbResponse = UtbResponse()
                utbResponse.statusCode = jsonResult.getInt("statusCode")
                utbResponse.message = jsonResult.getString("message")
                try {
                    utbResponse.data = JSONObject(jsonResult.getString("data"))
                } catch (ex: Exception) {
                }

                listener.invoke(utbResponse)
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()

    fun deleteRequest(
        method: String,
        params: Data,
        listener: (UtbResponse) -> Unit
    ) = Thread(Runnable {

        val client = OkHttpClient()
        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            Gson().toJson(params)
        )

        val url = HttpUrl.Builder()
            .scheme(schema)
            .host(host)
            .addPathSegment(url_api_path)

        method.split("/").forEach {
            url.addPathSegment(it)
        }

        val request: Request = Request.Builder()
            .url(url.build())
            .delete(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body!!.string()
                val jsonResult = JSONObject(body)
                val utbResponse = UtbResponse()
                utbResponse.statusCode = jsonResult.getInt("statusCode")
                utbResponse.message = jsonResult.getString("message")
                try {
                    utbResponse.data = JSONObject(jsonResult.getString("data"))
                } catch (ex: Exception) {
                }

                listener.invoke(utbResponse)
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()

    fun postFile(
        urlUploadFile: String,
        file: File,
        listener: (JSONObject) -> Unit
    ) = Thread(Runnable {
        val client = OkHttpClient()
        val url = URL(urlUploadFile)

        val urlRequest = HttpUrl.Builder()
            .scheme(url.protocol)
            .host(url.host)

        url.path.split("/").forEach {
            urlRequest.addPathSegment(it)
        }

        var session = ""
        url.query.split("=").forEach {
            session = it
        }

        urlRequest.addQueryParameter("sess_id", session)

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody()
            )
            .build()

        val request: Request = Request.Builder()
            .url(urlRequest.build())
            .addHeader("Accept", "*/*")
            .post(requestBody)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val jsonResult = JSONObject(response.body?.string())
                // {"files":[{"name":"ip_btob.txt","size":1565,"url":"https://uptobox.com/n1lnwbd5b4pe","deleteUrl":"https://uptobox.com/n1lnwbd5b4pe?killcode=95phfuwxrc"}]}
                listener.invoke(jsonResult)
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()

    fun setOnRequestListener(requestListener: RequestListener) {
        this.requestListener = requestListener;
    }

    fun downloadFileRequest(
        urlDownloadFile: String,
        destFile: File,
        complete: () -> Unit,
        error: (message: String?) -> Unit,
        progress: (downloaded: Long, target: Long) -> Unit
    ) = Thread(Runnable {
        val client = OkHttpClient()
        val url = URL(urlDownloadFile)

        val urlRequest = HttpUrl.Builder()
            .scheme(url.protocol)
            .host(url.host)

        url.path.split("/").forEach {
            urlRequest.addPathSegment(it)
        }

        val request: Request = Request.Builder()
            .url(urlRequest.build())
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 200) {
                    val outStream: OutputStream = FileOutputStream(destFile)
                    var inputStream: InputStream? = null
                    try {
                        inputStream = response.body!!.byteStream()
                        val buff = ByteArray(1024 * 4)
                        var downloaded: Long = 0
                        val target = response.body!!.contentLength()

                        progress.invoke(0L, target)
                        while (true) {
                            val readed: Int = inputStream.read(buff)
                            if (readed == -1) {
                                break
                            }
                            outStream.write(buff, 0, readed);

                            downloaded += readed.toLong()
                            progress.invoke(downloaded, target)
//                            if (isCancelled()) {
//                                break
//                            }
                        }
                    } catch (ignore: IOException) {
                        error.invoke(ignore.message)
                    } finally {
                        inputStream?.close()
                    }
                } else {
                    error.invoke(null)
                }
                complete.invoke()
            }
        } catch (ex: Exception) {
            if (this.requestListener != null) {
                this.requestListener!!.onError(ex.message!!)
            }
        }
    }).start()
}