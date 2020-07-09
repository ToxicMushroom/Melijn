package me.melijn.melijnbot.internals.web

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

object WebUtils {

    suspend fun getResponseFromUrl(
        httpClient: HttpClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String>
    ): String? {
        val fullUrlWithParams = url + params.entries.joinToString("&", "?",
            transform = { entry ->
                entry.key + "=" + entry.value
            }
        )

        val response = httpClient.get<String>(fullUrlWithParams) {
            headers {
                for ((key, value) in headers) {
                    this.append(key, value)
                }
            }
        }

        return try {
            response
        } catch (t: Throwable) {
            null
        }
    }

    suspend fun getJsonFromUrl(
        httpClient: HttpClient,
        url: String, params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): DataObject? {
        val response = getResponseFromUrl(httpClient, url, params, headers) ?: return null

        return DataObject.fromJson(response)
    }

    suspend fun getJsonAFromUrl(
        httpClient: HttpClient,
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap()
    ): DataArray? {
        val response = getResponseFromUrl(httpClient, url, params, headers) ?: return null

        return DataArray.fromJson(response)
    }
}