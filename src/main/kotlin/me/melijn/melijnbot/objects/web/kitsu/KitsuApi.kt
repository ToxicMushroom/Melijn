package me.melijn.melijnbot.objects.web.kitsu

import me.melijn.melijnbot.objectMapper
import me.melijn.melijnbot.objects.web.WebUtils
import okhttp3.OkHttpClient

class KitsuApi(val httpClient: OkHttpClient) {

    companion object {
        const val kitsuApiBase = "https://kitsu.io/api/edge"
    }

//    fun searchSeries(query: String): KitsuSeries {
//        val url = friendlyUrl("$kitsuApiBase/anime?filter[text]=$query")
//
//    }

    suspend fun <T> getObjectFromUrl(
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        obj: Class<T>
    ): T? {
        val response = WebUtils.getResponseFromUrl(httpClient, url, params, headers)
        return objectMapper.readValue(response, obj)
    }
}

fun friendlyUrl(url: String): String {
    return url
        .replace(" ", "%20")
        .replace("[", "%5B")
        .replace("]", "%5D")
}