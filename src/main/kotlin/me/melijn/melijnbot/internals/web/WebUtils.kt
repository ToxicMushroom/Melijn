package me.melijn.melijnbot.internals.web

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object WebUtils {

    val logger: Logger = LoggerFactory.getLogger(WebUtils.javaClass)

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

        return try {
            httpClient.get<String>(fullUrlWithParams) {
                headers {
                    for ((key, value) in headers) {
                        this.append(key, value)
                    }
                }
            }
        } catch (e: Throwable) {
            logger.warn("something went wrong when requesting to: $url")
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