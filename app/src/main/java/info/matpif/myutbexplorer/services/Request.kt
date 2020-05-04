package info.matpif.myutbexplorer.services

import com.google.gson.Gson
import info.matpif.myutbexplorer.models.UtbResponse
import info.matpif.myutbexplorer.services.data.Data
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.lang.Exception

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

    fun setOnRequestListener(requestListener: RequestListener) {
        this.requestListener = requestListener;
    }
}