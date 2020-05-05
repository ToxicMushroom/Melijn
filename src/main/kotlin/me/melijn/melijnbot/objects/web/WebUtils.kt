package me.melijn.melijnbot.objects.web

import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await

object WebUtils {
    val jsonMedia = "application/json".toMediaType()
    val textMedia = "text/html".toMediaType()

    suspend fun getResponseFromUrl(
        httpClient: OkHttpClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String>
    ): String? {
        val fullUrlWithParams = url + params.entries.joinToString("&", "?",
            transform = { entry ->
                entry.key + "=" + entry.value
            }
        )
        val requestBuilder = Request.Builder()
            .url(fullUrlWithParams)
            .get()

        for ((key, value) in headers) {
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        val response = httpClient.newCall(request).await()
        val responseBody = response.body
        if (responseBody == null) {
            response.close()
            return null
        }

        return try {
            val responseString = responseBody.string()
            response.close()
            responseString
        } catch (t: Throwable) {
            response.close()
            null
        }
    }

    suspend fun getJsonFromUrl(
        httpClient: OkHttpClient,
        url: String, params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): DataObject? {
        val response = getResponseFromUrl(httpClient, url, params, headers) ?: return null

        return DataObject.fromJson(response)
    }

    suspend fun getJsonAFromUrl(
        httpClient: OkHttpClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): DataArray? {
        val response = getResponseFromUrl(httpClient, url, params, headers) ?: return null

        return DataArray.fromJson(response)
    }
}