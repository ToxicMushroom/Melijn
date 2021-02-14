package me.melijn.melijnbot.internals.web.weebsh

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.future.await
import me.duncte123.weebJava.WeebApiBuilder
import me.duncte123.weebJava.models.WeebApi
import me.duncte123.weebJava.types.NSFWMode
import me.duncte123.weebJava.types.TokenType
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.toLCC

class WeebApi(val httpClient: HttpClient, val settings: Settings) {

    private val weebshApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo("Melijn", "latest", settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()


    suspend fun getUrl(type: String, nsfw: Boolean = false, apiOrder: Array<Type> = emptyArray()): String {
        for (api in apiOrder + Type.values().filterNot { apiOrder.contains(it) }) {
            val url = when (api) {
                Type.WEEBSH -> getWeebshUrl(type, nsfw)
                Type.XIG, Type.XIG_NSFW -> getXigUrl(type, nsfw)
                Type.MIKI -> getMikiUrl(type, nsfw)
            }
            if (url != null) return url
        }

        return MISSING_IMAGE_URL
    }

    private suspend fun getMikiUrl(type: String, nsfw: Boolean): String? {
        if (nsfw || !mikiList.contains(type)) return null

        return httpClient.getOrNull<MikiResponse>("https://api.miki.bot/images/random?tags=$type")?.url
    }

    private suspend fun getXigUrl(type: String, nsfw: Boolean): String? {
        if (nsfw && !nsfwXigList.contains(type)) return null
        if (!nsfw && !xigList.contains(type)) return null

        val endpoint = when (nsfw) {
            true -> "images/nsfw"
            false -> "images"
        }

        return httpClient.getOrNull<XigResponse>("https://shiro.gg/api/$endpoint/$type")?.url
    }

    private suspend fun getWeebshUrl(type: String, nsfw: Boolean): String? {
        val nsfwMode = when (nsfw) {
            true -> NSFWMode.ONLY_NSFW
            false -> NSFWMode.DISALLOW_NSFW
        }
        return try {
            weebshApi.getRandomImage(type, nsfwMode).submit().await()?.url
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        private val mikiList = listOf(
            "confused",
            "lewd",
            "pout",
            "stare",
            "cry",
            "smug",
            "highfive",
            "hug",
            "kiss",
            "lick",
            "pet",
            "poke",
            "slap",
            "shrug",
            "hug",
            "grope",
            "adore"
        )

        private val xigList = listOf(
            "avatars",
            "blush",
            "cry",
            "hug",
            "kiss",
            "neko",
            "nom",
            "pat",
            "pout",
            "slap",
            "smug",
            "wallpapers"
        )

        private val nsfwXigList = listOf(
            "bondage",
            "hentai",
            "thighs"
        )
    }

    enum class Type {
        WEEBSH,
        XIG,
        XIG_NSFW,
        MIKI
    }

    data class XigResponse(
        val code: Int,
        val url: String
    )

    data class MikiResponse(
        val id: Long,
        val tags: List<String>,
        val url: String
    )
}

suspend inline fun <reified T> HttpClient.getOrNull(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {},
    logger: org.slf4j.Logger? = null,
): T? {
    return try {
        get {
            url.takeFrom(urlString)
            block()
        }
    } catch (t: Throwable) {
        logger?.error("Error getting result from: $urlString", t)
        null
    }
}
