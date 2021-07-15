package me.melijn.melijnbot.internals.web.apis

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.melijn.melijnbot.internals.utils.ImageType

val BAD_TENOR_GIF = Regex("https?://tenor\\.com/view/(?:[a-zA-Z0-9_.]+-)+(\\d+)(?:\\?.*)?")
val VERYBAD_TENOR_GIF = Regex("https?://tenor\\.com/[a-zA-Z0-9_.]+\\.gif(?:\\?.*)?")

class TenorApi(val httpClient: HttpClient, private val apiKey: String) {

    suspend fun getUrl(url: String, acceptTypes: Set<ImageType>): String? {
        val parsedTenorId = if (url.matches(VERYBAD_TENOR_GIF)) {
            val resp = httpClient.get<HttpResponse>(url)
            val redirectedUrl = resp.request.url.toString()
            if (redirectedUrl != url) {
                BAD_TENOR_GIF.find(redirectedUrl)?.groupValues?.get(1) ?: return null
            } else return null
        } else BAD_TENOR_GIF.find(url)?.groupValues?.get(1) ?: return null

        val data = try {
            httpClient.get<TenorResponse>(
                "https://g.tenor.com/v1/gifs?ids=${parsedTenorId}&key=${apiKey}&limit=1&media_filter=minimal"
            )
        } catch (t: Throwable) {
            null
        }
        val result = data?.results?.firstOrNull()
        val minimalMedia = result?.media?.getOrNull(0)
        if (result == null || minimalMedia == null || result.id != parsedTenorId) return null
        return when {
            acceptTypes.contains(ImageType.GIF) -> minimalMedia.gif.url
            acceptTypes.contains(ImageType.PNG) -> minimalMedia.gif.preview
            else -> null
        }
    }
}

data class TenorResponse(
    val results: List<MinimalTenorItem>
) {
    data class MinimalTenorItem(
        val created: Double,
        val url: String,
        val media: List<MinimalMedia>,
        val tags: List<String>,
        val itemurl: String,
        val id: String
    ) {
        data class MinimalMedia(
            val tinygif: TinyGifMedia,
            val gif: GifMedia
        )
    }

    data class TinyGifMedia(
        val url: String,
        val preview: String,
        @JsonProperty("dims") // Width and height
        val dimensions: List<Int>,
        val bytesSize: Int
    )

    data class GifMedia(
        val url: String,
        val preview: String,
        @JsonProperty("dims") // Width and height
        val dimensions: List<Int>,
        val bytesSize: Int
    )
}